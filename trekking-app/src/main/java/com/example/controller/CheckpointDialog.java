package com.example.controller;

import com.example.model.Checkpoint;
import com.example.util.AlertUtils;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Window;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/**
 * Diálogo modal de creación o edición de un checkpoint.
 *
 * La validación se realiza dentro del propio diálogo para evitar que se
 * guarden datos inválidos y para mantener encapsulada la lógica del formulario.
 */
public class CheckpointDialog extends Dialog<Checkpoint> {

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

    private final TextField txtNombre =
            new TextField();

    private final TextField txtHora =
            new TextField();

    private final TextField txtLatitud =
            new TextField();

    private final TextField txtLongitud =
            new TextField();

    private final TextArea txtDescripcion =
            new TextArea();

    private final HBox errorBanner =
            new HBox(8);

    private final Label errorText =
            new Label();

    private final Checkpoint existingCheckpoint;

    public CheckpointDialog(
            Window owner,
            Checkpoint checkpointToEdit) {

        this.existingCheckpoint =
                checkpointToEdit;

        initOwner(owner);

        initModality(
                Modality.WINDOW_MODAL);

        setTitle(
                checkpointToEdit == null
                        ? "Crear Nuevo Checkpoint"
                        : "Editar Checkpoint");

        DialogPane dialogPane =
                getDialogPane();

        AlertUtils.applyTheme(this);

        configureButtons(
                dialogPane,
                checkpointToEdit != null);

        configureForm(
                dialogPane,
                checkpointToEdit);

        configureResultConverter();
    }

    private void configureButtons(
            DialogPane dialogPane,
            boolean isEditing) {

        ButtonType confirmar =
                new ButtonType(
                        isEditing
                                ? "Guardar"
                                : "Confirmar",
                        ButtonBar.ButtonData.OK_DONE);

        ButtonType cancelar =
                new ButtonType(
                        "Cancelar",
                        ButtonBar.ButtonData.CANCEL_CLOSE);

        dialogPane.getButtonTypes()
                .addAll(
                        confirmar,
                        cancelar);

        Button btnConfirmar =
                (Button) dialogPane
                        .lookupButton(confirmar);

        btnConfirmar.setDefaultButton(true);

        btnConfirmar.getStyleClass()
                .add("btn-primary");

        /*
         * Interceptamos el botón de confirmación.
         *
         * Si la validación falla, consumimos el evento y el diálogo no se
         * cierra.
         */
        btnConfirmar.addEventFilter(
                ActionEvent.ACTION,
                event -> {

                    if (!validateInput()) {
                        event.consume();
                    }
                });

        Button btnCancelar =
                (Button) dialogPane
                        .lookupButton(cancelar);

        btnCancelar.setCancelButton(true);

        btnCancelar.getStyleClass()
                .add("btn-secondary");
    }

    private void configureForm(
            DialogPane dialogPane,
            Checkpoint checkpointToEdit) {

        boolean isEditing =
                checkpointToEdit != null;

        Label titleLabel =
                new Label(
                        isEditing
                                ? "Editar Checkpoint"
                                : "Nuevo Checkpoint");

        titleLabel.getStyleClass()
                .add("dialog-header-title");

        Label subtitleLabel =
                new Label(
                        isEditing
                                ? "Modifique los atributos del punto de control de ruta."
                                : "Ingrese los datos del nuevo punto de control para la bitácora.");

        subtitleLabel.getStyleClass()
                .add("dialog-header-subtitle");

        VBox headerBox =
                new VBox(
                        2,
                        titleLabel,
                        subtitleLabel);

        Label errorIcon =
                new Label("⚠️");

        errorIcon.getStyleClass()
                .add("error-banner-icon");

        errorText.getStyleClass()
                .add("error-banner-text");

        errorBanner.setAlignment(
                Pos.CENTER_LEFT);

        errorBanner.getStyleClass()
                .add("error-banner");

        errorBanner.getChildren()
                .addAll(
                        errorIcon,
                        errorText);

        errorBanner.setVisible(false);
        errorBanner.setManaged(false);

        GridPane grid =
                new GridPane();

        grid.setHgap(12);
        grid.setVgap(12);

        grid.getStyleClass()
                .add("dialog-form-card");

        ColumnConstraints labelColumn =
                new ColumnConstraints();

        labelColumn.setHalignment(
                HPos.LEFT);

        labelColumn.setMinWidth(140);

        ColumnConstraints fieldColumn =
                new ColumnConstraints();

        fieldColumn.setHalignment(
                HPos.LEFT);

        fieldColumn.setHgrow(
                Priority.ALWAYS);

        grid.getColumnConstraints()
                .addAll(
                        labelColumn,
                        fieldColumn);

        txtNombre.setPromptText(
                "Ej. Checkpoint Alfa");

        txtHora.setPromptText(
                "Ej. 14:30");

        txtLatitud.setPromptText(
                "Ej. -31.4135");

        txtLongitud.setPromptText(
                "Ej. -64.1810");

        txtDescripcion.setPromptText(
                "Descripción opcional de la parada o control...");

        txtDescripcion.setPrefRowCount(3);
        txtDescripcion.setPrefColumnCount(22);

        GridPane.setFillWidth(
                txtDescripcion,
                true);

        txtLatitud.setTextFormatter(
                createDecimalTextFormatter());

        txtLongitud.setTextFormatter(
                createDecimalTextFormatter());

        txtHora.setTextFormatter(
                createHoraTextFormatter());

        txtNombre.textProperty()
                .addListener(
                        (obs, oldValue, newValue) ->
                                clearFieldError(txtNombre));

        txtHora.textProperty()
                .addListener(
                        (obs, oldValue, newValue) ->
                                clearFieldError(txtHora));

        txtLatitud.textProperty()
                .addListener(
                        (obs, oldValue, newValue) ->
                                clearFieldError(txtLatitud));

        txtLongitud.textProperty()
                .addListener(
                        (obs, oldValue, newValue) ->
                                clearFieldError(txtLongitud));

        if (isEditing) {

            txtNombre.setText(
                    checkpointToEdit.getNombre());

            txtHora.setText(
                    checkpointToEdit.getHora());

            txtLatitud.setText(
                    String.valueOf(
                            checkpointToEdit.getLatitud()));

            txtLongitud.setText(
                    String.valueOf(
                            checkpointToEdit.getLongitud()));

            txtDescripcion.setText(
                    checkpointToEdit.getDescripcion());
        }

        Platform.runLater(() -> {

            txtNombre.requestFocus();

            if (isEditing) {
                txtNombre.selectAll();
            }
        });

        Label lblNombre =
                new Label("Nombre");

        lblNombre.getStyleClass()
                .add("form-label");

        Label lblHora =
                new Label("Hora (HH:mm)");

        lblHora.getStyleClass()
                .add("form-label");

        Label lblLatitud =
                new Label("Latitud");

        lblLatitud.getStyleClass()
                .add("form-label");

        Label lblLongitud =
                new Label("Longitud");

        lblLongitud.getStyleClass()
                .add("form-label");

        Label lblDescripcion =
                new Label("Descripción");

        lblDescripcion.getStyleClass()
                .add("form-label");

        grid.addRow(
                0,
                lblNombre,
                txtNombre);

        grid.addRow(
                1,
                lblHora,
                txtHora);

        grid.addRow(
                2,
                lblLatitud,
                txtLatitud);

        grid.addRow(
                3,
                lblLongitud,
                txtLongitud);

        grid.addRow(
                4,
                lblDescripcion,
                txtDescripcion);

        VBox mainLayout =
                new VBox(
                        12,
                        headerBox,
                        errorBanner,
                        grid);

        mainLayout.setPadding(
                new Insets(12));

        dialogPane.setContent(
                mainLayout);
    }

    private void configureResultConverter() {

        setResultConverter(
                dialogButton -> {

                    if (dialogButton != null
                            && dialogButton.getButtonData()
                                    == ButtonBar.ButtonData.OK_DONE) {

                        return buildCheckpointFromInputs();
                    }

                    return null;
                });
    }

    /**
     * Valida los datos introducidos.
     *
     * @return true cuando todos los campos son válidos
     */
    private boolean validateInput() {

        clearAllErrors();

        String nombre =
                txtNombre.getText().trim();

        String hora =
                txtHora.getText().trim();

        String latitudRaw =
                txtLatitud.getText().trim();

        String longitudRaw =
                txtLongitud.getText().trim();

        if (nombre.isEmpty()) {

            showInlineError(
                    txtNombre,
                    "Por favor ingrese un nombre para el Checkpoint.");

            return false;
        }

        if (hora.isEmpty()
                || !isHoraValida(hora)) {

            showInlineError(
                    txtHora,
                    "La hora debe tener el formato HH:mm (Ej. 14:30).");

            return false;
        }

        if (latitudRaw.isEmpty()) {

            showInlineError(
                    txtLatitud,
                    "Por favor ingrese la latitud del Checkpoint.");

            return false;
        }

        try {

            double lat =
                    parseDecimal(latitudRaw);

            if (lat < LAT_MIN
                    || lat > LAT_MAX) {

                showInlineError(
                        txtLatitud,
                        "La latitud debe estar entre "
                                + LAT_MIN
                                + " y "
                                + LAT_MAX
                                + ".");

                return false;
            }

        } catch (NumberFormatException ex) {

            showInlineError(
                    txtLatitud,
                    "La latitud debe ser un valor numérico decimal.");

            return false;
        }

        if (longitudRaw.isEmpty()) {

            showInlineError(
                    txtLongitud,
                    "Por favor ingrese la longitud del Checkpoint.");

            return false;
        }

        try {

            double lon =
                    parseDecimal(longitudRaw);

            if (lon < LON_MIN
                    || lon > LON_MAX) {

                showInlineError(
                        txtLongitud,
                        "La longitud debe estar entre "
                                + LON_MIN
                                + " y "
                                + LON_MAX
                                + ".");

                return false;
            }

        } catch (NumberFormatException ex) {

            showInlineError(
                    txtLongitud,
                    "La longitud debe ser un valor numérico decimal.");

            return false;
        }

        return true;
    }

    private Checkpoint buildCheckpointFromInputs() {

        String nombre =
                txtNombre.getText().trim();

        String hora =
                txtHora.getText().trim();

        String latitudRaw =
                txtLatitud.getText().trim();

        String longitudRaw =
                txtLongitud.getText().trim();

        String descripcion =
                txtDescripcion.getText().trim();

        double latitud =
                parseDecimal(latitudRaw);

        double longitud =
                parseDecimal(longitudRaw);

        long id =
                existingCheckpoint != null
                        ? existingCheckpoint.getId()
                        : System.currentTimeMillis();

        return new Checkpoint(
                id,
                nombre,
                hora,
                latitud,
                longitud,
                descripcion);
    }

    private void showInlineError(
            TextField field,
            String message) {

        errorText.setText(message);

        errorBanner.setVisible(true);
        errorBanner.setManaged(true);

        if (field != null) {

            if (!field.getStyleClass()
                    .contains("field-error")) {

                field.getStyleClass()
                        .add("field-error");
            }

            field.requestFocus();
        }
    }

    private void clearFieldError(
            TextField field) {

        if (field != null) {
            field.getStyleClass()
                    .remove("field-error");
        }

        if (!txtNombre.getStyleClass()
                .contains("field-error")
                && !txtHora.getStyleClass()
                .contains("field-error")
                && !txtLatitud.getStyleClass()
                .contains("field-error")
                && !txtLongitud.getStyleClass()
                .contains("field-error")) {

            errorBanner.setVisible(false);
            errorBanner.setManaged(false);
        }
    }

    private void clearAllErrors() {

        txtNombre.getStyleClass()
                .remove("field-error");

        txtHora.getStyleClass()
                .remove("field-error");

        txtLatitud.getStyleClass()
                .remove("field-error");

        txtLongitud.getStyleClass()
                .remove("field-error");

        errorBanner.setVisible(false);
        errorBanner.setManaged(false);
    }

    private static boolean isHoraValida(
            String hora) {

        try {

            LocalTime.parse(
                    hora,
                    HORA_FORMATTER);

            return true;

        } catch (DateTimeParseException ignored) {

            return false;
        }
    }

    private static double parseDecimal(
            String raw) {

        return Double.parseDouble(
                raw.replace(',', '.'));
    }

    private static TextFormatter<String>
    createDecimalTextFormatter() {

        return new TextFormatter<>(
                change -> {

                    String newText =
                            change.getControlNewText();

                    return newText.isEmpty()
                            || newText.matches(
                                    DECIMAL_PATTERN)
                            ? change
                            : null;
                });
    }

    private static TextFormatter<String>
    createHoraTextFormatter() {

        return new TextFormatter<>(
                change -> {

                    String newText =
                            change.getControlNewText();

                    return newText.isEmpty()
                            || newText.matches(
                                    HORA_PATTERN)
                            ? change
                            : null;
                });
    }
}
