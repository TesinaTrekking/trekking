package com.example.controller;

import com.example.dao.RutaDAO;
import com.example.dao.SalidaDAO;
import com.example.model.Ruta;
import com.example.model.Salida;
import java.sql.SQLException;
import java.time.LocalDate;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TableRow;
import javafx.util.StringConverter;

public class SalidasController {
    private final SalidaDAO salidaDAO = new SalidaDAO();
    private final RutaDAO rutaDAO = new RutaDAO();
    @FXML private TableView<Salida> tablaSalidas;
    @FXML private TableColumn<Salida, Integer> colId;
    @FXML private TableColumn<Salida, LocalDate> colFecha;
    @FXML private TableColumn<Salida, String> colRuta;
    @FXML private TableColumn<Salida, String> colEstado;
    @FXML private DatePicker fechaPicker;
    @FXML private ComboBox<Ruta> rutaComboBox;
    @FXML private Label kpiProximas;
    @FXML private Label kpiRutas;
    @FXML private Label kpiClientes;
    @FXML private Label kpiInventario;
    @FXML private TextField buscarField;
    @FXML private ComboBox<String> estadoFiltro;

    @FXML private void initialize() {
        colId.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue().id()));
        colFecha.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue().fecha()));
        colRuta.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().rutaNombre()));
        colEstado.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().estado()));
        rutaComboBox.setItems(FXCollections.observableArrayList(rutaDAO.obtenerTodas()));
        estadoFiltro.setItems(FXCollections.observableArrayList("Todos", "planificada", "realizada", "cancelada"));
        estadoFiltro.setValue("Todos");
        rutaComboBox.setConverter(new StringConverter<>() {
            @Override public String toString(Ruta ruta) { return ruta == null ? "" : ruta.getNombre(); }
            @Override public Ruta fromString(String value) { return null; }
        });
        actualizarIndicadores();
        actualizarTabla();
        buscarField.textProperty().addListener((obs, oldValue, newValue) -> aplicarFiltros());
        estadoFiltro.valueProperty().addListener((obs, oldValue, newValue) -> aplicarFiltros());
        tablaSalidas.setRowFactory(table -> new TableRow<>() {
            @Override protected void updateItem(Salida salida, boolean empty) {
                super.updateItem(salida, empty);
                getStyleClass().remove("salida-pendiente-vencida");
                if (!empty && salida != null && "planificada".equals(salida.estado())
                        && salida.fecha().isBefore(LocalDate.now()))
                    getStyleClass().add("salida-pendiente-vencida");
            }
        });
    }

    @FXML private void crearSalida() {
        Ruta ruta = rutaComboBox.getValue();
        LocalDate fecha = fechaPicker.getValue();
        if (ruta == null || fecha == null) {
            aviso(Alert.AlertType.WARNING, "Datos incompletos", "Selecciona una Ruta activa y la fecha de la Salida.");
            return;
        }
        try {
            salidaDAO.crear(fecha, ruta.getId());
            actualizarIndicadores();
            actualizarTabla();
            fechaPicker.setValue(null);
            rutaComboBox.setValue(null);
        } catch (SQLException | RuntimeException e) {
            aviso(Alert.AlertType.ERROR, "No se pudo guardar la Salida", e.getMessage());
        }
    }

    @FXML private void marcarRealizada() { cerrar("realizada"); }
    @FXML private void cancelarSalida() { cerrar("cancelada"); }

    private void cerrar(String estado) {
        Salida salida = tablaSalidas.getSelectionModel().getSelectedItem();
        if (salida == null) {
            aviso(Alert.AlertType.INFORMATION, "Selecciona una Salida", "Selecciona una Salida planificada para continuar.");
            return;
        }
        try {
            salidaDAO.cambiarEstado(salida.id(), estado);
            actualizarIndicadores();
            actualizarTabla();
        } catch (SQLException e) {
            aviso(Alert.AlertType.ERROR, "No se pudo actualizar la Salida", e.getMessage());
        }
    }

    private void actualizarTabla() {
        try {
            FilteredList<Salida> filas = new FilteredList<>(FXCollections.observableArrayList(salidaDAO.obtenerTodas()), s -> true);
            tablaSalidas.setItems(filas);
            aplicarFiltros();
        }
        catch (RuntimeException e) { aviso(Alert.AlertType.ERROR, "No se pudieron cargar las Salidas", e.getMessage()); }
    }

    private void aplicarFiltros() {
        if (!(tablaSalidas.getItems() instanceof FilteredList<?> raw)) return;
        @SuppressWarnings("unchecked") FilteredList<Salida> filas = (FilteredList<Salida>) raw;
        String consulta = buscarField == null || buscarField.getText() == null ? "" : buscarField.getText().trim().toLowerCase();
        String estado = estadoFiltro == null ? "Todos" : estadoFiltro.getValue();
        filas.setPredicate(s -> (consulta.isEmpty() || s.rutaNombre().toLowerCase().contains(consulta))
                && (estado == null || "Todos".equals(estado) || s.estado().equals(estado)));
    }

    private void actualizarIndicadores() {
        try {
            var indicadores = salidaDAO.obtenerIndicadores();
            kpiProximas.setText(String.valueOf(indicadores.get("proximas")));
            kpiRutas.setText(String.valueOf(indicadores.get("rutas")));
            kpiClientes.setText(String.valueOf(indicadores.get("clientes")));
            kpiInventario.setText(String.valueOf(indicadores.get("criticos")));
        } catch (RuntimeException e) {
            aviso(Alert.AlertType.ERROR, "No se pudieron cargar los indicadores", e.getMessage());
        }
    }

    private void aviso(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alert = new Alert(tipo); alert.setTitle(titulo); alert.setHeaderText(null);
        alert.setContentText(mensaje == null ? "Ocurrió un error inesperado." : mensaje); alert.showAndWait();
    }
}
