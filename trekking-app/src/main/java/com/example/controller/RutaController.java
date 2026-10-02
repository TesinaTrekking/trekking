package com.example.controller;

import com.example.dao.RutaCheckpointDAO;
import com.example.dao.RutaDAO;
import com.example.model.Checkpoint;
import com.example.model.Ruta;

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
import javafx.stage.Stage;

public class RutaController implements Initializable {

        private final RutaDAO rutaDAO = new RutaDAO();
        private final RutaCheckpointDAO rutaCheckpointDAO = new RutaCheckpointDAO();

        private ObservableList<Ruta> rutas;
        private FilteredList<Ruta> rutasFiltradas;

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
        private Button eliminarButton;

        @FXML
        private ListView<Checkpoint> checkpointsRutaListView;

        @Override
        public void initialize(URL url, ResourceBundle rb) {

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

                configurarCheckpoints();
                configurarFiltros();
                cargarRutas();

                tablaRutas.getSortOrder().add(colNombre);
                tablaRutas.sort();

                tablaRutas.getSelectionModel()
                                .selectedItemProperty()
                                .addListener((observable, anterior, actual) -> {
                                        actualizarEstadoBotonEliminar();
                                        cargarCheckpointsDeRuta(actual);
                                });
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
                        return;
                }

                List<Checkpoint> checkpoints = rutaCheckpointDAO.obtenerPorRuta(ruta.getId());

                checkpointsRutaListView.setItems(
                                FXCollections.observableArrayList(checkpoints));
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
                actualizarEstadoBotonEliminar();

                cargarCheckpointsDeRuta(
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

                stage.setWidth(1100);
                stage.setHeight(1250);
                stage.setMinWidth(1000);
                stage.setMinHeight(850);

                stage.getScene().setRoot(root);
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

                stage.setWidth(1100);
                stage.setHeight(1250);
                stage.setMinWidth(1000);
                stage.setMinHeight(850);

                stage.getScene().setRoot(root);
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

                        mostrarAlerta(
                                        "Ruta inactiva",
                                        "La ruta seleccionada ya está inactiva.");

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

        private void actualizarEstadoBotonEliminar() {

                Ruta rutaSeleccionada = tablaRutas.getSelectionModel().getSelectedItem();

                eliminarButton.setDisable(
                                rutaSeleccionada == null
                                                || !rutaDAO.estaActiva(
                                                                rutaSeleccionada.getId()));
        }

        @FXML
        private void volverDashboard() throws IOException {

                FXMLLoader loader = new FXMLLoader(
                                getClass().getResource(
                                                "/com/example/fxml/dashboard.fxml"));

                Parent root = loader.load();

                tablaRutas.getScene().setRoot(root);
        }
}