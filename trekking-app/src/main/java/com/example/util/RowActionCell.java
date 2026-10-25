package com.example.util;

import javafx.geometry.Pos;
import javafx.scene.control.TableCell;
import javafx.scene.layout.HBox;

/** Shared layout for centered action buttons in table rows. */
public final class RowActionCell {
    private RowActionCell() { }

    public static void showCentered(TableCell<?, ?> cell, HBox actions) {
        if (!cell.getStyleClass().contains("row-actions-cell")) {
            cell.getStyleClass().add("row-actions-cell");
        }
        actions.getStyleClass().add("row-actions");
        actions.setMaxWidth(Double.MAX_VALUE);
        actions.setAlignment(Pos.CENTER);
        cell.setAlignment(Pos.CENTER);
        cell.setGraphic(actions);
    }
}
