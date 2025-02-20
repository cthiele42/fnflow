package org.ct42.fnflow.fnlib;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.AbstractMessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.util.MimeType;

import java.io.IOException;

@AutoConfiguration
public class MessageConverterConfiguration {

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JsonMessageConverter();
    }

    static class JsonMessageConverter extends AbstractMessageConverter {

        public JsonMessageConverter() {
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
