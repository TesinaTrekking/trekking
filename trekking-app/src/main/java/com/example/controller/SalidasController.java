package com.example.controller;

import com.example.dao.RutaDAO;
import com.example.dao.RutaCheckpointDAO;
import com.example.dao.SalidaDAO;
import com.example.model.Ruta;
import com.example.model.Salida;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;

public class SalidasController {
    private final SalidaDAO salidaDAO = new SalidaDAO();
    private final RutaDAO rutaDAO = new RutaDAO();
    @FXML private TableView<Salida> tablaSalidas;
    @FXML private TableColumn<Salida, Integer> colId;
    @FXML private TableColumn<Salida, LocalDate> colFecha;
    @FXML private TableColumn<Salida, String> colRuta;
    @FXML private TableColumn<Salida, String> colEstado;
    @FXML private Label kpiProximas, kpiRutas, kpiClientes, kpiInventario;
    @FXML private TextField buscarField;
    @FXML private ComboBox<String> estadoFiltro;

    @FXML private void initialize() {
        colId.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue().id()));
        colFecha.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue().fecha()));
        colRuta.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().rutaNombre()));
        colEstado.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().estado()));
        estadoFiltro.setItems(FXCollections.observableArrayList("Todos", "planificada", "realizada", "cancelada"));
        estadoFiltro.setValue("Todos");
        actualizarIndicadores(); actualizarTabla();
        buscarField.textProperty().addListener((obs, oldValue, newValue) -> aplicarFiltros());
        estadoFiltro.valueProperty().addListener((obs, oldValue, newValue) -> aplicarFiltros());
        tablaSalidas.setRowFactory(table -> new TableRow<>() {
            @Override protected void updateItem(Salida salida, boolean empty) {
                super.updateItem(salida, empty); getStyleClass().remove("salida-pendiente-vencida");
                if (!empty && salida != null && "planificada".equals(salida.estado()) && salida.fecha().isBefore(LocalDate.now()))
                    getStyleClass().add("salida-pendiente-vencida");
            }
        });
    }

    @FXML private void iniciarWizard() { new WizardSalida().mostrar(); }
    @FXML private void marcarRealizada() { cerrar("realizada"); }
    @FXML private void cancelarSalida() { cerrar("cancelada"); }

    private void cerrar(String estado) {
        Salida salida = tablaSalidas.getSelectionModel().getSelectedItem();
        if (salida == null) { aviso(Alert.AlertType.INFORMATION, "Selecciona una Salida", "Selecciona una Salida planificada para continuar."); return; }
        try { salidaDAO.cambiarEstado(salida.id(), estado); actualizarIndicadores(); actualizarTabla(); }
        catch (SQLException e) { aviso(Alert.AlertType.ERROR, "No se pudo actualizar la Salida", e.getMessage()); }
    }

    private void actualizarTabla() {
        try {
            FilteredList<Salida> filas = new FilteredList<>(FXCollections.observableArrayList(salidaDAO.obtenerTodas()), s -> true);
            tablaSalidas.setItems(filas); aplicarFiltros();
        } catch (RuntimeException e) { aviso(Alert.AlertType.ERROR, "No se pudieron cargar las Salidas", e.getMessage()); }
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
            kpiProximas.setText(String.valueOf(indicadores.get("proximas"))); kpiRutas.setText(String.valueOf(indicadores.get("rutas")));
            kpiClientes.setText(String.valueOf(indicadores.get("clientes"))); kpiInventario.setText(String.valueOf(indicadores.get("criticos")));
        } catch (RuntimeException e) { aviso(Alert.AlertType.ERROR, "No se pudieron cargar los indicadores", e.getMessage()); }
    }
    private void aviso(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alert = new Alert(tipo); alert.setTitle(titulo); alert.setHeaderText(null);
        alert.setContentText(mensaje == null ? "Ocurrió un error inesperado." : mensaje); alert.showAndWait();
    }

    private final class WizardSalida {
        private final RutaCheckpointDAO checkpointDAO = new RutaCheckpointDAO();
        private final Stage stage = new Stage();
        private final VBox content = new VBox(14);
        private final Label heading = new Label();
        private final DatePicker fecha = new DatePicker();
        private final ComboBox<Ruta> rutas = new ComboBox<>();
        private final ComboBox<String> modo = new ComboBox<>();
        private final TextField nombre = new TextField();
        private final TextField altitud = new TextField();
        private final ComboBox<String> terreno = opciones("Rocoso", "Boscoso", "Sendero", "Mixto");
        private final ComboBox<String> tecnica = opciones("Baja", "Media", "Alta");
        private final ComboBox<String> fisica = opciones("Baja", "Media", "Alta");
        private final Label detalle = new Label();
        private final Button anterior = new Button("Anterior"), siguiente = new Button("Siguiente"), cancelar = new Button("Cancelar");
        private int paso;
        private List<Ruta> rutasActivas;

        WizardSalida() {
            stage.initOwner(tablaSalidas.getScene().getWindow()); stage.initModality(Modality.WINDOW_MODAL);
            stage.setTitle("Nueva Salida");
            modo.setItems(FXCollections.observableArrayList("Elegir Ruta activa", "Crear Ruta nueva")); modo.setValue("Elegir Ruta activa");
            rutas.setConverter(new StringConverter<>() { public String toString(Ruta r) { return r == null ? "" : r.getNombre(); } public Ruta fromString(String s) { return null; } });
            modo.valueProperty().addListener((o, a, b) -> render()); rutas.valueProperty().addListener((o,a,b) -> actualizarDetalle());
            anterior.setOnAction(e -> { if (paso > 0) { paso--; render(); } });
            siguiente.setOnAction(e -> avanzar()); cancelar.setOnAction(e -> stage.close());
            content.setStyle("-fx-padding: 22;");
        }
        private ComboBox<String> opciones(String... valores) { return new ComboBox<>(FXCollections.observableArrayList(valores)); }
        void mostrar() { rutasActivas = rutaDAO.obtenerTodas(); rutas.setItems(FXCollections.observableArrayList(rutasActivas)); render(); stage.setScene(new Scene(content, 620, 520)); stage.showAndWait(); }
        private void render() {
            String[] titulos = {"Paso 1 de 5 · Datos de la salida", "Paso 2 de 5 · Ruta", "Paso 3 de 5 · Equipamiento", "Paso 4 de 5 · Participantes", "Paso 5 de 5 · Revisión"};
            heading.setText(titulos[paso]); content.getChildren().setAll(heading);
            if (paso == 0) { content.getChildren().addAll(new Label("Fecha de la salida *"), fecha); }
            if (paso == 1) {
                content.getChildren().addAll(new Label("Selecciona una Ruta activa o crea una nueva"), modo);
                if (isCreandoRuta()) {
                    content.getChildren().addAll(campo("Nombre *", nombre), campo("Altitud máxima (m) *", altitud),
                            new Label("Tipo de terreno"), terreno, new Label("Dificultad técnica"), tecnica,
                            new Label("Dificultad física"), fisica,
                            new Label("La Ruta nueva se guardará solo al confirmar la Salida. Se crea como borrador, sin Checkpoints."));
                } else { content.getChildren().addAll(new Label("Ruta activa"), rutas, detalle); actualizarDetalle(); }
            }
            if (paso == 2) content.getChildren().addAll(new Label("Equipamiento"), new Label("Sin equipamiento asignado. Puedes confirmar la Salida sin elementos."));
            if (paso == 3) content.getChildren().addAll(new Label("Participantes"), new Label("Sin participantes. Puedes confirmar la Salida sin clientes asociados."));
            if (paso == 4) content.getChildren().add(new Label(resumen()));
            anterior.setDisable(paso == 0); siguiente.setText(paso == 4 ? "Confirmar Salida" : "Siguiente");
            content.getChildren().addAll(new javafx.scene.layout.Region(), new javafx.scene.layout.HBox(10, cancelar, anterior, siguiente));
        }
        private VBox campo(String titulo, TextField field) { return new VBox(4, new Label(titulo), field); }
        private void actualizarDetalle() {
            Ruta r = rutas.getValue();
            if (r == null) { detalle.setText("Selecciona una Ruta para ver sus datos."); return; }
            String puntos = checkpointDAO.obtenerPorRuta(r.getId()).stream()
                    .map(cp -> cp.getHora() + " · " + cp.getNombre() + " · " + cp.getAltitud() + " m")
                    .collect(java.util.stream.Collectors.joining("\n"));
            detalle.setWrapText(true);
            detalle.setText(r.getNombre() + " · " + r.getAltitudMaxima() + " m · " + r.getTipoTerreno() + " · " + r.getDificultadTecnica() + "/" + r.getDificultadFisica()
                    + "\nCheckpoints que se copiarán:\n" + (puntos.isEmpty() ? "Sin Checkpoints" : puntos));
        }
        private String resumen() {
            String ruta = isCreandoRuta() ? nombre.getText().trim() : rutas.getValue() == null ? "(sin Ruta)" : rutas.getValue().getNombre();
            return "Revisa los datos antes de confirmar:\n\nFecha: " + fecha.getValue() + "\nRuta: " + ruta + "\nEstado inicial: planificada\nEquipamiento: ninguno\nParticipantes: ninguno\n\nAl confirmar se guardarán la Ruta nueva (si corresponde), su copia operativa y la Salida en una sola operación.";
        }
        private void avanzar() {
            if (paso == 0 && fecha.getValue() == null) { aviso(Alert.AlertType.WARNING, "Fecha obligatoria", "Selecciona la fecha de la Salida."); return; }
            if (paso == 1) {
                if (isCreandoRuta()) {
                    if (nombre.getText().isBlank() || altitud.getText().isBlank() || terreno.getValue() == null || tecnica.getValue() == null || fisica.getValue() == null) { aviso(Alert.AlertType.WARNING,"Datos incompletos","Completa los datos obligatorios de la Ruta."); return; }
                    try { if (Double.parseDouble(altitud.getText().trim()) < 0) throw new NumberFormatException(); }
                    catch (NumberFormatException ex) { aviso(Alert.AlertType.WARNING,"Altitud inválida","La altitud debe ser un número mayor o igual a cero."); return; }
                } else if (rutas.getValue() == null) { aviso(Alert.AlertType.WARNING,"Ruta obligatoria","Selecciona una Ruta activa."); return; }
            }
            if (paso < 4) { paso++; render(); return; }
            try {
                Ruta nueva = null; Integer rutaId = null;
                if (isCreandoRuta()) nueva = new Ruta(Ruta.normalizarNombre(nombre.getText()), Double.parseDouble(altitud.getText().trim()), terreno.getValue(), tecnica.getValue(), fisica.getValue());
                else rutaId = rutas.getValue().getId();
                salidaDAO.confirmar(fecha.getValue(), rutaId, nueva);
                stage.close(); actualizarIndicadores(); actualizarTabla();
            } catch (SQLException | RuntimeException ex) { aviso(Alert.AlertType.ERROR,"No se pudo confirmar la Salida",ex.getMessage()); }
        }
        private boolean isCreandoRuta() { return "Crear Ruta nueva".equals(modo.getValue()); }
    }
}
