package pe.edu.gui.utils;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.temporal.TemporalAccessor;
import java.util.UUID;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/** Contrato interno DAO-SP para SQL Server 2014. La API pública continúa usando JSON. */
public final class XmlPayload {

    private XmlPayload() {}

    /**
     * Serializa el contrato interno como XML con escape de valores; no construye etiquetas con
     * texto del usuario.
     */
    public static String of(Object value) {
        try {
            var output = new StringWriter();
            var writer = XMLOutputFactory.newFactory().createXMLStreamWriter(output);
            write(writer, "request", value);
            writer.flush();
            writer.close();
            return output.toString();
        } catch (XMLStreamException | ReflectiveOperationException e) {
            throw new IllegalArgumentException("No se pudo serializar la petición SQL", e);
        }
    }

    /**
     * Escribe registros, listas y valores simples; omite null y rechaza caracteres que SQL XML no
     * admite.
     */
    private static void write(XMLStreamWriter writer, String name, Object value)
            throws XMLStreamException, ReflectiveOperationException {
        if (value == null) {
            return;
        }
        writer.writeStartElement(name);
        if (value instanceof Iterable<?> values) {
            for (var item : values) {
                write(writer, "item", item);
            }
        } else if (value.getClass().isRecord()) {
            for (var component : value.getClass().getRecordComponents()) {
                write(writer, component.getName(), component.getAccessor().invoke(value));
            }
        } else if (value instanceof String
                || value instanceof Number
                || value instanceof Boolean
                || value instanceof UUID
                || value instanceof TemporalAccessor) {
            String text = value instanceof BigDecimal n ? n.toPlainString() : value.toString();
            for (int c : text.codePoints().toArray()) {
                if (!(c == 9
                        || c == 10
                        || c == 13
                        || (c >= 32 && c <= 0xD7FF)
                        || (c >= 0xE000 && c <= 0xFFFD)
                        || (c >= 0x10000 && c <= 0x10FFFF))) {
                    throw new IllegalArgumentException(
                            "El texto contiene caracteres no admitidos por XML 1.0");
                }
            }
            // Preservar CR al pasar por el parser XML (reintentos idempotentes).
            String[] segments = text.split("\r", -1);
            for (int i = 0; i < segments.length; i++) {
                if (i > 0) {
                    writer.writeEntityRef("#xD");
                }
                writer.writeCharacters(segments[i]);
            }
        } else {
            throw new IllegalArgumentException(
                    "Tipo no admitido para el contrato SQL: " + value.getClass().getSimpleName());
        }
        writer.writeEndElement();
    }
}
