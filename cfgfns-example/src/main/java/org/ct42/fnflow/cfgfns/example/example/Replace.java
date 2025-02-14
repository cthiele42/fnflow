package org.ct42.fnflow.cfgfns.example.example;

import org.ct42.fnflow.cfgfns.ConfigurableFunction;
import org.springframework.stereotype.Component;

@Component
public class Replace extends ConfigurableFunction<String, String, ReplaceProperties> {
    @Override
    public String apply(String input) {
        return input.replace(properties.getPattern(), properties.getReplace());
    }
}

