package pe.edu.gui.controller;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import pe.edu.gui.dto.RegistroInventario;
import pe.edu.gui.security.Access;
import pe.edu.gui.service.InventarioService;

import java.util.List;
import java.util.Map;

/** Entrada única para registrar bienes; el actor siempre procede de la sesión autenticada. */
@RestController
@RequestMapping("/api/inventory")
public class InventarioController {
    private final Access access;
    private final InventarioService service;

    public InventarioController(Access access, InventarioService service) {
        this.access = access;
        this.service = service;
    }

    @PostMapping("/registrations")
    @PreAuthorize("@access.has(authentication,302) and @access.has(authentication,1402)")
    public List<Map<String, Object>> registrar(
            Authentication auth, @Valid @RequestBody RegistroInventario dto) {
        return service.registrar(access.user(auth), dto);
    }
}
