package org.ct42.fnflow.batchfnlib.script;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
public class ScriptProperties {

    @NotEmpty
    private String script;

}
