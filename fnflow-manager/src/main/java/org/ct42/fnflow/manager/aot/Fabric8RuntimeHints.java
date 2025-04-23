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

package org.ct42.fnflow.manager.aot;

import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.client.impl.KubernetesClientImpl;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import org.springframework.aot.hint.*;

/**
 * @author Sajjad Safaeian
 */
public class Fabric8RuntimeHints implements RuntimeHintsRegistrar {

    private final BindingReflectionHintsRegistrar hintsRegistrar = new BindingReflectionHintsRegistrar();

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        hints.reflection()
                .registerTypes(
                        TypeReference.listOf(KubernetesClientImpl.class),
                        TypeHint.builtWith(MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS));
        var graph = new ClassGraph().acceptPackages("io.fabric8.kubernetes.api.model");
        try (var scan = graph.scan()) {
            var types = scan.getClassesImplementing(KubernetesResource.class).stream()
                    .map(ClassInfo::loadClass)
                    .toArray(Class[]::new);
            hintsRegistrar.registerReflectionHints(hints.reflection(), types);
        }

        hints.resources().registerPattern(".*\\.txt$");
        hints.resources().registerPattern("META-INF/vertx/vertx-version.txt");
    }
}