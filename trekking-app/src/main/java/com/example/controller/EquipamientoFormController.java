package com.example.controller;

import com.example.dao.EquipamientoDAO;
import com.example.model.Equipamiento;
import com.example.util.FormValidation;
import com.example.util.NavigationShell;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
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

    @FXML
    private Button guardarButton;

    @FXML
    private Label tituloLabel;

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

        FormValidation.watch(txtNombre, txtNombre.textProperty(),
                () -> textoSeguro(txtNombre.getText()).trim().isEmpty()
                        ? "El nombre es obligatorio."
                        : null);
        FormValidation.watch(cmbCategoria, cmbCategoria.valueProperty(),
                () -> cmbCategoria.getValue() == null
                        ? "Selecciona una categoria."
                        : null);
        FormValidation.watch(txtCantidad, txtCantidad.textProperty(),
                this::validarCantidad);
        FormValidation.watch(cmbEstado, cmbEstado.valueProperty(),
                () -> cmbEstado.getValue() == null
                        ? "Selecciona un estado."
                        : null);
    }

    @FXML
    private void guardarEquipamiento() {

        boolean valido = FormValidation.validateNow(txtNombre);
        valido = FormValidation.validateNow(cmbCategoria) && valido;
        valido = FormValidation.validateNow(txtCantidad) && valido;
        valido = FormValidation.validateNow(cmbEstado) && valido;
        if (!valido) {
            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Revisa los campos",
                    "Completa los datos requeridos y corrige los campos marcados.");
            return;
        }

        String nombre = textoSeguro(txtNombre.getText()).trim();
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

        int cantidad = Integer.parseInt(cantidadTexto);

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
            tituloLabel.setText("Nuevo equipamiento");
        } else {
            tituloLabel.setText("Editar equipamiento");
        }

        guardarButton.getStyleClass().removeAll(
                "action-add");
        if (!guardarButton.getStyleClass().contains("action-edit")) {
            guardarButton.getStyleClass().add("action-edit");
        }

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

    private String validarCantidad() {
        String cantidad = textoSeguro(txtCantidad.getText()).trim();
        if (cantidad.isEmpty()) {
            return "La cantidad es obligatoria.";
        }
        try {
            Integer.parseInt(cantidad);
            return null;
        } catch (NumberFormatException e) {
            return "La cantidad debe ser un numero entero.";
        }
    }

    private String textoSeguro(String valor) {
        return valor == null ? "" : valor;
    }
}