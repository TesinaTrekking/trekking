package com.example.controller;

import com.example.dao.RutaDAO;
import com.example.dao.RutaCheckpointDAO;
import com.example.dao.SalidaDAO;
import com.example.model.Ruta;
import com.example.model.Salida;
import com.example.model.Equipamiento;
import com.example.model.AsignacionEquipamiento;
import com.example.dao.EquipamientoDAO;
import com.example.dao.ClienteDAO;
import com.example.model.Cliente;
import com.example.model.AsignacionParticipante;
import java.sql.SQLException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.FileChooser;
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
        private final List<AsignacionEquipamiento> asignaciones = new ArrayList<>();
        private final List<AsignacionParticipante> participantes = new ArrayList<>();
        private final ClienteDAO clienteDAO = new ClienteDAO();
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
        private final ComboBox<Equipamiento> inventario = new ComboBox<>();
        private final TextField cantidadAsignada = new TextField("1");
        private final ListView<AsignacionEquipamiento> listaAsignaciones = new ListView<>();
        private final ComboBox<Cliente> clientes = new ComboBox<>();
        private final TextField buscarCliente = new TextField();
        private final ListView<AsignacionParticipante> listaParticipantes = new ListView<>();
        private List<Cliente> todosLosClientes = List.of();
        private final TextField nuevoNombre = new TextField();
        private final ComboBox<String> nuevaCategoria = new ComboBox<>(FXCollections.observableArrayList(Equipamiento.CATEGORIAS));
        private final ComboBox<String> nuevoEstado = new ComboBox<>(FXCollections.observableArrayList(Equipamiento.ESTADOS));
        private final TextField nuevoTotal = new TextField("0");
        private final TextField nuevaCantidadAsignada = new TextField("1");
        private final Button anterior = new Button("Anterior"), siguiente = new Button("Siguiente"), cancelar = new Button("Cancelar");
        private int paso;
        private List<Ruta> rutasActivas;

        WizardSalida() {
            stage.initOwner(tablaSalidas.getScene().getWindow()); stage.initModality(Modality.WINDOW_MODAL);
            stage.setTitle("Nueva Salida");
            modo.setItems(FXCollections.observableArrayList("Elegir Ruta activa", "Crear Ruta nueva")); modo.setValue("Elegir Ruta activa");
            rutas.setConverter(new StringConverter<>() { public String toString(Ruta r) { return r == null ? "" : r.getNombre(); } public Ruta fromString(String s) { return null; } });
            inventario.setConverter(new StringConverter<>() {
                public String toString(Equipamiento item) { return item == null ? "" : item.getNombre() + " · disponible: " + item.getCantidadDisponible(); }
                public Equipamiento fromString(String s) { return null; }
            });
            nuevaCategoria.setValue("Campamento"); nuevoEstado.setValue("Disponible");
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
            siguiente.setOnAction(e -> avanzar()); cancelar.setOnAction(e -> stage.close());
            content.setStyle("-fx-padding: 22;");
        }
        private ComboBox<String> opciones(String... valores) { return new ComboBox<>(FXCollections.observableArrayList(valores)); }
        void mostrar() {
            rutasActivas = rutaDAO.obtenerTodas(); rutas.setItems(FXCollections.observableArrayList(rutasActivas));
            inventario.setItems(FXCollections.observableArrayList(EquipamientoDAO.obtenerTodos()));
            todosLosClientes = clienteDAO.obtenerTodosLosClientes();
            render();
            ScrollPane scroll = new ScrollPane(content); scroll.setFitToWidth(true);
            stage.setScene(new Scene(scroll, 700, 680)); stage.showAndWait();
        }
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
            if (paso == 2) renderEquipamiento();
            if (paso == 3) renderParticipantes();
            if (paso == 4) content.getChildren().add(new Label(resumen()));
            anterior.setDisable(paso == 0); siguiente.setText(paso == 4 ? "Confirmar Salida" : "Siguiente");
            content.getChildren().addAll(new javafx.scene.layout.Region(), new javafx.scene.layout.HBox(10, cancelar, anterior, siguiente));
        }
        private VBox campo(String titulo, TextField field) { return new VBox(4, new Label(titulo), field); }
        private void renderEquipamiento() {
            listaAsignaciones.setItems(FXCollections.observableArrayList(asignaciones));
            Button agregar = new Button("Agregar elemento"); agregar.setOnAction(e -> agregarExistente());
            Button quitar = new Button("Quitar seleccionado"); quitar.setOnAction(e -> {
                AsignacionEquipamiento seleccionado = listaAsignaciones.getSelectionModel().getSelectedItem();
                if (seleccionado != null) { asignaciones.remove(seleccionado); renderEquipamiento(); }
            });
            Button crear = new Button("Crear y agregar"); crear.setOnAction(e -> crearYAgregar());
            content.getChildren().addAll(new Label("Elementos activos · disponible = total menos reservas"),
                    new javafx.scene.layout.HBox(8, inventario, cantidadAsignada, agregar), listaAsignaciones, quitar,
                    new Label("Crear un Elemento de inventario para esta Salida"), campo("Nombre", nuevoNombre),
                    new javafx.scene.layout.HBox(8, nuevaCategoria, nuevoEstado),
                    new javafx.scene.layout.HBox(8, campo("Cantidad total", nuevoTotal), campo("Cantidad a asignar", nuevaCantidadAsignada)), crear);
        }
        private void renderParticipantes() {
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
            dialog.initOwner(stage); dialog.initModality(Modality.WINDOW_MODAL); dialog.setTitle("Nuevo Cliente para la Salida");
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
                File archivo = selector.showOpenDialog(stage);
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
            String nombre = nuevoNombre.getText() == null ? "" : nuevoNombre.getText().trim();
            int total, cantidad;
            try { total = Integer.parseInt(nuevoTotal.getText().trim()); cantidad = Integer.parseInt(nuevaCantidadAsignada.getText().trim()); }
            catch (RuntimeException e) { total = -1; cantidad = 0; }
            if (nombre.isBlank() || total < 0 || cantidad <= 0 || cantidad > total || nuevaCategoria.getValue() == null || nuevoEstado.getValue() == null) {
                aviso(Alert.AlertType.WARNING, "Datos inválidos", "Completa el nombre, categoría y estado; el total debe ser no negativo y la asignación positiva, sin superar el total."); return;
            }
            Equipamiento nuevo = new Equipamiento(0, nombre, nuevaCategoria.getValue(), total, nuevoEstado.getValue());
            asignaciones.add(new AsignacionEquipamiento(nuevo, cantidad, true));
            nuevoNombre.clear(); nuevoTotal.setText("0"); nuevaCantidadAsignada.setText("1"); renderEquipamiento();
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
            String ruta = isCreandoRuta() ? nombre.getText().trim() : rutas.getValue() == null ? "(sin Ruta)" : rutas.getValue().getNombre();
            String equipo = asignaciones.isEmpty() ? "ninguno" : asignaciones.stream().map(a -> a.equipamiento().getNombre() + " × " + a.cantidad()).collect(java.util.stream.Collectors.joining(", "));
            String detalleParticipantes = participantes.isEmpty() ? "ninguno" : participantes.stream()
                    .map(p -> p.cliente().getApellido() + ", " + p.cliente().getNombre() + " · DNI " + p.cliente().getDni())
                    .collect(java.util.stream.Collectors.joining(", "));
            return "Revisa los datos antes de confirmar:\n\nFecha: " + fecha.getValue() + "\nRuta: " + ruta + "\nEstado inicial: planificada\nEquipamiento: " + equipo + "\nParticipantes: " + detalleParticipantes + "\n\nAl confirmar se guardarán la Ruta nueva (si corresponde), los Elementos nuevos asociados, los Clientes nuevos seleccionados, las reservas, las copias operativas y la Salida en una sola operación.";
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
                stage.close(); actualizarIndicadores(); actualizarTabla();
            } catch (SQLException | RuntimeException ex) { aviso(Alert.AlertType.ERROR,"No se pudo confirmar la Salida",ex.getMessage()); }
        }
        private boolean isCreandoRuta() { return "Crear Ruta nueva".equals(modo.getValue()); }
    }
}
