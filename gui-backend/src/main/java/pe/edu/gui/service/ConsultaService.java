package pe.edu.gui.service;

import org.springframework.stereotype.Service;

import pe.edu.gui.dto.Pagina;
import pe.edu.gui.interfaces.IConsultaDAO;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Casos de uso para listados paginados, catálogos de selección y dashboard. Coordina validaciones y
 * acceso a persistencia.
 */
@Service
public class ConsultaService {

    private final IConsultaDAO dao;

    public ConsultaService(IConsultaDAO dao) {
        this.dao = dao;
    }

    /**
     * Valida los límites y solicita una fila adicional para saber si existe otra página sin contar
     * todo el inventario.
     */
    public Pagina pagina(int actor, int module, int page, int size) {
        if (!Set.of(10, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120, 130, 140, 150).contains(module)
                || page < 0
                || page > 100000
                || size < 1
                || size > 100) {
            throw new IllegalArgumentException("Paginación o módulo inválido");
        }
        var rows = dao.listar(actor, module, page * size, size + 1);
        return new Pagina(
                rows.subList(0, Math.min(rows.size(), size)), page, size, rows.size() > size);
    }

    /**
     * Reúne los catálogos necesarios para los formularios. Las claves coinciden con el contrato del
     * frontend.
     */
    public Map<String, Object> lookups(int actor) {
        String[] keys = {
            "parameters",
            "families",
            "locations",
            "people",
            "institutions",
            "products",
            "movementTypes",
            "intakeLines",
            "specifications",
            "categorySpecifications",
            "permissions",
            "roles",
            "familySpecifications"
        };
        Map<String, Object> out = new LinkedHashMap<>();
        for (int i = 0; i < keys.length; i++) {
            out.put(keys[i], dao.lookup(actor, i + 1));
        }
        return out;
    }

    /** Consulta los resúmenes visibles para el usuario. Los cálculos se realizan en SQL Server. */
    public Map<String, Object> dashboard(int actor) {
        return Map.of("monthly", dao.dashboard(actor, 1), "categories", dao.dashboard(actor, 2));
    }
}
