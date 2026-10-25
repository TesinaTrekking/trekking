package com.example.controller;

import com.example.util.NavigationShell;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Contenido del shell de navegación lateral.
 *
 * Estructura en {@code shell.fxml}. Aquí solo vive el comportamiento:
 * sección activa y contenido central. La carga de páginas se delega
 * a {@link NavigationShell}.
 */
public class ShellController implements Initializable {

    @FXML
    private StackPane contentContainer;

    @FXML
    private HBox breadcrumbBar;

    @FXML
    private VBox navigationBox;

    @FXML
    private Button btnRutas;

    @FXML
    private Button btnInventario;

    @FXML
    private Button btnClientes;

    @FXML
    private Button btnSalidas;

    private String activeSection = "trips";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        refreshActiveStyles();
    }

    public void setActiveSection(String section) {
        this.activeSection = section;
        refreshActiveStyles();
    }

    public String getActiveSection() {
        return activeSection;
    }

    public void setContent(Parent page) {
        if (page != null) {
            contentContainer.getChildren().setAll(page);
            NavigationShell.renderBreadcrumbs(this, page.getId());
        } else {
            contentContainer.getChildren().clear();
            breadcrumbBar.getChildren().clear();
            breadcrumbBar.setManaged(false);
            breadcrumbBar.setVisible(false);
        }
    }

    public HBox getBreadcrumbBar() { return breadcrumbBar; }

    @FXML
    private void onSalidas() {
        navigate("trips");
    }

    @FXML
    private void onRutas() {
        navigate("routes");
    }

    @FXML
    private void onInventario() {
        navigate("inventory");
    }

    @FXML
    private void onClientes() {
        navigate("clients");
    }

    @FXML
    private void navigate(String section) {
        Scene scene = contentContainer.getScene();
        if (scene == null) {
            setActiveSection(section);
            return;
        }
        NavigationShell.navigate(scene, section);
    }

    private void refreshActiveStyles() {
        if (navigationBox == null) {
            return;
        }
        applyActive(btnSalidas, "trips");
        applyActive(btnRutas, "routes");
        applyActive(btnInventario, "inventory");
        applyActive(btnClientes, "clients");
    }

    private void applyActive(Button button, String section) {
        if (button == null) {
            return;
        }
        button.getStyleClass().remove("sidebar-button-active");
        if (section.equals(activeSection)
                && !button.getStyleClass().contains("sidebar-button-active")) {
            button.getStyleClass().add("sidebar-button-active");
        }
    }
}
