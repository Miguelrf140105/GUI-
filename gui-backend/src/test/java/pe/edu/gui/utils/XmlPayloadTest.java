package pe.edu.gui.utils;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

class XmlPayloadTest {
    public record Product(Integer id, String name, String brand, String model, Integer familyId) {}

    public record IntakeLine(int productId, int quantity, BigDecimal unitPrice) {}

    public record Intake(
            int typeId,
            int institutionId,
            LocalDate date,
            String reason,
            List<IntakeLine> lines,
            List<Integer> documentIds) {}

    @Test
    void escapesUserTextAndPreservesUnicodeAndCarriageReturns() throws Exception {
        var dto = new Product(null, "Laptop <&> ñ\r\nmodelo", "A&B", "'5440'", 1);
        String xml = XmlPayload.of(dto);
        var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        var doc = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        assertEquals(dto.name(), doc.getElementsByTagName("name").item(0).getTextContent());
        assertEquals("A&B", doc.getElementsByTagName("brand").item(0).getTextContent());
        assertFalse(xml.contains("<id>"));
        assertTrue(xml.contains("&lt;"));
        assertTrue(xml.contains("&amp;"));
    }

    @Test
    void serializesNestedLinesAndNumericIdCollections() {
        var dto =
                new Intake(
                        1,
                        4,
                        LocalDate.of(2026, 10, 22),
                        null,
                        List.of(new IntakeLine(8, 3, new BigDecimal("1E+3"))),
                        List.of(2, 5));
        String xml = XmlPayload.of(dto);
        assertTrue(
                xml.contains(
                        "<lines><item><productId>8</productId><quantity>3</quantity><unitPrice>1000</unitPrice></item></lines>"));
        assertTrue(xml.contains("<documentIds><item>2</item><item>5</item></documentIds>"));
    }

    @Test
    void serializesAnEmptyPermissionListAsAnEmptyCollection() {
        assertEquals("<request></request>", XmlPayload.of(List.of()));
    }

    @Test
    void rejectsXmlControlCharactersBeforeCallingSql() {
        assertThrows(
                IllegalArgumentException.class,
                () -> XmlPayload.of(new Product(null, "Laptop\u0000", null, null, 1)));
    }

    @Test
    void rejectsArbitraryObjectsOutsideTheDtoContract() {
        assertThrows(IllegalArgumentException.class, () -> XmlPayload.of(Map.of("id", 1)));
    }
}
