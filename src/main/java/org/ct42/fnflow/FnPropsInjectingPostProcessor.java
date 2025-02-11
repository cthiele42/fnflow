package org.ct42.fnflow;

import org.ct42.fnflow.functions.ConfigurableFunction;
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

@Component
@RegisterReflectionForBinding(ConfigurableFunction.class)
public class FnPropsInjectingPostProcessor implements BeanPostProcessor {
    public static final String FUNCTIONS_PREFIX = "functions";

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

            String cfgName = FUNCTIONS_PREFIX + "." + fnCfgName + "." + beanName;
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
