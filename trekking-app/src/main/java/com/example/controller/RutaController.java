package com.example.controller;

import com.example.App;
import com.example.dao.RutaDAO;
import com.example.model.Ruta;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.collections.ObservableList;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ButtonType;
import javafx.scene.control.cell.PropertyValueFactory;

public class RutaController implements Initializable {

        private final RutaDAO rutaDAO = new RutaDAO();

    @FXML
    private TableView<Ruta> tablaRutas;

    @FXML
    private TableColumn<Ruta, Integer> colId;

    @FXML
    private TableColumn<Ruta, String> colNombre;

    @FXML
    private TableColumn<Ruta, Double> colLatitudInicial;

    @FXML
    private TableColumn<Ruta, Double> colLongitudInicial;

    @FXML
    private TableColumn<Ruta, Double> colAltitudMaxima;

    @FXML
    private TableColumn<Ruta, String> colTipoTerreno;

    @FXML
    private TableColumn<Ruta, String> colDificultadTecnica;

    @FXML
    private TableColumn<Ruta, String> colDificultadFisica;


    @Override
    public void initialize(URL url, ResourceBundle rb) {

        colId.setCellValueFactory(
                new PropertyValueFactory<>("id"));

        colNombre.setCellValueFactory(
                new PropertyValueFactory<>("nombre"));
        colNombre.setComparator(String.CASE_INSENSITIVE_ORDER);
        colNombre.setSortType(TableColumn.SortType.ASCENDING);

        colLatitudInicial.setCellValueFactory(
                new PropertyValueFactory<>("latitudInicial"));

        colLongitudInicial.setCellValueFactory(
                new PropertyValueFactory<>("longitudInicial"));

        colAltitudMaxima.setCellValueFactory(
                new PropertyValueFactory<>("altitudMaxima"));

        colTipoTerreno.setCellValueFactory(
                new PropertyValueFactory<>("tipoTerreno"));

        colDificultadTecnica.setCellValueFactory(
                new PropertyValueFactory<>("dificultadTecnica"));

        colDificultadFisica.setCellValueFactory(
                new PropertyValueFactory<>("dificultadFisica"));


        cargarRutas();
        tablaRutas.getSortOrder().add(colNombre);
        tablaRutas.sort();
    }


    private void cargarRutas() {

        ObservableList<Ruta> rutas =
                FXCollections.observableArrayList(rutaDAO.obtenerTodas());

        tablaRutas.setItems(rutas);
    }


    @FXML
    private void switchToForm() throws IOException {

                App.consumirRutaEnEdicion();
        App.setRoot("ruta-form");
    }


    @FXML
        private void editarRuta() throws IOException {

        Ruta rutaSeleccionada =
                tablaRutas.getSelectionModel().getSelectedItem();

        if (rutaSeleccionada == null) {

            mostrarAlerta(
                    "Selecciona una ruta",
                    "Selecciona una ruta para editar."
            );

            return;
        }

        App.prepararEdicion(rutaSeleccionada);
    }


    @FXML
    private void eliminarRuta() {

        Ruta rutaSeleccionada =
                tablaRutas.getSelectionModel().getSelectedItem();

        if (rutaSeleccionada == null) {

            mostrarAlerta(
                    "Selecciona una ruta",
                    "Selecciona una ruta para eliminar."
            );

            return;
        }


        Alert confirmacion =
                new Alert(Alert.AlertType.CONFIRMATION);

        confirmacion.setTitle("Confirmar eliminación");
        confirmacion.setHeaderText("¿Estás seguro?");
        confirmacion.setContentText(
                "¿Eliminar la ruta \""
                        + rutaSeleccionada.getNombre()
                        + "\"?"
        );


        if (confirmacion.showAndWait().orElse(null)
                == ButtonType.OK) {

            rutaDAO.eliminar(
                    rutaSeleccionada.getId());

            cargarRutas();

            mostrarAlerta(
                    "Éxito",
                    "Ruta eliminada correctamente."
            );
        }
    }


    private void mostrarAlerta(
            String titulo,
            String mensaje) {

        Alert alerta =
                new Alert(Alert.AlertType.INFORMATION);

        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);

        alerta.showAndWait();
    }
}