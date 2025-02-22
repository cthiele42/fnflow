package org.ct42.fnflow.batchdlt;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class BatchDltConfiguration {
    @Bean
    ComposedFunction fnFlowComposedFnBean(ApplicationContext applicationContext) {
        return new ComposedFunction(applicationContext);
    }
}
