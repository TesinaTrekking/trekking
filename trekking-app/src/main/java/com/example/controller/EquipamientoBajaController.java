package com.example.controller;

import com.example.dao.EquipamientoDAO;
import com.example.model.Equipamiento;
import com.example.util.NavigationShell;
import com.example.util.TableEmptyState;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

public class EquipamientoBajaController {

    @FXML
    private TableView<Equipamiento> tablaEquipamientoBaja;

    @FXML
    private TableColumn<Equipamiento, Integer> colId;

    @FXML
    private TableColumn<Equipamiento, String> colNombre;

    @FXML
    private TableColumn<Equipamiento, String> colCategoria;

    @FXML
    private TableColumn<Equipamiento, Integer> colCantidad;

    @FXML
    private TableColumn<Equipamiento, Integer> colReservada;

    @FXML
    private TableColumn<Equipamiento, Integer> colDisponible;

    @FXML
    private TableColumn<Equipamiento, String> colEstado;
    @FXML private TableColumn<Equipamiento, Equipamiento> colAcciones;

    @FXML
    public void initialize() {

        colId.setCellValueFactory(
                new PropertyValueFactory<>("id"));

        colNombre.setCellValueFactory(
                new PropertyValueFactory<>("nombre"));

        colCategoria.setCellValueFactory(
                new PropertyValueFactory<>("categoria"));

        colCantidad.setCellValueFactory(
                new PropertyValueFactory<>("cantidad"));

        colReservada.setCellValueFactory(
                new PropertyValueFactory<>("cantidadReservada"));

        colDisponible.setCellValueFactory(
                new PropertyValueFactory<>("cantidadDisponible"));

        colEstado.setCellValueFactory(
                new PropertyValueFactory<>("estado"));
        colAcciones.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue()));
        colAcciones.setCellFactory(column -> new javafx.scene.control.TableCell<>() {
            @Override protected void updateItem(Equipamiento item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                javafx.scene.control.Button reactivar = com.example.util.RowActionButton.create("Reactivar elemento", com.example.util.RowActionButton.Icon.RESTORE, e -> {
                Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION, "¿Reactivar «" + item.getNombre() + "»? Volverá al uso activo.", javafx.scene.control.ButtonType.CANCEL, javafx.scene.control.ButtonType.OK);
                confirmacion.setTitle("Reactivar elemento"); confirmacion.setHeaderText(null);
                if (confirmacion.showAndWait().orElse(javafx.scene.control.ButtonType.CANCEL) == javafx.scene.control.ButtonType.OK) { EquipamientoDAO.reactivar(item.getId()); cargarEquipamientoBaja(); }
                });
                setAlignment(javafx.geometry.Pos.CENTER);
                setGraphic(reactivar);
            }
        });

        cargarEquipamientoBaja();
    }

    private void cargarEquipamientoBaja() {

        ObservableList<Equipamiento> equipamientos = EquipamientoDAO.obtenerDadosDeBaja();

        tablaEquipamientoBaja.setItems(equipamientos);
        TableEmptyState.update(tablaEquipamientoBaja, !equipamientos.isEmpty(), false,
                "No hay elementos dados de baja", "Los elementos dados de baja aparecerán aquí.",
                "No hay elementos dados de baja", "Los elementos dados de baja aparecerán aquí.",
                null, null);
    }

    @FXML
    private void restaurarEquipamiento() {

        Equipamiento seleccionado = tablaEquipamientoBaja
                .getSelectionModel()
                .getSelectedItem();

        if (seleccionado == null) {

            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Restaurar equipamiento",
                    "Seleccione un equipo para restaurar.");

            return;
        }

        EquipamientoDAO.reactivar(
                seleccionado.getId());

        cargarEquipamientoBaja();

        mostrarAlerta(
                Alert.AlertType.INFORMATION,
                "Equipamiento",
                "Equipo restaurado correctamente.");
    }

    @FXML
    private void cerrarVentana() {

        try {

            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource(
                            "/com/example/fxml/equipamiento.fxml"));

            javafx.scene.Parent root = loader.load();

            Stage stage = (Stage) tablaEquipamientoBaja
                    .getScene()
                    .getWindow();

            NavigationShell.setRoot(stage.getScene(), root);
            stage.setTitle("Equipamiento");

        } catch (java.io.IOException e) {

            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Error",
                    "No se pudo volver al listado de equipamiento.");
        }
    }

    private void mostrarAlerta(
            Alert.AlertType tipo,
            String titulo,
            String mensaje) {

        Alert alerta = new Alert(tipo);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
}
