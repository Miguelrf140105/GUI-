package pe.edu.gui.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLException;
import java.util.NoSuchElementException;

/** Traduce errores de validación, acceso y persistencia a respuestas HTTP para la web. */
@RestControllerAdvice
public class ApiErrors {

    private static final Logger LOG = LoggerFactory.getLogger(ApiErrors.class);

    /** Devuelve el motivo de una validación de negocio que el usuario puede corregir. */
    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail invalid(IllegalArgumentException e) {
        return problem(400, e.getMessage());
    }

    /** Responde a errores de los DTO sin exponer el detalle interno de Bean Validation. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validation(MethodArgumentNotValidException e) {
        return problem(
                400, "Revise los campos obligatorios, longitudes y formatos del formulario.");
    }

    /** Indica que el recurso solicitado no existe. */
    @ExceptionHandler(NoSuchElementException.class)
    ProblemDetail missing(NoSuchElementException e) {
        return problem(404, e.getMessage());
    }

    /** Responde sin revelar información adicional de un recurso al que no se tiene acceso. */
    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail denied(AccessDeniedException e) {
        return problem(403, "No tiene acceso a esta acción o recurso.");
    }

    /**
     * Traduce los códigos SQL conocidos; los fallos técnicos no exponen consultas ni credenciales
     * al cliente.
     */
    @ExceptionHandler(DataAccessException.class)
    ProblemDetail database(DataAccessException e) {
        Throwable root = e.getMostSpecificCause();
        if (root instanceof SQLException s) {
            int code = s.getErrorCode();
            if (code == 51003) {
                return problem(403, "No tiene acceso a esta acción o recurso.");
            }
            if (code == 51004) {
                return problem(404, "Registro inexistente.");
            }
            if (code >= 51000 && code <= 51099) {
                return problem(code == 51009 ? 409 : 400, s.getMessage());
            }
            if (code == 2601 || code == 2627) {
                return problem(
                        409,
                        "Ya existe un registro con esos identificadores, o el activo tiene un movimiento pendiente.");
            }
            if (code == 547) {
                return problem(400, "Revise las relaciones, cantidades y valores seleccionados.");
            }
        }
        LOG.error("Error de persistencia: {}", root.getClass().getSimpleName());
        return problem(503, "No se pudo completar la operación con la base de datos.");
    }

    /** Construye una respuesta de error con estado HTTP y descripción. */
    private ProblemDetail problem(int status, String detail) {
        return ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(status), detail);
    }
}
