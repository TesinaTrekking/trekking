package com.example.controller;

import com.example.dao.EquipamientoDAO;
import com.example.model.Equipamiento;
import com.example.util.NavigationShell;
import com.example.util.TableEmptyState;
import com.example.util.TableCreateRow;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.collections.transformation.FilteredList;

import java.io.IOException;
import java.util.Optional;

public class EquipamientoController {

    @FXML
    private TableView<Equipamiento> tablaEquipamiento;

    @FXML
    private TableColumn<Equipamiento, Integer> colId;

    @FXML
    private TableColumn<Equipamiento, String> colNombre;

    @FXML
    private TableColumn<Equipamiento, String> colCategoria;

    @FXML
    private TableColumn<Equipamiento, Integer> colCantidad;

    @FXML
    private TableColumn<Equipamiento, Integer> colReservada;

    @FXML
    private TableColumn<Equipamiento, Integer> colDisponible;

    @FXML
    private TextField txtBuscar;

    @FXML
    private TableColumn<Equipamiento, String> colEstado;
    @FXML private TableColumn<Equipamiento, Equipamiento> colAcciones;
    @FXML private javafx.scene.control.Button filaAltaButton;

    private ObservableList<Equipamiento> equipamientoOriginal;
    private FilteredList<Equipamiento> equipamientoFiltrado;

    @FXML
    public void initialize() {

        tablaEquipamiento.getStyleClass().add("table-with-create-row");
        tablaEquipamiento.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        colId.setCellValueFactory(
                new PropertyValueFactory<>("id"));

        colNombre.setCellValueFactory(
                new PropertyValueFactory<>("nombre"));

        colCategoria.setCellValueFactory(
                new PropertyValueFactory<>("categoria"));

        colCantidad.setCellValueFactory(
                new PropertyValueFactory<>("cantidad"));

        colReservada.setCellValueFactory(
                new PropertyValueFactory<>("cantidadReservada"));

        colDisponible.setCellValueFactory(
                new PropertyValueFactory<>("cantidadDisponible"));

        colEstado.setCellValueFactory(
                new PropertyValueFactory<>("estado"));
        colAcciones.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue()));

        tablaEquipamiento.setEditable(true);
        configurarTextoInline(colNombre, "nombre");
        colCategoria.setCellFactory(javafx.scene.control.cell.ComboBoxTableCell.forTableColumn(
                javafx.collections.FXCollections.observableArrayList(Equipamiento.CATEGORIAS)));
        colCategoria.setOnEditCommit(event -> guardarTextoInline(event.getRowValue(), "categoria", event.getNewValue()));
        colCantidad.setCellFactory(javafx.scene.control.cell.TextFieldTableCell.forTableColumn(new javafx.util.converter.IntegerStringConverter()));
        colCantidad.setOnEditCommit(event -> {
            Equipamiento item = event.getRowValue();
            try {
                if (event.getNewValue() == null || event.getNewValue() < 0) throw new IllegalArgumentException("El total debe ser un entero no negativo.");
                EquipamientoDAO.actualizar(new Equipamiento(item.getId(), item.getNombre(), item.getCategoria(), event.getNewValue(), item.getEstado()));
                cargarEquipamiento();
            } catch (RuntimeException ex) { mostrarAlerta(Alert.AlertType.ERROR, "No se pudo guardar el total", ex.getMessage()); cargarEquipamiento(); }
        });

        colAcciones.setCellFactory(column -> new javafx.scene.control.TableCell<>() {
            @Override protected void updateItem(Equipamiento item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                javafx.scene.layout.HBox actions = new javafx.scene.layout.HBox(3);
                javafx.scene.control.Button baja = com.example.util.RowActionButton.create("Dar de baja", com.example.util.RowActionButton.Icon.DELETE, e -> confirmarBaja(item));
                baja.getStyleClass().add("row-action-danger");
                actions.getChildren().addAll(
                        com.example.util.RowActionButton.create("Editar elemento", com.example.util.RowActionButton.Icon.EDIT, e -> abrirFormulario(item)), baja);
                actions.setAlignment(javafx.geometry.Pos.CENTER);
                setAlignment(javafx.geometry.Pos.CENTER);
                setGraphic(actions);
            }
        });

        txtBuscar.textProperty().addListener((observable, anterior, actual) -> aplicarFiltro());

        cargarEquipamiento();
    }

    private void configurarTextoInline(TableColumn<Equipamiento, String> columna, String campo) {
        columna.setCellFactory(javafx.scene.control.cell.TextFieldTableCell.forTableColumn());
        columna.setOnEditCommit(event -> guardarTextoInline(event.getRowValue(), campo, event.getNewValue()));
    }

    private void guardarTextoInline(Equipamiento item, String campo, String valor) {
            String nombre = campo.equals("nombre") ? valor.trim() : item.getNombre();
            String categoria = campo.equals("categoria") ? valor : item.getCategoria();
            if (nombre.isBlank() || !Equipamiento.categoriaValida(categoria)) {
                mostrarAlerta(Alert.AlertType.ERROR, "Dato inválido", "El nombre y una categoría válida son obligatorios."); cargarEquipamiento(); return;
            }
            try { EquipamientoDAO.actualizar(new Equipamiento(item.getId(), nombre, categoria, item.getCantidad(), item.getEstado())); cargarEquipamiento(); }
            catch (RuntimeException ex) { mostrarAlerta(Alert.AlertType.ERROR, "No se pudo guardar el cambio", ex.getMessage()); cargarEquipamiento(); }
    }

    private void cargarEquipamiento() {

        equipamientoOriginal = EquipamientoDAO.obtenerTodos();
        equipamientoFiltrado = new FilteredList<>(equipamientoOriginal, item -> true);
        tablaEquipamiento.setItems(equipamientoFiltrado);
        aplicarFiltro();
    }

    private void aplicarFiltro() {
        if (equipamientoFiltrado == null) return;
        String consulta = txtBuscar.getText() == null ? "" : txtBuscar.getText().trim().toLowerCase();
        equipamientoFiltrado.setPredicate(item -> consulta.isEmpty()
                || item.getNombre().toLowerCase().contains(consulta)
                || item.getCategoria().toLowerCase().contains(consulta)
                || item.getEstado().toLowerCase().contains(consulta));

        boolean hayEquipamiento = equipamientoOriginal != null && !equipamientoOriginal.isEmpty();
        boolean filtroActivo = !consulta.isEmpty();
        TableEmptyState.update(tablaEquipamiento, hayEquipamiento, filtroActivo,
                "Todavía no hay elementos", "Agrega elementos para administrar el Inventario.",
                "No hay elementos que coincidan", "Prueba con otro nombre, categoría o estado.",
                "+ Nuevo elemento", event -> nuevoEquipamiento());
        TableCreateRow.update(tablaEquipamiento, filaAltaButton,
                equipamientoFiltrado.size(), filtroActivo, 300);
    }

    @FXML
    private void nuevoEquipamiento() {

        abrirFormulario(null);
    }

    @FXML
    private void editarEquipamiento() {

        Equipamiento seleccionado =
                tablaEquipamiento.getSelectionModel().getSelectedItem();

        if (seleccionado == null) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Editar equipamiento",
                    "Seleccione un equipo para editar.");
            return;
        }

        abrirFormulario(seleccionado);
    }

    @FXML
    private void eliminarEquipamiento() {

        Equipamiento seleccionado =
                tablaEquipamiento.getSelectionModel().getSelectedItem();

        if (seleccionado == null) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Eliminar equipamiento",
                    "Seleccione un equipo para eliminar.");
            return;
        }

        Alert confirmacion = new Alert(
                Alert.AlertType.CONFIRMATION);

        int reservada = EquipamientoDAO.obtenerCantidadReservada(seleccionado.getId());
        confirmacion.setTitle("Dar de baja elemento de inventario");
        confirmacion.setHeaderText(null);
        confirmacion.setContentText(reservada > 0
                ? "Este elemento tiene " + reservada + " unidad(es) reservada(s). "
                        + "Las Salidas planificadas mantienen esa reserva. ¿Desea darlo de baja?"
                : "¿Está seguro de dar de baja el elemento seleccionado?");

        Optional<ButtonType> resultado =
                confirmacion.showAndWait();

        if (resultado.isPresent()
                && resultado.get() == ButtonType.OK) {

            EquipamientoDAO.eliminar(seleccionado.getId());
            cargarEquipamiento();

            mostrarAlerta(
                    Alert.AlertType.INFORMATION,
                    "Equipamiento",
                    "Elemento dado de baja correctamente.");
        }
    }

    private void confirmarBaja(Equipamiento seleccionado) {
        int reservada = EquipamientoDAO.obtenerCantidadReservada(seleccionado.getId());
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Dar de baja elemento de inventario");
        confirmacion.setHeaderText(null);
        confirmacion.setContentText("¿Dar de baja «" + seleccionado.getNombre() + "»? Se excluirá del uso activo."
                + (reservada > 0 ? " Las " + reservada + " unidades reservadas por Salidas planificadas se conservan." : ""));
        if (confirmacion.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            EquipamientoDAO.eliminar(seleccionado.getId()); cargarEquipamiento();
        }
    }

    @FXML
    private void mostrarEquipamientoBaja() {

        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/com/example/fxml/equipamiento-baja.fxml"));

            Parent root = loader.load();

            Stage stage = (Stage) tablaEquipamiento
                    .getScene()
                    .getWindow();

            NavigationShell.setRoot(stage.getScene(), root);
            stage.setTitle("Equipamiento dado de baja");

        } catch (IOException e) {

            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Error",
                    "No se pudo abrir la pantalla de equipos dados de baja.");
        }
    }

    @FXML
    private void volverDashboard() {

        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/com/example/fxml/dashboard.fxml"));

            Parent root = loader.load();

            Stage stage = (Stage) tablaEquipamiento
                    .getScene()
                    .getWindow();

            NavigationShell.setRoot(stage.getScene(), root);
            stage.setTitle("Trekking");

        } catch (IOException e) {

            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Error",
                    "No se pudo volver al dashboard.");
        }
    }

    private void abrirFormulario(Equipamiento equipamiento) {

        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/com/example/fxml/equipamiento-form.fxml"));

            Parent root = loader.load();

            EquipamientoFormController controller =
                    loader.getController();

            controller.cargarEquipamiento(equipamiento);

            Stage stage = (Stage) tablaEquipamiento
                    .getScene()
                    .getWindow();

            NavigationShell.setRoot(stage.getScene(), root);
            stage.setTitle(
                    equipamiento == null
                            ? "Nuevo equipamiento"
                            : "Editar equipamiento");

        } catch (IOException e) {

            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Error",
                    "No se pudo abrir el formulario de equipamiento.");
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
