package com.example.controller;

import com.example.dao.CheckpointDAO;
import com.example.dao.RutaCheckpointDAO;
import com.example.dao.RutaDAO;
import com.example.dao.RutaEquipamientoDAO;
import com.example.dao.EquipamientoDAO;
import com.example.model.Checkpoint;
import com.example.model.Ruta;
import com.example.model.Equipamiento;
import com.example.model.EquipamientoRequerido;
import com.example.model.RutaEquipamiento;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;
import javafx.stage.Stage;
import javafx.css.PseudoClass;
import javafx.util.Duration;

import java.io.IOException;
import java.util.List;
import java.util.function.UnaryOperator;

public class RutaFormController {

        private final RutaDAO rutaDAO = new RutaDAO();
        private final CheckpointDAO checkpointDAO = new CheckpointDAO();
        private final RutaCheckpointDAO rutaCheckpointDAO = new RutaCheckpointDAO();
        private final RutaEquipamientoDAO rutaEquipamientoDAO = new RutaEquipamientoDAO();

        private final ObservableList<Checkpoint> checkpointsSeleccionados = FXCollections.observableArrayList();
        private final ObservableList<EquipamientoRequerido> equipamientosSeleccionados = FXCollections
                        .observableArrayList();

        private Ruta rutaEnEdicion;

        @FXML
        private Label tituloForm;

        @FXML
        private TextField nombreField;

        @FXML
        private TextField altitudMaximaField;

        @FXML
        private ComboBox<String> tipoTerrenoCombo;

        @FXML
        private ComboBox<String> dificultadTecnicaCombo;

        @FXML
        private ComboBox<String> dificultadFisicaCombo;

        @FXML
        private ComboBox<Checkpoint> checkpointCombo;

        @FXML
        private ListView<Checkpoint> checkpointsListView;

        @FXML
        private Button agregarCheckpointButton;

        @FXML
        private Button quitarCheckpointButton;

        @FXML
        private Button subirCheckpointButton;

        @FXML
        private Button bajarCheckpointButton;
        @FXML

        private ComboBox<Equipamiento> equipamientoCombo;
        @FXML
        private TextField cantidadEquipamientoField;
        @FXML
        private ListView<EquipamientoRequerido> equipamientosListView;
        @FXML
        private Button agregarEquipamientoButton;
        @FXML
        private Button quitarEquipamientoButton;

        @FXML
        public void initialize() {

                configurarCampos();

                tipoTerrenoCombo.getItems().addAll(
                                "Rocoso",
                                "Boscoso",
                                "Sendero",
                                "Mixto");

                dificultadTecnicaCombo.getItems().addAll(
                                "Baja",
                                "Media",
                                "Alta");

                dificultadFisicaCombo.getItems().addAll(
                                "Baja",
                                "Media",
                                "Alta");

                configurarCheckpoints();
                configurarEquipamiento();

                rutaEnEdicion = null;
        }

        private void configurarCheckpoints() {

                checkpointsListView.setItems(checkpointsSeleccionados);

                checkpointCombo.setCellFactory(listView -> crearCeldaCheckpoint());

                checkpointCombo.setButtonCell(
                                crearCeldaCheckpoint());

                checkpointsListView.setCellFactory(listView -> crearCeldaCheckpoint());

                cargarCheckpointsDisponibles();
        }

        private ListCell<Checkpoint> crearCeldaCheckpoint() {

                return new ListCell<>() {

                        @Override
                        protected void updateItem(
                                        Checkpoint checkpoint,
                                        boolean empty) {

                                super.updateItem(checkpoint, empty);

                                if (empty || checkpoint == null) {
                                        setText(null);
                                } else {
                                        setText(
                                                        checkpoint.getNombre()
                                                                        + " - "
                                                                        + checkpoint.getHora());
                                }
                        }
                };
        }

        private void cargarCheckpointsDisponibles() {

                try {

                        checkpointCombo.getItems().setAll(
                                        checkpointDAO.obtenerTodos());

                } catch (Exception e) {

                        mostrarError(
                                        "Checkpoints",
                                        "No se pudieron cargar los checkpoints.");
                }
        }

        public void setRutaEnEdicion(Ruta ruta) {

                this.rutaEnEdicion = ruta;

                checkpointsSeleccionados.clear();

                if (ruta != null) {
                        tituloForm.setText("Editar ruta");
                        cargarRuta(ruta);
                        cargarCheckpointsDeRuta(ruta.getId());
                        cargarEquipamientoDeRuta(ruta.getId());
                } else {
                        tituloForm.setText("Nueva ruta");
                }
        }

        private void cargarCheckpointsDeRuta(int rutaId) {

                List<Checkpoint> checkpoints = rutaCheckpointDAO.obtenerPorRuta(rutaId);

                checkpointsSeleccionados.setAll(checkpoints);
        }

        @FXML
        private void agregarCheckpoint() {

                Checkpoint checkpoint = checkpointCombo.getValue();

                if (checkpoint == null) {

                        mostrarError(
                                        "Checkpoint",
                                        "Selecciona un checkpoint.");

                        return;
                }

                if (checkpointsSeleccionados.contains(checkpoint)) {

                        mostrarError(
                                        "Checkpoint",
                                        "Ese checkpoint ya pertenece a la ruta.");

                        return;
                }

                checkpointsSeleccionados.add(checkpoint);

                checkpointCombo.getSelectionModel().clearSelection();

                checkpointsListView.getSelectionModel().selectLast();
        }

        @FXML
        private void quitarCheckpoint() {

                int indice = checkpointsListView
                                .getSelectionModel()
                                .getSelectedIndex();

                if (indice < 0) {

                        mostrarError(
                                        "Checkpoint",
                                        "Selecciona un checkpoint de la lista.");

                        return;
                }

                checkpointsSeleccionados.remove(indice);
        }

        @FXML
        private void subirCheckpoint() {

                int indice = checkpointsListView
                                .getSelectionModel()
                                .getSelectedIndex();

                if (indice <= 0) {
                        return;
                }

                Checkpoint checkpoint = checkpointsSeleccionados.remove(indice);

                checkpointsSeleccionados.add(
                                indice - 1,
                                checkpoint);

                checkpointsListView
                                .getSelectionModel()
                                .select(indice - 1);
        }

        @FXML
        private void bajarCheckpoint() {

                int indice = checkpointsListView
                                .getSelectionModel()
                                .getSelectedIndex();

                if (indice < 0
                                || indice >= checkpointsSeleccionados.size() - 1) {
                        return;
                }

                Checkpoint checkpoint = checkpointsSeleccionados.remove(indice);

                checkpointsSeleccionados.add(
                                indice + 1,
                                checkpoint);

                checkpointsListView
                                .getSelectionModel()
                                .select(indice + 1);
        }

        private void configurarCantidadEquipamiento() {

                UnaryOperator<TextFormatter.Change> filtro = change -> {

                        String texto = change.getControlNewText();

                        if (texto.matches("\\d{0,4}")) {
                                return change;
                        }

                        return null;
                };

                cantidadEquipamientoField.setTextFormatter(
                                new TextFormatter<>(filtro));

                cantidadEquipamientoField.setTooltip(
                                new Tooltip("Ingrese una cantidad mayor o igual a 1"));
        }

        private void configurarEquipamiento() {

                equipamientosListView.setItems(equipamientosSeleccionados);

                equipamientosListView.setCellFactory(listView -> new ListCell<>() {

                        @Override
                        protected void updateItem(
                                        EquipamientoRequerido requerido,
                                        boolean empty) {

                                super.updateItem(requerido, empty);

                                if (empty || requerido == null) {
                                        setText(null);
                                } else {
                                        setText(
                                                        requerido.getEquipamiento().getNombre()
                                                                        + " — "
                                                                        + requerido.getCantidadRequerida());
                                }
                        }
                });

                equipamientoCombo.setItems(
                                EquipamientoDAO.obtenerTodos());

                equipamientoCombo.setCellFactory(listView -> new ListCell<>() {

                        @Override
                        protected void updateItem(
                                        Equipamiento equipamiento,
                                        boolean empty) {

                                super.updateItem(equipamiento, empty);

                                if (empty || equipamiento == null) {
                                        setText(null);
                                } else {
                                        setText(equipamiento.getNombre());
                                }
                        }
                });

                equipamientoCombo.setButtonCell(
                                new ListCell<>() {

                                        @Override
                                        protected void updateItem(
                                                        Equipamiento equipamiento,
                                                        boolean empty) {

                                                super.updateItem(equipamiento, empty);

                                                if (empty || equipamiento == null) {
                                                        setText(null);
                                                } else {
                                                        setText(equipamiento.getNombre());
                                                }
                                        }
                                });

                configurarCantidadEquipamiento();
        }

        private void cargarEquipamientoDeRuta(int rutaId) {

                equipamientosSeleccionados.clear();

                List<RutaEquipamiento> relaciones = rutaEquipamientoDAO.obtenerPorRuta(rutaId);

                for (RutaEquipamiento relacion : relaciones) {

                        Equipamiento equipamiento = EquipamientoDAO.obtenerPorId(
                                        relacion.getEquipamientoId());

                        if (equipamiento != null) {

                                equipamientosSeleccionados.add(
                                                new EquipamientoRequerido(
                                                                equipamiento,
                                                                relacion.getCantidadRequerida()));
                        }
                }
        }

        @FXML
        private void agregarEquipamiento() {

                Equipamiento equipamiento = equipamientoCombo.getValue();

                if (equipamiento == null) {
                        mostrarError(
                                        "Equipamiento requerido",
                                        "Seleccione un equipamiento.");
                        return;
                }

                String textoCantidad = cantidadEquipamientoField.getText().trim();

                if (textoCantidad.isEmpty()) {
                        mostrarError(
                                        "Cantidad requerida",
                                        "Ingrese la cantidad requerida.");
                        return;
                }

                int cantidad;

                try {
                        cantidad = Integer.parseInt(textoCantidad);
                } catch (NumberFormatException e) {
                        mostrarError(
                                        "Cantidad inválida",
                                        "La cantidad debe ser un número entero.");
                        return;
                }

                if (cantidad < 1) {
                        mostrarError(
                                        "Cantidad inválida",
                                        "La cantidad requerida debe ser mayor o igual a 1.");
                        return;
                }

                for (EquipamientoRequerido existente : equipamientosSeleccionados) {

                        if (existente.getEquipamiento().getId() == equipamiento.getId()) {

                                mostrarError(
                                                "Equipamiento duplicado",
                                                "Ese equipamiento ya está asociado a la ruta.");
                                return;
                        }
                }

                equipamientosSeleccionados.add(
                                new EquipamientoRequerido(
                                                equipamiento,
                                                cantidad));

                equipamientoCombo.getSelectionModel().clearSelection();
                cantidadEquipamientoField.clear();
        }

        @FXML
        private void quitarEquipamiento() {

                EquipamientoRequerido seleccionado = equipamientosListView
                                .getSelectionModel()
                                .getSelectedItem();

                if (seleccionado == null) {
                        mostrarError(
                                        "Equipamiento",
                                        "Seleccione un equipamiento de la lista.");
                        return;
                }

                equipamientosSeleccionados.remove(seleccionado);
        }

        @FXML
        private void volverListado() throws IOException {

                Stage stage = (Stage) nombreField.getScene().getWindow();

                FXMLLoader loader = new FXMLLoader(
                                getClass().getResource(
                                                "/com/example/fxml/rutas.fxml"));

                Parent root = loader.load();

                stage.getScene().setRoot(root);
        }

        @FXML
        private void guardarRuta() throws IOException {

                String nombre = Ruta.formatearNombre(
                                nombreField.getText());

                if (!validarNombre(nombre)) {
                        return;
                }

                if (!validarNombreDuplicado(nombre)) {
                        return;
                }

                if (tipoTerrenoCombo.getValue() == null) {

                        mostrarError(
                                        "Tipo de terreno",
                                        "Selecciona un tipo de terreno.");

                        return;
                }

                if (dificultadTecnicaCombo.getValue() == null) {

                        mostrarError(
                                        "Dificultad técnica",
                                        "Selecciona una dificultad técnica.");

                        return;
                }

                if (dificultadFisicaCombo.getValue() == null) {

                        mostrarError(
                                        "Dificultad física",
                                        "Selecciona una dificultad física.");

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
                                                altitudMaxima,
                                                tipoTerreno,
                                                dificultadTecnica,
                                                dificultadFisica)
                                : new Ruta(
                                                rutaEnEdicion.getId(),
                                                nombre,
                                                altitudMaxima,
                                                tipoTerreno,
                                                dificultadTecnica,
                                                dificultadFisica);

                if (rutaEnEdicion == null) {

                        int idGenerado = rutaDAO.insertar(ruta);

                        if (idGenerado == -1) {

                                mostrarError(
                                                "Error",
                                                "No se pudo guardar la ruta.");

                                return;
                        }

                        boolean relacionesGuardadas = rutaCheckpointDAO.reemplazarPorRuta(
                                        idGenerado,
                                        checkpointsSeleccionados);

                        if (!relacionesGuardadas) {

                                mostrarError(
                                                "Error",
                                                "La ruta se guardó, pero no se pudieron "
                                                                + "guardar sus checkpoints.");

                                return;
                        }

                        mostrarInformacion(
                                        "Ruta guardada",
                                        "La ruta se guardó correctamente.");

                } else {

                        boolean rutaActualizada = rutaDAO.actualizar(ruta);

                        if (!rutaActualizada) {

                                mostrarError(
                                                "Error",
                                                "No se pudo actualizar la ruta.");

                                return;
                        }

                        boolean relacionesGuardadas = rutaCheckpointDAO.reemplazarPorRuta(
                                        ruta.getId(),
                                        checkpointsSeleccionados);

                        if (!relacionesGuardadas) {

                                mostrarError(
                                                "Error",
                                                "La ruta se actualizó, pero no se pudieron "
                                                                + "guardar sus checkpoints.");

                                return;
                        }

                        mostrarInformacion(
                                        "Ruta actualizada",
                                        "La ruta se actualizó correctamente.");
                }

                volverListado();
        }

        private boolean validarNombre(String nombre) {

                if (nombre.isBlank()) {

                        mostrarError(
                                        "Nombre de ruta",
                                        "El nombre de la ruta es obligatorio.");

                        nombreField.requestFocus();

                        return false;
                }

                if (!Ruta.nombreValido(nombre)) {

                        mostrarError(
                                        "Nombre de ruta",
                                        "El nombre solamente puede contener letras, "
                                                        + "números y espacios.");

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
                                        "Ya existe una ruta con ese nombre.");

                        nombreField.requestFocus();

                        return false;
                }

                return true;
        }

        private Double obtenerAltitud() {

                String texto = altitudMaximaField.getText().trim();

                if (texto.isEmpty()) {

                        mostrarError(
                                        "Altitud máxima",
                                        "La altitud máxima es obligatoria.");

                        altitudMaximaField.requestFocus();

                        return null;
                }

                try {

                        double valor = Double.parseDouble(
                                        texto.replace(',', '.'));

                        if (!Double.isFinite(valor)) {

                                mostrarError(
                                                "Altitud máxima",
                                                "Ingresa un valor numérico válido.");

                                altitudMaximaField.requestFocus();

                                return null;
                        }

                        if (valor < 0) {

                                mostrarError(
                                                "Altitud máxima",
                                                "La altitud no puede ser negativa.");

                                altitudMaximaField.requestFocus();

                                return null;
                        }

                        return valor;

                } catch (NumberFormatException e) {

                        mostrarError(
                                        "Altitud máxima",
                                        "Ingresa un valor numérico válido.");

                        altitudMaximaField.requestFocus();

                        return null;
                }
        }

        private void marcarError(
                        TextField campo,
                        String mensaje) {

                campo.pseudoClassStateChanged(
                                PseudoClass.getPseudoClass("error"),
                                true);

                Tooltip tooltip = new Tooltip(mensaje);
                tooltip.setShowDelay(Duration.millis(200));
                tooltip.setShowDuration(Duration.seconds(5));
                tooltip.setHideDelay(Duration.millis(100));

                campo.setTooltip(tooltip);
        }

        private void limpiarError(TextField campo) {
                campo.pseudoClassStateChanged(
                                PseudoClass.getPseudoClass("error"),
                                false);
        }

        private void configurarCampos() {
                UnaryOperator<TextFormatter.Change> filtroNombre = change -> {
                        String nuevoTexto = change.getControlNewText();

                        if (nuevoTexto.length() > 100) {
                                marcarError(
                                                nombreField,
                                                "El nombre no puede superar los 100 caracteres.");
                                return null;
                        }

                        if (nuevoTexto.matches("[\\p{L}\\p{N} ]*")) {
                                limpiarError(nombreField);
                                return change;
                        }

                        marcarError(
                                        nombreField,
                                        "El nombre solamente puede contener letras, números y espacios.");
                        return null;
                };

                nombreField.setTextFormatter(
                                new TextFormatter<>(filtroNombre));

                nombreField.setTooltip(
                                new Tooltip(
                                                "Ingresa el nombre de la ruta."));

                configurarCampoNumerico(
                                altitudMaximaField,
                                "Ejemplo: 1250");
        }

        private void configurarCampoNumerico(
                        TextField campo,
                        String textoAyuda) {

                UnaryOperator<TextFormatter.Change> filtro = change -> {
                        String nuevoTexto = change.getControlNewText();

                        if (nuevoTexto.isEmpty()) {
                                limpiarError(campo);
                                return change;
                        }

                        if (nuevoTexto.matches(
                                        "-?\\d*[\\.,]?\\d*")) {
                                limpiarError(campo);
                                return change;
                        }

                        marcarError(
                                        campo,
                                        "Ingrese solamente un valor numérico válido.");

                        return null;
                };

                campo.setTextFormatter(
                                new TextFormatter<>(filtro));

                campo.setTooltip(
                                new Tooltip(textoAyuda));
        }

        private void cargarRuta(Ruta ruta) {

                nombreField.setText(
                                ruta.getNombre());

                altitudMaximaField.setText(
                                formatearNumero(
                                                ruta.getAltitudMaxima()));

                tipoTerrenoCombo.setValue(
                                ruta.getTipoTerreno());

                dificultadTecnicaCombo.setValue(
                                ruta.getDificultadTecnica());

                dificultadFisicaCombo.setValue(
                                ruta.getDificultadFisica());
        }

        private String formatearNumero(double valor) {
                return String.valueOf(valor);
        }

        private void mostrarError(
                        String titulo,
                        String mensaje) {

                Alert alerta = new Alert(Alert.AlertType.ERROR);

                alerta.setTitle(titulo);
                alerta.setHeaderText(null);
                alerta.setContentText(mensaje);
                alerta.showAndWait();
        }

        private void mostrarInformacion(
                        String titulo,
                        String mensaje) {

                Alert alerta = new Alert(Alert.AlertType.INFORMATION);

                alerta.setTitle(titulo);
                alerta.setHeaderText(null);
                alerta.setContentText(mensaje);
                alerta.showAndWait();
        }
}