package org.ct42.fnflow;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class ConfigurableFunctionRegistrarConfiguration {
    @Bean
    public static ConfigurableFunctionRegistrar configurableFunctionRegistrar(Environment environment) {
        return new ConfigurableFunctionRegistrar(environment);
    }
}
