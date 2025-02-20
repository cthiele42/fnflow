package org.ct42.fnflow.fnlib;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import org.ct42.fnflow.cfgfns.ConfigurableFunction;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.stereotype.Component;

@AutoConfiguration
@Component("hasValue")
public class HasValueValidator extends ConfigurableFunction<JsonNode, JsonNode, HasValueValidator.HasValueProperties> {

    @Override
    public JsonNode apply(JsonNode input) {
        return null;
    }

    @Data
    protected static class HasValueProperties {
        private String elementPath;
    }
}
