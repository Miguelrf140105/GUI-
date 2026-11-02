package pe.edu.gui.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Contrato cerrado: ningún identificador ni otro atributo es editable. */
public record AssetName(
        @NotBlank @Size(max = 200) String name,
        @NotBlank @Size(max = 200) String expectedName) {
    @JsonAnySetter
    public void rejectOtherFields(String field, Object value) {
        throw new IllegalArgumentException("Solo se permite cambiar el nombre del producto.");
    }
}
