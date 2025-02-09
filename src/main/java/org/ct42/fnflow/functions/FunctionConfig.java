package org.ct42.fnflow.functions;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
public abstract class FunctionConfig<T,R,C> {
    public ConfigurableFunction<T,R,C> function(C config) {
        ConfigurableFunction<T, R, C> fn = function();
        fn.setProperties(config);
        return fn;
    }

    @Bean
    @Scope("prototype")
    protected abstract ConfigurableFunction<T,R,C> function();
}
