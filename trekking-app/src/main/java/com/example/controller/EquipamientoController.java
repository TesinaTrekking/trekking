package com.example.controller;

import com.example.dao.EquipamientoDAO;
import com.example.model.Equipamiento;
import com.example.util.NavigationShell;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Optional;

public class EquipamientoController {

    @FXML
    private TableView<Equipamiento> tablaEquipamiento;

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

        cargarEquipamiento();
    }

    private void cargarEquipamiento() {

        ObservableList<Equipamiento> equipamientos =
                EquipamientoDAO.obtenerTodos();

        tablaEquipamiento.setItems(equipamientos);
    }

    @FXML
    private void nuevoEquipamiento() {

        abrirFormulario(null);
    }

    @FXML
    private void editarEquipamiento() {

        Equipamiento seleccionado =
                tablaEquipamiento.getSelectionModel().getSelectedItem();

        if (seleccionado == null) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Editar equipamiento",
                    "Seleccione un equipo para editar.");
            return;
        }

        abrirFormulario(seleccionado);
    }

    @FXML
    private void eliminarEquipamiento() {

        Equipamiento seleccionado =
                tablaEquipamiento.getSelectionModel().getSelectedItem();

        if (seleccionado == null) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Eliminar equipamiento",
                    "Seleccione un equipo para eliminar.");
            return;
        }

        Alert confirmacion = new Alert(
                Alert.AlertType.CONFIRMATION);

        confirmacion.setTitle("Eliminar equipamiento");
        confirmacion.setHeaderText(null);
        confirmacion.setContentText(
                "¿Está seguro de dar de baja el equipo seleccionado?");

        Optional<ButtonType> resultado =
                confirmacion.showAndWait();

        if (resultado.isPresent()
                && resultado.get() == ButtonType.OK) {

            EquipamientoDAO.eliminar(seleccionado.getId());
            cargarEquipamiento();

            mostrarAlerta(
                    Alert.AlertType.INFORMATION,
                    "Equipamiento",
                    "Equipo dado de baja correctamente.");
        }
    }

    @FXML
    private void mostrarEquipamientoBaja() {

        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/com/example/fxml/equipamiento-baja.fxml"));

            Parent root = loader.load();

            Stage stage = (Stage) tablaEquipamiento
                    .getScene()
                    .getWindow();

            NavigationShell.setRoot(stage.getScene(), root);
            stage.setTitle("Equipamiento dado de baja");

        } catch (IOException e) {

            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Error",
                    "No se pudo abrir la pantalla de equipos dados de baja.");
        }
    }

    @FXML
    private void volverDashboard() {

        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/com/example/fxml/dashboard.fxml"));

            Parent root = loader.load();

            Stage stage = (Stage) tablaEquipamiento
                    .getScene()
                    .getWindow();

            NavigationShell.setRoot(stage.getScene(), root);
            stage.setTitle("Trekking");

        } catch (IOException e) {

            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Error",
                    "No se pudo volver al dashboard.");
        }
    }

    private void abrirFormulario(Equipamiento equipamiento) {

        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/com/example/fxml/equipamiento-form.fxml"));

            Parent root = loader.load();

            EquipamientoFormController controller =
                    loader.getController();

            controller.cargarEquipamiento(equipamiento);

            Stage stage = (Stage) tablaEquipamiento
                    .getScene()
                    .getWindow();

            NavigationShell.setRoot(stage.getScene(), root);
            stage.setTitle(
                    equipamiento == null
                            ? "Nuevo equipamiento"
                            : "Editar equipamiento");

        } catch (IOException e) {

            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Error",
                    "No se pudo abrir el formulario de equipamiento.");
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