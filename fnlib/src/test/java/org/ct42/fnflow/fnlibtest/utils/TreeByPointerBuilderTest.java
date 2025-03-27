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

package org.ct42.fnflow.fnlibtest.utils;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.ct42.fnflow.fnlib.utils.TreeByPointerBuilder;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

/**
 * @author Claas Thiele
 */
public class TreeByPointerBuilderTest {
    @Test
    void testAddToEmptyObject() {
        ObjectNode root = JsonNodeFactory.instance.objectNode();
        JsonPointer pointer = JsonPointer.compile("/a/1/b");
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        node.put("x", "value");

        TreeByPointerBuilder.buildAndSet(pointer, root, node);

        then(root.at("/a/1/b/x").asText()).isEqualTo("value");
    }

    @Test
    void testAddToExistingPath() {
        ObjectNode root = JsonNodeFactory.instance.objectNode();
        root.set("a", JsonNodeFactory.instance.objectNode());
        JsonPointer pointer = JsonPointer.compile("/a/b/c");
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        node.put("x", "value");

        TreeByPointerBuilder.buildAndSet(pointer, root, node);

        then(root.at("/a/b/c/x").asText()).isEqualTo("value");
    }

    @Test
    void testNodeTypeMismatch() {
        ObjectNode root = JsonNodeFactory.instance.objectNode();
        root.set("a", JsonNodeFactory.instance.objectNode());
        JsonPointer pointer = JsonPointer.compile("/a/1/b");
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        node.put("x", "value");

        thenThrownBy(() -> TreeByPointerBuilder.buildAndSet(pointer, root, node))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Node type mismatch");

    }
}
