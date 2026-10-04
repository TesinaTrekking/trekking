package com.example.controller;

import com.example.dao.EquipamientoDAO;
import com.example.model.Equipamiento;

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
    private TableColumn<Equipamiento, String> colEstado;

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

        colEstado.setCellValueFactory(
                new PropertyValueFactory<>("estado"));

        cargarEquipamientoBaja();
    }

    private void cargarEquipamientoBaja() {

        ObservableList<Equipamiento> equipamientos = EquipamientoDAO.obtenerDadosDeBaja();

        tablaEquipamientoBaja.setItems(equipamientos);
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

            stage.getScene().setRoot(root);
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