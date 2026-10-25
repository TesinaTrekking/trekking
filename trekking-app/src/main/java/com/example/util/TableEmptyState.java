package com.example.util;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

public final class TableEmptyState {
    private TableEmptyState() { }

    public static Node create(String title, String description, String actionText,
            EventHandler<ActionEvent> action) {
        Label icon = new Label("▤");
        icon.getStyleClass().add("table-empty-icon");
        Label heading = new Label(title);
        heading.getStyleClass().add("table-empty-title");
        heading.setAlignment(Pos.CENTER);
        Label detail = new Label(description);
        detail.getStyleClass().add("table-empty-description");
        detail.setWrapText(true);
        detail.setMaxWidth(420);
        detail.setAlignment(Pos.CENTER);
        detail.setTextAlignment(TextAlignment.CENTER);

        VBox content = new VBox(8, icon, heading, detail);
        content.setAlignment(Pos.CENTER);
        content.getStyleClass().add("table-empty-state");
        if (action != null && actionText != null && !actionText.isBlank()) {
            Button button = new Button(actionText);
            button.setOnAction(action);
            button.getStyleClass().add("table-empty-action");
            content.getChildren().add(button);
        }
        return content;
    }

    public static void update(TableView<?> table, boolean hasRecords, boolean filtersActive,
            String emptyTitle, String emptyDescription,
            String noResultsTitle, String noResultsDescription,
            String actionText, EventHandler<ActionEvent> action) {
        boolean noMatches = hasRecords && filtersActive;
        table.setPlaceholder(create(
                noMatches ? noResultsTitle : emptyTitle,
                noMatches ? noResultsDescription : emptyDescription,
                actionText,
                action));
    }
}
