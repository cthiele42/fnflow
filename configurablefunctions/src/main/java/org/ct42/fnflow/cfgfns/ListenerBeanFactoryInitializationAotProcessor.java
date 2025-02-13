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

import org.springframework.aot.hint.MemberCategory;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.ResolvableType;

/**
 * Registers runtime hints for all configurable functions config data holders.
 *
 * @author Claas Thiele
 */
public class ListenerBeanFactoryInitializationAotProcessor implements BeanFactoryInitializationAotProcessor {

    @Override
    public BeanFactoryInitializationAotContribution processAheadOfTime(ConfigurableListableBeanFactory beanFactory) {
        var cfnBeans = beanFactory.getBeansOfType(ConfigurableFunction.class);
        if (!cfnBeans.isEmpty()) {
            return (ctx, code) -> cfnBeans.values().forEach(cfnBean -> {
                ResolvableType fnPropertiesType = ResolvableType
                        .forClass(cfnBean.getClass())
                        .as(ConfigurableFunction.class)
                        .getGeneric(2);
                var hints = ctx.getRuntimeHints();
                hints.reflection().registerType(fnPropertiesType.resolve(), MemberCategory.values());
            });
        }
        return null;
    }
}
