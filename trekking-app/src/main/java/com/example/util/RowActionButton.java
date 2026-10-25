package com.example.util;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.shape.SVGPath;

/** Compact, consistent vector icon button for table row toolbars. */
public final class RowActionButton {
    public enum Icon {
        EDIT("M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04a1 1 0 0 0 0-1.41l-2.34-2.34a1 1 0 0 0-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z"),
        DOWNLOAD("M5 20h14v-2H5v2zM19 9h-4V3H9v6H5l7 7 7-7z"),
        MEDICAL("M19 3H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V5a2 2 0 0 0-2-2zm0 16H5V5h14v14zm-7-2a5 5 0 1 0 0-10 5 5 0 0 0 0 10zm-1-8h2v2h2v2h-2v2h-2v-2H9v-2h2V9z"),
        DELETE("M6 19a2 2 0 0 0 2 2h8a2 2 0 0 0 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"),
        RESTORE("M12 5V1L7 6l5 5V7a5 5 0 1 1-4.9 6H5.08A7 7 0 1 0 12 5z"),
        CHECK("M9 16.17 4.83 12 3.41 13.41 9 19l12-12-1.41-1.41z"),
        CANCEL("M19 6.41 17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z");

        private final String path;
        Icon(String path) { this.path = path; }
    }

    private RowActionButton() { }

    public static Button create(String label, Icon icon, EventHandler<ActionEvent> action) {
        SVGPath graphic = new SVGPath();
        graphic.setContent(icon.path);
        graphic.getStyleClass().add("row-action-icon");
        Button button = new Button();
        button.getStyleClass().add("row-action-button");
        button.setGraphic(graphic);
        button.setTooltip(new Tooltip(label));
        button.setAccessibleText(label);
        button.setOnAction(action);
        return button;
    }
}
