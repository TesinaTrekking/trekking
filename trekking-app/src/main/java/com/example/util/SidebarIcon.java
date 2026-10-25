package com.example.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.scene.shape.SVGPath;

/** Loads the path data from one of the sidebar's SVG icon resources. */
public final class SidebarIcon {
    private static final String ICON_DIRECTORY = "/com/example/icons/";
    private static final Pattern PATH_DATA = Pattern.compile("<path\\b[^>]*\\bd=\"([^\"]+)\"");
    private static final double ICON_SIZE = 20.0;
    private static final double VIEWBOX_SIZE = 960.0;

    private SidebarIcon() { }

    public static SVGPath load(String fileName) {
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
            icon.setScaleX(ICON_SIZE / VIEWBOX_SIZE);
            icon.setScaleY(ICON_SIZE / VIEWBOX_SIZE);
            icon.setTranslateY(ICON_SIZE);
            icon.getStyleClass().add("sidebar-icon");
            return icon;
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo leer el icono del sidebar: " + resource, exception);
        }
    }
}
