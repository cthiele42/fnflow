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

package org.ct42.fnflow.cfgfns;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.aot.AbstractAotProcessor;
import org.springframework.core.env.Environment;

import java.util.Collections;
import java.util.Map;

/**
 * Registers bean instances for all configured functions.
 *
 * @author Claas Thiele
 */
public class ConfigurableFunctionRegistrar implements BeanDefinitionRegistryPostProcessor {
    private final Map<String, Map<String, Object>> functionCfgs;

    public ConfigurableFunctionRegistrar(Environment environment) {
        functionCfgs = Binder.get(environment)
                .bind(ConfigurableFunctionConfiguration.FUNCTIONS_PREFIX, Bindable.mapOf(String.class, (Class<Map<String, Object>>)(Class<?>)Map.class))
                .orElse(Collections.emptyMap());
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        for(Map.Entry<String, Map<String, Object>> fnPrototypeCfg : functionCfgs.entrySet()) {
            for(String key: fnPrototypeCfg.getValue().keySet()) {
                if(!registry.containsBeanDefinition(key)) {
                    GenericBeanDefinition fnBeanDefinition = new GenericBeanDefinition(registry.getBeanDefinition(fnPrototypeCfg.getKey()));
                    registry.registerBeanDefinition(key, fnBeanDefinition);
                }
            }
            if(!"true".equals(System.getProperty(AbstractAotProcessor.AOT_PROCESSING))) {
                registry.removeBeanDefinition(fnPrototypeCfg.getKey());
            }
        }
    }
}
