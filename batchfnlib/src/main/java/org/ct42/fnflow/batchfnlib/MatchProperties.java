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

package org.ct42.fnflow.batchfnlib;

import com.fasterxml.jackson.core.JsonPointer;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Claas Thiele
 */
@Data
@Validated
public class MatchProperties {
    @Pattern(regexp = "^[^_:\"*+\\/\\|?#><A-Z ,-][^:\"*+\\/\\|?#><A-Z ,]{0,126}$")
    private String index;

    @NotEmpty
    private String template;

    Map<String, JsonPointer> paramsFromInput = new HashMap<>();
    Map<String, String> literalParams = new HashMap<>();
}
