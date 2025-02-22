package org.ct42.fnflow.batchdlt;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.function.BiFunction;
import java.util.function.Function;

public class FunctionWrapper implements BiFunction<Flux<Message<JsonNode>>, Sinks.Many<Message<JsonNode>>, Flux<Message<JsonNode>>> {
    private final Function<JsonNode, JsonNode> target;

    public FunctionWrapper(Function<JsonNode, JsonNode> target) {
        this.target = target;
    }

    @Override
    public Flux<Message<JsonNode>> apply(Flux<Message<JsonNode>> messageFlux, Sinks.Many<Message<JsonNode>> error) {
        return messageFlux.map(m -> MessageBuilder
                .withPayload(target.apply(m.getPayload()))
                .copyHeaders(m.getHeaders())
                .build()).onErrorContinue((throwable, m) -> error.tryEmitNext((Message<JsonNode>)m)); //TODO add error message and stacktrace to message headers
    }
}
