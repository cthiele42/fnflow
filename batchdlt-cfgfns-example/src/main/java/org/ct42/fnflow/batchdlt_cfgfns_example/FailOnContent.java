package org.ct42.fnflow.batchdlt_cfgfns_example;

import com.fasterxml.jackson.databind.JsonNode;
import org.ct42.fnflow.cfgfns.ConfigurableFunction;
import org.springframework.stereotype.Component;

@Component("fail-on-content")
public class FailOnContent extends ConfigurableFunction<JsonNode, JsonNode, FailOnContentProperties> {
    @Override
    public JsonNode apply(JsonNode input) {
        if(input.get("text").asText().contains(properties.getFailOn()))
            throw new IllegalStateException("Content found initiated error");
        return input;
    }
}
