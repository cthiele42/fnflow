package org.ct42.fnflow.batchfnlib.script;


import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ct42.fnflow.batchdlt.BatchElement;
import org.ct42.fnflow.cfgfns.ConfigurableFunction;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.annotation.RegisterReflection;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component("ScriptRunner")
@RegisterReflection(classes = JsonPointer.class, memberCategories = MemberCategory.INVOKE_PUBLIC_METHODS)
public class ScriptRunner extends ConfigurableFunction<List<BatchElement>, List<BatchElement>, ScriptProperties> {
    @Override
    public List<BatchElement> apply(List<BatchElement> input) {
        try (Context context = Context.create()) {

            List<BatchElement> result = new ArrayList<>();

            input.forEach(batchElement -> {
                try {
                    context.getBindings("js").putMember("input", batchElement.getInput().toString());
                    Value evaluationResult = context.eval("js", properties.getScript());

                    ObjectMapper mapper = new ObjectMapper();
                    List<JsonNode> evaluatedJsons = new ArrayList<>();
                    for (int i = 0; i < evaluationResult.getArraySize(); i++) {
                        Object raw = evaluationResult.getArrayElement(i).as(Object.class);
                        String json = mapper.writeValueAsString(raw);
                        evaluatedJsons.add(mapper.readTree(json));
                    }

                    result.addAll(
                            evaluatedJsons
                                .stream()
                                .map(jsonNode -> {
                                    BatchElement element = new BatchElement(batchElement.getInput());
                                    element.processWithOutput(jsonNode);
                                    return element;
                                })
                                .toList()
                    );

                } catch (Exception e) {
                    batchElement.processWithError(e);
                    result.add(batchElement);
                }
            });

            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
