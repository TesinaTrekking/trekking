package com.example.controller;

import com.example.dao.EquipamientoDAO;
import com.example.model.Equipamiento;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class EquipamientoFormController {

    @FXML
    private TextField txtNombre;

    @FXML
    private ComboBox<String> cmbCategoria;

    @FXML
    private TextField txtCantidad;

    @FXML
    private ComboBox<String> cmbEstado;

    private Equipamiento equipamientoEditando;

    @FXML
    public void initialize() {

        cmbCategoria.getItems().addAll(
                "Campamento",
                "Navegación",
                "Comunicación",
                "Seguridad",
                "Escalada",
                "Vestimenta",
                "Hidratación",
                "Primeros Auxilios",
                "Herramientas",
                "Transporte"
        );

        cmbEstado.getItems().addAll(
                "Disponible",
                "Asignado",
                "En Reparación",
                "En Inspección",
                "Fuera de Servicio",
                "Extraviado"
        );
    }

    @FXML
    private void guardarEquipamiento() {

        String nombre = txtNombre.getText().trim();
        String categoria = cmbCategoria.getValue();
        String cantidadTexto = txtCantidad.getText().trim();
        String estado = cmbEstado.getValue();

        if (nombre.isEmpty()
                || categoria == null
                || cantidadTexto.isEmpty()
                || estado == null) {

            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Datos incompletos",
                    "Todos los campos son obligatorios.");

            return;
        }

        int cantidad;

        try {
            cantidad = Integer.parseInt(cantidadTexto);

        } catch (NumberFormatException e) {

            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Cantidad inválida",
                    "La cantidad debe contener solamente números.");

            return;
        }

        if (equipamientoEditando == null) {

            Equipamiento equipamiento = new Equipamiento(
                    0,
                    nombre,
                    categoria,
                    cantidad,
                    estado);

            EquipamientoDAO.insertar(equipamiento);

        } else {

            Equipamiento equipamiento = new Equipamiento(
                    equipamientoEditando.getId(),
                    nombre,
                    categoria,
                    cantidad,
                    estado);

            EquipamientoDAO.actualizar(equipamiento);
        }

        volverListado();
    }

    public void cargarEquipamiento(Equipamiento equipamiento) {

        equipamientoEditando = equipamiento;

        if (equipamiento == null) {
            return;
        }

        txtNombre.setText(equipamiento.getNombre());

        cmbCategoria.setValue(
                equipamiento.getCategoria());

        txtCantidad.setText(
                String.valueOf(equipamiento.getCantidad()));

        cmbEstado.setValue(
                equipamiento.getEstado());
    }

    @FXML
    private void cancelar() {
        volverListado();
    }

    private void volverListado() {

        try {

            javafx.fxml.FXMLLoader loader =
                    new javafx.fxml.FXMLLoader(
                            getClass().getResource(
                                    "/com/example/fxml/equipamiento.fxml"));

            javafx.scene.Parent root = loader.load();

            Stage stage =
                    (Stage) txtNombre.getScene().getWindow();

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