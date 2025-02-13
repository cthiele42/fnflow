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

package org.ct42.fnflow;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.ct42.fnflow.cfgfns.ConfigurableFunction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.function.context.FunctionCatalog;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.test.context.TestPropertySource;

import java.util.function.Function;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author Claas Thiele
 */
@SpringBootTest
@TestPropertySource(locations= "classpath:/decorate.properties")
public class DecorateTest {
    @Autowired
    private FunctionCatalog catalog;

    @Test
    public void testDecorate() {
        Function<String, String> fn = catalog.lookup(Function.class, "mydecorate1");
        then(fn.apply("testtext1")).isEqualTo("Deco1_testtext1_tail");

        Function<String, String> fn2 = catalog.lookup(Function.class, "mydecorate2");
        then(fn2.apply("testtext2")).isEqualTo("Deco2_testtext2_tail");
    }

    @SpringBootApplication
    @ComponentScan
    protected static class TestConfiguration {}

    @Component("decorate-camel-case")
    @RequiredArgsConstructor
    protected static class DecorateCamelCase extends ConfigurableFunction<String, String, DecorateProperties> {
        private final TailTokenService tailTokenService;

        @Override
        public String apply(String input) {
            return properties.getHeadToken() + "_" +
                    input + "_" +
                    tailTokenService.getToken();
        }
    }

    @Data
    protected static class DecorateProperties {
        private String headToken;
    }

    @Service
    protected static class TailTokenService {
        public String getToken() {
            return "tail";
        }
    }

}
