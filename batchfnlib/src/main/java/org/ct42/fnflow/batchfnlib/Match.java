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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.ct42.fnflow.batchdlt.BatchElement;
import org.ct42.fnflow.cfgfns.ConfigurableFunction;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.MsearchTemplateRequest;
import org.opensearch.client.opensearch.core.MsearchTemplateResponse;
import org.opensearch.client.opensearch.core.msearch.MultiSearchResponseItem;
import org.opensearch.client.opensearch.core.msearch.MultisearchHeader;
import org.opensearch.client.opensearch.core.msearch_template.RequestItem;
import org.opensearch.client.opensearch.core.msearch_template.TemplateConfig;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.annotation.RegisterReflection;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * @author Claas Thiele
 */
@Component("Match")
@RequiredArgsConstructor
@RegisterReflection(classes = JsonPointer.class, memberCategories = MemberCategory.INVOKE_PUBLIC_METHODS)
public class Match extends ConfigurableFunction<List<BatchElement>, List<BatchElement>, MatchProperties> {
    private final OpenSearchClient client;

    @Override
    public List<BatchElement> apply(List<BatchElement> input) {
        List<RequestItem> requestItems = new ArrayList<>();
        MultisearchHeader emptyHeader = new MultisearchHeader.Builder().build();
        input.forEach(batchElement -> {
            Map<String, JsonData> targetParams = new HashMap<>();
            properties.getParamsFromInput().forEach((key, value) -> targetParams.put(key, JsonData.of(batchElement.getInput().at(value))));
            properties.getLiteralParams().forEach((key, value) -> targetParams.put(key, JsonData.of(value)));
            requestItems.add(new RequestItem.Builder()
                    .header(emptyHeader)
                    .body(new TemplateConfig.Builder()
                            .id(properties.getTemplate())
                            .params(targetParams)
                            .build())
                .build());
        });
        try {
            MsearchTemplateResponse<JsonNode> response = client.msearchTemplate(
                    new MsearchTemplateRequest.Builder()
                            .searchTemplates(requestItems)
                            .index(properties.getIndex())
                            .build(),
                    JsonNode.class);
            for(int i = 0; i < response.responses().size(); i++) {
                MultiSearchResponseItem<JsonNode> r = response.responses().get(i);
                if(r.isResult()) {
                    ObjectNode result = JsonNodeFactory.instance.objectNode();
                    result.set("input", input.get(i).getInput());
                    ArrayNode matches = JsonNodeFactory.instance.arrayNode();
                    result.set("matches", matches);
                    r.result().hits().hits().forEach(hit -> {
                        ObjectNode matchResult = JsonNodeFactory.instance.objectNode();
                        matchResult.set("source", hit.source());
                        matchResult.put("id", hit.id());
                        matchResult.put("score", hit.score());
                        matches.add(matchResult);
                    });
                    input.get(i).processWithOutput(result);
                } else {
                    input.get(i).processWithError(new TemplateMatchError(r.failure().error().reason()));
                }
            }
        } catch (Exception e) {
            input.forEach(elem -> elem.processWithError(e));
        }
        return input;
    }
}
