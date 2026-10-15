package com.example.util;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import com.example.controller.ShellController;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;

/**
 * Shell de navegación lateral de la aplicación.
 *
 * Estructura en {@code shell.fxml} vía {@link ShellController}.
 * Aquí solo queda la gestión de estado y navegación entre secciones.
 */
public final class NavigationShell {

    private static final String STATE_KEY =
            NavigationShell.class.getName() + ".state";
    private static final String STYLESHEET =
            "/com/example/fxml/styles.css";

    private static final List<NavigationItem> ITEMS = List.of(
            new NavigationItem("Inicio", "dashboard", "dashboard.fxml"),
            new NavigationItem("Rutas", "routes", "rutas.fxml"),
            new NavigationItem("Checkpoints", "checkpoints", "checkpoints.fxml"),
            new NavigationItem("Equipamiento", "equipment", "equipamiento.fxml"),
            new NavigationItem("Clientes", "clients", "lista-clientes.fxml"),
            new NavigationItem("Recorridos", "trips", "recorridos.fxml"));

    private NavigationShell() {
    }

    public static void install(Scene scene) {
        install(scene, "dashboard");
    }

    public static void install(Scene scene, String activeSection) {
        Parent currentRoot = scene.getRoot();
        if (currentRoot != null && currentRoot.getStyleClass().contains("app-shell")) {
            Object existing = scene.getProperties().get(STATE_KEY);
            if (existing instanceof ShellController controller) {
                controller.setActiveSection(activeSection);
            }
            return;
        }
        addStylesheet(scene);
        try {
            FXMLLoader loader = new FXMLLoader(
                    NavigationShell.class.getResource("/com/example/fxml/shell.fxml"));
            Parent shell = loader.load();
            ShellController controller = loader.getController();
            controller.setActiveSection(activeSection);
            controller.setContent(currentRoot);
            scene.getProperties().put(STATE_KEY, controller);
            scene.setRoot(shell);
        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException("No se pudo instalar la navegación lateral.", e);
        }
    }

    public static void setRoot(Scene scene, Parent page) {
        Object value = scene.getProperties().get(STATE_KEY);
        if (!(value instanceof ShellController controller)) {
            throw new IllegalStateException(
                    "La navegación lateral no está instalada en esta ventana.");
        }
        controller.setContent(page);
    }

    public static void navigate(Scene scene, String section) {
        NavigationItem target = ITEMS.stream()
                .filter(item -> item.section().equals(section))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Sección desconocida: " + section));
        Object value = scene.getProperties().get(STATE_KEY);
        ShellController controller = value instanceof ShellController sc ? sc : null;
        if (controller == null) {
            install(scene, section);
            value = scene.getProperties().get(STATE_KEY);
            if (!(value instanceof ShellController sc2)) {
                throw new IllegalStateException(
                        "La navegación lateral no está instalada en esta ventana.");
            }
            controller = sc2;
        }
        try {
            Parent page = new FXMLLoader(
                    NavigationShell.class.getResource(
                            "/com/example/fxml/" + target.fxml()))
                    .load();
            controller.setActiveSection(section);
            controller.setContent(page);
        } catch (IOException | RuntimeException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("No se pudo abrir la sección");
            alert.setHeaderText(null);
            alert.setContentText(
                    "No se pudo abrir " + target.label() + ": " + e.getMessage());
            alert.showAndWait();
        }
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
}
