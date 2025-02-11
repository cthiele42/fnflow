package org.ct42.fnflow;

import org.ct42.fnflow.functions.ConfigurableFunction;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.aot.AbstractAotProcessor;
import org.springframework.core.env.Environment;

import java.util.Map;

public class ConfigurableFunctionRegistrar implements BeanDefinitionRegistryPostProcessor, ApplicationContextAware {
    public static final String FUNCTIONS_PREFIX = "functions";
    private final Map<String, Map<String, Object>> functionCfgs;
    private ApplicationContext context;

    public ConfigurableFunctionRegistrar(Environment environment) {
        functionCfgs = Binder.get(environment)
                .bind(FUNCTIONS_PREFIX, Bindable.mapOf(String.class, (Class<Map<String, Object>>)(Class<?>)Map.class))
                .get();
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        for(Map.Entry<String, Map<String, Object>> fnPrototypeCfg : functionCfgs.entrySet()) {
            ConfigurableFunction fnPrototype = context. getBean(fnPrototypeCfg.getKey(), ConfigurableFunction.class);
            Class<? extends ConfigurableFunction> fnClass = fnPrototype.getClass();

            if(!"true".equals(System.getProperty(AbstractAotProcessor.AOT_PROCESSING))) {
                registry.removeBeanDefinition(fnPrototypeCfg.getKey());
            }

            for(String key: fnPrototypeCfg.getValue().keySet()) {
                if(!registry.containsBeanDefinition(key)) {
                    GenericBeanDefinition fnBeanDefinition = new GenericBeanDefinition();
                    fnBeanDefinition.setBeanClass(fnClass);
                    fnBeanDefinition.setLazyInit(false);
                    fnBeanDefinition.setAbstract(false);
                    fnBeanDefinition.setAutowireCandidate(false);
                    fnBeanDefinition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
                    fnBeanDefinition.setScope(BeanDefinition.SCOPE_SINGLETON);

                    registry.registerBeanDefinition(key, fnBeanDefinition);
                }
            }
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
    }
}
