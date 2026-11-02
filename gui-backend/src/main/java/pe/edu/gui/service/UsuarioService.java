package pe.edu.gui.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.gui.dto.Requests;
import pe.edu.gui.dto.ResultadoRegistro;
import pe.edu.gui.interfaces.IUsuarioDAO;
import pe.edu.gui.utils.PoliticaContrasena;

import java.util.HashSet;
import java.util.Map;

/**
 * Casos de uso para cuentas, roles y alcance de acceso. Coordina validaciones y acceso a
 * persistencia.
 */
@Service
public class UsuarioService {

    private final IUsuarioDAO dao;

    private final PasswordEncoder encoder;

    public UsuarioService(IUsuarioDAO dao, PasswordEncoder encoder) {
        this.dao = dao;
        this.encoder = encoder;
    }

    /**
     * Guarda la cuenta y sus asignaciones; al editar, una contraseña vacía conserva la existente.
     */
    @Transactional
    public ResultadoRegistro guardar(int actor, Requests.User dto) {
        if (Integer.valueOf(actor).equals(dto.id())) {
            throw new IllegalArgumentException(
                    "No modifique sus propios privilegios desde este formulario");
        }
        if (dto.stateId() != 0 && dto.stateId() != 1) {
            throw new IllegalArgumentException("Estado de cuenta inválido");
        }
        if (dto.roleIds().isEmpty()) {
            throw new IllegalArgumentException("Asigne al menos un rol");
        }
        if (!dto.globalScope() && dto.locationIds().isEmpty()) {
            throw new IllegalArgumentException("Asigne un alcance de ubicación");
        }
        if (new HashSet<>(dto.roleIds()).size() != dto.roleIds().size()
                || new HashSet<>(dto.locationIds()).size() != dto.locationIds().size()) {
            throw new IllegalArgumentException("No repita roles ni ubicaciones");
        }
        String hash = null;
        if (dto.password() != null && !dto.password().isEmpty()) {
            PoliticaContrasena.validar(dto.password());
            hash = encoder.encode(dto.password());
        } else if (dto.id() == null) {
            throw new IllegalArgumentException("La contraseña inicial es obligatoria");
        }
        return dao.guardar(actor, dto, hash);
    }

    /** Consulta los roles y las raíces de ubicación asignadas a la cuenta. */
    public Map<String, Object> asignaciones(int actor, int id) {
        return dao.asignaciones(actor, id);
    }

    /**
     * Crea el administrador inicial solo si no existen usuarios; el SP decide esa condición de
     * forma atómica.
     */
    @Transactional
    public void bootstrap(String username, String password) {
        if (username.isBlank() && password.isBlank()) {
            return;
        }
        if (username.isBlank()) {
            throw new IllegalArgumentException("Indique el usuario inicial");
        }
        PoliticaContrasena.validar(password);
        dao.bootstrap(username, encoder.encode(password));
    }
}
