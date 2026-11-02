package pe.edu.gui.entity;

/** Nombre, tipo y contenido de un archivo cuya lectura ya fue autorizada. */
public record ArchivoAdjunto(String name, String mime, byte[] content) {}
