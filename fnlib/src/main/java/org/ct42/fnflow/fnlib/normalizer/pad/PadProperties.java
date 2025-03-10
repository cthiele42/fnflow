package org.ct42.fnflow.fnlib.normalizer.pad;

import com.fasterxml.jackson.core.JsonPointer;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

/**
 * @author Sajjad Safaeian
 */
@Data
@Validated
public class PadProperties {
    @NotNull
    private JsonPointer elementPath;

    @NotNull
    private Pad pad;

    @Min(value = 1, message = "Length should be more than 1.")
    private int length;

    private char fillerCharacter;

    public enum Pad {
        LEFT, RIGHT
    }
}
