package pe.edu.gui.conexion;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import pe.edu.gui.interfaces.ISqlExecutor;

import java.util.List;
import java.util.Map;

/**
 * Acceso JDBC sobre el DataSource administrado por Spring (Hikari o JNDI). JdbcTemplate reutiliza
 * la conexión vinculada a @Transactional y libera recursos. Nunca conserva una conexión compartida
 * ni devuelve null ante un error de SQL.
 */
@Component
public class Conexion implements ISqlExecutor {

    private final JdbcTemplate jdbc;

    public Conexion(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Ejecuta una consulta parametrizada y devuelve sus filas. JdbcTemplate libera los recursos y
     * respeta la transacción activa.
     */
    @Override
    public List<Map<String, Object>> query(String sql, Object... parameters) {
        return jdbc.queryForList(sql, parameters);
    }

    /** Ejecuta una operación sin filas de respuesta sobre la conexión administrada por Spring. */
    @Override
    public void execute(String sql, Object... parameters) {
        jdbc.update(sql, parameters);
    }
}
