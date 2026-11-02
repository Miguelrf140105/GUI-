package pe.edu.gui.interfaces;

import pe.edu.gui.dto.Requests;
import pe.edu.gui.dto.ResultadoRegistro;
import pe.edu.gui.entity.CredencialesUsuario;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Contrato de persistencia para cuentas, roles y alcance de acceso. */
public interface IUsuarioDAO {
    int passwordVersion(int id);

    /**
     * Busca los datos internos de autenticación de una cuenta activa. No devuelve una contraseña en
     * texto plano.
     */
    Optional<CredencialesUsuario> credenciales(String username);

    /**
     * Consulta identidad y permisos vigentes; no utiliza una copia de permisos guardada al iniciar
     * sesión.
     */
    List<Map<String, Object>> identidad(int id);

    /**
     * Guarda la cuenta y sus asignaciones; al editar, una contraseña vacía conserva la existente.
     */
    ResultadoRegistro guardar(int actor, Requests.User dto, String hash);

    /** Consulta los roles y las raíces de ubicación asignadas a la cuenta. */
    Map<String, Object> asignaciones(int actor, int id);

    /**
     * Crea el administrador inicial solo si no existen usuarios; el SP decide esa condición de
     * forma atómica.
     */
    void bootstrap(String username, String hash);
}
