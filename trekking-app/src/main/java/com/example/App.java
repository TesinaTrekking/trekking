package com.example;

import com.example.database.ConexionDB;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        ConexionDB.crearTabla();

        FXMLLoader loader = new FXMLLoader(
                App.class.getResource(
                        "/com/example/fxml/dashboard.fxml"
                )
        );

        Parent root = loader.load();

        Scene scene = new Scene(root, 1000, 700);

        stage.setTitle("Trekking App");
        stage.setScene(scene);
        stage.setMinWidth(800);
        stage.setMinHeight(600);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}