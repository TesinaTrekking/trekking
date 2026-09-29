package com.example.controller;

import com.example.dao.RutaDAO;
import com.example.model.Ruta;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;

import java.util.function.UnaryOperator;

public class RutaFormController {

    private static final int DECIMALES_COORDENADAS = 4;

    private final RutaDAO rutaDAO = new RutaDAO();

    private Ruta rutaEnEdicion;

    @FXML
    private Label tituloForm;

    @FXML
    private TextField nombreField;

    @FXML
    private TextField latitudInicialField;

    @FXML
    private TextField longitudInicialField;

    @FXML
    private TextField latitudFinalField;

    @FXML
    private TextField longitudFinalField;

    @FXML
    private TextField altitudMaximaField;

    @FXML
    private ComboBox<String> tipoTerrenoCombo;

    @FXML
    private ComboBox<String> dificultadTecnicaCombo;

    @FXML
    private ComboBox<String> dificultadFisicaCombo;

    @FXML
    public void initialize() {
        configurarCampos();

        tipoTerrenoCombo.getItems().addAll(
                "Rocoso",
                "Boscoso",
                "Sendero",
                "Mixto"
        );

        dificultadTecnicaCombo.getItems().addAll(
                "Baja",
                "Media",
                "Alta"
        );

        dificultadFisicaCombo.getItems().addAll(
                "Baja",
                "Media",
                "Alta"
        );

        rutaEnEdicion = null;
    }

    /**
     * Permite cargar una ruta cuando el formulario se utiliza
     * para editar. El DashboardController podrá utilizar este
     * método al integrar la navegación entre módulos.
     */
    public void setRutaEnEdicion(Ruta ruta) {
        this.rutaEnEdicion = ruta;

        if (ruta != null) {
            tituloForm.setText("Editar ruta");
            cargarRuta(ruta);
        } else {
            tituloForm.setText("Nueva ruta");
        }
    }

    @FXML
    private void volverListado() {
        // La navegación se conectará desde el dashboard.
    }

    @FXML
    private void guardarRuta() {

        String nombre = Ruta.formatearNombre(nombreField.getText());

        if (!validarNombre(nombre)) {
            return;
        }

        if (!validarNombreDuplicado(nombre)) {
            return;
        }

        if (tipoTerrenoCombo.getValue() == null) {
            mostrarError(
                    "Tipo de terreno",
                    "Selecciona un tipo de terreno."
            );
            return;
        }

        if (dificultadTecnicaCombo.getValue() == null) {
            mostrarError(
                    "Dificultad técnica",
                    "Selecciona una dificultad técnica."
            );
            return;
        }

        if (dificultadFisicaCombo.getValue() == null) {
            mostrarError(
                    "Dificultad física",
                    "Selecciona una dificultad física."
            );
            return;
        }

        Double latitudInicial = obtenerCoordenada(
                latitudInicialField,
                "Latitud inicial",
                -90,
                90
        );

        if (latitudInicial == null) {
            return;
        }

        Double longitudInicial = obtenerCoordenada(
                longitudInicialField,
                "Longitud inicial",
                -180,
                180
        );

        if (longitudInicial == null) {
            return;
        }

        Double latitudFinal = obtenerCoordenada(
                latitudFinalField,
                "Latitud final",
                -90,
                90
        );

        if (latitudFinal == null) {
            return;
        }

        Double longitudFinal = obtenerCoordenada(
                longitudFinalField,
                "Longitud final",
                -180,
                180
        );

        if (longitudFinal == null) {
            return;
        }

        Double altitudMaxima = obtenerAltitud();

        if (altitudMaxima == null) {
            return;
        }

        String tipoTerreno = tipoTerrenoCombo.getValue();
        String dificultadTecnica = dificultadTecnicaCombo.getValue();
        String dificultadFisica = dificultadFisicaCombo.getValue();

        Ruta ruta = rutaEnEdicion == null
                ? new Ruta(
                        nombre,
                        latitudInicial,
                        longitudInicial,
                        latitudFinal,
                        longitudFinal,
                        altitudMaxima,
                        tipoTerreno,
                        dificultadTecnica,
                        dificultadFisica
                )
                : new Ruta(
                        rutaEnEdicion.getId(),
                        nombre,
                        latitudInicial,
                        longitudInicial,
                        latitudFinal,
                        longitudFinal,
                        altitudMaxima,
                        tipoTerreno,
                        dificultadTecnica,
                        dificultadFisica
                );

        boolean resultado;

        if (rutaEnEdicion == null) {
            resultado = rutaDAO.insertar(ruta);
        } else {
            resultado = rutaDAO.actualizar(ruta);
        }

        if (resultado) {
            mostrarInformacion(
                    "Ruta guardada",
                    rutaEnEdicion == null
                            ? "La ruta se guardó correctamente."
                            : "La ruta se actualizó correctamente."
            );

            limpiarFormulario();
        } else {
            mostrarError(
                    "Error",
                    "No se pudo guardar la ruta."
            );
        }
    }

    private boolean validarNombre(String nombre) {

        if (nombre.isBlank()) {
            mostrarError(
                    "Nombre de ruta",
                    "El nombre de la ruta es obligatorio."
            );
            nombreField.requestFocus();
            return false;
        }

        if (!Ruta.nombreValido(nombre)) {
            mostrarError(
                    "Nombre de ruta",
                    "El nombre solamente puede contener letras, "
                            + "números y espacios."
            );
            nombreField.requestFocus();
            return false;
        }

        return true;
    }

    private boolean validarNombreDuplicado(String nombre) {

        int idExcluido = rutaEnEdicion == null
                ? -1
                : rutaEnEdicion.getId();

        if (rutaDAO.existeNombre(nombre, idExcluido)) {
            mostrarError(
                    "Nombre duplicado",
                    "Ya existe una ruta con ese nombre."
            );
            nombreField.requestFocus();
            return false;
        }

        return true;
    }

    private Double obtenerCoordenada(
            TextField campo,
            String nombreCampo,
            double minimo,
            double maximo) {

        String texto = campo.getText().trim();

        if (texto.isEmpty()) {
            mostrarError(
                    nombreCampo,
                    "Este campo es obligatorio."
            );
            campo.requestFocus();
            return null;
        }

        try {
            double valor = Double.parseDouble(
                    texto.replace(',', '.')
            );

            if (!Double.isFinite(valor)) {
                mostrarError(
                        nombreCampo,
                        "Ingresa un valor numérico válido."
                );
                campo.requestFocus();
                return null;
            }

            if (valor < minimo || valor > maximo) {
                mostrarError(
                        nombreCampo,
                        "El valor debe estar entre "
                                + minimo
                                + " y "
                                + maximo
                                + "."
                );
                campo.requestFocus();
                return null;
            }

            return valor;

        } catch (NumberFormatException e) {
            mostrarError(
                    nombreCampo,
                    "Ingresa un valor numérico válido."
            );
            campo.requestFocus();
            return null;
        }
    }

    private Double obtenerAltitud() {

        String texto = altitudMaximaField.getText().trim();

        if (texto.isEmpty()) {
            mostrarError(
                    "Altitud máxima",
                    "La altitud máxima es obligatoria."
            );
            altitudMaximaField.requestFocus();
            return null;
        }

        try {
            double valor = Double.parseDouble(
                    texto.replace(',', '.')
            );

            if (!Double.isFinite(valor)) {
                mostrarError(
                        "Altitud máxima",
                        "Ingresa un valor numérico válido."
                );
                altitudMaximaField.requestFocus();
                return null;
            }

            if (valor < 0) {
                mostrarError(
                        "Altitud máxima",
                        "La altitud no puede ser negativa."
                );
                altitudMaximaField.requestFocus();
                return null;
            }

            return valor;

        } catch (NumberFormatException e) {
            mostrarError(
                    "Altitud máxima",
                    "Ingresa un valor numérico válido."
            );
            altitudMaximaField.requestFocus();
            return null;
        }
    }

    private void configurarCampos() {

        UnaryOperator<TextFormatter.Change> filtroNombre =
                change -> {

                    String nuevoTexto =
                            change.getControlNewText();

                    if (nuevoTexto.length() > 100) {
                        return null;
                    }

                    if (nuevoTexto.matches(
                            "[\\p{L}\\p{N} ]*")) {
                        return change;
                    }

                    return null;
                };

        nombreField.setTextFormatter(
                new TextFormatter<>(filtroNombre)
        );

        nombreField.setTooltip(
                new Tooltip(
                        "Ingresa el nombre de la ruta."
                )
        );

        configurarCampoCoordenada(
                latitudInicialField,
                "Ejemplo: -31.4201"
        );

        configurarCampoCoordenada(
                longitudInicialField,
                "Ejemplo: -64.1888"
        );

        configurarCampoCoordenada(
                latitudFinalField,
                "Ejemplo: -31.4100"
        );

        configurarCampoCoordenada(
                longitudFinalField,
                "Ejemplo: -64.1800"
        );

        configurarCampoNumerico(
                altitudMaximaField,
                "Ejemplo: 1250"
        );
    }

    private void configurarCampoCoordenada(
            TextField campo,
            String textoAyuda) {

        UnaryOperator<TextFormatter.Change> filtro =
                change -> {

                    String nuevoTexto =
                            change.getControlNewText();

                    if (nuevoTexto.isEmpty()) {
                        return change;
                    }

                    if (!nuevoTexto.matches(
                            "-?\\d*[\\.,]?\\d*")) {
                        return null;
                    }

                    String normalizado =
                            nuevoTexto.replace(',', '.');

                    int posicionPunto =
                            normalizado.indexOf('.');

                    if (posicionPunto >= 0) {

                        int decimales =
                                normalizado.length()
                                        - posicionPunto
                                        - 1;

                        if (decimales >
                                DECIMALES_COORDENADAS) {
                            return null;
                        }
                    }

                    return change;
                };

        campo.setTextFormatter(
                new TextFormatter<>(filtro)
        );

        campo.setTooltip(
                new Tooltip(textoAyuda)
        );
    }

    private void configurarCampoNumerico(
            TextField campo,
            String textoAyuda) {

        UnaryOperator<TextFormatter.Change> filtro =
                change -> {

                    String nuevoTexto =
                            change.getControlNewText();

                    if (nuevoTexto.isEmpty()) {
                        return change;
                    }

                    if (nuevoTexto.matches(
                            "-?\\d*[\\.,]?\\d*")) {
                        return change;
                    }

                    return null;
                };

        campo.setTextFormatter(
                new TextFormatter<>(filtro)
        );

        campo.setTooltip(
                new Tooltip(textoAyuda)
        );
    }

    private void cargarRuta(Ruta ruta) {

        nombreField.setText(ruta.getNombre());

        latitudInicialField.setText(
                formatearNumero(
                        ruta.getLatitudInicial()
                )
        );

        longitudInicialField.setText(
                formatearNumero(
                        ruta.getLongitudInicial()
                )
        );

        latitudFinalField.setText(
                formatearNumero(
                        ruta.getLatitudFinal()
                )
        );

        longitudFinalField.setText(
                formatearNumero(
                        ruta.getLongitudFinal()
                )
        );

        altitudMaximaField.setText(
                formatearNumero(
                        ruta.getAltitudMaxima()
                )
        );

        tipoTerrenoCombo.setValue(
                ruta.getTipoTerreno()
        );

        dificultadTecnicaCombo.setValue(
                ruta.getDificultadTecnica()
        );

        dificultadFisicaCombo.setValue(
                ruta.getDificultadFisica()
        );
    }

    private String formatearNumero(double valor) {
        return String.valueOf(valor);
    }

    private void limpiarFormulario() {

        rutaEnEdicion = null;

        tituloForm.setText("Nueva ruta");

        nombreField.clear();
        latitudInicialField.clear();
        longitudInicialField.clear();
        latitudFinalField.clear();
        longitudFinalField.clear();
        altitudMaximaField.clear();

        tipoTerrenoCombo.getSelectionModel()
                .clearSelection();

        dificultadTecnicaCombo.getSelectionModel()
                .clearSelection();

        dificultadFisicaCombo.getSelectionModel()
                .clearSelection();
    }

    private void mostrarError(
            String titulo,
            String mensaje) {

        Alert alerta = new Alert(
                Alert.AlertType.ERROR
        );

        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }

    private void mostrarInformacion(
            String titulo,
            String mensaje) {

        Alert alerta = new Alert(
                Alert.AlertType.INFORMATION
        );

        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
}
