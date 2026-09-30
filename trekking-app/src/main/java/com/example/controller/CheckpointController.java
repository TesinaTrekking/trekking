package com.example.controller;

import com.example.dao.CheckpointDAO;
import com.example.model.Checkpoint;
import com.example.util.AlertUtils;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * * Controlador del módulo de gestión de Checkpoints. * * Se encarga de
 * construir la interfaz, cargar los datos desde la base de datos * y gestionar
 * las operaciones de crear, editar y eliminar checkpoints.
 */
public class CheckpointController {
    private static final Logger LOGGER = Logger.getLogger(CheckpointController.class.getName());
    private static final double SCENE_WIDTH = 1080.0;
    private static final double SCENE_HEIGHT = 640.0;
    private final CheckpointDAO checkpointDAO = new CheckpointDAO();
    private final ObservableList<Checkpoint> checkpointList = FXCollections.observableArrayList();
    private final FilteredList<Checkpoint> filteredList = new FilteredList<>(checkpointList, checkpoint -> true);
    private final SortedList<Checkpoint> sortedList = new SortedList<>(filteredList);
    private TableView<Checkpoint> tableView;
    private Stage primaryStage;
    private Label counterLabel;

    /**
     * * Inicializa el módulo y muestra su interfaz. * * @param primaryStage ventana
     * principal de la aplicación
     */
    public void initialize(Stage primaryStage) {
        this.primaryStage = primaryStage;
        if (!loadDataFromDatabase()) {
            return;
        }
        primaryStage.setTitle("Trekking App - Gestión de Checkpoints");
        primaryStage.setScene(buildScene());
        primaryStage.setMinWidth(850);
        primaryStage.setMinHeight(520);
    }

    private Scene buildScene() {
        tableView = new TableView<>(sortedList);
        sortedList.comparatorProperty().bind(tableView.comparatorProperty());
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        Label placeholderLabel = new Label(
                "No hay checkpoints registrados. " + "Use \"+ Nuevo Checkpoint\" para agregar uno.");
        placeholderLabel.getStyleClass().add("table-placeholder");
        tableView.setPlaceholder(placeholderLabel);
        configureColumns();
        configureRowInteractions();
        VBox mainLayout = new VBox(14, buildTopBar(), tableView, buildFooter());
        mainLayout.setPadding(new Insets(14));
        VBox.setVgrow(tableView, Priority.ALWAYS);
        Scene scene = new Scene(mainLayout, SCENE_WIDTH, SCENE_HEIGHT);
        applyStylesheet(scene);
        configureShortcuts(scene);
        return scene;
    }

    private void applyStylesheet(Scene scene) {
        /*
         * * El CSS ahora pertenece al proyecto integrado. * * Si styles.css todavía no
         * tiene estos estilos, por ahora la interfaz * seguirá funcionando y luego
         * unificamos los estilos.
         */ URL cssUrl = getClass().getResource("/com/example/fxml/styles.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }
    }

    private HBox buildTopBar() {
        Label brandTitle = new Label("Trekking App");
        brandTitle.getStyleClass().add("brand-title");
        Label brandSubtitle = new Label("Puntos de control de ruta");
        brandSubtitle.getStyleClass().add("brand-subtitle");
        Button volverButton = new Button("← Inicio");
        volverButton.getStyleClass().add("btn-secondary");
        volverButton.setOnAction(
            event -> volverDashboard(primaryStage));

        VBox brandContainer = new VBox(1, brandTitle, brandSubtitle);
        brandContainer.setAlignment(Pos.CENTER_LEFT);
        TextField filterField = new TextField();
        filterField.setPromptText("Filtrar por nombre, hora o descripción…");
        filterField.setPrefWidth(260);
        filterField.getStyleClass().add("search-field");
        filterField.textProperty()
                .addListener((obs, oldValue, newValue) -> filteredList.setPredicate(createFilterPredicate(newValue)));
        Button clearButton = new Button("✕");
        clearButton.getStyleClass().add("clear-search-button");
        clearButton.visibleProperty().bind(filterField.textProperty().isNotEmpty());
        clearButton.setOnAction(event -> filterField.clear());
        StackPane searchContainer = new StackPane(filterField, clearButton);
        StackPane.setAlignment(clearButton, Pos.CENTER_RIGHT);
        searchContainer.getStyleClass().add("search-container");
        Button newButton = new Button("+ Nuevo Checkpoint");
        newButton.getStyleClass().add("btn-primary");
        newButton.setOnAction(event -> showCreateDialog());
        Button editButton = new Button("Editar");
        editButton.getStyleClass().add("btn-secondary");
        editButton.disableProperty().bind(tableView.getSelectionModel().selectedItemProperty().isNull());
        editButton.setOnAction(event -> editSelected());
        Button deleteButton = new Button("Eliminar");
        deleteButton.getStyleClass().add("btn-danger");
        deleteButton.disableProperty().bind(tableView.getSelectionModel().selectedItemProperty().isNull());
        deleteButton.setOnAction(event -> deleteSelected());
        HBox actionsContainer = new HBox(8, newButton, editButton, deleteButton);
        actionsContainer.setAlignment(Pos.CENTER_LEFT);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox topBar = new HBox(16, volverButton, brandContainer, spacer, searchContainer, actionsContainer);
        topBar.getStyleClass().add("top-bar");
        topBar.setAlignment(Pos.CENTER_LEFT);
        return topBar;
    }

    private HBox buildFooter() {
        counterLabel = new Label();
        counterLabel.getStyleClass().add("counter-badge");
        updateCounter();
        checkpointList.addListener((ListChangeListener<Checkpoint>) change -> updateCounter());
        filteredList.addListener((ListChangeListener<Checkpoint>) change -> updateCounter());
        HBox footer = new HBox(counterLabel);
        footer.getStyleClass().add("footer-container");
        footer.setAlignment(Pos.CENTER_LEFT);
        return footer;
    }

    private void updateCounter() {
        int total = checkpointList.size();
        int visibles = filteredList.size();
        String unidad = total == 1 ? " checkpoint" : " checkpoints";
        counterLabel.setText(total == visibles ? total + unidad : "Mostrando " + visibles + " de " + total + unidad);
    }

    private static Predicate<Checkpoint> createFilterPredicate(String texto) {
        String filtro = texto == null ? "" : texto.trim().toLowerCase(Locale.ROOT);
        if (filtro.isEmpty()) {
            return checkpoint -> true;
        }
        return checkpoint -> containsIgnoreCase(checkpoint.getNombre(), filtro)
                || containsIgnoreCase(checkpoint.getHora(), filtro)
                || containsIgnoreCase(checkpoint.getDescripcion(), filtro);
    }

    private static boolean containsIgnoreCase(String valor, String filtro) {
        return valor != null && valor.toLowerCase(Locale.ROOT).contains(filtro);
    }

    private void configureShortcuts(Scene scene) {
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN),
                this::showCreateDialog);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.E, KeyCombination.SHORTCUT_DOWN),
                this::editSelected);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.DELETE), this::deleteSelected);
    }

    private void editSelected() {
        Checkpoint selected = tableView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            showEditDialog(selected);
        }
    }

    private void deleteSelected() {
        Checkpoint selected = tableView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            confirmAndDelete(selected);
        }
    }

    private void configureRowInteractions() {
        tableView.setRowFactory(tv -> {
            TableRow<Checkpoint> row = new TableRow<>();
            ContextMenu contextMenu = new ContextMenu();
            MenuItem editItem = new MenuItem("Editar");
            editItem.setOnAction(event -> {
                Checkpoint item = row.getItem();
                if (item != null) {
                    showEditDialog(item);
                }
            });
            MenuItem deleteItem = new MenuItem("Eliminar");
            deleteItem.setOnAction(event -> {
                Checkpoint item = row.getItem();
                if (item != null) {
                    confirmAndDelete(item);
                }
            });
            contextMenu.getItems().addAll(editItem, deleteItem);
            row.setContextMenu(contextMenu);
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    showEditDialog(row.getItem());
                }
            });
            return row;
        });
    }

    private void configureColumns() {
        TableColumn<Checkpoint, String> horaColumn = new TableColumn<>("Hora");
        horaColumn.setCellValueFactory(new PropertyValueFactory<>("hora"));
        horaColumn.setPrefWidth(85);
        horaColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("");
                    getStyleClass().remove("hora-cell");
                } else {
                    setText(item);
                    if (!getStyleClass().contains("hora-cell")) {
                        getStyleClass().add("hora-cell");
                    }
                }
            }
        });
        TableColumn<Checkpoint, String> nombreColumn = new TableColumn<>("Checkpoint / Nombre");
        nombreColumn.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        nombreColumn.setPrefWidth(240);
        TableColumn<Checkpoint, Number> latitudColumn = new TableColumn<>("Latitud");
        latitudColumn.setCellValueFactory(new PropertyValueFactory<>("latitud"));
        latitudColumn.setPrefWidth(120);
        setNumericCellFactory(latitudColumn);
        TableColumn<Checkpoint, Number> longitudColumn = new TableColumn<>("Longitud");
        longitudColumn.setCellValueFactory(new PropertyValueFactory<>("longitud"));
        longitudColumn.setPrefWidth(120);
        setNumericCellFactory(longitudColumn);
        TableColumn<Checkpoint, String> descripcionColumn = new TableColumn<>("Descripción");
        descripcionColumn.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        tableView.getColumns().addAll(horaColumn, nombreColumn, latitudColumn, longitudColumn, descripcionColumn);
    }

    private static void setNumericCellFactory(TableColumn<Checkpoint, Number> column) {
        column.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Number value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setText("");
                    getStyleClass().remove("coordinate-cell");
                } else {
                    setText(formatCoordinate(value.doubleValue()));
                    if (!getStyleClass().contains("coordinate-cell")) {
                        getStyleClass().add("coordinate-cell");
                    }
                }
            }
        });
    }

    private static String formatCoordinate(double value) {
        if (value == 0.0) {
            return "0.0";
        }
        String raw = String.valueOf(value);
        if (!raw.contains(".")) {
            return raw;
        }
        String trimmed = raw.replaceAll("0+$", "").replaceAll("\\.$", "");
        return trimmed.isEmpty() ? "0.0" : trimmed;
    }

    private boolean loadDataFromDatabase() {
        checkpointList.clear();
        try {
            checkpointList.addAll(checkpointDAO.obtenerTodos());
            return true;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al cargar los checkpoints", e);
            AlertUtils.showError(primaryStage, "Error de base de datos",
                    "No se pudieron cargar los checkpoints.\n" + e.getMessage());
            return false;
        }
    }

    private void showCreateDialog() {
        CheckpointDialog dialog = new CheckpointDialog(primaryStage, null);
        Optional<Checkpoint> result = dialog.showAndWait();
        result.ifPresent(newCheckpoint -> {
            try {
                long newId = checkpointDAO.insertar(newCheckpoint);
                if (newId != -1) {
                    newCheckpoint.setId(newId);
                }
                checkpointList.add(newCheckpoint);
                tableView.getSelectionModel().select(newCheckpoint);
                tableView.scrollTo(newCheckpoint);
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error al insertar checkpoint", e);
                AlertUtils.showError(primaryStage, "Error al guardar",
                        "No se pudo guardar el checkpoint " + "en la base de datos.\n" + e.getMessage());
            }
        });
    }

    private void showEditDialog(Checkpoint item) {
        CheckpointDialog dialog = new CheckpointDialog(primaryStage, item);
        Optional<Checkpoint> result = dialog.showAndWait();
        result.ifPresent(updated -> {
            item.setNombre(updated.getNombre());
            item.setHora(updated.getHora());
            item.setLatitud(updated.getLatitud());
            item.setLongitud(updated.getLongitud());
            item.setDescripcion(updated.getDescripcion());
            try {
                checkpointDAO.actualizar(item);
                tableView.refresh();
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error al actualizar checkpoint id=" + item.getId(), e);
                loadDataFromDatabase();
                AlertUtils.showError(primaryStage, "Error al actualizar", "No se pudieron guardar los cambios "
                        + "del checkpoint en la base de datos.\n" + e.getMessage());
            }
        });
    }

    private void confirmAndDelete(Checkpoint item) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmar eliminación");
        alert.setHeaderText("¿Eliminar Checkpoint?");
        alert.setContentText(
                "¿Está seguro de que desea eliminar '" + item.getNombre() + "'? Esta acción no se puede deshacer.");
        alert.initOwner(primaryStage);
        alert.initModality(Modality.WINDOW_MODAL);
        ButtonType confirmDelete = new ButtonType("Eliminar", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(confirmDelete, cancel);
        AlertUtils.applyTheme(alert);
        Button btnEliminar = (Button) alert.getDialogPane().lookupButton(confirmDelete);
        if (btnEliminar != null) {
            btnEliminar.getStyleClass().add("btn-danger");
        }
        Button btnCancelar = (Button) alert.getDialogPane().lookupButton(cancel);
        if (btnCancelar != null) {
            btnCancelar.getStyleClass().add("btn-secondary");
        }
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == confirmDelete) {
            try {
                if (checkpointDAO.eliminar(item.getId())) {
                    checkpointList.remove(item);
                } else {
                    AlertUtils.showError(primaryStage, "Error al eliminar",
                            "No se pudo eliminar el checkpoint " + "de la base de datos.");
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error al eliminar checkpoint id=" + item.getId(), e);
                AlertUtils.showError(primaryStage, "Error al eliminar",
                        "No se pudo eliminar el checkpoint " + "de la base de datos.\n" + e.getMessage());
            }
        }
    }

    private void volverDashboard(Stage stage) {

        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/com/example/fxml/dashboard.fxml"));

            Parent root = loader.load();

            stage.getScene().setRoot(root);

        } catch (IOException e) {

            AlertUtils.showError(
                    stage,
                    "Error",
                    "No se pudo volver al dashboard.");
        }
    }
}