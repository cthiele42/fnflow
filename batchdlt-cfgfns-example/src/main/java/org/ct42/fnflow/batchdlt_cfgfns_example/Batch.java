package org.ct42.fnflow.batchdlt_cfgfns_example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.ct42.fnflow.batchdlt.BatchElement;
import org.ct42.fnflow.cfgfns.ConfigurableFunction;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("batch")
public class Batch extends ConfigurableFunction<List<BatchElement>, List<BatchElement>, FailOnContentProperties> {
    @Override
    public List<BatchElement> apply(List<BatchElement> input) {
        int batchSize = input.size();
        for (BatchElement batchElement : input) {
            JsonNode treeNode = batchElement.getInput();
            String text = treeNode.get("text").asText();
            if (text.contains(properties.getFailOn())) {
                batchElement.processWithError(new IllegalStateException("Content found initiated error"));
            } else {
                ((ObjectNode) treeNode).put("text", "BLEN" + batchSize + " " + text);
                batchElement.processWithOutput(treeNode);
            }
        }
        return input;
    }
}
