/*
 * Copyright 2025-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ct42.fnflow.fnflow_json_processors_kafka.aot;

import org.opensearch.client.RestClient;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import java.io.IOException;

/**
 * Out of the box, processAot will run indefinitely if <code></code>org.opensearch.client:spring-data-opensearch-starter</code>
 * is included. This is because RestClient bean is starting a thread during bean initialization and this thread is not closed,
 * so the bean cannot be destructed in the processAot process.
 * This class is seeking for a bean RestClient and is calling its close() method so the bean can be destructed and the process processAot ends.
 *
 * @author Claas Thiele
 */
public class RestClientCloser implements BeanFactoryInitializationAotProcessor {

    @Override
    public BeanFactoryInitializationAotContribution processAheadOfTime(ConfigurableListableBeanFactory beanFactory) {
        var restClientBeans = beanFactory.getBeansOfType(RestClient.class);
        if (!restClientBeans.isEmpty()) {
            return (ctx, code) -> restClientBeans.values().forEach(cfnBean -> {
                try {
                    cfnBean.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        return null;
    }
}
