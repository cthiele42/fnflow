package org.ct42.fnflow.fnlib.normalizer.trim;

import com.fasterxml.jackson.core.JsonPointer;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

/**
 * @author Sajjad Safaeian
 */
@Data
@Validated
public class TrimProperties {
    @NotNull
    private JsonPointer elementPath;

    @NotNull
    private TrimMode mode;

    public enum TrimMode {
        RIGHT, LEFT, BOTH
    }
}
