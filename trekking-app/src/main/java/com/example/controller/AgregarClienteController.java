package com.example.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;

import com.example.dao.ClienteDAO;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Window;

public class AgregarClienteController {

    private static final long TAMANO_MAXIMO_FICHA = 2L * 1024 * 1024;

    @FXML
    private TextField dniField;

    @FXML
    private TextField nombreField;

    @FXML
    private TextField apellidoField;

    @FXML
    private DatePicker fechaNacimientoPicker;

    @FXML
    private TextField emailField;

    @FXML
    private TextField telefonoField;

    @FXML
    private ComboBox<String> sexoComboBox;

    @FXML
    private TextField contactoNombreField;

    @FXML
    private TextField contactoTelefonoField;

    @FXML
    private TextField contactoRelacionField;

    @FXML
    private CheckBox autorizacionMenoresCheckBox;

    @FXML
    private TextField tutorNombreField;

    @FXML
    private TextField tutorApellidoField;

    @FXML
    private TextField tutorDniField;

    @FXML
    private TextField tutorTelefonoField;

    @FXML
    private Label tutorNombreLabel;

    @FXML
    private Label tutorApellidoLabel;

    @FXML
    private Label tutorDniLabel;

    @FXML
    private Label tutorTelefonoLabel;

    @FXML
    private Label fichaMedicaLabel;

    private File fichaMedicaSeleccionada;
    private final ClienteDAO clienteDAO = new ClienteDAO();

    @FXML
    private void initialize() {
        sexoComboBox.getItems().addAll(
                "Masculino",
                "Femenino",
                "No binario",
                "Otro",
                "Prefiero no decir");
        actualizarCamposTutor();
    }

    @FXML
    private void cambiarAutorizacionMenores() {
        actualizarCamposTutor();
    }

    private void actualizarCamposTutor() {
        boolean mostrar = autorizacionMenoresCheckBox.isSelected();
        Label[] labels = {
            tutorNombreLabel,
            tutorApellidoLabel,
            tutorDniLabel,
            tutorTelefonoLabel
        };
        TextField[] fields = {
            tutorNombreField,
            tutorApellidoField,
            tutorDniField,
            tutorTelefonoField
        };
        for (Label label : labels) {
            label.setVisible(mostrar);
            label.setManaged(mostrar);
        }
        for (TextField field : fields) {
            field.setVisible(mostrar);
            field.setManaged(mostrar);
        }
    }

    @FXML
    private void seleccionarFichaMedica() {
        FileChooser selector = new FileChooser();
        selector.setTitle("Seleccionar ficha medica");
        selector.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                        "Archivos PDF", "*.pdf", "*.PDF"));
        Window ventana = fichaMedicaLabel.getScene().getWindow();
        File archivo = selector.showOpenDialog(ventana);
        if (archivo == null) {
            return;
        }
        if (archivo.length() > TAMANO_MAXIMO_FICHA
                || !archivo.getName().toLowerCase().endsWith(".pdf")) {
            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Ficha no valida",
                    "Elegi un PDF de hasta 2 MB.");
            return;
        }
        fichaMedicaSeleccionada = archivo;
        fichaMedicaLabel.setText(archivo.getName());
    }

    @FXML
    private void guardarCliente() {
        String dni = textoSeguro(dniField.getText()).trim();
        String nombre = textoSeguro(nombreField.getText()).trim();
        String apellido = textoSeguro(apellidoField.getText()).trim();
        LocalDate fechaNacimiento = fechaNacimientoPicker.getValue();
        String email = textoSeguro(emailField.getText()).trim();
        String telefono = textoSeguro(telefonoField.getText()).trim();
        String sexo = sexoComboBox.getValue();
        String contactoNombre = textoSeguro(contactoNombreField.getText()).trim();
        String contactoTelefono = textoSeguro(contactoTelefonoField.getText()).trim();
        String contactoRelacion = textoSeguro(contactoRelacionField.getText()).trim();
        boolean autorizacionMenores = autorizacionMenoresCheckBox.isSelected();
        String tutorNombre = textoSeguro(tutorNombreField.getText()).trim();
        String tutorApellido = textoSeguro(tutorApellidoField.getText()).trim();
        String tutorDni = textoSeguro(tutorDniField.getText()).trim();
        String tutorTelefono = textoSeguro(tutorTelefonoField.getText()).trim();

        String error = ClienteValidator.validar(
                dni, nombre, apellido, fechaNacimiento, email, telefono, sexo,
                contactoNombre, contactoTelefono, contactoRelacion,
                autorizacionMenores, tutorNombre, tutorApellido, tutorDni,
                tutorTelefono);
        if (error != null) {
            mostrarAlerta(Alert.AlertType.ERROR, "Revisa los datos", error);
            return;
        }

        byte[] fichaMedica = null;
        String nombreFichaMedica = null;
        if (fichaMedicaSeleccionada != null) {
            try {
                fichaMedica = Files.readAllBytes(fichaMedicaSeleccionada.toPath());
            } catch (IOException e) {
                mostrarAlerta(
                        Alert.AlertType.ERROR,
                        "No se pudo leer el archivo",
                        e.getMessage());
                return;
            }
            if (!esPdfValido(fichaMedica)) {
                mostrarAlerta(
                        Alert.AlertType.ERROR,
                        "Ficha no valida",
                        "Elegi un PDF de hasta 2 MB.");
                return;
            }
            nombreFichaMedica = fichaMedicaSeleccionada.getName();
        }

        boolean insertado = clienteDAO.insertarCliente(
                dni, nombre, apellido, fechaNacimiento, email, telefono, sexo,
                contactoNombre, contactoTelefono, contactoRelacion, true,
                autorizacionMenores, tutorNombre, tutorApellido, tutorDni,
                tutorTelefono, fichaMedica, nombreFichaMedica);
        if (!insertado) {
            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "No se pudo guardar",
                    "Verifica los datos y que el DNI no este registrado.");
            return;
        }

        boolean fichaGuardadaEnCarpeta = true;
        if (fichaMedica != null) {
            try {
                FichaMedica.guardarEnCarpeta(dni, fichaMedica);
            } catch (IOException e) {
                fichaGuardadaEnCarpeta = false;
            }
        }

        try {
            mostrarListaClientes();
            if (!fichaGuardadaEnCarpeta) {
                mostrarAlerta(
                        Alert.AlertType.WARNING,
                        "Ficha no guardada en carpeta",
                        "El cliente se guardo en la base de datos, pero no se pudo guardar una copia de la ficha en la carpeta.");
            }
            mostrarAlerta(
                    Alert.AlertType.INFORMATION,
                    "Cliente agregado",
                    "Los datos se guardaron correctamente.");
        } catch (IOException e) {
            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "No se pudo volver a la lista",
                    e.getMessage());
        }
    }

    @FXML
    private void irListaClientes() {
        try {
            mostrarListaClientes();
        } catch (IOException e) {
            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "No se pudo abrir la lista",
                    e.getMessage());
        }
    }

    private void mostrarListaClientes() throws IOException {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource(
                        "/com/example/fxml/lista-clientes.fxml"));
        Parent root = loader.load();
        fichaMedicaLabel.getScene().setRoot(root);
    }

    private boolean esPdfValido(byte[] contenido) {
        return contenido.length <= TAMANO_MAXIMO_FICHA
                && contenido.length >= 5
                && contenido[0] == '%'
                && contenido[1] == 'P'
                && contenido[2] == 'D'
                && contenido[3] == 'F'
                && contenido[4] == '-';
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

    private String textoSeguro(String valor) {
        return valor == null ? "" : valor;
    }
}
