package pe.edu.gui.utils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import javax.imageio.*;
import javax.imageio.stream.*;
import org.springframework.stereotype.Component;

/** Decodifica y vuelve a codificar píxeles: no conserva EXIF, GPS ni el archivo original. */
@Component
public class ProductImageCodec {
  public record Images(byte[] image, byte[] thumbnail, int width, int height) {}

  public Images compress(byte[] bytes) {
    if (bytes == null || bytes.length == 0 || bytes.length > 10 * 1024 * 1024)
      throw new IllegalArgumentException("La foto debe pesar entre 1 byte y 10 MB.");
    try (var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
      var readers = ImageIO.getImageReaders(input);
      if (!readers.hasNext())
        throw new IllegalArgumentException("Selecciona una imagen JPEG, PNG o WebP válida.");
      var reader = readers.next();
      try {
        String format = reader.getFormatName().toLowerCase(Locale.ROOT);
        if (!Set.of("jpeg", "jpg", "png", "webp").contains(format))
          throw new IllegalArgumentException("Solo se admiten fotos JPEG, PNG o WebP.");
        reader.setInput(input, true, true);
        int w = reader.getWidth(0), h = reader.getHeight(0);
        // Comprueba dimensiones antes de reservar el mapa completo de píxeles.
        if (w < 1 || h < 1 || w > 12000 || h > 12000 || (long) w * h > 40_000_000)
          throw new IllegalArgumentException("La imagen supera las dimensiones admitidas.");
        var param = reader.getDefaultReadParam();
        int sub = Math.max(1, Math.max(w, h) / 1600);
        param.setSourceSubsampling(sub, sub, 0, 0);
        var decoded = orient(reader.read(0, param), orientation(bytes));
        var main = fit(decoded, 1280);
        byte[] image = encodeBounded(main, 150 * 1024);
        // La miniatura parte de la imagen original decodificada, sin volver a degradar la
        // principal.
        byte[] thumb = encodeBounded(fit(decoded, 240), 30 * 1024);
        var actual = ImageIO.read(new ByteArrayInputStream(image));
        return new Images(image, thumb, actual.getWidth(), actual.getHeight());
      } finally {
        reader.dispose();
      }
    } catch (IOException e) {
      throw new IllegalArgumentException("No se pudo leer la foto. Prueba con otra imagen.", e);
    }
  }

  /** Lee únicamente la orientación EXIF; los metadatos originales no se escriben en la salida. */
  private int orientation(byte[] b) {
    if (b.length < 4 || (b[0] & 255) != 255 || (b[1] & 255) != 216) return 1;
    try {
      int pos = 2;
      while (pos + 4 <= b.length) {
        if ((b[pos] & 255) != 255) return 1;
        int marker = b[pos + 1] & 255;
        pos += 2;
        if (marker == 218 || marker == 217) return 1;
        int len = ((b[pos] & 255) << 8) | (b[pos + 1] & 255);
        if (len < 2 || pos + len > b.length) return 1;
        int data = pos + 2, end = pos + len;
        if (marker == 225
            && len >= 16
            && b[data] == 'E'
            && b[data + 1] == 'x'
            && b[data + 2] == 'i'
            && b[data + 3] == 'f'
            && b[data + 4] == 0
            && b[data + 5] == 0) {
          int base = data + 6;
          boolean little = b[base] == 'I' && b[base + 1] == 'I';
          if (!little && !(b[base] == 'M' && b[base + 1] == 'M')) return 1;
          var buffer =
              java.nio.ByteBuffer.wrap(b)
                  .order(little ? java.nio.ByteOrder.LITTLE_ENDIAN : java.nio.ByteOrder.BIG_ENDIAN);
          if (buffer.getShort(base + 2) != 42) return 1;
          long offset = Integer.toUnsignedLong(buffer.getInt(base + 4));
          if (offset > end - base - 2) return 1;
          int dir = base + (int) offset, count = Short.toUnsignedInt(buffer.getShort(dir));
          for (int i = 0; i < count; i++) {
            int entry = dir + 2 + i * 12;
            if (entry + 12 > end) return 1;
            if (Short.toUnsignedInt(buffer.getShort(entry)) == 274
                && buffer.getShort(entry + 2) == 3
                && buffer.getInt(entry + 4) == 1) {
              int value = Short.toUnsignedInt(buffer.getShort(entry + 8));
              return value >= 1 && value <= 8 ? value : 1;
            }
          }
        }
        pos += len;
      }
    } catch (IndexOutOfBoundsException ignored) {
      return 1;
    }
    return 1;
  }

  private BufferedImage orient(BufferedImage source, int orientation) {
    if (orientation == 1) return source;
    int w = source.getWidth(), h = source.getHeight();
    boolean swap = orientation >= 5;
    var out = new BufferedImage(swap ? h : w, swap ? w : h, BufferedImage.TYPE_INT_RGB);
    java.awt.geom.AffineTransform transform =
        switch (orientation) {
          case 2 -> new java.awt.geom.AffineTransform(-1, 0, 0, 1, w, 0);
          case 3 -> new java.awt.geom.AffineTransform(-1, 0, 0, -1, w, h);
          case 4 -> new java.awt.geom.AffineTransform(1, 0, 0, -1, 0, h);
          case 5 -> new java.awt.geom.AffineTransform(0, 1, 1, 0, 0, 0);
          case 6 -> new java.awt.geom.AffineTransform(0, 1, -1, 0, h, 0);
          case 7 -> new java.awt.geom.AffineTransform(0, -1, -1, 0, h, w);
          case 8 -> new java.awt.geom.AffineTransform(0, -1, 1, 0, 0, w);
          default -> new java.awt.geom.AffineTransform();
        };
    var g = out.createGraphics();
    try {
      g.drawImage(source, transform, null);
    } finally {
      g.dispose();
    }
    return out;
  }

  private BufferedImage fit(BufferedImage image, int max) {
    double scale = Math.min(1d, (double) max / Math.max(image.getWidth(), image.getHeight()));
    int w = Math.max(1, (int) Math.round(image.getWidth() * scale)),
        h = Math.max(1, (int) Math.round(image.getHeight() * scale));
    var out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
    var g = out.createGraphics();
    try {
      g.setColor(Color.WHITE);
      g.fillRect(0, 0, w, h);
      g.setRenderingHint(
          RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
      g.drawImage(image, 0, 0, w, h, null);
    } finally {
      g.dispose();
    }
    return out;
  }

  private byte[] encodeBounded(BufferedImage image, int limit) throws IOException {
    var current = image;
    for (int size = 0; size < 7; size++) {
      for (float quality : new float[] {0.82f, 0.68f, 0.52f, 0.36f}) {
        var writers = ImageIO.getImageWritersByMIMEType("image/webp");
        if (!writers.hasNext())
          throw new IllegalStateException("El compresor de fotos no está disponible.");
        var writer = writers.next();
        var bytes = new ByteArrayOutputStream();
        try (var output = new MemoryCacheImageOutputStream(bytes)) {
          var options = writer.getDefaultWriteParam();
          options.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
          for (String type : options.getCompressionTypes())
            if (type.equalsIgnoreCase("Lossy")) {
              options.setCompressionType(type);
              break;
            }
          options.setCompressionQuality(quality);
          writer.setOutput(output);
          writer.write(null, new IIOImage(current, null, null), options);
          output.flush();
        } finally {
          writer.dispose();
        }
        if (bytes.size() <= limit) return bytes.toByteArray();
      }
      current =
          fit(
              current,
              Math.max(64, (int) (Math.max(current.getWidth(), current.getHeight()) * 0.78)));
    }
    throw new IllegalArgumentException("No se pudo reducir la imagen al tamaño permitido.");
  }
}
