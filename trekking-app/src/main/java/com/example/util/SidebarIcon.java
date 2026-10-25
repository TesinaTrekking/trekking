package com.example.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;

/** Loads the path data from one of the sidebar's SVG icon resources. */
public final class SidebarIcon {
    private static final String ICON_DIRECTORY = "/com/example/icons/";
    private static final Pattern PATH_DATA = Pattern.compile("<path\\b[^>]*\\bd=\"([^\"]+)\"");
    private static final double ICON_SIZE = 20.0;
    private static final double VIEWBOX_SIZE = 960.0;

    private SidebarIcon() { }

    public static StackPane load(String fileName) {
        String resource = ICON_DIRECTORY + fileName;
        try (InputStream input = SidebarIcon.class.getResourceAsStream(resource)) {
            if (input == null) {
                throw new IllegalStateException("No se encontró el icono del sidebar: " + resource);
            }

            String svg = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            Matcher matcher = PATH_DATA.matcher(svg);
            if (!matcher.find()) {
                throw new IllegalStateException("El icono del sidebar no contiene un trazado: " + resource);
            }

            SVGPath icon = new SVGPath();
            icon.setContent(matcher.group(1));
            icon.getTransforms().addAll(
                    new Scale(ICON_SIZE / VIEWBOX_SIZE, ICON_SIZE / VIEWBOX_SIZE, 0, 0),
                    new Translate(0, ICON_SIZE));
            icon.getStyleClass().add("sidebar-icon");
            icon.setManaged(false);

            StackPane graphic = new StackPane(icon);
            graphic.setMinSize(ICON_SIZE, ICON_SIZE);
            graphic.setPrefSize(ICON_SIZE, ICON_SIZE);
            graphic.setMaxSize(ICON_SIZE, ICON_SIZE);
            graphic.getStyleClass().add("sidebar-icon-container");
            return graphic;
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo leer el icono del sidebar: " + resource, exception);
        }
    }
}
