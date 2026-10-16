package com.example.controller;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.example.service.DashboardService;
import com.example.util.NavigationShell;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

/**
 * Panel de Inicio: solo lectura + navegación.
 *
 * <p>Carga al entrar vía {@code initialize()} más botón Recargar manual,
 * sin auto-refresh (decisión Q10).
 */
public class DashboardController {

    private final DashboardService dashboardService = new DashboardService();

    @FXML
    private Label fechaHoyLabel;

    @FXML
    private Label kpiRutas;

    @FXML
    private Label kpiClientes;

    @FXML
    private Label kpiRecorridos;

    @FXML
    private Label kpiEquipo;

    @FXML
    private TableView<DashboardService.ProximoRecorrido> tablaProximos;

    @FXML
    private TableColumn<DashboardService.ProximoRecorrido, LocalDate> colFecha;

    @FXML
    private TableColumn<DashboardService.ProximoRecorrido, String> colRuta;

    @FXML
    private TableColumn<DashboardService.ProximoRecorrido, LocalTime> colHorario;

    @FXML
    private TableColumn<DashboardService.ProximoRecorrido, Integer> colParticipantes;

    @FXML
    private VBox alertasBox;

    @FXML
    private Label alertasEstado;

    @FXML
    private Label errorLabel;

    @FXML
    private void initialize() {
        colFecha.setCellValueFactory(celda -> new SimpleObjectProperty<>(
                celda.getValue() == null ? null : celda.getValue().fecha()));
        colRuta.setCellValueFactory(celda -> new SimpleStringProperty(
                celda.getValue() == null ? "" : celda.getValue().nombreRuta()));
        colHorario.setCellValueFactory(celda -> new SimpleObjectProperty<>(
                celda.getValue() == null ? null : celda.getValue().horaInicio()));
        colHorario.setCellFactory(col -> new javafx.scene.control.TableCell<>() {
            @Override
            protected void updateItem(LocalTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0
                        || getIndex() >= getTableView().getItems().size()) {
                    setText(null);
                    return;
                }
                DashboardService.ProximoRecorrido recorrido =
                        getTableView().getItems().get(getIndex());
                String inicio = recorrido.horaInicio() == null
                        ? "—"
                        : recorrido.horaInicio().toString();
                String fin = recorrido.horaFin() == null
                        ? ""
                        : "–" + recorrido.horaFin();
                setText(inicio + fin);
            }
        });
        colParticipantes.setCellValueFactory(celda ->
                new SimpleObjectProperty<>(celda.getValue() == null
                        ? null
                        : celda.getValue().participantes()));
        tablaProximos.setPlaceholder(
                new Label("Sin recorridos próximos en los siguientes 7 días."));
        cargar();
    }

    @FXML
    private void onRecargar() {
        cargar();
    }

    @FXML
    private void onVerTodosRecorridos() {
        navegar("trips");
    }

    @FXML
    private void onRutas() {
        navegar("routes");
    }

    @FXML
    private void onCheckpoints() {
        navegar("checkpoints");
    }

    @FXML
    private void onEquipamiento() {
        navegar("equipment");
    }

    @FXML
    private void onClientes() {
        navegar("clients");
    }

    @FXML
    private void onRecorridos() {
        navegar("trips");
    }

    private void cargar() {
        try {
            if (fechaHoyLabel != null) {
                fechaHoyLabel.setText(LocalDate.now().format(
                        DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            }
            DashboardService.Kpis kpis = dashboardService.obtenerKpis();
            kpiRutas.setText(String.valueOf(kpis.rutasActivas()));
            kpiClientes.setText(String.valueOf(kpis.clientesActivos()));
            kpiRecorridos.setText(String.valueOf(kpis.recorridosProximos()));
            kpiEquipo.setText(String.valueOf(kpis.equipamientoCritico()));

            List<DashboardService.ProximoRecorrido> proximos =
                    dashboardService.obtenerProximosRecorridos();
            tablaProximos.setItems(
                    FXCollections.observableArrayList(proximos));

            List<DashboardService.Alerta> alertas =
                    dashboardService.obtenerAlertas();
            mostrarAlertas(alertas);

            mostrarError(null);
        } catch (RuntimeException e) {
            mostrarError(
                    "No se pudo cargar el panel: " + e.getMessage()
                            + ". Probá con Recargar.");
        }
    }

    private void mostrarAlertas(List<DashboardService.Alerta> alertas) {
        alertasBox.getChildren().clear();
        if (alertas.isEmpty()) {
            alertasEstado.setText("Todo al día ✅");
            alertasEstado.setVisible(true);
            return;
        }
        alertasEstado.setVisible(false);
        for (DashboardService.Alerta alerta : alertas) {
            Button boton = new Button(alerta.mensaje() + "  →");
            boton.getStyleClass().add("alert-item");
            boton.setMaxWidth(Double.MAX_VALUE);
            boton.setWrapText(true);
            String destino = alerta.seccionDestino();
            boton.setOnAction(event -> navegar(destino));
            alertasBox.getChildren().add(boton);
        }
    }

    private void mostrarError(String mensaje) {
        if (errorLabel == null) {
            return;
        }
        if (mensaje == null || mensaje.isBlank()) {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        } else {
            errorLabel.setText(mensaje);
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        }
    }

    private void navegar(String seccion) {
        if (tablaProximos.getScene() == null) {
            return;
        }
        NavigationShell.navigate(tablaProximos.getScene(), seccion);
    }
}
