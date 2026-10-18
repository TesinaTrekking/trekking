
package com.example.controller;

import com.example.dao.CheckpointDAO;
import com.example.model.Checkpoint;
import com.example.util.AlertUtils;
import com.example.util.FormValidation;
import com.example.util.NavigationShell;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Platform;
import javafx.css.PseudoClass; // NUEVO
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip; // NUEVO
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration; // NUEVO

/**
 * Formulario en-contenedor de creación/edición de checkpoint.
 *
 * Estructura en {@code checkpoint-form.fxml}. Se navega con
 * {@link NavigationShell#setRoot}, igual que equipamiento y rutas.
 */
public class CheckpointFormController implements Initializable {

    private static final Logger LOGGER =
            Logger.getLogger(CheckpointFormController.class.getName());

    private static final double LAT_MIN = -90.0;
    private static final double LAT_MAX = 90.0;

    private static final double LON_MIN = -180.0;
    private static final double LON_MAX = 180.0;

    private static final DateTimeFormatter HORA_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm")
                    .withLocale(Locale.ROOT);

    private static final String DECIMAL_PATTERN =
            "-?\\d*([.,]\\d*)?";

    private static final String HORA_PATTERN =
            "\\d{0,2}:?\\d{0,2}";

    @FXML
    private Label tituloLabel;

    @FXML
    private Label subtituloLabel;

    @FXML
    private HBox errorBanner;

    @FXML
    private Label errorText;

    @FXML
    private TextField txtNombre;

    @FXML
    private TextField txtHora;

    @FXML
    private TextField txtLatitud;

    @FXML
    private TextField txtLongitud;

    @FXML
    private TextArea txtDescripcion;

    @FXML
    private Button guardarButton;

    private final CheckpointDAO checkpointDAO = new CheckpointDAO();
    private Checkpoint checkpointEnEdicion;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        txtLatitud.setTextFormatter(createDecimalTextFormatter(txtLatitud)); // CAMBIADO
        txtLongitud.setTextFormatter(createDecimalTextFormatter(txtLongitud)); // CAMBIADO
        txtHora.setTextFormatter(createHoraTextFormatter(txtHora)); // CAMBIADO

        // CAMBIADO: limpiar los estados visuales y luego recalcular la validación.
        txtNombre.textProperty().addListener(
                (obs, oldValue, newValue) -> limpiarError(txtNombre));
        txtHora.textProperty().addListener(
                (obs, oldValue, newValue) -> limpiarError(txtHora));
        txtLatitud.textProperty().addListener(
                (obs, oldValue, newValue) -> limpiarError(txtLatitud));
        txtLongitud.textProperty().addListener(
                (obs, oldValue, newValue) -> limpiarError(txtLongitud));

        configurarTooltips(); // NUEVO
        configurarValidacionesEnTiempoReal();

        Platform.runLater(() -> {
            txtNombre.requestFocus();
            if (checkpointEnEdicion != null) {
                txtNombre.selectAll();
            }
        });
    }

    public void cargarCheckpoint(Checkpoint checkpoint) {
        this.checkpointEnEdicion = checkpoint;
        boolean isEditing = checkpoint != null;

        tituloLabel.setText(isEditing ? "Editar Checkpoint" : "Nuevo Checkpoint");
        subtituloLabel.setText(isEditing
                ? "Modifique los atributos del punto de control de ruta."
                : "Ingrese los datos del nuevo punto de control para la bitácora.");

        guardarButton.getStyleClass().removeAll("action-add", "action-edit");
        guardarButton.getStyleClass().add(isEditing ? "action-edit" : "action-add");

        if (isEditing) {
            txtNombre.setText(checkpoint.getNombre());
            txtHora.setText(checkpoint.getHora());
            txtLatitud.setText(String.valueOf(checkpoint.getLatitud()));
            txtLongitud.setText(String.valueOf(checkpoint.getLongitud()));
            txtDescripcion.setText(checkpoint.getDescripcion());
        }
    }

    @FXML
    private void guardarCheckpoint() {
        if (!validateInput()) {
            return;
        }

        String nombre = txtNombre.getText().trim();
        String hora = txtHora.getText().trim();
        double latitud = parseDecimal(txtLatitud.getText().trim());
        double longitud = parseDecimal(txtLongitud.getText().trim());
        String descripcion = txtDescripcion.getText().trim();

        try {
            if (checkpointEnEdicion == null) {
                Checkpoint nuevo = new Checkpoint(
                        0L, nombre, hora, latitud, longitud, descripcion);
                long newId = checkpointDAO.insertar(nuevo);
                if (newId != -1) {
                    nuevo.setId(newId);
                }
            } else {
                checkpointEnEdicion.setNombre(nombre);
                checkpointEnEdicion.setHora(hora);
                checkpointEnEdicion.setLatitud(latitud);
                checkpointEnEdicion.setLongitud(longitud);
                checkpointEnEdicion.setDescripcion(descripcion);
                checkpointDAO.actualizar(checkpointEnEdicion);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al guardar checkpoint", e);
            AlertUtils.showError(getWindow(), "Error al guardar",
                    "No se pudo guardar el checkpoint en la base de datos.\n" + e.getMessage());
            return;
        }

        volverListado();
    }

    @FXML
    private void cancelar() {
        volverListado();
    }

    private void volverListado() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/fxml/checkpoints.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) txtNombre.getScene().getWindow();
            NavigationShell.setRoot(stage.getScene(), root);
            stage.setTitle("Checkpoints");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error al volver al listado de checkpoints", e);
            AlertUtils.showError(getWindow(), "Error",
                    "No se pudo volver al listado de checkpoints.");
        }
    }

    private Window getWindow() {
        if (txtNombre.getScene() == null) {
            return null;
        }
        return txtNombre.getScene().getWindow();
    }

    /**
     * Valida los datos introducidos.
     *
     * @return true cuando todos los campos son válidos
     */
    public boolean validateInput() {
        limpiarTodosLosErrores(); // CAMBIADO

        FormValidation.validateNow(txtNombre);
        FormValidation.validateNow(txtHora);
        FormValidation.validateNow(txtLatitud);
        FormValidation.validateNow(txtLongitud);

        String nombre = txtNombre.getText().trim();
        String hora = txtHora.getText().trim();
        String latitudRaw = txtLatitud.getText().trim();
        String longitudRaw = txtLongitud.getText().trim();

        if (nombre.isEmpty()) {
            mostrarErrorDeValidacion(
                    txtNombre, "Por favor ingrese un nombre para el Checkpoint."); // CAMBIADO
            return false;
        }

        if (hora.isEmpty() || !isHoraValida(hora)) {
            mostrarErrorDeValidacion(
                    txtHora, "La hora debe tener el formato HH:mm (Ej. 14:30)."); // CAMBIADO
            return false;
        }

        if (latitudRaw.isEmpty()) {
            mostrarErrorDeValidacion(
                    txtLatitud, "Por favor ingrese la latitud del Checkpoint."); // CAMBIADO
            return false;
        }

        try {
            double lat = parseDecimal(latitudRaw);
            if (!Double.isFinite(lat) || lat < LAT_MIN || lat > LAT_MAX) { // CAMBIADO
                mostrarErrorDeValidacion(
                        txtLatitud,
                        "La latitud debe estar entre " + LAT_MIN + " y " + LAT_MAX + "."); // CAMBIADO
                return false;
            }
        } catch (NumberFormatException ex) {
            mostrarErrorDeValidacion(
                    txtLatitud, "La latitud debe ser un valor numérico decimal."); // CAMBIADO
            return false;
        }

        if (longitudRaw.isEmpty()) {
            mostrarErrorDeValidacion(
                    txtLongitud, "Por favor ingrese la longitud del Checkpoint."); // CAMBIADO
            return false;
        }

        try {
            double lon = parseDecimal(longitudRaw);
            if (!Double.isFinite(lon) || lon < LON_MIN || lon > LON_MAX) { // CAMBIADO
                mostrarErrorDeValidacion(
                        txtLongitud,
                        "La longitud debe estar entre " + LON_MIN + " y " + LON_MAX + "."); // CAMBIADO
                return false;
            }
        } catch (NumberFormatException ex) {
            mostrarErrorDeValidacion(
                    txtLongitud, "La longitud debe ser un valor numérico decimal."); // CAMBIADO
            return false;
        }

        return true;
    }

    private void configurarValidacionesEnTiempoReal() {
        FormValidation.watch(txtNombre, txtNombre.textProperty(),
                () -> txtNombre.getText().trim().isEmpty()
                        ? "Por favor ingrese un nombre para el Checkpoint."
                        : null);

        FormValidation.watch(txtHora, txtHora.textProperty(),
                () -> isHoraValida(txtHora.getText().trim())
                        ? null
                        : "La hora debe tener el formato HH:mm (Ej. 14:30).");

        FormValidation.watch(txtLatitud, txtLatitud.textProperty(),
                () -> validarCoordenada(
                        txtLatitud.getText(), LAT_MIN, LAT_MAX, "latitud"));

        FormValidation.watch(txtLongitud, txtLongitud.textProperty(),
                () -> validarCoordenada(
                        txtLongitud.getText(), LON_MIN, LON_MAX, "longitud"));
    }

    private String validarCoordenada(
            String texto, double minimo, double maximo, String nombre) {

        if (texto.trim().isEmpty()) {
            return "Por favor ingrese la " + nombre + " del Checkpoint.";
        }

        try {
            double valor = parseDecimal(texto.trim());

            if (!Double.isFinite(valor) || valor < minimo || valor > maximo) {
                return "La " + nombre + " debe estar entre "
                        + minimo + " y " + maximo + ".";
            }

            return null;
        } catch (NumberFormatException e) {
            return "La " + nombre + " debe ser un valor numérico decimal."; // CAMBIADO
        }
    }

    // NUEVO: sigue el patrón de RutaFormController para el estado :error.
    private void marcarError(TextField campo, String mensaje) {
        campo.pseudoClassStateChanged(PseudoClass.getPseudoClass("error"), true);

        Tooltip tooltip = new Tooltip(mensaje);
        tooltip.setShowDelay(Duration.millis(200));
        tooltip.setShowDuration(Duration.seconds(5));
        tooltip.setHideDelay(Duration.millis(100));
        campo.setTooltip(tooltip);

        errorText.setText(mensaje);
        errorBanner.setVisible(true);
        errorBanner.setManaged(true);

        campo.requestFocus();
    }

    // NUEVO: limpia tanto :error como el estado :invalid de FormValidation.
    private void limpiarError(TextField campo) {
        campo.pseudoClassStateChanged(PseudoClass.getPseudoClass("error"), false);
        FormValidation.clearError(campo);
        actualizarBanner();
    }

    // NUEVO: muestra el error del campo y conserva el banner existente.
    private void mostrarErrorDeValidacion(TextField campo, String mensaje) {
        marcarError(campo, mensaje);
    }

    // NUEVO: oculta el banner cuando no quedan errores de envío.
    private void actualizarBanner() {
        boolean hayErrores = txtNombre.getPseudoClassStates()
                .contains(PseudoClass.getPseudoClass("error"))
                || txtHora.getPseudoClassStates()
                .contains(PseudoClass.getPseudoClass("error"))
                || txtLatitud.getPseudoClassStates()
                .contains(PseudoClass.getPseudoClass("error"))
                || txtLongitud.getPseudoClassStates()
                .contains(PseudoClass.getPseudoClass("error"));

        if (!hayErrores) {
            errorBanner.setVisible(false);
            errorBanner.setManaged(false);
        }
    }

    // NUEVO: los tooltips originales se conservan al limpiar errores.
    private void configurarTooltips() {
        txtNombre.setTooltip(new Tooltip("Ingrese el nombre del checkpoint."));
        txtHora.setTooltip(new Tooltip("Ingrese la hora en formato HH:mm."));
        txtLatitud.setTooltip(new Tooltip("Ingrese una latitud entre -90 y 90."));
        txtLongitud.setTooltip(new Tooltip("Ingrese una longitud entre -180 y 180."));
    }

    // NUEVO: limpia los errores anteriores antes de validar nuevamente.
    private void limpiarTodosLosErrores() {
        limpiarError(txtNombre);
        limpiarError(txtHora);
        limpiarError(txtLatitud);
        limpiarError(txtLongitud);

        errorBanner.setVisible(false);
        errorBanner.setManaged(false);
    }

    private static boolean isHoraValida(String hora) {
        try {
            LocalTime.parse(hora, HORA_FORMATTER);
            return true;
        } catch (DateTimeParseException ignored) {
            return false;
        }
    }

    private static double parseDecimal(String raw) {
        return Double.parseDouble(raw.replace(',', '.'));
    }

    // CAMBIADO: el filtro ahora recibe el campo para marcar caracteres rechazados.
    private TextFormatter<String> createDecimalTextFormatter(TextField campo) {
        return new TextFormatter<>(change -> {
            String newText = change.getControlNewText();

            if (newText.isEmpty() || newText.matches(DECIMAL_PATTERN)) {
                limpiarError(campo); // NUEVO
                return change;
            }

            marcarError(campo, "Ingrese un número decimal válido."); // NUEVO
            return null;
        });
    }

    // CAMBIADO: el filtro ahora recibe el campo para marcar caracteres rechazados.
    private TextFormatter<String> createHoraTextFormatter(TextField campo) {
        return new TextFormatter<>(change -> {
            String newText = change.getControlNewText();

            if (newText.isEmpty() || newText.matches(HORA_PATTERN)) {
                limpiarError(campo); // NUEVO
                return change;
            }

            marcarError(campo, "La hora debe escribirse en formato HH:mm."); // NUEVO
            return null;
        });
    }
}
