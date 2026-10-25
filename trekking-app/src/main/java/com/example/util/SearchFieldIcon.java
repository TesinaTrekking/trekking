package com.example.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;
import javafx.scene.transform.Scale;

/** Adds the shared search icon to a search field without intercepting input. */
public final class SearchFieldIcon {
    private static final String ICON_RESOURCE = "/com/example/icons/SEARCH.svg";
    private static final Pattern PATH_DATA = Pattern.compile("<path\\b[^>]*\\bd=\"([^\"]+)\"");

    private SearchFieldIcon() { }

    public static void install(TextField field) {
        if (!(field.getParent() instanceof StackPane container)) {
            throw new IllegalArgumentException("El campo de búsqueda debe estar dentro de un StackPane");
        }
        container.getChildren().add(createIcon());
    }

    public static StackPane wrap(TextField field) {
        StackPane container = new StackPane(field);
        container.getStyleClass().add("search-container");
        install(field);
        return container;
    }

    private static SVGPath createIcon() {
        try (InputStream input = SearchFieldIcon.class.getResourceAsStream(ICON_RESOURCE)) {
            if (input == null) throw new IllegalStateException("No se encontró el icono: " + ICON_RESOURCE);
            String svg = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            Matcher matcher = PATH_DATA.matcher(svg);
            if (!matcher.find()) throw new IllegalStateException("El icono no contiene un trazado: " + ICON_RESOURCE);

            SVGPath icon = new SVGPath();
            icon.setContent(matcher.group(1));
            icon.getTransforms().add(new Scale(16.0 / 960.0, 16.0 / 960.0));
            icon.getStyleClass().add("search-icon");
            icon.setMouseTransparent(true);
            icon.setManaged(false);
            icon.setLayoutX(12);
            icon.setLayoutY(10);
            return icon;
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo leer el icono: " + ICON_RESOURCE, exception);
        }
    }
}
