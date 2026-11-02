package pe.edu.gui.dao;

import java.util.*;
import org.springframework.stereotype.Repository;
import pe.edu.gui.interfaces.*;
import pe.edu.gui.utils.ProductImageCodec.Images;

@Repository
public class ProductoFotoDAO implements IProductoFotoDAO {
  private final ISqlExecutor sql;

  public ProductoFotoDAO(ISqlExecutor sql) {
    this.sql = sql;
  }

  public List<Map<String, Object>> listar(int actor, int product) {
    return sql.query("EXEC DOC.sp_ProductoFotos ?,?", actor, product);
  }

  public byte[] leer(int actor, UUID photo, boolean thumbnail) {
    return (byte[])
        sql.query("EXEC DOC.sp_ProductoFotoLeer ?,?,?", actor, photo.toString(), thumbnail).stream()
            .findFirst()
            .orElseThrow(() -> new NoSuchElementException("Foto inexistente"))
            .get("content");
  }

  public UUID guardar(int actor, int product, String name, String hash, Images images) {
    var row =
        sql.query(
                "EXEC DOC.sp_ProductoFotoGuardar ?,?,?,?,?,?,?,?,?",
                actor,
                product,
                UUID.randomUUID().toString(),
                name,
                hash,
                images.width(),
                images.height(),
                images.image(),
                images.thumbnail())
            .get(0);
    return UUID.fromString(String.valueOf(row.get("id")));
  }

  public void cambiar(int actor, int product, UUID photo, String action) {
    sql.execute(
        "EXEC DOC.sp_ProductoFotoCambiar ?,?,?,?", actor, product, photo.toString(), action);
  }
}
