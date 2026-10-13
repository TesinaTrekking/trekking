package com.example.service;

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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import com.example.dao.EquipamientoDAO;
import com.example.dao.RecorridoDAO;
import com.example.dao.RutaCheckpointDAO;
import com.example.dao.RutaDAO;
import com.example.dao.RutaEquipamientoDAO;
import com.example.database.ConexionDB;
import com.example.model.Checkpoint;
import com.example.model.Equipamiento;
import com.example.model.Recorrido;
import com.example.model.Ruta;
import com.example.model.RutaEquipamiento;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

public class FichaOperativaPDFService {

        private static final String REGULAR_FONT = "/com/example/fonts/OpenSans-Regular.ttf";
        private static final String BOLD_FONT = "/com/example/fonts/OpenSans-Bold.ttf";
        private static final float PAGE_MARGIN = 42;
        private static final float CONTENT_BOTTOM = 42;
        private static final float CELL_PADDING = 5;
        private static final float TABLE_LINE_HEIGHT = 11;
        private static final DateTimeFormatter FECHA_FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        private static final DateTimeFormatter FECHA_HORA_FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        private final RecorridoDAO recorridoDAO = new RecorridoDAO();
        private final RutaDAO rutaDAO = new RutaDAO();
        private final RutaCheckpointDAO rutaCheckpointDAO = new RutaCheckpointDAO();
        private final RutaEquipamientoDAO rutaEquipamientoDAO = new RutaEquipamientoDAO();

        public void exportar(int recorridoId, Path destino)
                        throws SQLException, IOException {
                FichaOperativa ficha = cargarFicha(recorridoId);
                LocalDateTime fechaGeneracion = LocalDateTime.now();
                Path destinoAbsoluto = destino.toAbsolutePath();
                Path directorio = destinoAbsoluto.getParent();
                if (directorio == null) {
                        throw new IOException("La ruta de destino no tiene una carpeta.");
                }

                String nombreTemporal = destinoAbsoluto.getFileName().toString();
                String prefijo = nombreTemporal.length() < 3
                                ? "pdf"
                                : nombreTemporal.substring(0, Math.min(nombreTemporal.length(), 30));
                Path temporal = Files.createTempFile(directorio, prefijo, ".tmp");
                try {
                        generarPDF(ficha, fechaGeneracion, temporal);
                        try {
                                Files.move(
                                                temporal,
                                                destinoAbsoluto,
                                                StandardCopyOption.ATOMIC_MOVE,
                                                StandardCopyOption.REPLACE_EXISTING);
                        } catch (AtomicMoveNotSupportedException e) {
                                Files.move(
                                                temporal,
                                                destinoAbsoluto,
                                                StandardCopyOption.REPLACE_EXISTING);
                        }
                } finally {
                        Files.deleteIfExists(temporal);
                }
        }

        private FichaOperativa cargarFicha(int recorridoId) throws SQLException {
                try (Connection conexion = ConexionDB.conectar()) {
                        conexion.setAutoCommit(false);
                        try {
                                Recorrido recorrido = recorridoDAO.obtenerPorId(
                                                conexion, recorridoId);
                                if (recorrido == null) {
                                        throw new SQLException(
                                                        "El recorrido seleccionado ya no existe.");
                                }

                                Ruta ruta = rutaDAO.obtenerPorId(
                                                conexion, recorrido.getRutaId());
                                if (ruta == null) {
                                        throw new SQLException(
                                                        "La ruta del recorrido ya no existe.");
                                }
                                List<CheckpointFicha> checkpoints = obtenerCheckpoints(conexion, ruta.getId());
                                List<ParticipanteFicha> participantes = obtenerParticipantes(conexion,
                                                recorrido.getId());
                                List<EquipamientoFicha> equipamiento = obtenerEquipamiento(conexion, ruta.getId());

                                conexion.commit();
                                System.out.println("=== DIAGNÓSTICO FICHA PDF ===");
                                System.out.println("Recorrido seleccionado: " + recorrido.getId());
                                System.out.println("Ruta del recorrido: " + recorrido.getRutaId());
                                System.out.println("Ruta cargada: " + ruta.getId()
                                                + " - " + ruta.getNombre());
                                System.out.println("Checkpoints encontrados: " + checkpoints.size());
                                System.out.println("Participantes encontrados: " + participantes.size());
                                System.out.println("Equipamientos encontrados: " + equipamiento.size());
                                return new FichaOperativa(
                                                recorrido,
                                                ruta,
                                                checkpoints,
                                                participantes,
                                                equipamiento);
                        } catch (SQLException e) {
                                conexion.rollback();
                                throw e;
                        }
                }
        }

        private List<CheckpointFicha> obtenerCheckpoints(
                        Connection conexion,
                        int rutaId) throws SQLException {
                List<CheckpointFicha> checkpoints = new ArrayList<>();
                List<Checkpoint> checkpointsRuta = rutaCheckpointDAO.obtenerPorRuta(conexion, rutaId);
                int orden = 1;
                for (Checkpoint checkpoint : checkpointsRuta) {
                        checkpoints.add(new CheckpointFicha(
                                        orden++,
                                        textoOpcional(checkpoint.getNombre()),
                                        textoOpcional(checkpoint.getHora()),
                                        String.format(
                                                        Locale.ROOT,
                                                        "%.5f, %.5f",
                                                        checkpoint.getLatitud(),
                                                        checkpoint.getLongitud()),
                                        textoOpcional(checkpoint.getDescripcion())));
                }
                return checkpoints;
        }

        private List<ParticipanteFicha> obtenerParticipantes(
                        Connection conexion,
                        int recorridoId) throws SQLException {
                String sql = """
                                SELECT c.id, c.dni, c.nombre, c.apellido,
                                       COALESCE(ce.nombre, c.contacto_emergencia_nombre)
                                           AS contacto_nombre,
                                       COALESCE(ce.telefono, c.contacto_emergencia_telefono)
                                           AS contacto_telefono,
                                       COALESCE(ce.relacion, c.contacto_emergencia_relacion)
                                           AS contacto_relacion
                                FROM recorrido_cliente rc
                                JOIN clientes c ON c.id = rc.cliente_id
                                LEFT JOIN contactos_emergencia ce ON ce.cliente_id = c.id
                                WHERE rc.recorrido_id = ?
                                ORDER BY c.apellido, c.nombre, c.id
                                """;
                List<ParticipanteFicha> participantes = new ArrayList<>();
                try (PreparedStatement statement = conexion.prepareStatement(sql)) {
                        statement.setInt(1, recorridoId);
                        try (ResultSet resultSet = statement.executeQuery()) {
                                while (resultSet.next()) {
                                        participantes.add(new ParticipanteFicha(
                                                        resultSet.getInt("id"),
                                                        textoOpcional(resultSet.getString("dni")),
                                                        textoOpcional(resultSet.getString("nombre")),
                                                        textoOpcional(resultSet.getString("apellido")),
                                                        textoOpcional(
                                                                        resultSet.getString("contacto_nombre")),
                                                        textoOpcional(
                                                                        resultSet.getString("contacto_relacion")),
                                                        textoOpcional(
                                                                        resultSet.getString("contacto_telefono"))));
                                }
                        }
                }
                return participantes;
        }

        private List<EquipamientoFicha> obtenerEquipamiento(
                        Connection conexion,
                        int rutaId) throws SQLException {
                List<EquipamientoFicha> requerimientos = new ArrayList<>();
                List<RutaEquipamiento> rutaRequerimientos = rutaEquipamientoDAO.obtenerPorRuta(conexion, rutaId);
                for (RutaEquipamiento requerimiento : rutaRequerimientos) {
                        Equipamiento equipamiento = EquipamientoDAO.obtenerPorId(
                                        conexion,
                                        requerimiento.getEquipamientoId());
                        requerimientos.add(new EquipamientoFicha(
                                        equipamiento == null
                                                        ? "Elemento no disponible (ID "
                                                                        + requerimiento.getEquipamientoId() + ")"
                                                        : textoOpcional(equipamiento.getNombre()),
                                        requerimiento.getCantidadRequerida()));
                }
                requerimientos.sort(
                                Comparator.comparing(
                                                EquipamientoFicha::nombre,
                                                String.CASE_INSENSITIVE_ORDER));
                return requerimientos;
        }

        private void generarPDF(
                        FichaOperativa ficha,
                        LocalDateTime fechaGeneracion,
                        Path destino) throws IOException {
                try (PDDocument documento = new PDDocument();
                                InputStream regularStream = getClass().getResourceAsStream(REGULAR_FONT);
                                InputStream boldStream = getClass().getResourceAsStream(BOLD_FONT)) {
                        if (regularStream == null || boldStream == null) {
                                throw new IOException(
                                                "No se encontraron las fuentes incluidas para el PDF.");
                        }
                        PDType0Font regular = PDType0Font.load(documento, regularStream);
                        PDType0Font bold = PDType0Font.load(documento, boldStream);
                        Renderizador renderizador = new Renderizador(documento, regular, bold);
                        renderizador.renderizar(ficha, fechaGeneracion);
                        renderizador.finalizarDocumento();
                        documento.save(destino.toFile());
                }
        }

        private static String textoOpcional(String texto) {
                return texto == null || texto.isBlank()
                                ? "No especificado"
                                : texto.trim();
        }

        private record FichaOperativa(
                        Recorrido recorrido,
                        Ruta ruta,
                        List<CheckpointFicha> checkpoints,
                        List<ParticipanteFicha> participantes,
                        List<EquipamientoFicha> equipamiento) {
        }

        private record CheckpointFicha(
                        int orden,
                        String nombre,
                        String hora,
                        String coordenadas,
                        String descripcion) {
        }

        private record ParticipanteFicha(
                        int id,
                        String dni,
                        String nombre,
                        String apellido,
                        String contactoNombre,
                        String contactoRelacion,
                        String contactoTelefono) {
        }

        private record EquipamientoFicha(String nombre, int cantidad) {
        }

        private static final class Renderizador {

                private static final float TITULO_SIZE = 19;
                private static final float SECTION_SIZE = 13;
                private static final float BODY_SIZE = 9;
                private static final float PAGE_NUMBER_SIZE = 8;

                private final PDDocument documento;
                private final PDType0Font regular;
                private final PDType0Font bold;
                private PDPageContentStream contenido;
                private PDPage pagina;
                private float y;

                private Renderizador(
                                PDDocument documento,
                                PDType0Font regular,
                                PDType0Font bold) {
                        this.documento = documento;
                        this.regular = regular;
                        this.bold = bold;
                }

                private void renderizar(
                                FichaOperativa ficha,
                                LocalDateTime fechaGeneracion) throws IOException {
                        nuevaPagina();
                        escribirTexto(
                                        PAGE_MARGIN,
                                        y,
                                        "Ficha operativa del recorrido",
                                        bold,
                                        TITULO_SIZE);
                        y -= 24;
                        escribirTexto(
                                        PAGE_MARGIN,
                                        y,
                                        "Generada el " + FECHA_HORA_FORMATO.format(fechaGeneracion),
                                        regular,
                                        BODY_SIZE);
                        y -= 22;

                        seccion("Datos del recorrido");
                        campo("Identificador", Integer.toString(ficha.recorrido().getId()));
                        campo("Ruta", textoOpcional(ficha.ruta().getNombre()));
                        campo(
                                        "Fecha",
                                        ficha.recorrido().getFecha() == null
                                                        ? "No especificado"
                                                        : FECHA_FORMATO.format(ficha.recorrido().getFecha()));
                        campo(
                                        "Horario",
                                        "Inicio: "
                                                        + valorHora(ficha.recorrido().getHoraInicio())
                                                        + "    Fin: "
                                                        + valorHora(ficha.recorrido().getHoraFin()));

                        seccion("Datos de la ruta");
                        campo(
                                        "Altitud máxima",
                                        String.format(
                                                        Locale.ROOT,
                                                        "%.0f m",
                                                        ficha.ruta().getAltitudMaxima()));
                        campo("Tipo de terreno", textoOpcional(ficha.ruta().getTipoTerreno()));
                        campo(
                                        "Dificultad técnica",
                                        textoOpcional(ficha.ruta().getDificultadTecnica()));
                        campo(
                                        "Dificultad física",
                                        textoOpcional(ficha.ruta().getDificultadFisica()));

                        seccion("Checkpoints");
                        tabla(
                                        List.of("#", "Checkpoint", "Horario", "Coordenadas",
                                                        "Descripción"),
                                        new float[] { 24, 95, 56, 95, 229 },
                                        ficha.checkpoints().stream()
                                                        .map(checkpoint -> List.of(
                                                                        Integer.toString(checkpoint.orden()),
                                                                        checkpoint.nombre(),
                                                                        checkpoint.hora(),
                                                                        checkpoint.coordenadas(),
                                                                        checkpoint.descripcion()))
                                                        .toList());
                        seccion("Participantes");
                        tabla(
                                        List.of(
                                                        "Identificador",
                                                        "Nombre y apellido",
                                                        "Contacto de emergencia",
                                                        "Relación",
                                                        "Teléfono de emergencia"),
                                        new float[] { 83, 112, 115, 76, 113 },
                                        ficha.participantes().stream()
                                                        .map(participante -> List.of(
                                                                        participante.dni().equals("No especificado")
                                                                                        ? "ID " + participante.id()
                                                                                        : "DNI " + participante.dni()
                                                                                                        + " (ID "
                                                                                                        + participante.id()
                                                                                                        + ")",
                                                                        participante.apellido() + ", "
                                                                                        + participante.nombre(),
                                                                        participante.contactoNombre(),
                                                                        participante.contactoRelacion(),
                                                                        participante.contactoTelefono()))
                                                        .toList());

                        seccion("Equipamiento requerido para la ruta");
                        System.out.println("PDF checkpoints: " + ficha.checkpoints().size());
                        System.out.println("PDF equipos: " + ficha.equipamiento().size());
                        tabla(
                                        List.of("Elemento", "Cantidad requerida"),
                                        new float[] { 399, 100 },
                                        ficha.equipamiento().stream()
                                                        .map(item -> List.of(
                                                                        item.nombre(),
                                                                        Integer.toString(item.cantidad())))
                                                        .toList());

                        seccion("Información médica");
                        parrafo(
                                        "Los adjuntos médicos no se incorporan ni se interpretan "
                                                        + "en esta ficha. Consultarlos por separado "
                                                        + "siguiendo el protocolo de privacidad.");
                }

                private void finalizarDocumento() throws IOException {
                        if (contenido != null) {
                                contenido.close();
                                contenido = null;
                        }
                        int total = documento.getNumberOfPages();
                        for (int i = 0; i < total; i++) {
                                PDPage paginaActual = documento.getPage(i);
                                String numero = "Página " + (i + 1) + " / " + total;
                                float anchoTexto = regular.getStringWidth(numero)
                                                * PAGE_NUMBER_SIZE / 1000;
                                try (PDPageContentStream pie = new PDPageContentStream(
                                                documento,
                                                paginaActual,
                                                PDPageContentStream.AppendMode.APPEND,
                                                true,
                                                true)) {
                                        pie.beginText();
                                        pie.setFont(regular, PAGE_NUMBER_SIZE);
                                        pie.newLineAtOffset(
                                                        paginaActual.getMediaBox().getWidth()
                                                                        - PAGE_MARGIN - anchoTexto,
                                                        22);
                                        pie.showText(numero);
                                        pie.endText();
                                }
                        }
                }

                private void seccion(String titulo) throws IOException {
                        System.out.printf(
                                        "SECCIÓN: %s | página: %d | y: %.1f%n",
                                        titulo,
                                        documento.getNumberOfPages(),
                                        y);
                        asegurarEspacio(28);
                        y -= 3;
                        escribirTexto(PAGE_MARGIN, y, titulo, bold, SECTION_SIZE);
                        y -= 8;
                        contenido.setStrokingColor(
                                        190 / 255f,
                                        198 / 255f,
                                        205 / 255f);
                        contenido.moveTo(PAGE_MARGIN, y);
                        contenido.lineTo(
                                        pagina.getMediaBox().getWidth() - PAGE_MARGIN,
                                        y);
                        contenido.stroke();
                        y -= 13;
                }

                private void campo(String etiqueta, String valor) throws IOException {
                        List<String> lineas = envolver(
                                        valor,
                                        regular,
                                        BODY_SIZE,
                                        pagina.getMediaBox().getWidth()
                                                        - 2 * PAGE_MARGIN - 127);
                        asegurarEspacio(16);
                        for (int i = 0; i < lineas.size(); i++) {
                                asegurarEspacio(13);
                                float base = y;
                                if (i == 0) {
                                        escribirTexto(
                                                        PAGE_MARGIN,
                                                        base,
                                                        etiqueta + ":",
                                                        bold,
                                                        BODY_SIZE);
                                }
                                escribirTexto(
                                                PAGE_MARGIN + 127,
                                                base,
                                                lineas.get(i),
                                                regular,
                                                BODY_SIZE);
                                y -= 13;
                        }
                        y -= 3;
                }

                private void parrafo(String texto) throws IOException {
                        List<String> lineas = envolver(
                                        texto,
                                        regular,
                                        BODY_SIZE,
                                        pagina.getMediaBox().getWidth() - 2 * PAGE_MARGIN);
                        for (String linea : lineas) {
                                asegurarEspacio(13);
                                escribirTexto(PAGE_MARGIN, y, linea, regular, BODY_SIZE);
                                y -= 13;
                        }
                }

                private void tabla(
                                List<String> encabezados,
                                float[] anchos,
                                List<List<String>> filas) throws IOException {
                        float anchoDisponible = pagina.getMediaBox().getWidth() - 2 * PAGE_MARGIN;
                        float sumaAnchos = 0;
                        for (float ancho : anchos) {
                                sumaAnchos += ancho;
                        }
                        if (Math.abs(sumaAnchos - anchoDisponible) > 1) {
                                float factorEscala = anchoDisponible / sumaAnchos;
                                for (int i = 0; i < anchos.length; i++) {
                                        anchos[i] *= factorEscala;
                                }
                        }

                        dibujarEncabezado(encabezados, anchos);
                        System.out.println("Tabla: " + encabezados.get(0)
                                        + " | filas recibidas: " + filas.size());
                        if (filas.isEmpty()) {
                                dibujarFila(
                                                List.of("No hay datos disponibles"),
                                                new float[] { anchoDisponible },
                                                BODY_SIZE,
                                                15);
                                return;
                        }

                        for (List<String> fila : filas) {
                                System.out.println("Dibujando fila: " + fila);
                                List<List<String>> celdas = new ArrayList<>();
                                int lineasRestantes = 1;
                                for (int i = 0; i < fila.size(); i++) {
                                        List<String> lineas = envolver(
                                                        fila.get(i),
                                                        regular,
                                                        BODY_SIZE,
                                                        anchos[i] - 2 * CELL_PADDING);
                                        celdas.add(lineas);
                                        lineasRestantes = Math.max(
                                                        lineasRestantes,
                                                        lineas.size());
                                }

                                int[] posiciones = new int[celdas.size()];
                                while (lineasRestantes > 0) {
                                        float espacio = y - CONTENT_BOTTOM - 2 * CELL_PADDING;
                                        int lineasPagina = (int) Math.floor(
                                                        espacio / TABLE_LINE_HEIGHT);
                                        if (lineasPagina < 1) {
                                                nuevaPagina();
                                                dibujarEncabezado(encabezados, anchos);
                                                continue;
                                        }

                                        int cantidadLineas = Math.min(
                                                        lineasRestantes,
                                                        lineasPagina);
                                        float altura = cantidadLineas * TABLE_LINE_HEIGHT
                                                        + 2 * CELL_PADDING;
                                        dibujarSegmentoFila(
                                                        celdas,
                                                        posiciones,
                                                        anchos,
                                                        cantidadLineas,
                                                        altura,
                                                        lineasRestantes > cantidadLineas);
                                        y -= altura;

                                        for (int i = 0; i < posiciones.length; i++) {
                                                posiciones[i] = Math.min(
                                                                celdas.get(i).size(),
                                                                posiciones[i] + cantidadLineas);
                                        }
                                        lineasRestantes = maximoLineasRestantes(
                                                        celdas,
                                                        posiciones);
                                        if (lineasRestantes > 0) {
                                                nuevaPagina();
                                                dibujarEncabezado(encabezados, anchos);
                                        }
                                }
                        }
                        y -= 8;
                }

                private int maximoLineasRestantes(
                                List<List<String>> celdas,
                                int[] posiciones) {
                        int maximo = 0;
                        for (int i = 0; i < celdas.size(); i++) {
                                maximo = Math.max(
                                                maximo,
                                                celdas.get(i).size() - posiciones[i]);
                        }
                        return maximo;
                }

                private void dibujarEncabezado(
                                List<String> encabezados,
                                float[] anchos) throws IOException {
                        List<List<String>> celdas = new ArrayList<>();
                        int maxLineas = 1;
                        for (int i = 0; i < encabezados.size(); i++) {
                                List<String> lineas = envolver(
                                                encabezados.get(i),
                                                bold,
                                                8,
                                                anchos[i] - 2 * CELL_PADDING);
                                celdas.add(lineas);
                                maxLineas = Math.max(maxLineas, lineas.size());
                        }
                        float altura = maxLineas * 10 + 2 * CELL_PADDING;
                        asegurarEspacio(altura + TABLE_LINE_HEIGHT + 2 * CELL_PADDING);
                        System.out.printf(
                                        "ENCABEZADO: %s | página: %d | y: %.1f%n",
                                        encabezados.get(0),
                                        documento.getNumberOfPages(),
                                        y);
                        dibujarFondoYBordes(anchos, altura, 232, 237, 241);
                        float x = PAGE_MARGIN + CELL_PADDING;
                        for (int i = 0; i < celdas.size(); i++) {
                                for (int j = 0; j < celdas.get(i).size(); j++) {
                                        escribirTexto(
                                                        x,
                                                        y - CELL_PADDING - 8 - j * 10,
                                                        celdas.get(i).get(j),
                                                        bold,
                                                        8);
                                }
                                x += anchos[i];
                        }
                        y -= altura;
                }

                private void dibujarSegmentoFila(
                                List<List<String>> celdas,
                                int[] posiciones,
                                float[] anchos,
                                int cantidadLineas,
                                float altura,
                                boolean continuacion) throws IOException {
                        dibujarFondoYBordes(anchos, altura, 255, 255, 255);
                        float x = PAGE_MARGIN + CELL_PADDING;
                        for (int i = 0; i < celdas.size(); i++) {
                                boolean mostrarContinuacion = continuacion && i == 0 && posiciones[i] > 0;
                                if (mostrarContinuacion) {
                                        escribirTexto(
                                                        x,
                                                        y - CELL_PADDING - BODY_SIZE,
                                                        "continúa",
                                                        regular,
                                                        BODY_SIZE);
                                } else {
                                        for (int j = 0; j < cantidadLineas; j++) {
                                                int indice = posiciones[i] + j;
                                                if (indice >= celdas.get(i).size()) {
                                                        break;
                                                }
                                                System.out.printf(
                                                                "CELDA | página=%d | columna=%d | x=%.1f | y=%.1f | texto='%s'%n",
                                                                documento.getNumberOfPages(),
                                                                i,
                                                                x,
                                                                y - CELL_PADDING - BODY_SIZE - j * TABLE_LINE_HEIGHT,
                                                                celdas.get(i).get(indice));
                                                escribirTexto(
                                                                x,
                                                                y - CELL_PADDING - BODY_SIZE
                                                                                - j * TABLE_LINE_HEIGHT,
                                                                celdas.get(i).get(indice),
                                                                regular,
                                                                BODY_SIZE);
                                        }
                                }
                                x += anchos[i];
                        }
                }

                private void dibujarFila(
                                List<String> valores,
                                float[] anchos,
                                float tamano,
                                float altura) throws IOException {
                        dibujarFondoYBordes(anchos, altura, 255, 255, 255);
                        escribirTexto(
                                        PAGE_MARGIN + CELL_PADDING,
                                        y - CELL_PADDING - tamano,
                                        valores.get(0),
                                        regular,
                                        tamano);
                        y -= altura;
                }

                private void dibujarFondoYBordes(
                                float[] anchos,
                                float altura,
                                int rojo,
                                int verde,
                                int azul) throws IOException {
                        float x = PAGE_MARGIN;
                        contenido.setNonStrokingColor(
                                        rojo / 255f,
                                        verde / 255f,
                                        azul / 255f);
                        contenido.setStrokingColor(
                                        205 / 255f,
                                        211 / 255f,
                                        216 / 255f);
                        for (float ancho : anchos) {
                                contenido.addRect(x, y - altura, ancho, altura);
                                contenido.fillAndStroke();
                                x += ancho;
                        }
                }

                private void asegurarEspacio(float altura) throws IOException {
                        if (y - altura < CONTENT_BOTTOM) {
                                nuevaPagina();
                        }
                }

                private void nuevaPagina() throws IOException {
                        if (contenido != null) {
                                contenido.close();
                        }
                        pagina = new PDPage(PDRectangle.A4);
                        documento.addPage(pagina);
                        contenido = new PDPageContentStream(documento, pagina);
                        y = pagina.getMediaBox().getHeight() - PAGE_MARGIN;
                }

                private void escribirTexto(
                                float x,
                                float y,
                                String texto,
                                PDType0Font fuente,
                                float tamano) throws IOException {
                        contenido.beginText();
                        contenido.setNonStrokingColor(0, 0, 0); // Negro explícito
                        contenido.setFont(fuente, tamano);
                        contenido.newLineAtOffset(x, y);
                        contenido.showText(texto);
                        contenido.endText();
                }

                private List<String> envolver(
                                String texto,
                                PDType0Font fuente,
                                float tamano,
                                float anchoMaximo) throws IOException {
                        List<String> lineas = new ArrayList<>();
                        String valor = texto == null ? "No especificado" : texto;
                        for (String parrafo : valor.split("\\R", -1)) {
                                String linea = "";
                                for (String palabra : parrafo.trim().split("\\s+")) {
                                        if (palabra.isEmpty()) {
                                                continue;
                                        }
                                        if (ancho(palabra, fuente, tamano) > anchoMaximo) {
                                                if (!linea.isEmpty()) {
                                                        lineas.add(linea);
                                                        linea = "";
                                                }
                                                for (String segmento : dividirPalabra(
                                                                palabra,
                                                                fuente,
                                                                tamano,
                                                                anchoMaximo)) {
                                                        lineas.add(segmento);
                                                }
                                                continue;
                                        }
                                        String candidata = linea.isEmpty()
                                                        ? palabra
                                                        : linea + " " + palabra;
                                        if (ancho(candidata, fuente, tamano) > anchoMaximo) {
                                                lineas.add(linea);
                                                linea = palabra;
                                        } else {
                                                linea = candidata;
                                        }
                                }
                                lineas.add(linea);
                        }
                        return lineas.isEmpty() ? List.of("") : lineas;
                }

                private List<String> dividirPalabra(
                                String palabra,
                                PDType0Font fuente,
                                float tamano,
                                float anchoMaximo) throws IOException {
                        List<String> segmentos = new ArrayList<>();
                        StringBuilder segmento = new StringBuilder();
                        for (int indice = 0; indice < palabra.length();) {
                                int punto = palabra.codePointAt(indice);
                                String candidato = segmento.toString()
                                                + new String(Character.toChars(punto));
                                if (!segmento.isEmpty()
                                                && ancho(candidato, fuente, tamano) > anchoMaximo) {
                                        segmentos.add(segmento.toString());
                                        segmento.setLength(0);
                                }
                                segmento.appendCodePoint(punto);
                                indice += Character.charCount(punto);
                        }
                        if (!segmento.isEmpty()) {
                                segmentos.add(segmento.toString());
                        }
                        return segmentos;
                }

                private float ancho(
                                String texto,
                                PDType0Font fuente,
                                float tamano) throws IOException {
                        return fuente.getStringWidth(texto) * tamano / 1000;
                }

                private String valorHora(java.time.LocalTime hora) {
                        return hora == null ? "No especificado" : hora.toString();
                }
        }
}
