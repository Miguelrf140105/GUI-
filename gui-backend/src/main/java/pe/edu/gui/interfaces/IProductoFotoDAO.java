package pe.edu.gui.interfaces;

import java.util.*;
import pe.edu.gui.utils.ProductImageCodec.Images;

public interface IProductoFotoDAO {
  List<Map<String, Object>> listar(int actor, int product);

  byte[] leer(int actor, UUID photo, boolean thumbnail);

  UUID guardar(int actor, int product, String name, String hash, Images images);

  void cambiar(int actor, int product, UUID photo, String action);
}
