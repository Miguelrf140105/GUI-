package pe.edu.gui.dto;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.UUID;
/** Una solicitud por lote, sin compartir la identidad ni el historial de sus unidades. */
public record MovementBatch(
    @NotNull @Size(min=1,max=100) List<@NotNull @Positive Integer> assetIds,
    @NotNull @Positive Integer typeId,
    @Positive Integer locationId,
    @Positive Integer institutionId,
    @NotBlank @Size(max=500) String reason,
    @NotNull UUID idempotencyKey) {}
