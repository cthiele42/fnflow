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

package org.ct42.fnflow.cfgfnstest.replace;

import lombok.Data;
import org.ct42.fnflow.cfgfns.ConfigurableFunction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.function.context.FunctionCatalog;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;
import org.springframework.test.context.TestPropertySource;

import java.util.function.Function;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author Claas Thiele
 */
@SpringBootTest
@TestPropertySource(locations= "classpath:/replace.properties")
public class ReplaceTest {
    @Autowired
    private FunctionCatalog catalog;

    @Test
    public void testReplace() {
        Function<String, String> fn = catalog.lookup("cats-birds|dogs-cats");
        then(fn.apply("dogs and cats are not being friends")).isEqualTo("cats and birds are not being friends");
    }

    @SpringBootApplication
    @ComponentScan
    protected static class TestConfiguration {}

    @Component("replace")
    protected static class Replace extends ConfigurableFunction<String, String, ReplaceProperties> {
        @Override
        public String apply(String input) {
            return input.replace(properties.getPattern(), properties.getReplace());
        }
    }

    @Data
    protected static class ReplaceProperties {
        private String pattern;
        private String replace;
    }
}
