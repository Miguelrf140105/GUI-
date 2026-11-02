package pe.edu.gui.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Random;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class ProductImageCodecTest {
  final ProductImageCodec codec = new ProductImageCodec();

  byte[] png(int w, int h, boolean noise) throws IOException {
    var image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
    var random = new Random(7);
    for (int y = 0; y < h; y++)
      for (int x = 0; x < w; x++) image.setRGB(x, y, noise ? random.nextInt(0xffffff) : 0xabcdef);
    var bytes = new ByteArrayOutputStream();
    ImageIO.write(image, "png", bytes);
    return bytes.toByteArray();
  }

  @Test
  void comprimeEnWebpYGeneraMiniaturaLimitada() throws Exception {
    byte[] original = png(1800, 1000, true);
    var result = codec.compress(original);
    assertTrue(result.image().length <= 150 * 1024);
    assertTrue(result.thumbnail().length <= 30 * 1024);
    assertTrue(result.image().length < original.length);
    assertTrue(result.width() <= 1280);
    var image = ImageIO.read(new ByteArrayInputStream(result.image()));
    var thumb = ImageIO.read(new ByteArrayInputStream(result.thumbnail()));
    assertNotNull(image);
    assertNotNull(thumb);
    assertTrue(thumb.getWidth() <= 240 && thumb.getHeight() <= 240);
    assertEquals(
        "RIFF", new String(result.image(), 0, 4, java.nio.charset.StandardCharsets.US_ASCII));
  }

  @Test
  void noAmpliaFotosPequenas() throws Exception {
    var result = codec.compress(png(12, 15, false));
    assertEquals(12, result.width());
    assertEquals(15, result.height());
  }

  @Test
  void rechazaArchivosInvalidosYFormatosNoPermitidos() throws Exception {
    assertThrows(IllegalArgumentException.class, () -> codec.compress(new byte[0]));
    assertThrows(IllegalArgumentException.class, () -> codec.compress("not an image".getBytes()));
    var bytes = new ByteArrayOutputStream();
    ImageIO.write(new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB), "gif", bytes);
    assertThrows(IllegalArgumentException.class, () -> codec.compress(bytes.toByteArray()));
  }

  @Test
  void rechazaDimensionesAntesDeDecodificar() throws Exception {
    var bytes = png(13000, 1, false);
    assertThrows(IllegalArgumentException.class, () -> codec.compress(bytes));
  }

  @Test
  void mismoContenidoProduceMismoBinario() throws Exception {
    var bytes = png(32, 32, false);
    assertArrayEquals(codec.compress(bytes).image(), codec.compress(bytes).image());
  }

  @Test
  void aceptaWebpYOrientaFotosDeCamara() throws Exception {
    var webp = codec.compress(png(32, 18, false));
    assertNotNull(codec.compress(webp.image()));
    var raw = new ByteArrayOutputStream();
    ImageIO.write(new BufferedImage(30, 18, BufferedImage.TYPE_INT_RGB), "jpeg", raw);
    // JPEG APP1: EXIF little-endian con Orientation=6 (90 grados).
    byte[] exif =
        new byte[] {
          (byte) 255, (byte) 225, 0, 34, 'E', 'x', 'i', 'f', 0, 0, 'I', 'I', 42, 0, 8, 0, 0, 0, 1,
          0, 18, 1, 3, 0, 1, 0, 0, 0, 6, 0, 0, 0, 0, 0, 0, 0
        };
    byte[] jpeg = raw.toByteArray();
    var oriented = new ByteArrayOutputStream();
    oriented.write(jpeg, 0, 2);
    oriented.write(exif);
    oriented.write(jpeg, 2, jpeg.length - 2);
    var result = codec.compress(oriented.toByteArray());
    assertEquals(18, result.width());
    assertEquals(30, result.height());
  }
}
