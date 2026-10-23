package com.example.service;

import com.example.database.ConexionDB;
import com.example.model.Salida;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

/** Exports the current operational snapshot persisted for a Salida. */
public class FichaOperativaPDFService {
    private static final String REGULAR_FONT = "/com/example/fonts/OpenSans-Regular.ttf";
    private static final String BOLD_FONT = "/com/example/fonts/OpenSans-Bold.ttf";
    private static final float MARGIN = 42;
    private static final float BOTTOM = 42;
    private static final float LINE_HEIGHT = 13;

    public String nombreArchivo(Salida salida) {
        String nombreRuta = salida.rutaNombre().trim().replaceAll("[^\\p{L}\\p{N}._-]+", "-")
                .replaceAll("^-+|-+$", "");
        if (nombreRuta.isBlank()) nombreRuta = "Ruta";
        return nombreRuta + "-" + salida.fecha() + "-salida-" + salida.id() + ".pdf";
    }

    public void exportar(int salidaId, Path destino) throws SQLException, IOException {
        Ficha ficha = cargarFicha(salidaId);
        Path absoluto = destino.toAbsolutePath();
        Path directorio = absoluto.getParent();
        if (directorio == null) throw new IOException("La ruta de destino no tiene una carpeta.");
        Path temporal = Files.createTempFile(directorio, "ficha-", ".tmp");
        try {
            generarPDF(ficha, temporal);
            try {
                Files.move(temporal, absoluto, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporal, absoluto, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporal);
        }
    }

    private Ficha cargarFicha(int id) throws SQLException {
        try (Connection c = ConexionDB.conectar()) {
            Salida salida;
            try (PreparedStatement ps = c.prepareStatement("SELECT * FROM salidas WHERE id = ?")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new SQLException("La Salida seleccionada ya no existe.");
                    salida = new Salida(rs.getInt("id"), rs.getInt("ruta_id"), LocalDate.parse(rs.getString("fecha")),
                            rs.getString("estado"), rs.getString("ruta_nombre"), rs.getDouble("ruta_altitud_maxima"),
                            rs.getString("ruta_tipo_terreno"), rs.getString("ruta_dificultad_tecnica"),
                            rs.getString("ruta_dificultad_fisica"));
                }
            }
            return new Ficha(salida, cargarCheckpoints(c, id), cargarEquipamiento(c, id), cargarParticipantes(c, id));
        }
    }

    private List<String> cargarCheckpoints(Connection c, int id) throws SQLException {
        List<String> filas = new ArrayList<>();
        String sql = "SELECT nombre,hora,latitud,longitud,altitud,descripcion FROM salida_checkpoints WHERE salida_id=? ORDER BY orden";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                int n = 1;
                while (rs.next()) filas.add(String.format(Locale.ROOT, "%d | %s | %s | %.5f, %.5f | %.0f m | %s", n++,
                        valor(rs.getString("nombre")), valor(rs.getString("hora")), rs.getDouble("latitud"),
                        rs.getDouble("longitud"), rs.getDouble("altitud"), valor(rs.getString("descripcion"))));
            }
        }
        return filas;
    }

    private List<String> cargarEquipamiento(Connection c, int id) throws SQLException {
        List<String> filas = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement("SELECT nombre_copia,categoria_copia,cantidad FROM salida_equipamiento WHERE salida_id=? ORDER BY nombre_copia COLLATE NOCASE")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) filas.add(valor(rs.getString("nombre_copia")) + " | " + valor(rs.getString("categoria_copia"))
                        + " | " + rs.getInt("cantidad"));
            }
        }
        return filas;
    }

    private List<String> cargarParticipantes(Connection c, int id) throws SQLException {
        List<String> filas = new ArrayList<>();
        String sql = "SELECT dni_copia,nombre_copia,apellido_copia,telefono_copia,emergencia_nombre_copia,emergencia_telefono_copia,emergencia_relacion_copia FROM salida_participantes WHERE salida_id=? ORDER BY apellido_copia COLLATE NOCASE,nombre_copia COLLATE NOCASE";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) filas.add(String.join(" | ", valor(rs.getString("dni_copia")),
                        valor(rs.getString("apellido_copia")) + ", " + valor(rs.getString("nombre_copia")),
                        valor(rs.getString("telefono_copia")), valor(rs.getString("emergencia_nombre_copia")),
                        valor(rs.getString("emergencia_telefono_copia")), valor(rs.getString("emergencia_relacion_copia"))));
            }
        }
        return filas;
    }

    private void generarPDF(Ficha ficha, Path destino) throws IOException {
        try (PDDocument documento = new PDDocument();
             InputStream regularStream = getClass().getResourceAsStream(REGULAR_FONT);
             InputStream boldStream = getClass().getResourceAsStream(BOLD_FONT)) {
            if (regularStream == null || boldStream == null) throw new IOException("No se encontraron las fuentes incluidas para el PDF.");
            PDType0Font regular = PDType0Font.load(documento, regularStream);
            PDType0Font bold = PDType0Font.load(documento, boldStream);
            Render render = new Render(documento, regular, bold);
            Salida s = ficha.salida();
            render.title("Ficha operativa de la salida");
            render.section("Datos de la salida");
            render.line("Identificador: " + s.id());
            render.line("Fecha: " + s.fecha());
            render.line("Estado: " + valor(s.estado()));
            render.section("Datos técnicos de la Ruta");
            render.line("Nombre: " + valor(s.rutaNombre()));
            render.line(String.format(Locale.ROOT, "Altitud máxima: %.0f m", s.rutaAltitudMaxima()));
            render.line("Tipo de terreno: " + valor(s.rutaTipoTerreno()));
            render.line("Dificultad técnica: " + valor(s.rutaDificultadTecnica()));
            render.line("Dificultad física: " + valor(s.rutaDificultadFisica()));
            render.section("Checkpoints");
            render.line("Orden | nombre | horario | coordenadas | altitud | descripción");
            render.rows(ficha.checkpoints(), "Sin checkpoints registrados");
            render.section("Equipamiento");
            render.line("Elemento | categoría | cantidad");
            render.rows(ficha.equipamiento(), "Sin equipamiento asignado");
            render.section("Participantes");
            render.line("DNI | nombre y apellido | teléfono | contacto de emergencia | teléfono | relación");
            render.rows(ficha.participantes(), "Sin participantes");
            render.section("Información médica");
            render.line("Los adjuntos médicos no se incorporan ni interpretan en esta ficha.");
            render.finish();
            documento.save(destino.toFile());
        }
    }

    private static String valor(String valor) { return valor == null || valor.isBlank() ? "No especificado" : valor.trim(); }
    private record Ficha(Salida salida, List<String> checkpoints, List<String> equipamiento, List<String> participantes) {}

    private static final class Render {
        private final PDDocument doc;
        private final PDType0Font regular;
        private final PDType0Font bold;
        private PDPageContentStream stream;
        private PDPage page;
        private float y;

        Render(PDDocument doc, PDType0Font regular, PDType0Font bold) { this.doc = doc; this.regular = regular; this.bold = bold; }
        void title(String text) throws IOException { ensurePage(); write(text, bold, 18); y -= 24; }
        void section(String text) throws IOException { ensure(26); y -= 5; write(text, bold, 12); y -= 16; }
        void line(String text) throws IOException { for (String part : wrap(text, regular, 9, page.getMediaBox().getWidth() - 2 * MARGIN)) { ensure(LINE_HEIGHT); write(part, regular, 9); y -= LINE_HEIGHT; } }
        void rows(List<String> rows, String empty) throws IOException { if (rows.isEmpty()) line(empty); else for (String row : rows) line(row); }
        void finish() throws IOException { if (stream != null) stream.close(); }
        private void ensure(float height) throws IOException { if (page == null || y - height < BOTTOM) { if (stream != null) stream.close(); page = new PDPage(PDRectangle.A4); doc.addPage(page); stream = new PDPageContentStream(doc, page); y = page.getMediaBox().getHeight() - MARGIN; } }
        private void ensurePage() throws IOException { ensure(1); }
        private void write(String text, PDType0Font font, float size) throws IOException { stream.beginText(); stream.setFont(font, size); stream.newLineAtOffset(MARGIN, y); stream.showText(text); stream.endText(); }
        private List<String> wrap(String text, PDType0Font font, float size, float max) throws IOException {
            List<String> result = new ArrayList<>(); StringBuilder line = new StringBuilder();
            for (String word : text.split("\\s+")) {
                String candidate = line.isEmpty() ? word : line + " " + word;
                if (!line.isEmpty() && font.getStringWidth(candidate) * size / 1000 > max) { result.add(line.toString()); line.setLength(0); }
                if (!line.isEmpty()) line.append(' '); line.append(word);
            }
            if (!line.isEmpty()) result.add(line.toString());
            return result;
        }
    }
}
