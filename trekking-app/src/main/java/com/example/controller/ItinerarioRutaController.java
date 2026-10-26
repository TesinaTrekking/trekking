package com.example.controller;

import com.example.dao.RutaCheckpointDAO;
import com.example.model.Checkpoint;
import com.example.model.Ruta;
import com.example.util.AlertUtils;
import com.example.util.NavigationShell;
import com.example.util.ToastNotifications;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.application.Platform;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Edición pendiente y guardado atómico del Itinerario de una Ruta. */
public class ItinerarioRutaController {
    @FXML private Label titulo, resumen, estado;
    @FXML private ListView<Checkpoint> lista;
    private final RutaCheckpointDAO dao = new RutaCheckpointDAO();
    private Ruta ruta;
    private final List<Checkpoint> puntos = new ArrayList<>();
    private boolean cambios;

    public void cargar(Ruta ruta) {
        this.ruta = ruta;
        titulo.setText("Itinerario · " + ruta.getNombre());
        resumen.setText(String.format("Altitud máxima %.0f m · %s · Dificultad %s / %s", ruta.getAltitudMaxima(), ruta.getTipoTerreno(), ruta.getDificultadTecnica(), ruta.getDificultadFisica()));
        puntos.addAll(dao.obtenerPorRuta(ruta.getId()));
        javafx.application.Platform.runLater(() -> NavigationShell.setNavigationGuard(lista.getScene(), this::confirmarSalida));
        lista.setCellFactory(v -> new ListCell<>() {
            @Override protected void updateItem(Checkpoint cp, boolean empty) {
                super.updateItem(cp, empty);
                setText(empty || cp == null ? null : String.format("%s  ·  %s  ·  %.0f m  ·  %.5f, %.5f", cp.getHora(), cp.getNombre(), cp.getAltitud(), cp.getLatitud(), cp.getLongitud()));
            }
        });
        refrescar();
    }

    private void refrescar() {
        lista.setItems(FXCollections.observableArrayList(puntos));
        estado.setText((puntos.isEmpty() ? "Borrador · sin Checkpoints" : puntos.size() + " Checkpoints") + (cambios ? " · Cambios sin guardar" : "") + " · La Ruta necesita cero o al menos dos Checkpoints.");
    }

    @FXML private void agregar() { editar(null); }
    @FXML private void editar() { Checkpoint cp = lista.getSelectionModel().getSelectedItem(); if (cp != null) editar(cp); }
    @FXML private void quitar() {
        Checkpoint cp = lista.getSelectionModel().getSelectedItem();
        if (cp != null) { puntos.remove(cp); cambios = true; refrescar(); }
    }
    private void editar(Checkpoint original) {
        TextField nombre = new TextField(original == null ? "" : original.getNombre());
        TextField hora = new TextField(original == null ? "" : original.getHora());
        TextField lat = new TextField(original == null ? "" : Double.toString(original.getLatitud()));
        TextField lon = new TextField(original == null ? "" : Double.toString(original.getLongitud()));
        TextField alt = new TextField(original == null ? "0" : Double.toString(original.getAltitud()));
        TextArea descripcion = new TextArea(original == null ? "" : original.getDescripcion()); descripcion.setPrefRowCount(3);
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.getStyleClass().add("form-grid");
        String[] labels = {"Nombre *", "Hora (HH:mm) *", "Latitud *", "Longitud *", "Altitud (m) *", "Descripción"};
        javafx.scene.Node[] fields = {nombre, hora, lat, lon, alt, descripcion};
        for (int i = 0; i < fields.length; i++) { grid.add(new Label(labels[i]), 0, i); grid.add(fields[i], 1, i); }
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initModality(javafx.stage.Modality.WINDOW_MODAL);
        dialog.initOwner(lista.getScene().getWindow());
        dialog.setTitle(original == null ? "Nuevo Checkpoint" : "Editar Checkpoint");
        dialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/com/example/fxml/styles.css").toExternalForm());
        dialog.getDialogPane().getStyleClass().add("dialog-form-card");
        dialog.getDialogPane().setPrefWidth(560);
        dialog.getDialogPane().setContent(grid);
        ButtonType cancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType guardar = new ButtonType(
                original == null ? "Agregar checkpoint" : "Guardar cambios",
                ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(cancelar, guardar);
        Platform.runLater(nombre::requestFocus);
        if (dialog.showAndWait().orElse(cancelar) != guardar) return;
        try {
            String h = hora.getText().trim();
            if (nombre.getText().isBlank() || !h.matches("(?:[01]\\d|2[0-3]):[0-5]\\d")) throw new IllegalArgumentException("Completa el nombre y usa hora HH:mm (24 horas).");
            double latitude = Double.parseDouble(lat.getText().trim().replace(',', '.'));
            double longitude = Double.parseDouble(lon.getText().trim().replace(',', '.'));
            double altitude = Double.parseDouble(alt.getText().trim().replace(',', '.'));
            if (!Double.isFinite(latitude) || latitude < -90 || latitude > 90 || !Double.isFinite(longitude) || longitude < -180 || longitude > 180 || !Double.isFinite(altitude) || altitude < 0 || altitude > ruta.getAltitudMaxima()) throw new IllegalArgumentException("Revisa coordenadas y altitud. La altitud debe estar entre 0 y " + ruta.getAltitudMaxima() + " m.");
            Checkpoint cp = original == null ? new Checkpoint(0L, nombre.getText().trim(), h, latitude, longitude, altitude, descripcion.getText().trim()) : original;
            cp.setNombre(nombre.getText().trim()); cp.setHora(h); cp.setLatitud(latitude); cp.setLongitud(longitude); cp.setAltitud(altitude); cp.setDescripcion(descripcion.getText().trim());
            if (original == null) puntos.add(cp);
            puntos.sort(Comparator.comparing(Checkpoint::getHora));
            cambios = true; refrescar(); lista.getSelectionModel().select(cp);
        } catch (IllegalArgumentException ex) { alerta("Checkpoint inválido", ex.getMessage()); }
    }
    @FXML private void subir() { mover(-1); }
    @FXML private void bajar() { mover(1); }
    private void mover(int delta) {
        int i = lista.getSelectionModel().getSelectedIndex(), j = i + delta;
        if (i < 0 || j < 0 || j >= puntos.size() || !puntos.get(i).getHora().equals(puntos.get(j).getHora())) return;
        java.util.Collections.swap(puntos, i, j); cambios = true; refrescar(); lista.getSelectionModel().select(j);
    }
    @FXML private void guardar() {
        if (puntos.size() == 1) { alerta("Itinerario incompleto", "El Itinerario necesita inicio y fin. Agrega otro Checkpoint o quita el único punto."); return; }
        if (dao.reemplazarPorRuta(ruta.getId(), List.copyOf(puntos))) {
            cambios = false;
            refrescar();
            ToastNotifications.show(lista.getScene().getWindow(), "Itinerario guardado", ToastNotifications.Kind.SUCCESS);
        }
        else alerta("No se pudo guardar", "El Itinerario anterior se conserva. Verifica la altitud máxima y vuelve a intentar.");
    }
    @FXML private void volver() {
        NavigationShell.navigate(lista.getScene(), "routes");
    }
    private boolean confirmarSalida() {
        if (cambios) {
            ButtonType guardar = new ButtonType("Guardar"), descartar = new ButtonType("Descartar"), seguir = new ButtonType("Seguir editando", ButtonBar.ButtonData.CANCEL_CLOSE);
            Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Hay cambios pendientes en el Itinerario.", guardar, descartar, seguir); a.setTitle("Cambios sin guardar"); a.setHeaderText("¿Qué quieres hacer?");
            ButtonType respuesta = AlertUtils.showAndWait(a, lista).orElse(seguir);
            if (respuesta == seguir) return false;
            if (respuesta == guardar) { guardar(); if (cambios) return false; }
        }
        NavigationShell.setNavigationGuard(lista.getScene(), null);
        return true;
    }
    private void alerta(String titulo, String texto) { Alert a = new Alert(Alert.AlertType.INFORMATION, texto); a.setTitle(titulo); a.setHeaderText(null); AlertUtils.showAndWait(a, lista); }
}
