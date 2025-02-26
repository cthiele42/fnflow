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

package org.ct42.fnflow.cfgfnstest.validator;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.ct42.fnflow.cfgfns.ConfigurableFunction;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import static org.assertj.core.api.BDDAssertions.thenThrownBy;

/**
 * @author Sajjad Safaeian
 */
public class ValidatorTest {

    @Test
    public void testReplace() {
        thenThrownBy(
                () -> new SpringApplicationBuilder(TestConfiguration.class)
                .properties("cfgfns.replace-validator.cats-birds.replace=").run()
        )
        .hasRootCauseExactlyInstanceOf(BindValidationException.class);
    }

    @SpringBootApplication
    @ComponentScan
    protected static class TestConfiguration {}

    @Component("replace-validator")
    protected static class ReplaceValidator extends ConfigurableFunction<String, String, ReplaceProperties> {
        @Override
        public String apply(String input) {
            return input.replace(properties.getPattern(), properties.getReplace());
        }
    }

    @Data
    @Validated
    protected static class ReplaceProperties {
        @NotEmpty
        private String pattern;

        @NotEmpty
        private String replace;
    }
}
