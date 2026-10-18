
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
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;
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

    private static final Logger LOGGER =
            Logger.getLogger(CheckpointFormController.class.getName());
    private static final PseudoClass ERROR =
            PseudoClass.getPseudoClass("error");
    private static final PseudoClass INVALID =
            PseudoClass.getPseudoClass("invalid");

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
    private final Map<TextField, String> mensajesError = new IdentityHashMap<>();
    private Checkpoint checkpointEnEdicion;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        txtLatitud.setTextFormatter(createDecimalTextFormatter(txtLatitud));
        txtLongitud.setTextFormatter(createDecimalTextFormatter(txtLongitud));
        txtHora.setTextFormatter(createHoraTextFormatter(txtHora));

        configurarTooltips();
        configurarValidacionesEnTiempoReal();
        txtNombre.textProperty().addListener(
                (obs, oldValue, newValue) -> limpiarErrorTrasCambio(txtNombre));
        txtHora.textProperty().addListener(
                (obs, oldValue, newValue) -> limpiarErrorTrasCambio(txtHora));
        txtLatitud.textProperty().addListener(
                (obs, oldValue, newValue) -> limpiarErrorTrasCambio(txtLatitud));
        txtLongitud.textProperty().addListener(
                (obs, oldValue, newValue) -> limpiarErrorTrasCambio(txtLongitud));
        txtNombre.focusedProperty().addListener(
                (obs, wasFocused, isFocused) -> actualizarErrorTrasCambioDeFoco(txtNombre));
        txtHora.focusedProperty().addListener(
                (obs, wasFocused, isFocused) -> actualizarErrorTrasCambioDeFoco(txtHora));
        txtLatitud.focusedProperty().addListener(
                (obs, wasFocused, isFocused) -> actualizarErrorTrasCambioDeFoco(txtLatitud));
        txtLongitud.focusedProperty().addListener(
                (obs, wasFocused, isFocused) -> actualizarErrorTrasCambioDeFoco(txtLongitud));

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
                if (newId == -1) {
                    AlertUtils.showError(getWindow(), "Error al guardar",
                            "No se pudo confirmar el guardado del checkpoint.");
                    return;
                }
                nuevo.setId(newId);
            } else {
                checkpointEnEdicion.setNombre(nombre);
                checkpointEnEdicion.setHora(hora);
                checkpointEnEdicion.setLatitud(latitud);
                checkpointEnEdicion.setLongitud(longitud);
                checkpointEnEdicion.setDescripcion(descripcion);
                int filasActualizadas = checkpointDAO.actualizar(checkpointEnEdicion);
                if (filasActualizadas == 0) {
                    AlertUtils.showError(getWindow(), "Error al guardar",
                            "No se pudo actualizar el checkpoint.");
                    return;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al guardar checkpoint", e);
            AlertUtils.showError(getWindow(), "Error al guardar",
                    "No se pudo guardar el checkpoint en la base de datos.\n" + e.getMessage());
            return;
        }

        mostrarConfirmacionExito();
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
        limpiarTodosLosErrores();

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
                    txtNombre, "Por favor ingrese un nombre para el Checkpoint.");
            return false;
        }

        if (hora.isEmpty() || !isHoraValida(hora)) {
            mostrarErrorDeValidacion(
                    txtHora, "La hora debe tener el formato HH:mm (Ej. 14:30).");
            return false;
        }

        if (latitudRaw.isEmpty()) {
            mostrarErrorDeValidacion(
                    txtLatitud, "Por favor ingrese la latitud del Checkpoint.");
            return false;
        }

        try {
            double lat = parseDecimal(latitudRaw);
            if (!Double.isFinite(lat) || lat < LAT_MIN || lat > LAT_MAX) {
                mostrarErrorDeValidacion(
                        txtLatitud,
                        "La latitud debe estar entre " + LAT_MIN + " y " + LAT_MAX + ".");
                return false;
            }
        } catch (NumberFormatException ex) {
            mostrarErrorDeValidacion(
                    txtLatitud, "La latitud debe ser un valor numérico decimal.");
            return false;
        }

        if (longitudRaw.isEmpty()) {
            mostrarErrorDeValidacion(
                    txtLongitud, "Por favor ingrese la longitud del Checkpoint.");
            return false;
        }

        try {
            double lon = parseDecimal(longitudRaw);
            if (!Double.isFinite(lon) || lon < LON_MIN || lon > LON_MAX) {
                mostrarErrorDeValidacion(
                        txtLongitud,
                        "La longitud debe estar entre " + LON_MIN + " y " + LON_MAX + ".");
                return false;
            }
        } catch (NumberFormatException ex) {
            mostrarErrorDeValidacion(
                    txtLongitud, "La longitud debe ser un valor numérico decimal.");
            return false;
        }

        return true;
    }

    private void configurarValidacionesEnTiempoReal() {
        FormValidation.watch(txtNombre, txtNombre.textProperty(),
                this::validarNombre);

        FormValidation.watch(txtHora, txtHora.textProperty(),
                this::validarHora);

        FormValidation.watch(txtLatitud, txtLatitud.textProperty(),
                () -> validarCoordenada(
                        txtLatitud.getText(), LAT_MIN, LAT_MAX, "latitud"));

        FormValidation.watch(txtLongitud, txtLongitud.textProperty(),
                () -> validarCoordenada(
                        txtLongitud.getText(), LON_MIN, LON_MAX, "longitud"));
    }

    private String validarNombre() {
        return txtNombre.getText().trim().isEmpty()
                ? "Por favor ingrese un nombre para el Checkpoint."
                : null;
    }

    private String validarHora() {
        return isHoraValida(txtHora.getText().trim())
                ? null
                : "La hora debe tener el formato HH:mm (Ej. 14:30).";
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
            return "La " + nombre + " debe ser un valor numérico decimal.";
        }
    }

    private void marcarError(TextField campo, String mensaje) {
        mensajesError.put(campo, mensaje);
        campo.pseudoClassStateChanged(ERROR, true);
        FormValidation.showError(campo, mensaje);

        errorText.setText(mensaje);
        errorBanner.setVisible(true);
        errorBanner.setManaged(true);

        campo.requestFocus();
    }

    private void limpiarError(TextField campo) {
        mensajesError.remove(campo);
        campo.pseudoClassStateChanged(ERROR, false);
        FormValidation.clearError(campo);
        actualizarBanner();
    }

    private void limpiarErrorTrasCambio(TextField campo) {
        mensajesError.remove(campo);
        campo.pseudoClassStateChanged(ERROR, false);
        FormValidation.validateNow(campo);
        actualizarBanner();
    }

    private void actualizarErrorTrasCambioDeFoco(TextField campo) {
        if (campo.getPseudoClassStates().contains(ERROR)
                && mensajeValidacionActual(campo) == null) {
            String mensaje = mensajesError.get(campo);
            if (mensaje != null) {
                FormValidation.showError(campo, mensaje);
            }
        }
        actualizarBanner();
    }

    private void mostrarErrorDeValidacion(TextField campo, String mensaje) {
        marcarError(campo, mensaje);
    }

    private void actualizarBanner() {
        TextField campoConError = primerCampoConError();
        boolean hayErrores = campoConError != null;
        if (hayErrores) {
            String mensaje = mensajeValidacionActual(campoConError);
            if (mensaje == null) {
                mensaje = mensajesError.get(campoConError);
            }
            if (mensaje != null) {
                errorText.setText(mensaje);
            }
        }
        errorBanner.setVisible(hayErrores);
        errorBanner.setManaged(hayErrores);
    }

    private String mensajeValidacionActual(TextField campo) {
        if (campo == txtNombre) {
            return validarNombre();
        }
        if (campo == txtHora) {
            return validarHora();
        }
        if (campo == txtLatitud) {
            return validarCoordenada(
                    txtLatitud.getText(), LAT_MIN, LAT_MAX, "latitud");
        }
        if (campo == txtLongitud) {
            return validarCoordenada(
                    txtLongitud.getText(), LON_MIN, LON_MAX, "longitud");
        }
        return null;
    }

    private TextField primerCampoConError() {
        if (tieneError(txtNombre)) {
            return txtNombre;
        }
        if (tieneError(txtHora)) {
            return txtHora;
        }
        if (tieneError(txtLatitud)) {
            return txtLatitud;
        }
        if (tieneError(txtLongitud)) {
            return txtLongitud;
        }
        return null;
    }

    private boolean tieneError(TextField campo) {
        return campo.getPseudoClassStates().contains(ERROR)
                || campo.getPseudoClassStates().contains(INVALID);
    }

    private void configurarTooltips() {
        txtNombre.setTooltip(new Tooltip("Ingrese el nombre del checkpoint."));
        txtHora.setTooltip(new Tooltip("Ingrese la hora en formato HH:mm."));
        txtLatitud.setTooltip(new Tooltip("Ingrese una latitud entre -90 y 90."));
        txtLongitud.setTooltip(new Tooltip("Ingrese una longitud entre -180 y 180."));
    }

    private void limpiarTodosLosErrores() {
        limpiarError(txtNombre);
        limpiarError(txtHora);
        limpiarError(txtLatitud);
        limpiarError(txtLongitud);

        errorBanner.setVisible(false);
        errorBanner.setManaged(false);
    }

    private void mostrarConfirmacionExito() {
        boolean esAlta = checkpointEnEdicion == null;
        Alert alerta = new Alert(Alert.AlertType.INFORMATION);
        alerta.setTitle(esAlta ? "Checkpoint creado" : "Checkpoint actualizado");
        alerta.setHeaderText(null);
        alerta.setContentText(esAlta
                ? "El checkpoint se creó correctamente."
                : "El checkpoint se actualizó correctamente.");
        alerta.showAndWait();
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

    private TextFormatter<String> createDecimalTextFormatter(TextField campo) {
        return new TextFormatter<>(change -> {
            String newText = change.getControlNewText();

            if (newText.isEmpty() || newText.matches(DECIMAL_PATTERN)) {
                limpiarError(campo);
                return change;
            }

            marcarError(campo, "Ingrese un número decimal válido.");
            return null;
        });
    }

    private TextFormatter<String> createHoraTextFormatter(TextField campo) {
        return new TextFormatter<>(change -> {
            String newText = change.getControlNewText();

            if (newText.isEmpty() || newText.matches(HORA_PATTERN)) {
                limpiarError(campo);
                return change;
            }

            marcarError(campo, "La hora debe escribirse en formato HH:mm.");
            return null;
        });
    }
}
