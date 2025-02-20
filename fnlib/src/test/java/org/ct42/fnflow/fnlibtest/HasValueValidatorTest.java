package org.ct42.fnflow.fnlibtest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.function.context.FunctionCatalog;
import org.springframework.cloud.stream.binder.test.EnableTestBinder;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.Environment;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(locations= "classpath:/hasvalue.properties")
public class HasValueValidatorTest {

    @Autowired
    private InputDestination input;

    @Autowired
    private OutputDestination output;

    @Test
    void haveValueValidatorTest() {
        this.input.send(new GenericMessage<>("""
		{
			"name": "ROOT",
			"child": {
				"name": "CHILD"
			}
		}
		""".getBytes()));

    }

    @SpringBootApplication
    @EnableTestBinder
    @ComponentScan
    protected static class HasValueValidatorTestApplication {}

}
