package com.example.util;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import com.example.controller.CheckpointController;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;

public final class NavigationShell {

    private static final String STATE_KEY =
            NavigationShell.class.getName() + ".state";
    private static final String STYLESHEET =
            "/com/example/fxml/styles.css";
    private static final double SIDEBAR_WIDTH = 230;

    private static final List<NavigationItem> ITEMS = List.of(
            new NavigationItem("Inicio", "dashboard", "dashboard.fxml"),
            new NavigationItem("Rutas", "routes", "rutas.fxml"),
            new NavigationItem("Checkpoints", "checkpoints", null),
            new NavigationItem("Equipamiento", "equipment", "equipamiento.fxml"),
            new NavigationItem("Clientes", "clients", "lista-clientes.fxml"),
            new NavigationItem("Recorridos", "trips", "recorridos.fxml"));

    private NavigationShell() {
    }

    public static void install(Scene scene) {
        install(scene, "dashboard");
    }

    public static void install(Scene scene, String activeSection) {
        NavigationState state = new NavigationState(scene, activeSection);
        scene.getProperties().put(STATE_KEY, state);
        addStylesheet(scene);
        if (!(scene.getRoot() instanceof ShellPane)) {
            scene.setRoot(state.createShell(scene.getRoot()));
        }
    }

    public static void setRoot(Scene scene, Parent page) {
        Object value = scene.getProperties().get(STATE_KEY);
        if (!(value instanceof NavigationState state)) {
            throw new IllegalStateException(
                    "La navegación lateral no está instalada en esta ventana.");
        }
        scene.setRoot(state.createShell(page));
    }

    private static void addStylesheet(Scene scene) {
        URL stylesheet = NavigationShell.class.getResource(STYLESHEET);
        if (stylesheet != null
                && !scene.getStylesheets().contains(stylesheet.toExternalForm())) {
            scene.getStylesheets().add(stylesheet.toExternalForm());
        }
    }

    private record NavigationItem(String label, String section, String fxml) {
    }

    private static final class NavigationState {

        private final Scene scene;
        private String activeSection;

        private NavigationState(Scene scene, String activeSection) {
            this.scene = scene;
            this.activeSection = activeSection;
        }

        private Parent createShell(Parent page) {
            ShellPane shell = new ShellPane();
            shell.getStyleClass().add("app-shell");

            VBox sidebar = new VBox(8);
            sidebar.setMinWidth(SIDEBAR_WIDTH);
            sidebar.setPrefWidth(SIDEBAR_WIDTH);
            sidebar.setMaxWidth(SIDEBAR_WIDTH);
            sidebar.getStyleClass().add("app-sidebar");

            Label brand = new Label("TREKKING");
            brand.getStyleClass().add("sidebar-brand");
            Label caption = new Label("GESTIÓN DE ACTIVIDADES");
            caption.getStyleClass().add("sidebar-caption");
            sidebar.getChildren().addAll(brand, caption);

            VBox navigation = new VBox(6);
            navigation.getStyleClass().add("sidebar-navigation");
            VBox.setVgrow(navigation, Priority.ALWAYS);
            for (NavigationItem item : ITEMS) {
                Button button = new Button(item.label());
                button.setMaxWidth(Double.MAX_VALUE);
                button.getStyleClass().add("sidebar-button");
                if (item.section().equals(activeSection)) {
                    button.getStyleClass().add("sidebar-button-active");
                }
                button.setOnAction(event -> navigate(item));
                navigation.getChildren().add(button);
            }
            sidebar.getChildren().add(navigation);

            Label footer = new Label("Sistema de gestión de trekking");
            footer.getStyleClass().add("sidebar-footer");
            sidebar.getChildren().add(footer);

            StackPane content = new StackPane(page);
            content.setMinSize(0, 0);
            content.getStyleClass().add("app-content");
            shell.setLeft(sidebar);
            shell.setCenter(content);
            return shell;
        }

        private void navigate(NavigationItem item) {
            activeSection = item.section();
            if (item.fxml() == null) {
                new CheckpointController().initialize(
                        (Stage) scene.getWindow());
                return;
            }
            try {
                Parent page = new FXMLLoader(
                        NavigationShell.class.getResource(
                                "/com/example/fxml/" + item.fxml()))
                        .load();
                NavigationShell.setRoot(scene, page);
            } catch (IOException | RuntimeException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("No se pudo abrir la sección");
                alert.setHeaderText(null);
                alert.setContentText(
                        "No se pudo abrir " + item.label() + ": " + e.getMessage());
                alert.showAndWait();
            }
        }
    }

    private static final class ShellPane extends BorderPane {
    }
}
