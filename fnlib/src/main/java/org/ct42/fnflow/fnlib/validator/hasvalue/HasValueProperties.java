package org.ct42.fnflow.fnlib.validator.hasvalue;

import com.fasterxml.jackson.core.JsonPointer;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

/**
 * @author Sajjad Safaeian
 */
@Data
@Validated
public class HasValueProperties {
    @NotNull
    private JsonPointer elementPath;
}
