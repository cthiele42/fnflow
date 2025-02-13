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

import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ResolvableType;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

/**
 * @author Claas Thiele
 */
@Component
@RegisterReflectionForBinding(ConfigurableFunction.class)
public class FnPropsInjectingPostProcessor implements BeanPostProcessor {
    @Autowired
    private ApplicationContext applicationContext;

    @Override
    public Object postProcessBeforeInitialization( Object bean, String beanName ) throws BeansException {
        if(bean instanceof ConfigurableFunction<?,?,?>) {
            String fnCfgName = convertCamelCaseToKebap(bean.getClass().getSimpleName());

            ResolvableType fnPropertiesType = ResolvableType
                    .forClass(bean.getClass())
                    .as(ConfigurableFunction.class)
                    .getGeneric(2);

            String cfgName = ConfigurableFunctionConfiguration.FUNCTIONS_PREFIX + "." + fnCfgName + "." + beanName;
            BindResult<?> bindResult = Binder.get(applicationContext.getEnvironment())
                    .bind(cfgName, Bindable.of(fnPropertiesType.resolve()));
            if(bindResult.isBound()) {
                Object props = bindResult.get();
                try {
                    Field propField = bean.getClass().getSuperclass().getDeclaredField("properties");
                    propField.setAccessible(true);
                    propField.set(bean, props);
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    throw new IllegalStateException("Not able to access properties field in " + bean.getClass().getSimpleName(), e);
                }
            }
        }
        return bean;
    }

    private String convertCamelCaseToKebap(String input) {
        return input
                .replaceAll("([A-Z])(?=[A-Z])", "$1-")
                .replaceAll("([a-z])([A-Z])", "$1-$2")
                .toLowerCase();
    }
}
