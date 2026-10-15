package com.example.controller;

import com.example.util.NavigationShell;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
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
    private VBox navigationBox;

    @FXML
    private Button btnInicio;

    @FXML
    private Button btnRutas;

    @FXML
    private Button btnCheckpoints;

    @FXML
    private Button btnEquipamiento;

    @FXML
    private Button btnClientes;

    @FXML
    private Button btnRecorridos;

    private String activeSection = "dashboard";

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
        } else {
            contentContainer.getChildren().clear();
        }
    }

    @FXML
    private void onInicio() {
        navigate("dashboard");
    }

    @FXML
    private void onRutas() {
        navigate("routes");
    }

    @FXML
    private void onCheckpoints() {
        navigate("checkpoints");
    }

    @FXML
    private void onEquipamiento() {
        navigate("equipment");
    }

    @FXML
    private void onClientes() {
        navigate("clients");
    }

    @FXML
    private void onRecorridos() {
        navigate("trips");
    }

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
        applyActive(btnInicio, "dashboard");
        applyActive(btnRutas, "routes");
        applyActive(btnCheckpoints, "checkpoints");
        applyActive(btnEquipamiento, "equipment");
        applyActive(btnClientes, "clients");
        applyActive(btnRecorridos, "trips");
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
