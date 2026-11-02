package pe.edu.gui.dto;

import java.util.List;
import java.util.Map;

/** Página de resultados. hasMore indica si hay registros después de la página actual. */
public record Pagina(List<Map<String, Object>> rows, int page, int size, boolean hasMore) {}
