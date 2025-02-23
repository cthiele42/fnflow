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

package org.ct42.fnflow.batchdlt;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ResolvableType;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Holds all functions composed together.
 * The function definition has to be provided with property <code>org.ct42.fnflow.function.definition</code>.
 * This bean has to be configured as one and only regular function for Spring Cloud Function the following way:
 * <code>spring.cloud.function.definition=fnFlowComposedFnBean</code>
 *
 * @author Claas Thiele
 */
public class ComposedFunction implements Function<Flux<Message<JsonNode>>, Tuple2<Flux<Message<JsonNode>>, Flux<Message<JsonNode>>>> {
    private final ApplicationContext ctx;

    public ComposedFunction(ApplicationContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public Tuple2<Flux<Message<JsonNode>>, Flux<Message<JsonNode>>> apply(Flux<Message<JsonNode>> messageFlux) {
        String definition = Binder.get(ctx.getEnvironment()).bind("org.ct42.fnflow.function.definition", Bindable.of(String.class)).orElse(null);
        if(definition == null || definition.isEmpty()) throw new IllegalStateException("org.ct42.fnflow.function.definition not configured");
        String[] fns = definition.split("\\|");

        Sinks.Many<Message<JsonNode>> errorSink = Sinks.many().unicast().onBackpressureBuffer();

        ResolvableType imperativeType = ResolvableType.forClassWithGenerics(Function.class, JsonNode.class, JsonNode.class);
        Set<String> imperativeBeans = Set.of(ctx.getBeanNamesForType(imperativeType));
        ResolvableType batchListType = ResolvableType.forClassWithGenerics(List.class, BatchElement.class);
        ResolvableType batchType = ResolvableType.forClassWithGenerics(Function.class, batchListType, batchListType);
        Set<String> batchBeans = Set.of(ctx.getBeanNamesForType(batchType));

        Flux<Message<JsonNode>> intermediate = messageFlux.doOnComplete(errorSink::tryEmitComplete);

        for (String fn : fns) {
            if(imperativeBeans.contains(fn)) {
                Function<JsonNode, JsonNode> fnBean = (Function<JsonNode, JsonNode>) ctx.getBean(fn, Function.class);
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
