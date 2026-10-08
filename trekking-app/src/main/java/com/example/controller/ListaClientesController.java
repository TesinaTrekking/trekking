package com.example.controller;

import java.io.IOException;
import java.awt.Desktop;
import java.nio.file.Files;
import java.net.URL;
import java.util.ResourceBundle;

import com.example.dao.ClienteDAO;
import com.example.model.Cliente;
import com.example.util.NavigationShell;

import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.event.ActionEvent;

@SuppressWarnings("unused")
public class ListaClientesController implements Initializable {

    @FXML
    private TableView<Cliente> tablaClientes;

    @FXML
    private TextField buscarField;

    @FXML
    private CheckBox mostrarInactivosCheckBox;

    @FXML
    private TableColumn<Cliente, String> colNombre;

    @FXML
    private TableColumn<Cliente, String> colApellido;

    @FXML
    private TableColumn<Cliente, String> colEmail;

    @FXML
    private TableColumn<Cliente, String> colSexo;

    @FXML
    private TableColumn<Cliente, Boolean> colActivo;

    @FXML
    private TableColumn<Cliente, String> colDni;

    @FXML
    private TableColumn<Cliente, String> colTelefono;

    @FXML
    private TableColumn<Cliente, java.time.LocalDate> colFechaNacimiento;

    private final ClienteDAO clienteDAO = new ClienteDAO();

    private FilteredList<Cliente> clientesFiltrados;

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        tablaClientes.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY);

        colDni.setCellValueFactory(
                new PropertyValueFactory<>("dni"));

        colNombre.setCellValueFactory(
                new PropertyValueFactory<>("nombre"));

        colApellido.setCellValueFactory(
                new PropertyValueFactory<>("apellido"));

        colEmail.setCellValueFactory(
                new PropertyValueFactory<>("email"));

        colTelefono.setCellValueFactory(
                new PropertyValueFactory<>("telefono"));

        colFechaNacimiento.setCellValueFactory(
                new PropertyValueFactory<>("fechaNacimiento"));

        colSexo.setCellValueFactory(
                new PropertyValueFactory<>("sexo"));

        colActivo.setCellValueFactory(
                new PropertyValueFactory<>("activo"));

        buscarField.textProperty().addListener(
                (observable, anterior, actual) -> aplicarFiltros());

        mostrarInactivosCheckBox.selectedProperty().addListener(
                (observable, anterior, actual) -> aplicarFiltros());

        cargarClientes();
    }

    private void cargarClientes() {

        ObservableList<Cliente> clientes =
                clienteDAO.obtenerTodosLosClientes();

        clientesFiltrados = new FilteredList<>(clientes);

        tablaClientes.setItems(clientesFiltrados);

        aplicarFiltros();
    }

    private void aplicarFiltros() {

        if (clientesFiltrados == null) {
            return;
        }

        String texto =
                buscarField.getText().trim().toLowerCase();

        boolean mostrarInactivos =
                mostrarInactivosCheckBox.isSelected();

        clientesFiltrados.setPredicate(cliente -> {

            boolean coincideTexto =
                    texto.isEmpty()
                    || contiene(cliente.getDni(), texto)
                    || contiene(cliente.getNombre(), texto)
                    || contiene(cliente.getApellido(), texto);

            return coincideTexto
                    && (mostrarInactivos || cliente.isActivo());
        });
    }

    private boolean contiene(String valor, String texto) {

        return valor != null
                && valor.toLowerCase().contains(texto);
    }

    @FXML
    private void irAgregarCliente(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource(
                        "/com/example/fxml/agregar-cliente.fxml"));
        Parent root = loader.load();
        NavigationShell.setRoot(
                ((Node) event.getSource()).getScene(), root);
    }

    @FXML
    private void volverDashboard() throws IOException {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource(
                        "/com/example/fxml/dashboard.fxml"));
        Parent root = loader.load();
        NavigationShell.setRoot(tablaClientes.getScene(), root);
    }

    @FXML
    private void editarCliente() {

        Cliente clienteSeleccionado =
                tablaClientes.getSelectionModel()
                        .getSelectedItem();

        if (clienteSeleccionado == null) {

            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Cliente no seleccionado",
                    "Selecciona un cliente para editarlo.");

            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/com/example/fxml/editar-cliente.fxml"));
            loader.setController(new EditarClienteController(clienteSeleccionado));
            Parent root = loader.load();
            NavigationShell.setRoot(tablaClientes.getScene(), root);

        } catch (IOException e) {
            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "No se pudo abrir la edicion",
                    e.getMessage());
        }
    }

    @FXML
    private void abrirFichaMedica() {

        Cliente clienteSeleccionado =
                tablaClientes.getSelectionModel()
                        .getSelectedItem();

        if (clienteSeleccionado == null) {

            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Cliente no seleccionado",
                    "Selecciona un cliente para abrir su ficha medica.");

            return;
        }

        try {

            com.example.model.FichaMedica fichaMedica =
                    clienteDAO.obtenerFichaMedica(clienteSeleccionado.getId());

            if (fichaMedica != null && fichaMedica.getDatos() != null) {
                if (!Desktop.isDesktopSupported()
                        || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                    throw new IOException("El sistema no tiene disponible un visor de PDF.");
                }
                java.nio.file.Path archivoTemporal =
                        Files.createTempFile("ficha-medica-", ".pdf");
                Files.write(archivoTemporal, fichaMedica.getDatos());
                archivoTemporal.toFile().deleteOnExit();
                Desktop.getDesktop().open(archivoTemporal.toFile());
            } else {
                FichaMedica.abrirDesdeCarpeta(
                        clienteSeleccionado.getDni());
            }

        } catch (IOException e) {

            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "No se pudo abrir la ficha",
                    e.getMessage());
        }
    }

    @FXML
    private void eliminarCliente() {

        Cliente clienteSeleccionado =
                tablaClientes.getSelectionModel()
                        .getSelectedItem();

        if (clienteSeleccionado == null) {

            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Cliente no seleccionado",
                    "Selecciona un cliente para darlo de baja.");

            return;
        }

        Alert confirmacion =
                new Alert(Alert.AlertType.CONFIRMATION);

        confirmacion.setTitle("Dar de baja cliente");
        confirmacion.setHeaderText(null);

        confirmacion.setContentText(
                "Queres dar de baja a "
                + clienteSeleccionado.getNombre()
                + " "
                + clienteSeleccionado.getApellido()
                + "?");

        if (confirmacion.showAndWait().orElse(null)
                == javafx.scene.control.ButtonType.OK) {

            clienteDAO.eliminarCliente(
                    clienteSeleccionado.getId());

            cargarClientes();

            mostrarAlerta(
                    Alert.AlertType.INFORMATION,
                    "Baja realizada",
                    "El cliente ya no figura como activo.");
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
