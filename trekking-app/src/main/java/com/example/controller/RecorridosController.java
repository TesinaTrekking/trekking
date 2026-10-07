package com.example.controller;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import com.example.dao.ClienteDAO;
import com.example.dao.RecorridoDAO;
import com.example.dao.RutaDAO;
import com.example.model.Cliente;
import com.example.model.Recorrido;
import com.example.model.Ruta;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.SelectionMode;
import javafx.util.StringConverter;

public class RecorridosController {

    private final RecorridoDAO recorridoDAO = new RecorridoDAO();
    private final RutaDAO rutaDAO = new RutaDAO();
    private final ClienteDAO clienteDAO = new ClienteDAO();

    @FXML
    private TableView<Recorrido> tablaRecorridos;

    @FXML
    private TableColumn<Recorrido, Integer> colId;

    @FXML
    private TableColumn<Recorrido, String> colRuta;

    @FXML
    private TableColumn<Recorrido, LocalDate> colFecha;

    @FXML
    private TableColumn<Recorrido, LocalTime> colHoraInicio;

    @FXML
    private TableColumn<Recorrido, LocalTime> colHoraFin;

    @FXML
    private ComboBox<Ruta> rutaComboBox;

    @FXML
    private DatePicker fechaPicker;

    @FXML
    private TextField horaInicioField;

    @FXML
    private TextField horaFinField;

    @FXML
    private ListView<Cliente> participantesListView;

    @FXML
    private void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colRuta.setCellValueFactory(new PropertyValueFactory<>("nombreRuta"));
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        colHoraInicio.setCellValueFactory(
                new PropertyValueFactory<>("horaInicio"));
        colHoraFin.setCellValueFactory(new PropertyValueFactory<>("horaFin"));

        rutaComboBox.setItems(FXCollections.observableArrayList(
                rutaDAO.obtenerTodasIncluyendoInactivas()));
        rutaComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Ruta ruta) {
                if (ruta == null) {
                    return "";
                }
                return ruta.getNombre()
                        + (rutaDAO.estaActiva(ruta.getId())
                                ? ""
                                : " (inactiva)");
            }

            @Override
            public Ruta fromString(String texto) {
                return null;
            }
        });

        participantesListView.getSelectionModel().setSelectionMode(
                SelectionMode.MULTIPLE);
        participantesListView.setItems(FXCollections.observableArrayList(
                clienteDAO.obtenerTodosLosClientes()));
        participantesListView.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(Cliente cliente, boolean empty) {
                super.updateItem(cliente, empty);
                if (empty || cliente == null) {
                    setText(null);
                } else {
                    setText(
                            cliente.getDni()
                                    + " - "
                                    + cliente.getApellido()
                                    + ", "
                                    + cliente.getNombre()
                                    + (cliente.isActivo() ? "" : " (inactivo)"));
                }
            }
        });

        tablaRecorridos.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, anterior, recorrido) ->
                        mostrarRecorrido(recorrido));

        actualizarTabla();
        limpiarFormulario();
    }

    @FXML
    private void nuevoRecorrido() {
        tablaRecorridos.getSelectionModel().clearSelection();
        limpiarFormulario();
    }

    @FXML
    private void guardarRecorrido() {
        Ruta ruta = rutaComboBox.getValue();
        LocalDate fecha = fechaPicker.getValue();
        if (ruta == null || fecha == null) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Datos incompletos",
                    "Selecciona una ruta y la fecha del recorrido.");
            return;
        }

        LocalTime horaInicio;
        LocalTime horaFin;
        try {
            horaInicio = parsearHora(horaInicioField.getText());
            horaFin = parsearHora(horaFinField.getText());
        } catch (DateTimeParseException e) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Formato de hora invalido",
                    "Escribe las horas en formato HH:mm, por ejemplo 08:30.");
            return;
        }
        if (horaInicio != null && horaFin != null
                && !horaFin.isAfter(horaInicio)) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Horario invalido",
                    "La hora de finalizacion debe ser posterior a la de inicio.");
            return;
        }

        Recorrido seleccionado = tablaRecorridos.getSelectionModel()
                .getSelectedItem();
        if (seleccionado == null && !rutaDAO.estaActiva(ruta.getId())) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Ruta inactiva",
                    "Para crear un recorrido debes seleccionar una ruta activa.");
            return;
        }

        Recorrido recorrido = new Recorrido(
                seleccionado == null ? 0 : seleccionado.getId(),
                ruta.getId(),
                ruta.getNombre(),
                fecha,
                horaInicio,
                horaFin);
        List<Integer> clienteIds = new ArrayList<>();
        for (Cliente cliente : participantesListView.getSelectionModel()
                .getSelectedItems()) {
            clienteIds.add(cliente.getId());
        }

        boolean guardado = seleccionado == null
                ? recorridoDAO.insertar(recorrido, clienteIds)
                : recorridoDAO.actualizar(recorrido, clienteIds);
        if (!guardado) {
            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "No se pudo guardar",
                    "El recorrido o sus participantes no pudieron guardarse. Revisa la consola para ver el detalle.");
            return;
        }

        actualizarTabla();
        limpiarFormulario();
        mostrarAlerta(
                Alert.AlertType.INFORMATION,
                "Recorrido guardado",
                "Los datos y participantes del recorrido se guardaron correctamente.");
    }

    @FXML
    private void eliminarRecorrido() {
        Recorrido seleccionado = tablaRecorridos.getSelectionModel()
                .getSelectedItem();
        if (seleccionado == null) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Recorrido no seleccionado",
                    "Selecciona un recorrido para eliminarlo.");
            return;
        }

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Eliminar recorrido");
        confirmacion.setHeaderText(null);
        confirmacion.setContentText(
                "Se eliminara el recorrido de "
                        + seleccionado.getNombreRuta()
                        + " del "
                        + seleccionado.getFecha()
                        + " y sus asociaciones con clientes. Continuar?");
        if (confirmacion.showAndWait().orElse(null) != ButtonType.OK) {
            return;
        }

        if (!recorridoDAO.eliminar(seleccionado.getId())) {
            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "No se pudo eliminar",
                    "El recorrido no pudo eliminarse. Revisa la consola para ver el detalle.");
            return;
        }
        actualizarTabla();
        limpiarFormulario();
    }

    @FXML
    private void volverDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/com/example/fxml/dashboard.fxml"));
            Parent root = loader.load();
            tablaRecorridos.getScene().setRoot(root);
        } catch (IOException e) {
            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "No se pudo volver al dashboard",
                    e.getMessage());
        }
    }

    private void mostrarRecorrido(Recorrido recorrido) {
        if (recorrido == null) {
            return;
        }

        rutaComboBox.getSelectionModel().select(
                rutaComboBox.getItems().stream()
                        .filter(ruta -> ruta.getId() == recorrido.getRutaId())
                        .findFirst()
                        .orElse(null));
        fechaPicker.setValue(recorrido.getFecha());
        horaInicioField.setText(formatearHora(recorrido.getHoraInicio()));
        horaFinField.setText(formatearHora(recorrido.getHoraFin()));
        participantesListView.getSelectionModel().clearSelection();

        List<Integer> participantes =
                recorridoDAO.obtenerClientes(recorrido.getId());
        for (int i = 0; i < participantesListView.getItems().size(); i++) {
            Cliente cliente = participantesListView.getItems().get(i);
            if (participantes.contains(cliente.getId())) {
                participantesListView.getSelectionModel().select(i);
            }
        }
    }

    private void limpiarFormulario() {
        rutaComboBox.getSelectionModel().clearSelection();
        fechaPicker.setValue(null);
        horaInicioField.clear();
        horaFinField.clear();
        participantesListView.getSelectionModel().clearSelection();
    }

    private void actualizarTabla() {
        tablaRecorridos.setItems(FXCollections.observableArrayList(
                recorridoDAO.obtenerTodos()));
    }

    private LocalTime parsearHora(String texto) {
        String valor = texto == null ? "" : texto.trim();
        return valor.isEmpty() ? null : LocalTime.parse(valor);
    }

    private String formatearHora(LocalTime hora) {
        return hora == null ? "" : hora.toString();
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
