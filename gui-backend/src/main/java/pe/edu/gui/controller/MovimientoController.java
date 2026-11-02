package pe.edu.gui.controller;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pe.edu.gui.dto.Requests;
import pe.edu.gui.dto.ResultadoRegistro;
import pe.edu.gui.entity.EtapaMovimiento;
import pe.edu.gui.security.Access;
import pe.edu.gui.service.MovimientoService;

/**
 * Endpoints REST para solicitudes y etapas de traslado, donación, mantenimiento o baja. La
 * identidad del ejecutor se obtiene de la sesión.
 */
@RestController
@RequestMapping("/api/movements")
public class MovimientoController {

    private final Access access;

    private final MovimientoService service;

    public MovimientoController(Access access, MovimientoService service) {
        this.access = access;
        this.service = service;
    }

    /**
     * Comprueba activo, destino y contraparte. Un reintento con la misma clave y datos recupera la
     * solicitud existente.
     */
    @PostMapping
    @PreAuthorize("@access.has(authentication,402)")
    public ResultadoRegistro solicitar(
            Authentication auth, @Valid @RequestBody Requests.Movement dto) {
        return service.solicitar(access.user(auth), dto);
    }

    /** Solicita el paso de pendiente a autorizado para el movimiento indicado. */
    @PostMapping("/authorize")
    @PreAuthorize("@access.has(authentication,403)")
    public ResultadoRegistro autorizar(
            Authentication auth, @Valid @RequestBody Requests.Transition dto) {
        return service.avanzar(access.user(auth), EtapaMovimiento.AUTORIZAR, dto);
    }

    /** Registra el despacho del movimiento autorizado; la ubicación aún no se cambia. */
    @PostMapping("/dispatch")
    @PreAuthorize("@access.has(authentication,404)")
    public ResultadoRegistro despachar(
            Authentication auth, @Valid @RequestBody Requests.Transition dto) {
        return service.avanzar(access.user(auth), EtapaMovimiento.DESPACHAR, dto);
    }

    /**
     * Confirma la recepción. El SP actualiza la ubicación y agrega el evento de Kardex en la misma
     * transacción.
     */
    @PostMapping("/receive")
    @PreAuthorize("@access.has(authentication,405)")
    public ResultadoRegistro recibir(
            Authentication auth, @Valid @RequestBody Requests.Transition dto) {
        return service.avanzar(access.user(auth), EtapaMovimiento.RECIBIR, dto);
    }

    @PostMapping("/batch")
    @PreAuthorize("@access.has(authentication,402)")
    public java.util.List<MovimientoService.UnitMovement> solicitarLote(Authentication auth,
            @Valid @RequestBody pe.edu.gui.dto.MovementBatch dto) {
        return service.solicitarLote(access.user(auth),dto);
    }
}
