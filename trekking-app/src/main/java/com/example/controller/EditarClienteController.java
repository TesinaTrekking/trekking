package com.example.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import com.example.dao.ClienteDAO;
import com.example.model.Cliente;

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

public class EditarClienteController {

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
    private CheckBox activoCheckBox;

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

    private final Cliente cliente;
    private final ClienteDAO clienteDAO = new ClienteDAO();
    private File fichaMedicaSeleccionada;
    private boolean eliminarFichaMedica;

    public EditarClienteController(Cliente cliente) {
        this.cliente = cliente;
    }

    @FXML
    private void initialize() {
        sexoComboBox.getItems().addAll(
                "Masculino",
                "Femenino",
                "No binario",
                "Otro",
                "Prefiero no decir");

        dniField.setText(textoSeguro(cliente.getDni()));
        nombreField.setText(textoSeguro(cliente.getNombre()));
        apellidoField.setText(textoSeguro(cliente.getApellido()));
        fechaNacimientoPicker.setValue(cliente.getFechaNacimiento());
        emailField.setText(textoSeguro(cliente.getEmail()));
        telefonoField.setText(textoSeguro(cliente.getTelefono()));
        sexoComboBox.setValue(cliente.getSexo());
        contactoNombreField.setText(
                textoSeguro(cliente.getContactoEmergenciaNombre()));
        contactoTelefonoField.setText(
                textoSeguro(cliente.getContactoEmergenciaTelefono()));
        contactoRelacionField.setText(
                textoSeguro(cliente.getContactoEmergenciaRelacion()));
        autorizacionMenoresCheckBox.setSelected(
                cliente.isAutorizacionMenores());
        tutorNombreField.setText(textoSeguro(cliente.getTutorNombre()));
        tutorApellidoField.setText(textoSeguro(cliente.getTutorApellido()));
        tutorDniField.setText(textoSeguro(cliente.getTutorDni()));
        tutorTelefonoField.setText(textoSeguro(cliente.getTutorTelefono()));
        actualizarCamposTutor();
        activoCheckBox.setSelected(cliente.isActivo());

        com.example.model.FichaMedica fichaMedica =
                clienteDAO.obtenerFichaMedica(cliente.getId());
        if (fichaMedica != null) {
            fichaMedicaLabel.setText(fichaMedica.getNombreArchivo());
        } else if (Files.isRegularFile(
                Path.of("fichas_medicas", cliente.getDni() + ".pdf"))) {
            fichaMedicaLabel.setText(cliente.getDni() + ".pdf");
        } else {
            fichaMedicaLabel.setText("Sin ficha medica");
        }
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
        eliminarFichaMedica = false;
        fichaMedicaLabel.setText(archivo.getName());
    }

    @FXML
    private void borrarFichaMedica() {
        fichaMedicaSeleccionada = null;
        eliminarFichaMedica = true;
        fichaMedicaLabel.setText("Se eliminara al guardar");
    }

    @FXML
    private void cancelarEdicion() {
        try {
            mostrarListaClientes();
        } catch (IOException e) {
            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "No se pudo abrir la lista",
                    e.getMessage());
        }
    }

    @FXML
    private void guardarCambios() {
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
                fichaMedica = Files.readAllBytes(
                        fichaMedicaSeleccionada.toPath());
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

        boolean actualizarFicha = fichaMedicaSeleccionada != null
                || eliminarFichaMedica;
        boolean actualizado = clienteDAO.actualizarCliente(
                cliente.getId(), dni, nombre, apellido, fechaNacimiento,
                email, telefono, sexo, contactoNombre, contactoTelefono,
                contactoRelacion, activoCheckBox.isSelected(),
                autorizacionMenores, tutorNombre, tutorApellido, tutorDni,
                tutorTelefono, fichaMedica, nombreFichaMedica, actualizarFicha);
        if (!actualizado) {
            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "No se pudo guardar",
                    "Verifica los datos y que el DNI no este registrado.");
            return;
        }

        try {
            if (fichaMedicaSeleccionada != null) {
                FichaMedica.guardarEnCarpeta(dni, fichaMedica);
                if (!cliente.getDni().equals(dni)) {
                    FichaMedica.borrarDeCarpeta(cliente.getDni());
                }
            } else if (eliminarFichaMedica) {
                FichaMedica.borrarDeCarpeta(cliente.getDni());
            } else if (!cliente.getDni().equals(dni)) {
                FichaMedica.renombrarEnCarpeta(cliente.getDni(), dni);
            }
        } catch (IOException e) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Ficha no actualizada en carpeta",
                    "Los datos se guardaron en la base, pero la copia de la ficha no pudo actualizarse: "
                            + e.getMessage());
        }

        try {
            mostrarListaClientes();
            mostrarAlerta(
                    Alert.AlertType.INFORMATION,
                    "Cliente actualizado",
                    "Los cambios se guardaron.");
        } catch (IOException e) {
            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "No se pudo volver a la lista",
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
