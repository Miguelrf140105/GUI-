package pe.edu.gui.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pe.edu.gui.dto.Pagina;
import pe.edu.gui.security.Access;
import pe.edu.gui.service.ConsultaService;

import java.util.Map;

/**
 * Endpoints REST para listados paginados, catálogos de selección y dashboard. La identidad del
 * ejecutor se obtiene de la sesión.
 */
@RestController
@RequestMapping("/api")
public class ConsultaController {

    private final Access access;

    private final ConsultaService service;

    public ConsultaController(Access access, ConsultaService service) {
        this.access = access;
        this.service = service;
    }

    /**
     * Obtiene una página del módulo indicado, limitada a los datos que puede consultar el usuario.
     */
    @GetMapping("/modules/{module}")
    public Pagina listar(
            Authentication auth,
            @PathVariable int module,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return service.pagina(access.user(auth), module, page, size);
    }

    /**
     * Reúne los catálogos necesarios para los formularios. Las claves coinciden con el contrato del
     * frontend.
     */
    @GetMapping("/lookups")
    public Map<String, Object> lookups(Authentication auth) {
        return service.lookups(access.user(auth));
    }

    /** Consulta los resúmenes visibles para el usuario. Los cálculos se realizan en SQL Server. */
    @GetMapping("/dashboard")
    public Map<String, Object> dashboard(Authentication auth) {
        return service.dashboard(access.user(auth));
    }
}
