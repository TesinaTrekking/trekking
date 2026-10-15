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
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Formulario en-contenedor de creación/edición de checkpoint.
 *
 * Estructura en {@code checkpoint-form.fxml}. Se navega con
 * {@link NavigationShell#setRoot}, igual que equipamiento y rutas.
 */
public class CheckpointFormController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(CheckpointFormController.class.getName());

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
        txtLatitud.setTextFormatter(createDecimalTextFormatter());
        txtLongitud.setTextFormatter(createDecimalTextFormatter());
        txtHora.setTextFormatter(createHoraTextFormatter());

        txtNombre.textProperty().addListener((obs, oldValue, newValue) -> clearFieldError(txtNombre));
        txtHora.textProperty().addListener((obs, oldValue, newValue) -> clearFieldError(txtHora));
        txtLatitud.textProperty().addListener((obs, oldValue, newValue) -> clearFieldError(txtLatitud));
        txtLongitud.textProperty().addListener((obs, oldValue, newValue) -> clearFieldError(txtLongitud));

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
        clearAllErrors();
        FormValidation.validateNow(txtNombre);
        FormValidation.validateNow(txtHora);
        FormValidation.validateNow(txtLatitud);
        FormValidation.validateNow(txtLongitud);

        String nombre = txtNombre.getText().trim();
        String hora = txtHora.getText().trim();
        String latitudRaw = txtLatitud.getText().trim();
        String longitudRaw = txtLongitud.getText().trim();

        if (nombre.isEmpty()) {
            showInlineError(txtNombre, "Por favor ingrese un nombre para el Checkpoint.");
            return false;
        }

        if (hora.isEmpty() || !isHoraValida(hora)) {
            showInlineError(txtHora, "La hora debe tener el formato HH:mm (Ej. 14:30).");
            return false;
        }

        if (latitudRaw.isEmpty()) {
            showInlineError(txtLatitud, "Por favor ingrese la latitud del Checkpoint.");
            return false;
        }

        try {
            double lat = parseDecimal(latitudRaw);
            if (lat < LAT_MIN || lat > LAT_MAX) {
                showInlineError(txtLatitud,
                        "La latitud debe estar entre " + LAT_MIN + " y " + LAT_MAX + ".");
                return false;
            }
        } catch (NumberFormatException ex) {
            showInlineError(txtLatitud, "La latitud debe ser un valor numérico decimal.");
            return false;
        }

        if (longitudRaw.isEmpty()) {
            showInlineError(txtLongitud, "Por favor ingrese la longitud del Checkpoint.");
            return false;
        }

        try {
            double lon = parseDecimal(longitudRaw);
            if (lon < LON_MIN || lon > LON_MAX) {
                showInlineError(txtLongitud,
                        "La longitud debe estar entre " + LON_MIN + " y " + LON_MAX + ".");
                return false;
            }
        } catch (NumberFormatException ex) {
            showInlineError(txtLongitud, "La longitud debe ser un valor numérico decimal.");
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
                () -> validarCoordenada(txtLatitud.getText(), LAT_MIN, LAT_MAX, "latitud"));
        FormValidation.watch(txtLongitud, txtLongitud.textProperty(),
                () -> validarCoordenada(txtLongitud.getText(), LON_MIN, LON_MAX, "longitud"));
    }

    private String validarCoordenada(String texto, double minimo, double maximo, String nombre) {
        if (texto.trim().isEmpty()) {
            return "Por favor ingrese la " + nombre + " del Checkpoint.";
        }
        try {
            double valor = parseDecimal(texto.trim());
            if (!Double.isFinite(valor) || valor < minimo || valor > maximo) {
                return "La " + nombre + " debe estar entre " + minimo + " y " + maximo + ".";
            }
            return null;
        } catch (NumberFormatException e) {
            return "La " + nombre + " debe ser un valor numerico decimal.";
        }
    }

    private void showInlineError(TextField field, String message) {
        errorText.setText(message);
        errorBanner.setVisible(true);
        errorBanner.setManaged(true);
        if (field != null) {
            if (!field.getStyleClass().contains("field-error")) {
                field.getStyleClass().add("field-error");
            }
            field.requestFocus();
        }
    }

    private void clearFieldError(TextField field) {
        if (field != null) {
            field.getStyleClass().remove("field-error");
        }
        if (!txtNombre.getStyleClass().contains("field-error")
                && !txtHora.getStyleClass().contains("field-error")
                && !txtLatitud.getStyleClass().contains("field-error")
                && !txtLongitud.getStyleClass().contains("field-error")) {
            errorBanner.setVisible(false);
            errorBanner.setManaged(false);
        }
    }

    private void clearAllErrors() {
        txtNombre.getStyleClass().remove("field-error");
        txtHora.getStyleClass().remove("field-error");
        txtLatitud.getStyleClass().remove("field-error");
        txtLongitud.getStyleClass().remove("field-error");
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

    private static TextFormatter<String> createDecimalTextFormatter() {
        return new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            return newText.isEmpty() || newText.matches(DECIMAL_PATTERN) ? change : null;
        });
    }

    private static TextFormatter<String> createHoraTextFormatter() {
        return new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            return newText.isEmpty() || newText.matches(HORA_PATTERN) ? change : null;
        });
    }
}
