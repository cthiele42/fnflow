package org.ct42.fnflow.batchdlt;

import com.fasterxml.jackson.databind.JsonNode;

public class BatchElement {
    private final JsonNode input;
    private JsonNode output;
    private Throwable error;

    public BatchElement(JsonNode input) {
        this.input = input;
    }

    public JsonNode getInput() {
        return input;
    }

    public void processWithOutput(JsonNode output) {
        this.output = output;
    }

    public void processWithError(Throwable error) {
        this.error = error;
    }

    public JsonNode getOutput() {
        return output;
    }

    public Throwable getError() {
        return error;
    }
}
