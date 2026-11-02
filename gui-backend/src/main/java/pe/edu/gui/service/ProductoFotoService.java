package pe.edu.gui.service;

import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.gui.interfaces.IProductoFotoDAO;
import pe.edu.gui.utils.*;

@Service
public class ProductoFotoService {
  private final IProductoFotoDAO dao;
  private final ProductImageCodec codec;

  public ProductoFotoService(IProductoFotoDAO dao, ProductImageCodec codec) {
    this.dao = dao;
    this.codec = codec;
  }

  public List<Map<String, Object>> listar(int actor, int product) {
    valid(product);
    return dao.listar(actor, product);
  }

  public byte[] leer(int actor, UUID photo, boolean thumbnail) {
    return dao.leer(actor, photo, thumbnail);
  }

  @Transactional
  public UUID subir(int actor, int product, String name, byte[] bytes) {
    valid(product);
    // Verifica acceso antes de gastar recursos en decodificación; SQL vuelve a comprobar la
    // escritura.
    dao.listar(actor, product);
    var images = codec.compress(bytes);
    try {
      String hash =
          HexFormat.of()
              .formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(images.image()));
      return dao.guardar(actor, product, FilePolicy.safeName(name), hash, images);
    } catch (java.security.NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  @Transactional
  public void cambiar(int actor, int product, UUID photo, String action) {
    valid(product);
    dao.cambiar(actor, product, photo, action);
  }

  private void valid(int product) {
    if (product < 1) throw new IllegalArgumentException("Producto inválido");
  }
}
