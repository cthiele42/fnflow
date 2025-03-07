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

package org.ct42.fnflow.cfgfns;

import java.util.function.Function;

/**
 * Base class to be extended by all configurable function implementations.
 *
 * @param <T> function input type
 * @param <R> function return type
 * @param <C> configuration type
 *
 * @author Claas Thiele
 */
public abstract class ConfigurableFunction<T,R,C> implements Function<T,R> {
    /**
     * External configuration will be injected here.
     */
    protected C properties;

    @Override
    public abstract R apply(T input);
}
