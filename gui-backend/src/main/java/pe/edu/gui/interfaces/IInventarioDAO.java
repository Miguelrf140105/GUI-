package pe.edu.gui.interfaces;

import pe.edu.gui.dto.RegistroInventario;

import java.util.List;
import java.util.Map;

/** Persistencia del alta completa; la clave de reintento evita duplicar recepciones. */
public interface IInventarioDAO {
    List<Map<String, Object>> registrar(int actor, RegistroInventario dto);
}
