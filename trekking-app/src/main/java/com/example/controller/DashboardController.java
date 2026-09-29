package com.example.controller;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Node;
import javafx.event.ActionEvent;

public class DashboardController {

    @FXML
    private void abrirRutas(ActionEvent event) throws IOException {

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource(
                        "/com/example/fxml/rutas.fxml"
                )
        );

        Parent root = loader.load();

        Node source = (Node) event.getSource();

        source.getScene().setRoot(root);
    }
}