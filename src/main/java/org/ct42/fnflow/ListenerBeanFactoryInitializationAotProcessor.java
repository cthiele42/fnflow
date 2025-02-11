package org.ct42.fnflow;

import org.ct42.fnflow.functions.ConfigurableFunction;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.ResolvableType;

import java.util.Map;

public class ListenerBeanFactoryInitializationAotProcessor implements BeanFactoryInitializationAotProcessor {

    @Override
    public BeanFactoryInitializationAotContribution processAheadOfTime(ConfigurableListableBeanFactory beanFactory) {
        Map<String, ConfigurableFunction> cfnBeans = beanFactory.getBeansOfType(ConfigurableFunction.class);
        if (!cfnBeans.isEmpty()) {
            return (ctx, code) -> {
                cfnBeans.values().forEach( cfnBean -> {
                    ResolvableType fnPropertiesType = ResolvableType
                            .forClass(cfnBean.getClass())
                            .as(ConfigurableFunction.class)
                            .getGeneric(2);
                    var hints = ctx.getRuntimeHints();
                    hints.reflection().registerType(fnPropertiesType.resolve(), MemberCategory.values());
                });
            };
        }
        return null;
    }
}
