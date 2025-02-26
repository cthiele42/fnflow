/*
 * Copyright 2025-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ct42.fnflow.batchdlttest.multioutcompose;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.ResolvableType;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.time.Duration;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Tests the composition of BiFunction based functions, for the moment with type fixed to String
 *
 * @author Claas Thiele
 */
@SpringBootTest
public class MultiOutComposeTest {
    @Autowired
    GenericApplicationContext applicationContext;

    @Test
    public void testMultiOutCompose() {
        ComposedFunction composedFunction = new ComposedFunction("bifun|bifun2|batchfun", applicationContext);
        Tuple2<Flux<String>, Flux<String>> outs = composedFunction.apply(Flux.range(0, 10).map(s -> "T" + s));

        StepVerifier.create(outs.getT1())
                .expectNext(
                        "BLEN2: MO2: MO: T0",
                        "BLEN2: MO2: MO: T1",
                        "BLEN2: MO2: MO: T4",
                        "BLEN2: MO2: MO: T9"
                )
                .verifyComplete();
        StepVerifier.create(outs.getT2())
                .recordWith(ArrayList::new)
                .thenConsumeWhile(x -> true)
                .expectRecordedMatches(elems -> elems.size() == 6 &&
                        Arrays.compare(elems.stream().filter(s -> !s.startsWith("ERRB:")).toArray(String[]::new), new String[]{
                                "ERR: T3",
                                "ERR2: MO: T5",
                                "ERR: T7",
                                "ERR2: MO: T8",
                        }) == 0 &&
                        Arrays.compare(elems.stream().filter(s -> s.startsWith("ERRB:")).toArray(String[]::new), new String[]{
                                "ERRB: MO2: MO: T2",
                                "ERRB: MO2: MO: T6"
                        }) == 0).verifyComplete();
    }

    public static class ComposedFunction implements Function<Flux<String>, Tuple2<Flux<String>, Flux<String>>> {
        private final String definition;
        private final GenericApplicationContext ctx;

        public ComposedFunction(String definition, GenericApplicationContext ctx) {
            this.definition = definition;
            this.ctx = ctx;
        }

        @Override
        public Tuple2<Flux<String>, Flux<String>> apply(Flux<String> stringFlux) {
            Sinks.Many<String> errorSink = Sinks.many().unicast().onBackpressureBuffer();

            ResolvableType imperativeType = ResolvableType.forClassWithGenerics(Function.class, String.class, String.class);
            Set<String> imperativeBeans = Set.of(ctx.getBeanNamesForType(imperativeType));

            ResolvableType batchListType = ResolvableType.forClassWithGenerics(List.class, BatchElement.class);
            ResolvableType batchType = ResolvableType.forClassWithGenerics(Function.class, batchListType, batchListType);
            Set<String> batchBeans = Set.of(ctx.getBeanNamesForType(batchType));

            String[] fns = definition.split("\\|");

            Flux<String> intermediate = stringFlux.doOnComplete(errorSink::tryEmitComplete);

            for (String fn : fns) {
                if(imperativeBeans.contains(fn)) {
                    Function<String, String> fnBean = (Function<String, String>) ctx.getBean(fn, Function.class);
                    FunctionWrapper wrappedFn = new FunctionWrapper(fnBean);
                    intermediate = wrappedFn.apply(intermediate, errorSink);
                } else if(batchBeans.contains(fn)) {
                    Function<List<BatchElement>, List<BatchElement>> batchFnBean = (Function<List<BatchElement>, List<BatchElement>>) ctx.getBean(fn, Function.class);
                    BatchFnWrapper wrappedBatchFn = new BatchFnWrapper(batchFnBean);
                    intermediate = wrappedBatchFn.apply(intermediate, errorSink);
                } else {
                    throw new IllegalStateException("No matching bean found for name " + fn);
                }
            }
            return Tuples.of(intermediate, errorSink.asFlux());
        }
    }

    public static class FunctionWrapper implements BiFunction<Flux<String>, Sinks.Many<String>, Flux<String>> {
        private final Function<String, String> target;

        public FunctionWrapper(Function<String, String> target) {
            this.target = target;
        }

        @Override
        public Flux<String> apply(Flux<String> stringFlux, Sinks.Many<String> error) {
            return stringFlux.map(target).onErrorContinue((throwable, s) -> error.tryEmitNext(throwable.getMessage() + s));
        }
    }

    public static class BatchFnWrapper implements BiFunction<Flux<String>, Sinks.Many<String>, Flux<String>> {
        public static final int MAX_SIZE = 2;
        public static final Duration MAX_TIME = Duration.ofMillis(500);

        private final Function<List<BatchElement>, List<BatchElement>> target;

        public BatchFnWrapper(Function<List<BatchElement>, List<BatchElement>> target) {
            this.target = target;
        }

        @Override
        public Flux<String> apply(Flux<String> stringFlux, Sinks.Many<String> error) {
            return stringFlux.bufferTimeout(MAX_SIZE, MAX_TIME).flatMapSequential(b -> {
                List<BatchElement> result = target.apply(b.stream().map(BatchElement::new).toList());
                result.forEach(e -> {
                    if(e.getError() != null) error.tryEmitNext(e.getError().getMessage() + ": " + e.getInput());
                });
                return Flux.fromStream(result.stream().filter(e -> e.getOutput() != null).map(BatchElement::getOutput));
            });
        }
    }

    @SpringBootApplication
    @ComponentScan
    protected static class TestConfiguration {
    }

    @Component("bifun")
    protected final static class BiFunLogic implements Function<String, String> {
        @Override
        public String apply(String s) {
            if (s.contains("T3") || s.contains("T7")) throw new RuntimeException("ERR: ");
            return "MO: " + s;
        }
    }

    @Component("bifun2")
    protected final static class BiFunLogic2 implements Function<String, String> {
        @Override
        public String apply(String s) {
            if (s.contains("T5") || s.contains("T8")) throw new RuntimeException("ERR2: ");
            return "MO2: " + s;
        }
    }

    public static final class BatchElement {
        private final String input;
        private String output;
        private Throwable error;

        public BatchElement(String input) {
            this.input = input;
        }

        public String getInput() {
            return input;
        }

        public void processWithOutput(String output) {
            this.output = output;
        }

        public void processWithError(Throwable error) {
            this.error = error;
        }

        public String getOutput() {
            return output;
        }

        public Throwable getError() {
            return error;
        }
    }

    @Component("batchfun")
    protected final static class BatchFun implements Function<List<BatchElement>, List<BatchElement>> {
        /**
         * @param b the batch; will be not empty
         * @return the batch with processed values
         */
        @Override
        public List<BatchElement> apply(List<BatchElement> b) {
            b.forEach(e -> {
                if (e.getInput().contains("T2") || e.getInput().contains("T6")) {
                    e.processWithError(new IllegalStateException("ERRB"));
                } else {
                    e.processWithOutput("BLEN" + b.size() + ": " + e.getInput());
                }
            });
            return b;
        }
    }
}
