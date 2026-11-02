package pe.edu.gui.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** DTOs de entrada: las relaciones siempre son IDs numéricos. */
public final class Requests {

    private Requests() {}

    /** Cabecera reutilizable del comprobante. */
    public record Document(
            @NotNull @Positive Integer typeId,
            @NotNull @Positive Integer institutionId,
            @NotBlank @Size(max = 60) String number,
            @NotNull LocalDate date,
            @DecimalMin("0.00") @Digits(integer = 16, fraction = 2) BigDecimal amount) {}

    /** Tipo de producto, como Laptop, asociado a una categoría por su ID. */
    public record Family(
            @NotBlank @Size(max = 100) String name, @NotNull @Positive Integer categoryId) {}

    /** Valor de catálogo asociado a un grupo. Las relaciones de negocio usan el ID. */
    public record Parameter(
            @NotBlank @Size(max = 150) String name,
            @Size(max = 50) String code,
            @NotNull @Positive Integer groupId) {}

    public record IntakeDelete(@NotNull @Positive Integer id) {}

    public record InstitutionDelete(@NotNull @Positive Integer id) {}

    public record LocationDelete(@NotNull @Positive Integer id) {}

    /** Ubicación del árbol. parentId es nulo para una raíz; id nulo indica una ubicación nueva. */
    public record Location(
            Integer id,
            @NotBlank @Size(max = 150) String name,
            @Size(max = 50) String code,
            @NotNull @Positive Integer typeId,
            @Positive Integer parentId,
            @Positive Integer responsibleId) {}

    /** Proveedor, donante o institución destinataria de una operación. */
    public record Institution(
            @Positive Integer id,
            @NotBlank @Size(max = 200) String name,
            @NotBlank @Size(max = 30) String document,
            @Size(max = 150) String contact,
            @Email @Size(max = 150) String email,
            @Size(max = 40) String phone,
            @Size(max = 250) String address,
            Integer stateId) {}

    /** Valor de un atributo EAV identificado por su definición numérica. */
    public record Specification(
            @NotNull @Positive Integer id, @NotBlank @Size(max = 500) String value) {}

    /**
     * Solicitud sobre una unidad. La clave UUID permite reconocer el mismo envío al reintentarlo.
     */
    public record Movement(
            @NotNull @Positive Integer assetId,
            @NotNull @Positive Integer typeId,
            @Positive Integer locationId,
            @Positive Integer institutionId,
            @NotBlank @Size(max = 500) String reason,
            @NotNull UUID idempotencyKey) {}

    /** ID del movimiento que debe avanzar. El endpoint determina la etapa solicitada. */
    public record Transition(@NotNull @Positive Long movementId) {}

    /** Programación de atención de un activo con fecha y técnico responsable. */
    public record Maintenance(
            @NotNull @Positive Integer assetId,
            @NotNull LocalDate date,
            @NotBlank @Size(max = 500) String description,
            @NotNull @Positive Integer technicianId) {}

    /** Rol y conjunto de IDs de permisos. id nulo indica un rol nuevo. */
    public record Role(
            Integer id,
            @NotBlank @Size(max = 80) String name,
            @NotNull @Size(max = 200) List<@Positive Integer> permissionIds) {}

    /** Cuenta, roles y alcance. Una contraseña vacía conserva el hash al editar una cuenta. */
    public record User(
            Integer id,
            @NotBlank @Size(max = 80) String username,
            @NotBlank @Size(max = 150) String name,
            @NotBlank @Email @Size(max = 150) String email,
            @Size(max = 72) String password,
            @NotNull Integer stateId,
            @NotEmpty List<@Positive Integer> roleIds,
            @NotNull List<@Positive Integer> locationIds,
            boolean globalScope) {}
}
