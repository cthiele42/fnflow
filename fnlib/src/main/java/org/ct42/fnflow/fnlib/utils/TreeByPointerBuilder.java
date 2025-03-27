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

package org.ct42.fnflow.fnlib.utils;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;

/**
 * Utility class adding nodes to a node tree along JsonPointer path to set a node where the pointer is pointing to.
 *
 * @author Claas Thiele
 */
public class TreeByPointerBuilder {
    /**
     * Build non-existing nodes in target tree and set node on path built.
     *
     * @param pointer to the node to be set
     * @param targetRoot object the node should be added to
     * @param node to be added
     * @throws IllegalArgumentException on node type mismatch
     */
    public static void buildAndSet(JsonPointer pointer, ObjectNode targetRoot, JsonNode node) {
        String name = pointer.last().toString().substring(1);
        JsonPointer parent = pointer.head();
        JsonNode parentNode = parent.length() == 0 ? targetRoot : targetRoot.at(parent);

        if(parentNode.isMissingNode() || parentNode.isNull()) { //recursively build tree on pointer path
            if(StringUtils.isNumeric(name)) {
                parentNode = JsonNodeFactory.instance.arrayNode();
                int index = Integer.parseInt(name);
                for (int i = 0; i <= index; i++) {
                    ((ArrayNode)parentNode).addNull();
                }
                ((ArrayNode)parentNode).set(index, node);
            } else {
                parentNode = JsonNodeFactory.instance.objectNode();
                ((ObjectNode)parentNode).set(name, node);
            }
            TreeByPointerBuilder.buildAndSet(parent, targetRoot, parentNode);
        } else if (StringUtils.isNumeric(name) && !parentNode.isArray()) {
            throw new IllegalArgumentException("Node type mismatch");
        } else if (StringUtils.isNumeric(name) && parentNode.isArray()) {
            int index = Integer.parseInt(name);
            for (int i = parentNode.size(); i <= index; i++) {
                ((ArrayNode)parentNode).addNull();
            }
            ((ArrayNode)parentNode).set(index, node);
        } else {
            ((ObjectNode)parentNode).set(name, node);
        }
    }
}
