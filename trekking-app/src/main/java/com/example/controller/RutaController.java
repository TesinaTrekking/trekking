package com.example.controller;

import com.example.dao.RutaCheckpointDAO;
import com.example.dao.RutaDAO;
import com.example.dao.RutaEquipamientoDAO;
import com.example.dao.EquipamientoDAO;
import com.example.model.Checkpoint;
import com.example.model.EquipamientoRequerido;
import com.example.model.Ruta;
import com.example.model.RutaEquipamiento;
import com.example.model.Equipamiento;
import com.example.util.NavigationShell;
import com.example.util.TableEmptyState;
import com.example.util.TableCreateRow;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.ListCell;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class RutaController implements Initializable {

        private final RutaDAO rutaDAO = new RutaDAO();
        private final RutaCheckpointDAO rutaCheckpointDAO = new RutaCheckpointDAO();
        private final RutaEquipamientoDAO rutaEquipamientoDAO = new RutaEquipamientoDAO();

        private ObservableList<Ruta> rutas;
        private FilteredList<Ruta> rutasFiltradas;
        private final ObservableList<EquipamientoRequerido> equipamientosRutaSeleccionada = FXCollections
                        .observableArrayList();

        @FXML
        private TableView<Ruta> tablaRutas;

        @FXML
        private TableColumn<Ruta, Integer> colId;

        @FXML
        private TableColumn<Ruta, String> colNombre;

        @FXML
        private TableColumn<Ruta, Double> colAltitudMaxima;

        @FXML
        private TableColumn<Ruta, String> colTipoTerreno;

        @FXML
        private TableColumn<Ruta, String> colDificultadTecnica;

        @FXML
        private TableColumn<Ruta, String> colDificultadFisica;
        @FXML private TableColumn<Ruta, Ruta> colAcciones;
        @FXML private Button filaAltaButton;

        @FXML
        private TextField buscarField;

        @FXML
        private ComboBox<String> filtroTerrenoCombo;

        @FXML
        private ComboBox<String> filtroDificultadTecnicaCombo;

        @FXML
        private ComboBox<String> filtroDificultadFisicaCombo;

        @FXML
        private CheckBox mostrarInactivasCheckBox;

        @FXML
        private ListView<Checkpoint> checkpointsRutaListView;
        @FXML
        private ListView<EquipamientoRequerido> equipamientosListView;

        @FXML
        private Label estadoItinerarioLabel;

        @Override
        public void initialize(URL url, ResourceBundle rb) {

                tablaRutas.getStyleClass().add("table-with-create-row");
                tablaRutas.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

                colId.setCellValueFactory(
                                new PropertyValueFactory<>("id"));

                colNombre.setCellValueFactory(
                                new PropertyValueFactory<>("nombre"));

                colNombre.setComparator(String.CASE_INSENSITIVE_ORDER);
                colNombre.setSortType(TableColumn.SortType.ASCENDING);

                colAltitudMaxima.setCellValueFactory(
                                new PropertyValueFactory<>("altitudMaxima"));

                colTipoTerreno.setCellValueFactory(
                                new PropertyValueFactory<>("tipoTerreno"));

                colDificultadTecnica.setCellValueFactory(
                                new PropertyValueFactory<>("dificultadTecnica"));

                colDificultadFisica.setCellValueFactory(
                                new PropertyValueFactory<>("dificultadFisica"));
                colAcciones.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue()));

                tablaRutas.setEditable(true);
                tablaRutas.setRowFactory(table -> new javafx.scene.control.TableRow<>() {
                        @Override protected void updateItem(Ruta ruta, boolean empty) {
                                super.updateItem(ruta, empty);
                                setEditable(!empty && ruta != null && rutaDAO.estaActiva(ruta.getId()));
                        }
                });
                configurarTextoInline(colNombre, "nombre");
                configurarComboInline(colTipoTerreno, "terreno", List.of("Rocoso", "Boscoso", "Sendero", "Mixto"));
                configurarComboInline(colDificultadTecnica, "tecnica", List.of("Baja", "Media", "Alta"));
                configurarComboInline(colDificultadFisica, "fisica", List.of("Baja", "Media", "Alta"));
                colAltitudMaxima.setCellFactory(javafx.scene.control.cell.TextFieldTableCell.forTableColumn(new javafx.util.converter.DoubleStringConverter()));
                colAltitudMaxima.setOnEditCommit(event -> {
                        Ruta ruta = event.getRowValue();
                        try {
                                if (event.getNewValue() == null || event.getNewValue() < 0) throw new IllegalArgumentException("La altitud debe ser mayor o igual a cero.");
                                ruta.setAltitudMaxima(event.getNewValue()); guardarRutaInline(ruta);
                        } catch (RuntimeException ex) { mostrarAlerta("Dato inválido", ex.getMessage()); cargarRutas(); }
                });

                colAcciones.setCellFactory(column -> new javafx.scene.control.TableCell<>() {
                        @Override protected void updateItem(Ruta ruta, boolean empty) {
                                super.updateItem(ruta, empty);
                                if (empty || ruta == null) { setGraphic(null); return; }
                                boolean activa = rutaDAO.estaActiva(ruta.getId());
                                javafx.scene.layout.HBox actions = new javafx.scene.layout.HBox(3);
                                Button estado = com.example.util.RowActionButton.create(activa ? "Dar de baja" : "Reactivar",
                                        activa ? com.example.util.RowActionButton.Icon.DELETE : com.example.util.RowActionButton.Icon.RESTORE,
                                        e -> cambiarEstado(ruta));
                                if (activa) estado.getStyleClass().add("row-action-danger");
                                actions.getChildren().addAll(
                                        com.example.util.RowActionButton.create("Editar ruta", com.example.util.RowActionButton.Icon.EDIT, e -> abrirEdicion(ruta)), estado);
                                actions.setAlignment(javafx.geometry.Pos.CENTER);
                                setAlignment(javafx.geometry.Pos.CENTER);
                                setGraphic(actions);
                        }
                });

                configurarCheckpoints();
                configurarEquipamiento();
                configurarFiltros();
                cargarRutas();

                tablaRutas.getSortOrder().add(colNombre);
                tablaRutas.sort();

                tablaRutas.getSelectionModel()
                                .selectedItemProperty()
                                .addListener((observable, anterior, actual) -> {
                                        cargarCheckpointsDeRuta(actual);
                                        cargarEquipamientoDeRuta(actual);
                                });
                actualizarEstadoTabla();
        }

        private void configurarTextoInline(TableColumn<Ruta, String> columna, String campo) {
                columna.setCellFactory(javafx.scene.control.cell.TextFieldTableCell.forTableColumn());
                columna.setOnEditCommit(event -> {
                        Ruta ruta = event.getRowValue(); String valor = event.getNewValue() == null ? "" : event.getNewValue().trim();
                        if (valor.isBlank()) { mostrarAlerta("Dato inválido", "El valor no puede estar vacío."); cargarRutas(); return; }
                        switch (campo) {
                                case "nombre" -> { if (!Ruta.nombreValido(valor)) { mostrarAlerta("Nombre inválido", "Usa palabras separadas por espacios."); cargarRutas(); return; } ruta.setNombre(Ruta.formatearNombre(valor)); }
                                case "terreno" -> ruta.setTipoTerreno(valor);
                                case "tecnica" -> ruta.setDificultadTecnica(valor);
                                case "fisica" -> ruta.setDificultadFisica(valor);
                                default -> { return; }
                        }
                        guardarRutaInline(ruta);
                });
        }

        private void configurarComboInline(TableColumn<Ruta, String> columna, String campo, List<String> opciones) {
                columna.setCellFactory(javafx.scene.control.cell.ComboBoxTableCell.forTableColumn(FXCollections.observableArrayList(opciones)));
                columna.setOnEditCommit(event -> {
                        Ruta ruta = event.getRowValue(); String valor = event.getNewValue();
                        switch (campo) {
                                case "terreno" -> ruta.setTipoTerreno(valor);
                                case "tecnica" -> ruta.setDificultadTecnica(valor);
                                case "fisica" -> ruta.setDificultadFisica(valor);
                                default -> { return; }
                        }
                        guardarRutaInline(ruta);
                });
        }

        private void guardarRutaInline(Ruta ruta) {
                if (!rutaDAO.estaActiva(ruta.getId())) { mostrarAlerta("Ruta inactiva", "Reactiva la Ruta antes de editar sus celdas."); cargarRutas(); return; }
                if (!rutaDAO.actualizar(ruta)) { mostrarAlerta("No se pudo guardar", "La Ruta conserva sus datos anteriores."); }
                cargarRutas(); tablaRutas.getSelectionModel().select(ruta);
        }

        private void configurarCheckpoints() {

                checkpointsRutaListView.setCellFactory(listView -> new javafx.scene.control.ListCell<Checkpoint>() {

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
                });
        }

        private void cargarCheckpointsDeRuta(Ruta ruta) {

                if (ruta == null) {
                        checkpointsRutaListView.getItems().clear();
                        estadoItinerarioLabel.setText("Selecciona una Ruta para ver su estado.");
                        return;
                }

                List<Checkpoint> checkpoints = rutaCheckpointDAO.obtenerPorRuta(ruta.getId());

                checkpointsRutaListView.setItems(
                                FXCollections.observableArrayList(checkpoints));
                estadoItinerarioLabel.setText(checkpoints.isEmpty()
                                ? "Borrador · sin Checkpoints"
                                : checkpoints.size() + " Checkpoints · Completa");
        }

        private void configurarEquipamiento() {

                equipamientosListView.setItems(
                                equipamientosRutaSeleccionada);

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
        }

        private void cargarEquipamientoDeRuta(Ruta ruta) {

                equipamientosRutaSeleccionada.clear();

                if (ruta == null) {
                        return;
                }

                List<RutaEquipamiento> relaciones = rutaEquipamientoDAO.obtenerPorRuta(ruta.getId());

                for (RutaEquipamiento relacion : relaciones) {

                        Equipamiento equipamiento = EquipamientoDAO.obtenerPorId(
                                        relacion.getEquipamientoId());

                        if (equipamiento != null) {

                                equipamientosRutaSeleccionada.add(
                                                new EquipamientoRequerido(
                                                                equipamiento,
                                                                relacion.getCantidadRequerida()));
                        }
                }
        }

        private void cargarRutas() {

                if (mostrarInactivasCheckBox.isSelected()) {

                        rutas = FXCollections.observableArrayList(
                                        rutaDAO.obtenerTodasIncluyendoInactivas());

                } else {

                        rutas = FXCollections.observableArrayList(
                                        rutaDAO.obtenerTodas());
                }

                rutasFiltradas = new FilteredList<>(rutas);

                tablaRutas.setItems(rutasFiltradas);

                aplicarFiltros();
                cargarCheckpointsDeRuta(
                                tablaRutas.getSelectionModel().getSelectedItem());
                cargarEquipamientoDeRuta(
                                tablaRutas.getSelectionModel().getSelectedItem());
        }

        @FXML
        private void nuevaRuta() throws IOException {

                Stage stage = (Stage) tablaRutas.getScene().getWindow();

                FXMLLoader loader = new FXMLLoader(
                                getClass().getResource(
                                                "/com/example/fxml/ruta-form.fxml"));

                Parent root = loader.load();

                RutaFormController controller = loader.getController();

                controller.setRutaEnEdicion(null);

                NavigationShell.setRoot(stage.getScene(), root, "ruta-alta");
        }

        @FXML
        private void editarRuta() throws IOException {

                Ruta rutaSeleccionada = tablaRutas.getSelectionModel().getSelectedItem();

                if (rutaSeleccionada == null) {
                        mostrarAlerta(
                                        "Selecciona una ruta",
                                        "Selecciona una ruta para editar.");
                        return;
                }

                Stage stage = (Stage) tablaRutas.getScene().getWindow();

                FXMLLoader loader = new FXMLLoader(
                                getClass().getResource(
                                                "/com/example/fxml/ruta-form.fxml"));

                Parent root = loader.load();

                RutaFormController controller = loader.getController();

                controller.setRutaEnEdicion(rutaSeleccionada);


                NavigationShell.setRoot(stage.getScene(), root, "ruta-edicion");
        }

        private void abrirEdicion(Ruta rutaSeleccionada) {
                try {
                        Stage stage = (Stage) tablaRutas.getScene().getWindow();
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/fxml/ruta-form.fxml"));
                        Parent root = loader.load();
                        loader.<RutaFormController>getController().setRutaEnEdicion(rutaSeleccionada);
                        NavigationShell.setRoot(stage.getScene(), root, "ruta-edicion");
                } catch (IOException ex) { mostrarAlerta("Error", "No se pudo abrir el formulario de Ruta."); }
        }

        private void cambiarEstado(Ruta ruta) {
                boolean activa = rutaDAO.estaActiva(ruta.getId());
                Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
                confirmacion.setTitle(activa ? "Dar de baja Ruta" : "Reactivar Ruta");
                confirmacion.setHeaderText(null);
                confirmacion.setContentText("¿" + (activa ? "Dar de baja" : "Reactivar") + " la Ruta «" + ruta.getNombre() + "»?"
                                + (activa ? " Quedará excluida del uso activo." : ""));
                if (confirmacion.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
                boolean actualizado = activa ? rutaDAO.eliminar(ruta.getId()) : rutaDAO.reactivar(ruta.getId());
                if (!actualizado) mostrarAlerta("Error", "No se pudo actualizar el estado de la Ruta.");
                cargarRutas(); tablaRutas.getSelectionModel().select(ruta);
        }

        @FXML
        private void eliminarRuta() {

                Ruta rutaSeleccionada = tablaRutas.getSelectionModel().getSelectedItem();

                if (rutaSeleccionada == null) {

                        mostrarAlerta(
                                        "Selecciona una ruta",
                                        "Selecciona una ruta para eliminar.");

                        return;
                }

                if (!rutaDAO.estaActiva(rutaSeleccionada.getId())) {
                        if (rutaDAO.reactivar(rutaSeleccionada.getId())) {
                                cargarRutas();
                                mostrarAlerta("Ruta reactivada", "La Ruta volvió a estar activa.");
                        } else {
                                mostrarAlerta("Error", "No se pudo reactivar la Ruta.");
                        }
                        return;
                }

                Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);

                confirmacion.setTitle("Confirmar eliminación");
                confirmacion.setHeaderText("¿Estás seguro?");

                confirmacion.setContentText(
                                "¿Eliminar la ruta \""
                                                + rutaSeleccionada.getNombre()
                                                + "\"?");

                if (confirmacion.showAndWait().orElse(null) == ButtonType.OK) {

                        if (rutaDAO.eliminar(rutaSeleccionada.getId())) {

                                cargarRutas();

                                mostrarAlerta(
                                                "Éxito",
                                                "Ruta eliminada correctamente.");

                        } else {

                                mostrarAlerta(
                                                "Error",
                                                "No se pudo eliminar la ruta.");
                        }
                }
        }

        private void configurarFiltros() {

                filtroTerrenoCombo.getItems().addAll(
                                "Todos",
                                "Rocoso",
                                "Boscoso",
                                "Sendero",
                                "Mixto");

                filtroDificultadTecnicaCombo.getItems().addAll(
                                "Todas",
                                "Baja",
                                "Media",
                                "Alta");

                filtroDificultadFisicaCombo.getItems().addAll(
                                "Todas",
                                "Baja",
                                "Media",
                                "Alta");

                filtroTerrenoCombo.setValue("Todos");
                filtroDificultadTecnicaCombo.setValue("Todas");
                filtroDificultadFisicaCombo.setValue("Todas");

                buscarField.textProperty().addListener(
                                (observable, anterior, actual) -> aplicarFiltros());

                filtroTerrenoCombo.valueProperty().addListener(
                                (observable, anterior, actual) -> aplicarFiltros());

                filtroDificultadTecnicaCombo.valueProperty().addListener(
                                (observable, anterior, actual) -> aplicarFiltros());

                filtroDificultadFisicaCombo.valueProperty().addListener(
                                (observable, anterior, actual) -> aplicarFiltros());

                mostrarInactivasCheckBox.selectedProperty().addListener(
                                (observable, anterior, actual) -> cargarRutas());
        }

        private void aplicarFiltros() {

                if (rutasFiltradas == null) {
                        return;
                }

                String texto = buscarField.getText()
                                .trim()
                                .toLowerCase();

                String terreno = filtroTerrenoCombo.getValue();

                String dificultadTecnica = filtroDificultadTecnicaCombo.getValue();

                String dificultadFisica = filtroDificultadFisicaCombo.getValue();

                rutasFiltradas.setPredicate(ruta -> {

                        boolean coincideNombre = texto.isEmpty()
                                        || ruta.getNombre()
                                                        .toLowerCase()
                                                        .contains(texto);

                        boolean coincideTerreno = terreno.equals("Todos")
                                        || ruta.getTipoTerreno()
                                                        .equals(terreno);

                        boolean coincideDificultadTecnica = dificultadTecnica.equals("Todas")
                                        || ruta.getDificultadTecnica()
                                                        .equals(dificultadTecnica);

                        boolean coincideDificultadFisica = dificultadFisica.equals("Todas")
                                        || ruta.getDificultadFisica()
                                                        .equals(dificultadFisica);

                        return coincideNombre
                                        && coincideTerreno
                                        && coincideDificultadTecnica
                                        && coincideDificultadFisica;
                });
                actualizarEstadoTabla();
        }

        private void actualizarEstadoTabla() {
                boolean filtrosActivos = !buscarField.getText().isBlank()
                                || !"Todos".equals(filtroTerrenoCombo.getValue())
                                || !"Todas".equals(filtroDificultadTecnicaCombo.getValue())
                                || !"Todas".equals(filtroDificultadFisicaCombo.getValue())
                                || mostrarInactivasCheckBox.isSelected();
                boolean hayRutas = rutas != null && !rutas.isEmpty();
                TableEmptyState.update(tablaRutas, hayRutas, filtrosActivos,
                                "Todavía no hay rutas", "Crea una Ruta para empezar a planificar Salidas.",
                                "No hay rutas que coincidan", "Prueba con otra búsqueda o cambia los filtros.",
                                "+ Nueva Ruta", event -> {
                                        try {
                                                nuevaRuta();
                                        } catch (IOException e) {
                                                mostrarAlerta("Error", "No se pudo abrir el formulario de Ruta.");
                                        }
                                });
                TableCreateRow.update(tablaRutas, filaAltaButton,
                                tablaRutas.getItems() == null ? 0 : tablaRutas.getItems().size(),
                                filtrosActivos, 430);
        }

        private void mostrarAlerta(
                        String titulo,
                        String mensaje) {

                Alert alerta = new Alert(Alert.AlertType.INFORMATION);

                alerta.setTitle(titulo);
                alerta.setHeaderText(null);
                alerta.setContentText(mensaje);
                alerta.showAndWait();
        }

        @FXML
        private void volverDashboard() throws IOException {

                FXMLLoader loader = new FXMLLoader(
                                getClass().getResource(
                                                "/com/example/fxml/dashboard.fxml"));

                Parent root = loader.load();

                NavigationShell.setRoot(tablaRutas.getScene(), root);
        }
}
