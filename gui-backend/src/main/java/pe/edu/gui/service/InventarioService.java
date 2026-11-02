package pe.edu.gui.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.gui.dto.RegistroInventario;
import pe.edu.gui.interfaces.IInventarioDAO;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Valida la recepción antes de pedir su persistencia atómica en SQL Server. */
@Service
public class InventarioService {
    private final IInventarioDAO dao;

    public InventarioService(IInventarioDAO dao) {
        this.dao = dao;
    }

    /** Todos los modelos, documentos y unidades se confirman juntos o se revierten juntos. */
    @Transactional
    public List<Map<String, Object>> registrar(int actor, RegistroInventario dto) {
        if ((dto.institutionId() == null) == (dto.newInstitution() == null)) {
            throw new IllegalArgumentException("Seleccione un proveedor o registre uno nuevo");
        }
        if (dto.newInstitution() != null
                && (dto.newInstitution().id() != null
                        || (dto.newInstitution().stateId() != null
                                && dto.newInstitution().stateId() != 1))) {
            throw new IllegalArgumentException(
                    "El proveedor nuevo debe estar activo y sin ID previo");
        }
        var models = new HashSet<Integer>();
        var serials = new HashSet<String>();
        int total = 0;
        for (var line : dto.lines()) {
            total += line.quantity();
            if (line.productId() == null && (line.name() == null || line.name().isBlank())) {
                throw new IllegalArgumentException("Indique el nombre del nuevo producto");
            }
            if (line.productId() != null && !models.add(line.productId())) {
                throw new IllegalArgumentException(
                        "Agrupe las unidades del mismo modelo en una línea");
            }
            if (!line.serials().isEmpty() && line.serials().size() != line.quantity()) {
                throw new IllegalArgumentException(
                        "Indique una serie por unidad o deje la lista vacía");
            }
            for (String serial : line.serials()) {
                if (!serial.isBlank() && !serials.add(serial.strip().toUpperCase(Locale.ROOT))) {
                    throw new IllegalArgumentException("No repita series dentro de la recepción");
                }
            }
            var specifications = new HashSet<Integer>();
            for (var spec : line.specifications()) {
                if (!specifications.add(spec.id())) {
                    throw new IllegalArgumentException(
                            "No repita una característica en el producto");
                }
            }
        }
        if (total < 1 || total > 1000) {
            throw new IllegalArgumentException("Registre entre 1 y 1000 unidades por recepción");
        }
        if (new HashSet<>(dto.documentIds()).size() != dto.documentIds().size()) {
            throw new IllegalArgumentException("No repita comprobantes");
        }
        return dao.registrar(actor, dto);
    }
}
