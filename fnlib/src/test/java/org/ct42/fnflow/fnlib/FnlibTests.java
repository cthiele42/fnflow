package org.ct42.fnflow.fnlib;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.EnableTestBinder;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.AbstractMessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;

import java.io.IOException;
import java.util.function.Function;

import static org.assertj.core.api.BDDAssertions.then;

@SpringBootTest
class FnlibTests {
	@Autowired
	private InputDestination input;

	@Autowired
	private OutputDestination output;

	@Test
	public void testEmptyConfiguration() throws Exception{
		this.input.send(new GenericMessage<>("""
		{
			"name": "ROOT",
			"child": {
				"name": "CHILD"
			}
		}
		""".getBytes()));

		byte[] payload = output.receive().getPayload();
		ObjectMapper mapper = new ObjectMapper();
		JsonNode tree = mapper.readValue(payload, JsonNode.class);
		then(tree.get("name").asText()).isEqualTo("CHILD");
	}

	@SpringBootApplication
	@EnableTestBinder
	@ComponentScan
	public static class SampleConfiguration {
		@Bean
		public MessageConverter customMessageConverter() {
			return new MyCustomMessageConverter();
		}
	}

	@Component
	protected static class NodeFn implements Function<JsonNode, JsonNode> {
		@Override
		public JsonNode apply(JsonNode node) {
			return node.get("child");
		}
	}

	static class MyCustomMessageConverter extends AbstractMessageConverter {

		public MyCustomMessageConverter() {
			super(new MimeType("application", "json"));
		}

		@Override
		protected boolean supports(Class<?> clazz) {
			return (JsonNode.class.equals(clazz));
		}

		@Override
		protected Object convertFromInternal(Message<?> message, Class<?> targetClass, Object conversionHint) {
			Object payload = message.getPayload();
			if(payload instanceof JsonNode){
				return payload;
            } else {
				ObjectMapper mapper = new ObjectMapper();
				try {
					return mapper.readValue((byte[])payload, JsonNode.class);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}
}
