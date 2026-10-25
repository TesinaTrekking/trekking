package com.example.controller;

import com.example.dao.RutaDAO;
import com.example.dao.RutaCheckpointDAO;
import com.example.dao.SalidaDAO;
import com.example.service.FichaOperativaPDFService;
import com.example.model.Ruta;
import com.example.model.Salida;
import com.example.model.Checkpoint;
import com.example.model.Equipamiento;
import com.example.model.AsignacionEquipamiento;
import com.example.dao.EquipamientoDAO;
import com.example.dao.ClienteDAO;
import com.example.model.Cliente;
import com.example.model.AsignacionParticipante;
import com.example.util.NavigationShell;
import com.example.util.TableEmptyState;
import com.example.util.TableHeight;
import com.example.util.ComboBoxDisplayLabel;
import java.sql.SQLException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Locale;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;

public class SalidasController {
    private final SalidaDAO salidaDAO = new SalidaDAO();
    private final FichaOperativaPDFService fichaPDFService = new FichaOperativaPDFService();
    private final RutaDAO rutaDAO = new RutaDAO();
    @FXML private TableView<Salida> tablaSalidas;
    @FXML private TableColumn<Salida, Integer> colId;
    @FXML private TableColumn<Salida, LocalDate> colFecha;
    @FXML private TableColumn<Salida, String> colRuta;
    @FXML private TableColumn<Salida, String> colEstado;
    @FXML private TableColumn<Salida, String> colParticipantes;
    @FXML private TableColumn<Salida, String> colEquipamiento;
    @FXML private TableColumn<Salida, Salida> colAcciones;
    @FXML private Label kpiProximas, kpiRutas, kpiClientes, kpiInventario;
    @FXML private TextField buscarField;
    @FXML private ComboBox<String> estadoFiltro;
    @FXML private Button btnRealizada, btnCancelar;
    private Map<Integer, SalidaDAO.ResumenOperativo> resumenes = Map.of();
    private List<Salida> salidasOriginales = List.of();

    @FXML private void initialize() {
        tablaSalidas.getStyleClass().add("listing-table");
        tablaSalidas.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        colId.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue().id()));
        colFecha.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue().fecha()));
        colRuta.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().rutaNombre()));
        colEstado.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().estado()));
        colParticipantes.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(resumenParticipantes(data.getValue())));
        colEquipamiento.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(resumenEquipamiento(data.getValue())));
        colAcciones.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue()));
        colAcciones.setCellFactory(column -> new TableCell<>() {
            @Override protected void updateItem(Salida salida, boolean empty) {
                super.updateItem(salida, empty);
                if (empty || salida == null) { setGraphic(null); return; }
                javafx.scene.layout.HBox actions = new javafx.scene.layout.HBox(3);
                actions.getChildren().addAll(
                        com.example.util.RowActionButton.create("Editar salida", com.example.util.RowActionButton.Icon.EDIT, e -> { tablaSalidas.getSelectionModel().select(salida); editarSalida(); }),
                        com.example.util.RowActionButton.create("Descargar ficha operativa PDF", com.example.util.RowActionButton.Icon.DOWNLOAD, e -> { tablaSalidas.getSelectionModel().select(salida); generarFichaPDF(); }));
                if ("planificada".equals(salida.estado())) {
                    javafx.scene.control.Button cancelar = com.example.util.RowActionButton.create("Cancelar salida", com.example.util.RowActionButton.Icon.CANCEL, e -> { tablaSalidas.getSelectionModel().select(salida); cancelarSalida(); });
                    cancelar.getStyleClass().add("row-action-danger");
                    actions.getChildren().addAll(
                            com.example.util.RowActionButton.create("Marcar realizada", com.example.util.RowActionButton.Icon.CHECK, e -> { tablaSalidas.getSelectionModel().select(salida); marcarRealizada(); }), cancelar);
                }
                com.example.util.RowActionCell.showCentered(this, actions);
            }
        });
        estadoFiltro.setItems(FXCollections.observableArrayList(
                "Todos", "Planificada", "Realizada", "Cancelada"));
        ComboBoxDisplayLabel.prefixValue(estadoFiltro, "Estado");
        estadoFiltro.setValue("Todos");
        actualizarIndicadores(); actualizarTabla();
        tablaSalidas.getSelectionModel().selectedItemProperty().addListener((obs, anterior, seleccionada) -> actualizarAcciones(seleccionada));
        actualizarAcciones(null);
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
    @FXML private void editarSalida() {
        Salida seleccionada = tablaSalidas.getSelectionModel().getSelectedItem();
        if (seleccionada == null) { aviso(Alert.AlertType.INFORMATION, "Selecciona una Salida", "Selecciona la Salida que quieres editar."); return; }
        new EditorSalida(seleccionada).mostrar();
    }
    @FXML private void marcarRealizada() { cerrar("realizada"); }
    @FXML private void cancelarSalida() { cerrar("cancelada"); }

    @FXML private void generarFichaPDF() {
        Salida salida = tablaSalidas.getSelectionModel().getSelectedItem();
        if (salida == null) {
            aviso(Alert.AlertType.INFORMATION, "Selecciona una Salida", "Selecciona la Salida cuya ficha quieres generar.");
            return;
        }
        FileChooser selector = new FileChooser();
        selector.setTitle("Guardar ficha operativa");
        selector.getExtensionFilters().add(new FileChooser.ExtensionFilter("Documento PDF", "*.pdf"));
        selector.setInitialFileName(fichaPDFService.nombreArchivo(salida));
        java.io.File archivo = selector.showSaveDialog(tablaSalidas.getScene().getWindow());
        if (archivo == null) return;
        try {
            fichaPDFService.exportar(salida.id(), archivo.toPath());
            aviso(Alert.AlertType.INFORMATION, "Ficha generada", "La ficha operativa se guardó correctamente.");
        } catch (SQLException | IOException e) {
            aviso(Alert.AlertType.ERROR, "No se pudo generar la ficha", e.getMessage());
        }
    }

    private void cerrar(String estado) {
        Salida salida = tablaSalidas.getSelectionModel().getSelectedItem();
        if (salida == null) { aviso(Alert.AlertType.INFORMATION, "Selecciona una Salida", "Selecciona una Salida planificada para continuar."); return; }
        if (!"planificada".equals(salida.estado())) {
            aviso(Alert.AlertType.INFORMATION, "Salida ya cerrada", "Solo se pueden cerrar Salidas planificadas."); return;
        }
        String accion = "realizada".equals(estado) ? "marcar como realizada" : "cancelar";
        Alert confirmar = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Quieres " + accion + " la Salida " + salida.id() + "? Su reserva de stock se liberará y el estado no se podrá revertir desde el flujo normal.",
                ButtonType.CANCEL, ButtonType.OK);
        confirmar.setTitle("Cerrar Salida"); confirmar.setHeaderText("Confirmar cierre");
        if (confirmar.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        try { salidaDAO.cambiarEstado(salida.id(), estado); actualizarIndicadores(); actualizarTabla(); }
        catch (SQLException e) { aviso(Alert.AlertType.ERROR, "No se pudo actualizar la Salida", e.getMessage()); }
    }

    private void actualizarAcciones(Salida seleccionada) {
        boolean planificada = seleccionada != null && "planificada".equals(seleccionada.estado());
        if (btnRealizada != null) btnRealizada.setDisable(!planificada);
        if (btnCancelar != null) btnCancelar.setDisable(!planificada);
    }

    private String resumenParticipantes(Salida salida) {
        SalidaDAO.ResumenOperativo resumen = resumenes.get(salida.id());
        return resumen == null ? "—" : textoResumen(resumen.participantes());
    }

    private String resumenEquipamiento(Salida salida) {
        SalidaDAO.ResumenOperativo resumen = resumenes.get(salida.id());
        return resumen == null ? "—" : textoResumen(resumen.equipamiento());
    }

    private String textoResumen(String texto) {
        return texto == null || texto.isBlank() ? "—" : texto;
    }

    private void actualizarTabla() {
        try {
            resumenes = salidaDAO.obtenerResumenesOperativos();
            salidasOriginales = salidaDAO.obtenerTodas();
            FilteredList<Salida> filas = new FilteredList<>(FXCollections.observableArrayList(salidasOriginales), s -> true);
            tablaSalidas.setItems(filas); aplicarFiltros();
        } catch (RuntimeException e) { aviso(Alert.AlertType.ERROR, "No se pudieron cargar las Salidas", e.getMessage()); }
    }
    private void aplicarFiltros() {
        if (!(tablaSalidas.getItems() instanceof FilteredList<?> raw)) return;
        @SuppressWarnings("unchecked") FilteredList<Salida> filas = (FilteredList<Salida>) raw;
        String consulta = buscarField == null || buscarField.getText() == null ? "" : buscarField.getText().trim().toLowerCase(Locale.ROOT);
        String estadoSeleccionado = estadoFiltro == null ? "Todos" : estadoFiltro.getValue();
        String estado = switch (estadoSeleccionado == null ? "Todos" : estadoSeleccionado) {
            case "Planificada" -> "planificada";
            case "Realizada" -> "realizada";
            case "Cancelada" -> "cancelada";
            default -> null;
        };
        filas.setPredicate(s -> (consulta.isEmpty() || s.rutaNombre().toLowerCase(Locale.ROOT).contains(consulta)
                        || contieneCliente(s, consulta))
                && (estado == null || s.estado().equals(estado)));
        boolean filtrosActivos = !consulta.isEmpty() || estado != null;
        boolean haySalidas = !salidasOriginales.isEmpty();
        TableEmptyState.update(tablaSalidas, haySalidas, filtrosActivos,
                "Todavía no hay Salidas", "Planifica una Salida para comenzar a organizar una actividad.",
                "No hay Salidas que coincidan", "Prueba con otra Ruta, Cliente o estado.",
                "+ Nueva Salida", event -> iniciarWizard());
        TableHeight.update(tablaSalidas, filas.size(), 500);
    }

    private boolean contieneCliente(Salida salida, String consulta) {
        SalidaDAO.ResumenOperativo resumen = resumenes.get(salida.id());
        return resumen != null && resumen.participantes() != null
                && resumen.participantes().toLowerCase(Locale.ROOT).contains(consulta);
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

    private final class EditorSalida {
        private final Salida original;
        private final Scene escenaAplicacion = tablaSalidas.getScene();
        private final VBox content = new VBox(10);
        private final DatePicker fecha = new DatePicker();
        private final ComboBox<Ruta> rutas = new ComboBox<>();
        private final ListView<Checkpoint> puntos = new ListView<>();
        private final List<Checkpoint> checkpoints = new ArrayList<>();
        private final Label detalleRuta = new Label();
        private boolean actualizarDesdeRuta;
        private final List<Checkpoint> checkpointsGuardarEnRuta = new ArrayList<>();
        private final List<AsignacionEquipamiento> equipamiento = new ArrayList<>();
        private final List<AsignacionParticipante> participantes = new ArrayList<>();
        private final java.util.Set<Integer> refrescarEquipamiento = new java.util.HashSet<>();
        private final java.util.Set<Integer> refrescarParticipantes = new java.util.HashSet<>();
        private final ComboBox<Equipamiento> selectorEquipo = new ComboBox<>();
        private final TextField cantidadEquipo = new TextField("1");
        private final ListView<AsignacionEquipamiento> listaEquipo = new ListView<>();
        private final ComboBox<Cliente> selectorCliente = new ComboBox<>();
        private final ListView<AsignacionParticipante> listaParticipantes = new ListView<>();
        private final java.util.Set<Integer> equiposActivos = new java.util.HashSet<>();
        private boolean cambiosRealizados;

        EditorSalida(Salida original) { this.original = original; }

        void mostrar() {
            try {
                checkpoints.addAll(salidaDAO.obtenerCheckpoints(original.id()));
                equipamiento.addAll(salidaDAO.obtenerEquipamiento(original.id()));
                participantes.addAll(salidaDAO.obtenerParticipantes(original.id()));
                rutas.setItems(FXCollections.observableArrayList(rutaDAO.obtenerTodasIncluyendoInactivas()));
            } catch (SQLException e) {
                aviso(Alert.AlertType.ERROR, "No se pudo abrir la Salida", e.getMessage()); return;
            }
            rutas.setConverter(new StringConverter<>() { public String toString(Ruta r) { return r == null ? "" : r.getNombre(); } public Ruta fromString(String s) { return null; } });
            fecha.setValue(original.fecha());
            fecha.valueProperty().addListener((o, anterior, nueva) -> {
                if (!java.util.Objects.equals(anterior, nueva)) cambiosRealizados = true;
            });
            rutas.getItems().stream().filter(r -> r.getId() == original.rutaId()).findFirst().ifPresent(rutas::setValue);
            rutas.valueProperty().addListener((o, a, b) -> {
                if (b != null && b.getId() != original.rutaId() && !rutaDAO.estaActiva(b.getId())) {
                    rutas.setValue(a); aviso(Alert.AlertType.WARNING, "Ruta inactiva", "No se puede cambiar a una Ruta inactiva."); return;
                }
                if (b != null && a != null && b.getId() != a.getId()) {
                    cambiosRealizados = true;
                    actualizarDesdeRuta = false;
                    checkpointsGuardarEnRuta.clear();
                    checkpoints.clear(); checkpoints.addAll(new RutaCheckpointDAO().obtenerPorRuta(b.getId())); actualizarPuntos();
                }
                actualizarDetalle();
            });
            detalleRuta.setWrapText(true); actualizarDetalle(); actualizarPuntos();
            configurarAsignaciones();
            Button agregar = new Button("Agregar Checkpoint"), editar = new Button("Editar seleccionado"), quitar = new Button("Quitar seleccionado");
            Button refrescar = new Button("Actualizar desde ruta");
            agregar.setOnAction(e -> editarCheckpoint(null));
            editar.setOnAction(e -> { Checkpoint cp = puntos.getSelectionModel().getSelectedItem(); if (cp != null) editarCheckpoint(cp); });
            quitar.setOnAction(e -> {
                Checkpoint cp = puntos.getSelectionModel().getSelectedItem();
                if (cp != null) {
                    cambiosRealizados = true;
                    checkpoints.remove(cp);
                    checkpointsGuardarEnRuta.remove(cp);
                    actualizarPuntos();
                }
            });
            refrescar.setOnAction(e -> {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Se reemplazarán los datos copiados de Ruta y todos sus Checkpoints. Se perderán las modificaciones locales de Checkpoints. Equipamiento y Participantes se conservarán.", ButtonType.CANCEL, ButtonType.OK);
                confirm.setTitle("Actualizar desde ruta"); confirm.setHeaderText("¿Reemplazar la copia operativa?");
                if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK && rutas.getValue() != null) {
                    cambiosRealizados = true;
                    actualizarDesdeRuta = true;
                    checkpointsGuardarEnRuta.clear();
                    checkpoints.clear(); checkpoints.addAll(new RutaCheckpointDAO().obtenerPorRuta(rutas.getValue().getId())); actualizarPuntos();
                }
            });
            content.setStyle("-fx-padding: 20;");
            puntos.setPrefHeight(150);
            puntos.setMaxHeight(220);
            listaEquipo.setPrefHeight(130);
            listaEquipo.setMaxHeight(200);
            listaParticipantes.setPrefHeight(130);
            listaParticipantes.setMaxHeight(200);
            content.getChildren().addAll(new Label("Fecha de la Salida"), fecha, new Label("Ruta"), rutas, detalleRuta,
                    new Label("Checkpoints operativos (se admite una lista vacía o de cualquier tamaño)"), puntos,
                    new javafx.scene.layout.HBox(8, agregar, editar, quitar), refrescar,
                    new Label("Equipamiento"), new javafx.scene.layout.HBox(8, selectorEquipo, cantidadEquipo, boton("Agregar", this::agregarEquipo)), listaEquipo,
                    boton("Quitar equipamiento seleccionado", this::quitarEquipoSeleccionado),
                    new Label("Participantes"), new javafx.scene.layout.HBox(8, selectorCliente, boton("Agregar", this::agregarParticipante)), listaParticipantes,
                    boton("Quitar participante seleccionado", this::quitarParticipanteSeleccionado));
            Label titulo = new Label("Editar Salida " + original.id());
            titulo.getStyleClass().add("dashboard-title");
            VBox encabezado = new VBox(titulo);
            encabezado.getStyleClass().add("wizard-header");
            ScrollPane scroll = new ScrollPane(content);
            scroll.setFitToWidth(true);
            scroll.setFitToHeight(true);
            scroll.getStyleClass().add("wizard-scroll");
            Button cancelar = new Button("Cancelar");
            cancelar.setOnAction(e -> cancelarEdicion());
            Button guardar = new Button("Guardar cambios");
            guardar.getStyleClass().add("action-add");
            guardar.setOnAction(e -> guardar());
            javafx.scene.layout.HBox acciones = new javafx.scene.layout.HBox(10, cancelar, guardar);
            acciones.getStyleClass().add("wizard-actions");
            javafx.scene.layout.BorderPane root = new javafx.scene.layout.BorderPane();
            root.setTop(encabezado);
            root.setCenter(scroll);
            root.setBottom(acciones);
            NavigationShell.setNavigationGuard(escenaAplicacion, this::confirmarCambioDeSeccion);
            NavigationShell.setRoot(escenaAplicacion, root, "salida-edicion");
        }

        private void configurarAsignaciones() {
            var equipos = new ArrayList<Equipamiento>(EquipamientoDAO.obtenerTodosParaEdicion(original.id()));
            equipos.forEach(e->equiposActivos.add(e.getId()));
            EquipamientoDAO.obtenerDadosDeBaja().forEach(e -> { if (equipamiento.stream().anyMatch(a -> a.equipamiento().getId()==e.getId())) equipos.add(e); });
            equipamiento.forEach(a -> { if(equipos.stream().noneMatch(e->e.getId()==a.equipamiento().getId())) equipos.add(a.equipamiento()); });
            selectorEquipo.setItems(FXCollections.observableArrayList(equipos));
            selectorEquipo.setConverter(new StringConverter<>() { public String toString(Equipamiento e){return e==null?"":e.getNombre()+" · disponible: "+e.getCantidadDisponible();} public Equipamiento fromString(String s){return null;} });
            selectorCliente.setConverter(new StringConverter<>() { public String toString(Cliente c){return c==null?"":c.getApellido()+", "+c.getNombre()+" · DNI "+c.getDni();} public Cliente fromString(String s){return null;} });
            var todos = new ClienteDAO().obtenerTodosLosClientes();
            var clientes = new ArrayList<Cliente>();
            todos.stream().filter(Cliente::isActivo).forEach(clientes::add);
            participantes.forEach(a -> { if(clientes.stream().noneMatch(c->c.getId()==a.cliente().getId())) clientes.add(a.cliente()); });
            selectorCliente.setItems(FXCollections.observableArrayList(clientes));
            listaEquipo.setCellFactory(v->new ListCell<>(){@Override protected void updateItem(AsignacionEquipamiento a,boolean empty){super.updateItem(a,empty);setText(empty||a==null?null:a.equipamiento().getNombre()+" · "+a.cantidad());}});
            listaParticipantes.setCellFactory(v->new ListCell<>(){@Override protected void updateItem(AsignacionParticipante a,boolean empty){super.updateItem(a,empty);setText(empty||a==null?null:a.cliente().getApellido()+", "+a.cliente().getNombre()+" · DNI "+a.cliente().getDni());}});
            renderAsignaciones();
        }

        private void renderAsignaciones(){listaEquipo.setItems(FXCollections.observableArrayList(equipamiento));listaParticipantes.setItems(FXCollections.observableArrayList(participantes));}
        private Button boton(String texto,Runnable accion){Button b=new Button(texto);b.setOnAction(e->accion.run());return b;}
        private void agregarEquipo() {
            try {
                Equipamiento item = selectorEquipo.getValue();
                int cantidad = Integer.parseInt(cantidadEquipo.getText().trim());
                if (item == null || cantidad <= 0) throw new IllegalArgumentException();
                int indice = -1;
                for (int i = 0; i < equipamiento.size(); i++) {
                    if (equipamiento.get(i).equipamiento().getId() == item.getId()) indice = i;
                }
                if (indice >= 0) {
                    var asignacion = equipamiento.get(indice);
                    equipamiento.set(indice, new AsignacionEquipamiento(
                            asignacion.equipamiento(), asignacion.cantidad() + cantidad, false));
                } else {
                    equipamiento.add(new AsignacionEquipamiento(item, cantidad, false));
                }
                cambiosRealizados = true;
                renderAsignaciones();
            } catch (RuntimeException e) {
                aviso(Alert.AlertType.WARNING, "Cantidad inválida", "Selecciona un elemento y una cantidad positiva.");
            }
        }

        private void quitarEquipoSeleccionado() {
            AsignacionEquipamiento asignacion = listaEquipo.getSelectionModel().getSelectedItem();
            if (asignacion == null) return;
            cambiosRealizados = true;
            equipamiento.remove(asignacion);
            refrescarEquipamiento.add(asignacion.equipamiento().getId());
            if (!equiposActivos.contains(asignacion.equipamiento().getId())) {
                selectorEquipo.getItems().removeIf(item -> item.getId() == asignacion.equipamiento().getId());
            }
            renderAsignaciones();
        }

        private void agregarParticipante() {
            Cliente cliente = selectorCliente.getValue();
            if (cliente == null) return;
            if (participantes.stream().anyMatch(a -> a.cliente().getId() == cliente.getId())) {
                aviso(Alert.AlertType.INFORMATION, "Cliente ya asociado", "Ese Cliente ya está asociado a esta Salida.");
                return;
            }
            participantes.add(new AsignacionParticipante(cliente, false));
            cambiosRealizados = true;
            renderAsignaciones();
        }

        private void quitarParticipanteSeleccionado() {
            AsignacionParticipante asignacion = listaParticipantes.getSelectionModel().getSelectedItem();
            if (asignacion == null) return;
            cambiosRealizados = true;
            participantes.remove(asignacion);
            refrescarParticipantes.add(asignacion.cliente().getId());
            if (!asignacion.cliente().isActivo()) {
                selectorCliente.getItems().removeIf(cliente -> cliente.getId() == asignacion.cliente().getId());
            }
            renderAsignaciones();
        }

        private void actualizarPuntos() {
            puntos.setItems(FXCollections.observableArrayList(checkpoints));
            puntos.setCellFactory(v -> new ListCell<>() { @Override protected void updateItem(Checkpoint cp, boolean empty) {
                super.updateItem(cp, empty); setText(empty || cp == null ? null : cp.getHora() + " · " + cp.getNombre() + " · " + cp.getAltitud() + " m");
            }});
        }

        private void actualizarDetalle() {
            Ruta r = rutas.getValue();
            if (r != null) detalleRuta.setText(r.getNombre() + " · altitud máxima " + r.getAltitudMaxima() + " m · " + r.getTipoTerreno() + " · " + r.getDificultadTecnica() + "/" + r.getDificultadFisica());
        }

        private void editarCheckpoint(Checkpoint existente) {
            Ruta ruta = rutas.getValue(); if (ruta == null) return;
            TextField nombre = new TextField(existente == null ? "" : existente.getNombre());
            TextField hora = new TextField(existente == null ? "" : existente.getHora());
            TextField latitud = new TextField(existente == null ? "" : String.valueOf(existente.getLatitud()));
            TextField longitud = new TextField(existente == null ? "" : String.valueOf(existente.getLongitud()));
            TextField altitud = new TextField(existente == null ? "" : String.valueOf(existente.getAltitud()));
            TextField descripcion = new TextField(existente == null ? "" : existente.getDescripcion());
            CheckBox guardarRuta = new CheckBox("Guardar también en la Ruta");
            boolean activa = rutaDAO.estaActiva(ruta.getId());
            int cantidadPlantilla = new RutaCheckpointDAO().obtenerPorRuta(ruta.getId()).size();
            boolean permiteGuardar = cantidadPlantilla + 1 >= 2;
            guardarRuta.setVisible(existente == null && activa && permiteGuardar);
            guardarRuta.setManaged(existente == null && activa && permiteGuardar);
            if (!activa) guardarRuta.setTooltip(new javafx.scene.control.Tooltip("La Ruta está inactiva."));
            Dialog<ButtonType> dialog = new Dialog<>(); dialog.initOwner(escenaAplicacion.getWindow()); dialog.setTitle(existente == null ? "Agregar Checkpoint" : "Editar Checkpoint");
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
            GridPane grid = new GridPane(); grid.setHgap(8); grid.setVgap(8);
            String[] labels = {"Nombre *", "Hora (HH:mm) *", "Latitud *", "Longitud *", "Altitud (m) *", "Descripción"};
            TextField[] fields = {nombre, hora, latitud, longitud, altitud, descripcion};
            for (int i = 0; i < fields.length; i++) { grid.add(new Label(labels[i]), 0, i); grid.add(fields[i], 1, i); }
            grid.add(guardarRuta, 1, fields.length); dialog.getDialogPane().setContent(grid);
            if (dialog.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
            try {
                double altitudMaxima = rutas.getValue().getId() == original.rutaId() && !actualizarDesdeRuta
                        ? original.rutaAltitudMaxima() : ruta.getAltitudMaxima();
                Checkpoint cp = new Checkpoint(0L, nombre.getText().trim(), hora.getText().trim(),
                        Double.parseDouble(latitud.getText().trim().replace(',', '.')), Double.parseDouble(longitud.getText().trim().replace(',', '.')),
                        Double.parseDouble(altitud.getText().trim().replace(',', '.')), descripcion.getText().trim());
                if (cp.getNombre().isBlank() || !cp.getHora().matches("(?:[01]\\d|2[0-3]):[0-5]\\d") || cp.getLatitud() < -90 || cp.getLatitud() > 90
                        || cp.getLongitud() < -180 || cp.getLongitud() > 180 || cp.getAltitud() < 0 || cp.getAltitud() > altitudMaxima)
                    throw new IllegalArgumentException("Verifica los campos; la altitud debe estar entre cero y " + altitudMaxima + " m.");
                if (existente == null) {
                    checkpoints.add(cp);
                } else {
                    int index = checkpoints.indexOf(existente);
                    checkpoints.set(index, cp);
                    int indexRuta = checkpointsGuardarEnRuta.indexOf(existente);
                    if (indexRuta >= 0) checkpointsGuardarEnRuta.set(indexRuta, cp);
                }
                if (guardarRuta.isSelected()) checkpointsGuardarEnRuta.add(cp);
                cambiosRealizados = true;
                actualizarPuntos();
            } catch (RuntimeException ex) { aviso(Alert.AlertType.WARNING, "Checkpoint inválido", ex.getMessage() == null ? "Completa correctamente todos los campos." : ex.getMessage()); }
        }

        private void guardar() {
            try {
                List<Checkpoint> copia = List.copyOf(checkpoints);
                salidaDAO.actualizar(original.id(), fecha.getValue(), rutas.getValue().getId(), copia, List.copyOf(checkpointsGuardarEnRuta), actualizarDesdeRuta, List.copyOf(equipamiento), List.copyOf(participantes), java.util.Set.copyOf(refrescarEquipamiento), java.util.Set.copyOf(refrescarParticipantes));
                NavigationShell.setNavigationGuard(escenaAplicacion, null);
                volverAlListado();
            } catch (SQLException | RuntimeException e) { aviso(Alert.AlertType.ERROR, "No se pudo actualizar la Salida", e.getMessage()); }
        }

        private boolean confirmarCambioDeSeccion() {
            if (!cambiosRealizados) {
                NavigationShell.setNavigationGuard(escenaAplicacion, null);
                return true;
            }
            boolean descartar = confirmarDescarte("Cambiar de sección");
            if (descartar) NavigationShell.setNavigationGuard(escenaAplicacion, null);
            return descartar;
        }

        private boolean confirmarDescarte(String contexto) {
            Alert confirmar = new Alert(Alert.AlertType.CONFIRMATION,
                    "Hay cambios sin guardar en la Salida. ¿Quieres descartarlos?",
                    ButtonType.CANCEL, ButtonType.OK);
            confirmar.setTitle("Descartar cambios de Salida");
            confirmar.setHeaderText(contexto);
            return confirmar.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
        }

        private void cancelarEdicion() {
            if (cambiosRealizados && !confirmarDescarte("Cancelar edición")) return;
            NavigationShell.setNavigationGuard(escenaAplicacion, null);
            volverAlListado();
        }

        private void volverAlListado() {
            try {
                javafx.scene.Parent pagina = new javafx.fxml.FXMLLoader(
                        getClass().getResource("/com/example/fxml/salidas.fxml")).load();
                NavigationShell.setRoot(escenaAplicacion, pagina);
            } catch (IOException ex) {
                aviso(Alert.AlertType.ERROR, "No se pudo volver a Salidas", ex.getMessage());
            }
        }
    }

    private final class WizardSalida {
        private final Scene escenaAplicacion = tablaSalidas.getScene();
        private final RutaCheckpointDAO checkpointDAO = new RutaCheckpointDAO();
        private final List<AsignacionEquipamiento> asignaciones = new ArrayList<>();
        private final List<AsignacionParticipante> participantes = new ArrayList<>();
        private final ClienteDAO clienteDAO = new ClienteDAO();
        private final VBox content = new VBox(14);
        private final Label heading = new Label();
        private final Label progress = new Label();
        private final DatePicker fecha = new DatePicker();
        private final ComboBox<Ruta> rutas = new ComboBox<>();
        private final ComboBox<String> modo = new ComboBox<>();
        private final TextField nombre = new TextField();
        private final TextField altitud = new TextField();
        private final ComboBox<String> terreno = opciones("Rocoso", "Boscoso", "Sendero", "Mixto");
        private final ComboBox<String> tecnica = opciones("Baja", "Media", "Alta");
        private final ComboBox<String> fisica = opciones("Baja", "Media", "Alta");
        private final Label detalle = new Label();
        private final ComboBox<Equipamiento> inventario = new ComboBox<>();
        private final TextField cantidadAsignada = new TextField("1");
        private final ListView<AsignacionEquipamiento> listaAsignaciones = new ListView<>();
        private final ComboBox<Cliente> clientes = new ComboBox<>();
        private final TextField buscarCliente = new TextField();
        private final ListView<AsignacionParticipante> listaParticipantes = new ListView<>();
        private List<Cliente> todosLosClientes = List.of();
        private final Button anterior = new Button("Anterior"), siguiente = new Button("Siguiente"), cancelar = new Button("Cancelar");
        private int paso;
        private List<Ruta> rutasActivas;

        WizardSalida() {
            modo.setItems(FXCollections.observableArrayList("Elegir Ruta activa", "Crear Ruta nueva")); modo.setValue("Elegir Ruta activa");
            rutas.setConverter(new StringConverter<>() { public String toString(Ruta r) { return r == null ? "" : r.getNombre(); } public Ruta fromString(String s) { return null; } });
            inventario.setConverter(new StringConverter<>() {
                public String toString(Equipamiento item) { return item == null ? "" : item.getNombre() + " · disponible: " + item.getCantidadDisponible(); }
                public Equipamiento fromString(String s) { return null; }
            });
            listaAsignaciones.setCellFactory(view -> new ListCell<>() {
                @Override protected void updateItem(AsignacionEquipamiento item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.equipamiento().getNombre() + " · " + item.cantidad()
                            + (item.nuevo() ? " · nuevo" : ""));
                }
            });
            clientes.setConverter(new StringConverter<>() {
                public String toString(Cliente c) { return c == null ? "" : c.getApellido() + ", " + c.getNombre() + " · DNI " + c.getDni(); }
                public Cliente fromString(String s) { return null; }
            });
            listaParticipantes.setCellFactory(view -> new ListCell<>() {
                @Override protected void updateItem(AsignacionParticipante item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.cliente().getApellido() + ", " + item.cliente().getNombre()
                            + " · DNI " + item.cliente().getDni() + (item.nuevo() ? " · nuevo" : ""));
                }
            });
            buscarCliente.textProperty().addListener((o, a, b) -> actualizarClientesFiltrados());
            modo.valueProperty().addListener((o, a, b) -> render()); rutas.valueProperty().addListener((o,a,b) -> actualizarDetalle());
            anterior.setOnAction(e -> { if (paso > 0) { paso--; render(); } });
            siguiente.setOnAction(e -> avanzar()); cancelar.setOnAction(e -> cancelarWizard());
            content.setStyle("-fx-padding: 22;");
        }
        private ComboBox<String> opciones(String... valores) { return new ComboBox<>(FXCollections.observableArrayList(valores)); }
        void mostrar() {
            rutasActivas = rutaDAO.obtenerTodas(); rutas.setItems(FXCollections.observableArrayList(rutasActivas));
            inventario.setItems(FXCollections.observableArrayList(EquipamientoDAO.obtenerTodos()));
            todosLosClientes = clienteDAO.obtenerTodosLosClientes();
            render();
            VBox encabezado = new VBox(6, new Label("Nueva Salida"), progress);
            encabezado.getStyleClass().add("wizard-header");
            ScrollPane scroll = new ScrollPane(content);
            scroll.setFitToWidth(true);
            scroll.setFitToHeight(true);
            scroll.getStyleClass().add("wizard-scroll");
            javafx.scene.layout.BorderPane root = new javafx.scene.layout.BorderPane();
            root.setTop(encabezado);
            root.setCenter(scroll);
            root.setBottom(new javafx.scene.layout.HBox(10, cancelar, anterior, siguiente));
            root.getBottom().getStyleClass().add("wizard-actions");
            NavigationShell.setNavigationGuard(escenaAplicacion, this::confirmarCambioDeSeccion);
            NavigationShell.setRoot(escenaAplicacion, root, "salida-alta");
        }
        private void render() {
            String[] titulos = {"Paso 1 de 5 · Datos de la salida", "Paso 2 de 5 · Ruta", "Paso 3 de 5 · Equipamiento", "Paso 4 de 5 · Participantes", "Paso 5 de 5 · Revisión"};
            heading.setText(titulos[paso]);
            progress.setText(java.util.stream.IntStream.range(0, titulos.length)
                    .mapToObj(i -> (i < paso ? "✓ " : i == paso ? "● " : "○ ") + titulos[i].substring(titulos[i].indexOf('·') + 2))
                    .collect(java.util.stream.Collectors.joining("     ")));
            content.getChildren().clear();
            content.getChildren().add(heading);
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
            if (paso == 2) renderEquipamiento();
            if (paso == 3) renderParticipantes();
            if (paso == 4) {
                Label revision = new Label(resumen());
                revision.setWrapText(true);
                revision.setMaxWidth(Double.MAX_VALUE);
                content.getChildren().add(revision);
            }
            anterior.setDisable(paso == 0); siguiente.setText(paso == 4 ? "Confirmar Salida" : "Siguiente");
        }
        private boolean confirmarCambioDeSeccion() {
            if (!hayCambios()) return true;
            Alert confirmar = new Alert(Alert.AlertType.CONFIRMATION,
                    "Hay datos ingresados en la Salida. ¿Quieres descartar el borrador?",
                    ButtonType.CANCEL, ButtonType.OK);
            confirmar.setTitle("Descartar Nueva Salida");
            confirmar.setHeaderText("Cambiar de sección");
            boolean descartar = confirmar.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
            if (descartar) NavigationShell.setNavigationGuard(escenaAplicacion, null);
            return descartar;
        }
        private boolean hayCambios() {
            return fecha.getValue() != null || rutas.getValue() != null || isCreandoRuta()
                    || inventario.getValue() != null || !"1".equals(cantidadAsignada.getText().trim())
                    || clientes.getValue() != null || !asignaciones.isEmpty()
                    || !participantes.isEmpty() || !nombre.getText().isBlank()
                    || !altitud.getText().isBlank() || terreno.getValue() != null
                    || tecnica.getValue() != null || fisica.getValue() != null
                    || !buscarCliente.getText().isBlank();
        }
        private void cancelarWizard() {
            if (hayCambios() && !confirmarCambioDeSeccion()) return;
            cerrarWizard();
        }
        private void cerrarWizard() {
            NavigationShell.setNavigationGuard(escenaAplicacion, null);
            try {
                javafx.scene.Parent pagina = new javafx.fxml.FXMLLoader(
                        getClass().getResource("/com/example/fxml/salidas.fxml")).load();
                NavigationShell.setRoot(escenaAplicacion, pagina);
            } catch (IOException ex) {
                aviso(Alert.AlertType.ERROR, "No se pudo volver a Salidas", ex.getMessage());
            }
        }
        private VBox campo(String titulo, TextField field) { return new VBox(4, new Label(titulo), field); }
        private void renderEquipamiento() {
            listaAsignaciones.setPrefHeight(150);
            listaAsignaciones.setMaxHeight(220);
            listaAsignaciones.setItems(FXCollections.observableArrayList(asignaciones));
            Button agregar = new Button("Agregar elemento"); agregar.setOnAction(e -> agregarExistente());
            Button quitar = new Button("Quitar seleccionado"); quitar.setOnAction(e -> {
                AsignacionEquipamiento seleccionado = listaAsignaciones.getSelectionModel().getSelectedItem();
                if (seleccionado != null) { asignaciones.remove(seleccionado); renderEquipamiento(); }
            });
            Button crear = new Button("Crear y agregar"); crear.setOnAction(e -> crearYAgregar());
            content.getChildren().addAll(new Label("Elementos activos · disponible = total menos reservas"),
                    new javafx.scene.layout.HBox(8, inventario, cantidadAsignada, agregar), listaAsignaciones, quitar, crear);
        }
        private void renderParticipantes() {
            listaParticipantes.setPrefHeight(150);
            listaParticipantes.setMaxHeight(220);
            buscarCliente.setPromptText("Buscar por DNI, nombre o apellido");
            actualizarClientesFiltrados();
            listaParticipantes.setItems(FXCollections.observableArrayList(participantes));
            Button agregar = new Button("Agregar Cliente seleccionado");
            agregar.setOnAction(e -> agregarClienteExistente());
            Button crear = new Button("Crear Cliente nuevo");
            crear.setOnAction(e -> crearClienteParticipante());
            Button quitar = new Button("Quitar seleccionado");
            quitar.setOnAction(e -> {
                AsignacionParticipante seleccionado = listaParticipantes.getSelectionModel().getSelectedItem();
                if (seleccionado != null) { participantes.remove(seleccionado); render(); }
            });
            content.getChildren().addAll(new Label("Clientes activos · puedes confirmar sin Participantes"),
                    buscarCliente, clientes, agregar, crear, new Label("Participantes de esta Salida"), listaParticipantes, quitar);
        }
        private void actualizarClientesFiltrados() {
            String consulta = buscarCliente.getText() == null ? "" : buscarCliente.getText().trim().toLowerCase();
            var filtrados = todosLosClientes.stream().filter(Cliente::isActivo)
                    .filter(c -> consulta.isEmpty() || c.getDni().toLowerCase().contains(consulta)
                            || c.getNombre().toLowerCase().contains(consulta) || c.getApellido().toLowerCase().contains(consulta))
                    .toList();
            clientes.setItems(FXCollections.observableArrayList(filtrados));
            if (clientes.getValue() != null && !filtrados.contains(clientes.getValue())) clientes.setValue(null);
        }
        private void agregarClienteExistente() {
            Cliente cliente = clientes.getValue();
            if (cliente == null) { aviso(Alert.AlertType.WARNING, "Selecciona un Cliente", "Busca y selecciona un Cliente activo."); return; }
            if (participantes.stream().anyMatch(p -> p.cliente().getId() == cliente.getId())) {
                aviso(Alert.AlertType.INFORMATION, "Cliente ya asociado", "Ese Cliente ya está asociado a esta Salida."); return;
            }
            participantes.add(new AsignacionParticipante(cliente, false)); render();
        }
        private void crearClienteParticipante() {
            Dialog<Cliente> dialog = new Dialog<>();
            dialog.initOwner(escenaAplicacion.getWindow()); dialog.initModality(Modality.WINDOW_MODAL); dialog.setTitle("Nuevo Cliente para la Salida");
            ButtonType guardar = new ButtonType("Agregar a la Salida", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(guardar, ButtonType.CANCEL);
            TextField dni = new TextField(), nombre = new TextField(), apellido = new TextField(), email = new TextField(), telefono = new TextField();
            DatePicker nacimiento = new DatePicker();
            ComboBox<String> sexo = new ComboBox<>(FXCollections.observableArrayList("Masculino", "Femenino", "No binario", "Otro", "Prefiero no decir"));
            TextField emergenciaNombre = new TextField(), emergenciaTelefono = new TextField(), emergenciaRelacion = new TextField();
            CheckBox autorizacion = new CheckBox("Autorización de menores confirmada");
            TextField tutorNombre = new TextField(), tutorApellido = new TextField(), tutorDni = new TextField(), tutorTelefono = new TextField();
            Label fichaMedica = new Label("Sin archivo seleccionado");
            File[] archivoFicha = {null};
            Button seleccionarFicha = new Button("Seleccionar ficha médica PDF");
            seleccionarFicha.setOnAction(e -> {
                FileChooser selector = new FileChooser(); selector.setTitle("Seleccionar ficha médica");
                selector.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivos PDF", "*.pdf", "*.PDF"));
                File archivo = selector.showOpenDialog(escenaAplicacion.getWindow());
                if (archivo != null) { archivoFicha[0] = archivo; fichaMedica.setText(archivo.getName()); }
            });
            GridPane formulario = new GridPane(); formulario.setHgap(10); formulario.setVgap(7); formulario.setPadding(new javafx.geometry.Insets(12));
            Object[][] campos = {{"DNI *", dni}, {"Nombre *", nombre}, {"Apellido *", apellido}, {"Nacimiento *", nacimiento},
                    {"Email", email}, {"Teléfono *", telefono}, {"Sexo *", sexo}, {"Contacto de emergencia", emergenciaNombre},
                    {"Teléfono de emergencia", emergenciaTelefono}, {"Relación", emergenciaRelacion}, {"Tutor", tutorNombre},
                    {"Apellido del tutor", tutorApellido}, {"DNI del tutor", tutorDni}, {"Teléfono del tutor", tutorTelefono},
                    {"Ficha médica (opcional)", new javafx.scene.layout.HBox(8, seleccionarFicha, fichaMedica)}};
            for (int i = 0; i < campos.length; i++) {
                formulario.add(new Label((String) campos[i][0]), 0, i); formulario.add((javafx.scene.Node) campos[i][1], 1, i);
                if (campos[i][1] instanceof Control control) control.setMaxWidth(Double.MAX_VALUE);
            }
            formulario.add(autorizacion, 1, campos.length);
            dialog.getDialogPane().setContent(new ScrollPane(formulario));
            dialog.setResultConverter(button -> button == guardar ? new Cliente(0, dni.getText().trim(), nombre.getText().trim(),
                    apellido.getText().trim(), nacimiento.getValue(), email.getText().trim(), telefono.getText().trim(), sexo.getValue(),
                    emergenciaNombre.getText().trim(), emergenciaTelefono.getText().trim(), emergenciaRelacion.getText().trim(),
                    autorizacion.isSelected(), tutorNombre.getText().trim(), tutorApellido.getText().trim(), tutorDni.getText().trim(),
                    tutorTelefono.getText().trim(), true, LocalDate.now(), LocalDate.now()) : null);
            Cliente nuevo = dialog.showAndWait().orElse(null);
            if (nuevo == null) return;
            String error = ClienteValidator.validar(nuevo.getDni(), nuevo.getNombre(), nuevo.getApellido(), nuevo.getFechaNacimiento(),
                    nuevo.getEmail(), nuevo.getTelefono(), nuevo.getSexo(), nuevo.getContactoEmergenciaNombre(),
                    nuevo.getContactoEmergenciaTelefono(), nuevo.getContactoEmergenciaRelacion(), nuevo.isAutorizacionMenores(),
                    nuevo.getTutorNombre(), nuevo.getTutorApellido(), nuevo.getTutorDni(), nuevo.getTutorTelefono());
            if (error != null) { aviso(Alert.AlertType.ERROR, "Revisa los datos del Cliente", error); return; }
            byte[] contenidoFicha = null;
            String nombreFicha = null;
            if (archivoFicha[0] != null) {
                if (archivoFicha[0].length() > 2L * 1024 * 1024
                        || !archivoFicha[0].getName().toLowerCase().endsWith(".pdf")) {
                    aviso(Alert.AlertType.ERROR, "Ficha médica no válida", "Elige un PDF de hasta 2 MB."); return;
                }
                try {
                    contenidoFicha = Files.readAllBytes(archivoFicha[0].toPath());
                    if (contenidoFicha.length < 5 || contenidoFicha[0] != '%'
                            || contenidoFicha[1] != 'P' || contenidoFicha[2] != 'D'
                            || contenidoFicha[3] != 'F' || contenidoFicha[4] != '-') {
                        aviso(Alert.AlertType.ERROR, "Ficha médica no válida", "El archivo seleccionado no tiene formato PDF válido."); return;
                    }
                    nombreFicha = archivoFicha[0].getName();
                } catch (IOException ex) {
                    aviso(Alert.AlertType.ERROR, "No se pudo leer la ficha médica", ex.getMessage()); return;
                }
            }
            Cliente existente = todosLosClientes.stream().filter(c -> c.getDni().trim().equals(nuevo.getDni())).findFirst().orElse(null);
            if (existente != null) {
                if (!existente.isActivo()) {
                    aviso(Alert.AlertType.WARNING, "DNI de Cliente inactivo", "Ese DNI pertenece a un Cliente inactivo. Debe reactivarse desde Clientes antes de asociarlo.");
                } else {
                    Alert confirmar = new Alert(Alert.AlertType.CONFIRMATION);
                    confirmar.setTitle("DNI ya registrado"); confirmar.setHeaderText("Usar el Cliente existente");
                    confirmar.setContentText("El DNI pertenece a " + existente.getNombre() + " " + existente.getApellido() + ". ¿Quieres asociar ese registro?");
                    if (confirmar.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                        if (participantes.stream().noneMatch(p -> p.cliente().getId() == existente.getId())) participantes.add(new AsignacionParticipante(existente, false));
                        render();
                    }
                }
                return;
            }
            if (participantes.stream().anyMatch(p -> p.cliente().getDni().equals(nuevo.getDni()))) {
                aviso(Alert.AlertType.INFORMATION, "Cliente ya asociado", "Ese DNI ya está asociado a esta Salida."); return;
            }
            participantes.add(new AsignacionParticipante(nuevo, true, contenidoFicha, nombreFicha)); render();
        }
        private void agregarExistente() {
            Equipamiento seleccionado = inventario.getValue();
            int cantidad;
            try { cantidad = Integer.parseInt(cantidadAsignada.getText().trim()); }
            catch (RuntimeException e) { cantidad = 0; }
            if (seleccionado == null || cantidad <= 0) { aviso(Alert.AlertType.WARNING, "Cantidad inválida", "Selecciona un Elemento y una cantidad entera positiva."); return; }
            int acumulada = asignaciones.stream().filter(a -> !a.nuevo() && a.equipamiento().getId() == seleccionado.getId()).mapToInt(AsignacionEquipamiento::cantidad).sum();
            if (acumulada + cantidad > seleccionado.getCantidadDisponible()) {
                aviso(Alert.AlertType.WARNING, "Stock insuficiente", "Disponible para " + seleccionado.getNombre() + ": " + seleccionado.getCantidadDisponible() + "."); return;
            }
            int indice = -1;
            for (int i = 0; i < asignaciones.size(); i++) if (!asignaciones.get(i).nuevo() && asignaciones.get(i).equipamiento().getId() == seleccionado.getId()) indice = i;
            if (indice >= 0) { AsignacionEquipamiento anterior = asignaciones.get(indice); asignaciones.set(indice, new AsignacionEquipamiento(seleccionado, anterior.cantidad() + cantidad, false)); }
            else asignaciones.add(new AsignacionEquipamiento(seleccionado, cantidad, false));
            cantidadAsignada.setText("1"); renderEquipamiento();
        }
        private void crearYAgregar() {
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.initOwner(escenaAplicacion.getWindow());
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.setTitle("Nuevo Elemento de inventario");
            ButtonType agregar = new ButtonType("Agregar a la Salida", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(agregar, ButtonType.CANCEL);
            TextField nombreCampo = new TextField();
            ComboBox<String> categoria = new ComboBox<>(FXCollections.observableArrayList(Equipamiento.CATEGORIAS));
            ComboBox<String> estado = new ComboBox<>(FXCollections.observableArrayList(Equipamiento.ESTADOS));
            TextField totalCampo = new TextField("0"), cantidadCampo = new TextField("1");
            categoria.setValue("Campamento"); estado.setValue("Disponible");
            GridPane formulario = new GridPane(); formulario.setHgap(10); formulario.setVgap(8);
            formulario.addRow(0, new Label("Nombre"), nombreCampo);
            formulario.addRow(1, new Label("Categoría"), categoria);
            formulario.addRow(2, new Label("Estado"), estado);
            formulario.addRow(3, new Label("Cantidad total"), totalCampo);
            formulario.addRow(4, new Label("Cantidad a asignar"), cantidadCampo);
            dialog.getDialogPane().setContent(formulario);
            if (dialog.showAndWait().orElse(ButtonType.CANCEL) != agregar) return;
            String nombre = nombreCampo.getText() == null ? "" : nombreCampo.getText().trim();
            int total, cantidad;
            try { total = Integer.parseInt(totalCampo.getText().trim()); cantidad = Integer.parseInt(cantidadCampo.getText().trim()); }
            catch (RuntimeException e) { total = -1; cantidad = 0; }
            if (nombre.isBlank() || total < 0 || cantidad <= 0 || cantidad > total || categoria.getValue() == null || estado.getValue() == null) {
                aviso(Alert.AlertType.WARNING, "Datos inválidos", "Completa el nombre, categoría y estado; el total debe ser no negativo y la asignación positiva, sin superar el total."); return;
            }
            Equipamiento nuevo = new Equipamiento(0, nombre, categoria.getValue(), total, estado.getValue());
            asignaciones.add(new AsignacionEquipamiento(nuevo, cantidad, true));
            renderEquipamiento();
        }
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
            Ruta rutaSeleccionada = rutas.getValue();
            String ruta;
            String puntos;
            if (isCreandoRuta()) {
                ruta = nombre.getText().trim();
                puntos = "Sin Checkpoints";
            } else if (rutaSeleccionada == null) {
                ruta = "(sin Ruta)";
                puntos = "Sin Checkpoints";
            } else {
                ruta = rutaSeleccionada.getNombre();
                puntos = checkpointDAO.obtenerPorRuta(rutaSeleccionada.getId()).stream()
                        .map(cp -> cp.getHora() + " · " + cp.getNombre() + " · " + cp.getLatitud() + ", "
                                + cp.getLongitud() + " · " + cp.getAltitud() + " m"
                                + (cp.getDescripcion() == null || cp.getDescripcion().isBlank() ? "" : " · " + cp.getDescripcion()))
                        .collect(java.util.stream.Collectors.joining("\n"));
                if (puntos.isBlank()) puntos = "Sin Checkpoints";
            }
            String datosRuta = isCreandoRuta()
                    ? "Altitud máxima: " + altitud.getText().trim() + " m · Terreno: " + terreno.getValue()
                            + " · Dificultad técnica: " + tecnica.getValue() + " · Dificultad física: " + fisica.getValue()
                    : rutaSeleccionada == null ? "" : "Altitud máxima: " + rutaSeleccionada.getAltitudMaxima()
                            + " m · Terreno: " + rutaSeleccionada.getTipoTerreno() + " · Dificultad técnica: "
                            + rutaSeleccionada.getDificultadTecnica() + " · Dificultad física: "
                            + rutaSeleccionada.getDificultadFisica();
            String equipo = asignaciones.isEmpty() ? "ninguno" : asignaciones.stream()
                    .map(a -> "Ref #" + a.equipamiento().getId() + " · " + a.equipamiento().getNombre()
                            + " · " + a.equipamiento().getCategoria() + " × " + a.cantidad())
                    .collect(java.util.stream.Collectors.joining("\n"));
            String detalleParticipantes = participantes.isEmpty() ? "ninguno" : participantes.stream()
                    .map(p -> "Ref #" + p.cliente().getId() + " · DNI " + p.cliente().getDni() + " · "
                            + p.cliente().getApellido() + ", " + p.cliente().getNombre()
                            + " · Teléfono: " + textoResumen(p.cliente().getTelefono())
                            + " · Emergencia: " + textoResumen(p.cliente().getContactoEmergenciaNombre())
                            + " / " + textoResumen(p.cliente().getContactoEmergenciaTelefono())
                            + " / " + textoResumen(p.cliente().getContactoEmergenciaRelacion()))
                    .collect(java.util.stream.Collectors.joining("\n"));
            return "Revisa los datos antes de confirmar:\n\nFecha: " + fecha.getValue() + "\nRuta: " + ruta
                    + (datosRuta.isBlank() ? "" : "\n" + datosRuta) + "\nCheckpoints:\n" + puntos
                    + "\nEstado inicial: planificada\nEquipamiento: " + equipo + "\nParticipantes: " + detalleParticipantes
                    + "\n\nAl confirmar se guardarán la Ruta nueva (si corresponde), los Elementos nuevos asociados, los Clientes nuevos seleccionados, las reservas, las copias operativas y la Salida en una sola operación.";
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
                salidaDAO.confirmar(fecha.getValue(), rutaId, nueva, List.copyOf(asignaciones), List.copyOf(participantes));
                actualizarIndicadores(); actualizarTabla(); cerrarWizard();
            } catch (SQLException | RuntimeException ex) { aviso(Alert.AlertType.ERROR,"No se pudo confirmar la Salida",ex.getMessage()); }
        }
        private boolean isCreandoRuta() { return "Crear Ruta nueva".equals(modo.getValue()); }
    }
}
