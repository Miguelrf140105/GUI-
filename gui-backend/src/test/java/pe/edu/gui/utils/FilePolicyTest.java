package pe.edu.gui.utils;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class FilePolicyTest {

    @Test
    void rejectsHtmlDisguisedAsImage() {
        assertThrows(
                IllegalArgumentException.class,
                () -> FilePolicy.detect("<script>alert(1)</script>".getBytes()));
    }

    @Test
    void rejectsEmptyAndOversizeFiles() {
        assertThrows(IllegalArgumentException.class, () -> FilePolicy.detect(new byte[0]));
        assertThrows(IllegalArgumentException.class, () -> FilePolicy.detect(new byte[10485761]));
    }

    @Test
    void recognizesSignatures() {
        assertEquals("application/pdf", FilePolicy.detect("%PDF-1.7".getBytes()));
        assertEquals(
                "image/png",
                FilePolicy.detect(new byte[] {(byte) 137, 80, 78, 71, 13, 10, 26, 10}));
        assertEquals(
                "image/jpeg", FilePolicy.detect(new byte[] {(byte) 255, (byte) 216, (byte) 255}));
    }

    @Test
    void stripsHeaderAndPathCharacters() {
        assertEquals(
                ".._factura__X-Test_bad.pdf", FilePolicy.safeName("../factura\r\nX-Test/bad.pdf"));
    }
}
