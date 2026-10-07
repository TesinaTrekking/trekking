package com.example.controller;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.event.ActionEvent;

public class DashboardController {

    @FXML
    private void abrirRutas(ActionEvent event) throws IOException {

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource(
                        "/com/example/fxml/rutas.fxml"));

        Parent root = loader.load();

        Node source = (Node) event.getSource();

        source.getScene().setRoot(root);
    }

    @FXML
    private void abrirEquipamiento(ActionEvent event) throws IOException {

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource(
                        "/com/example/fxml/equipamiento.fxml"));

        Parent root = loader.load();

        Node source = (Node) event.getSource();

        source.getScene().setRoot(root);
    }

    @FXML
    private void abrirClientes(ActionEvent event) throws IOException {

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource(
                        "/com/example/fxml/lista-clientes.fxml"));

        Parent root = loader.load();

        Node source = (Node) event.getSource();

        source.getScene().setRoot(root);
    }

    @FXML
    private void abrirCheckpoints(ActionEvent event) {

        Node source = (Node) event.getSource();

        CheckpointController controller = new CheckpointController();

        controller.initialize(
                (Stage) source.getScene().getWindow());
    }
}