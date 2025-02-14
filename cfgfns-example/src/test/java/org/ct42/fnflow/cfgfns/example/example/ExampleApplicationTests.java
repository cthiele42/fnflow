package org.ct42.fnflow.cfgfns.example.example;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.function.context.FunctionCatalog;

import java.util.function.Function;

import static org.assertj.core.api.BDDAssertions.then;


@SpringBootTest
class ExampleApplicationTests {
	@Autowired
	FunctionCatalog functionCatalog;

	@Test
	void composedFunctionTest() {
		Function<String, String> fn = functionCatalog.lookup("cats-birds|dogs-cats");
		String result = fn.apply("dogs and cats are not being friends");
		then(result).isEqualTo("cats and birds are not being friends");
	}

}
