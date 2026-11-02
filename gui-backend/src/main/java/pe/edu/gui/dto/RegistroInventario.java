package pe.edu.gui.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import pe.edu.gui.dto.Requests.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Una recepción completa: sus comprobantes y las unidades físicas que ingresan. */
public record RegistroInventario(
        @NotNull UUID idempotencyKey,
        @NotNull @Positive Integer typeId,
        @Positive Integer institutionId,
        @Valid Requests.Institution newInstitution,
        @NotNull LocalDate date,
        @Size(max = 500) String reason,
        @NotNull @Positive Integer locationId,
        @Positive Integer responsibleId,
        @NotEmpty @Size(max = 100) List<@NotNull @Valid Linea> lines,
        @NotNull @Size(max = 50) List<@NotNull @Positive Integer> documentIds,
        @NotNull @Size(max = 50) List<@NotNull @Valid Comprobante> documents) {

    /** Puede reutilizar un modelo o crearlo dentro del mismo registro. */
    public record Linea(
            @NotNull @Positive Integer familyId,
            @Positive Integer productId,
            @Size(max = 200) String name,
            @Size(max = 80) String brand,
            @Size(max = 80) String model,
            @NotNull @Min(1) @Max(1000) Integer quantity,
            @DecimalMin("0.00") @Digits(integer = 16, fraction = 2) BigDecimal unitPrice,
            @NotNull @Size(max = 1000) List<@NotNull @Size(max = 100) String> serials,
            @NotNull @Size(max = 100) List<@NotNull @Valid Specification> specifications) {}

    /** La misma cabecera respalda todas las líneas de esta recepción. */
    public record Comprobante(
            @NotNull @Positive Integer typeId,
            @NotBlank @Size(max = 60) String number,
            @NotNull LocalDate date,
            @DecimalMin("0.00") @Digits(integer = 16, fraction = 2) BigDecimal amount) {}
}
