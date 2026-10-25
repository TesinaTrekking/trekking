package com.example.util;

import javafx.scene.control.Button;
import javafx.scene.control.TableView;

public final class TableCreateRow {
    private static final double HEADER_HEIGHT = 40;
    private static final double ROW_HEIGHT = 36;

    private TableCreateRow() { }

    public static void update(TableView<?> table, Button button, int visibleRows,
            boolean filtersActive, double emptyHeight) {
        boolean showButton = visibleRows > 0 && !filtersActive;
        button.setVisible(showButton);
        button.setManaged(showButton);

        table.setFixedCellSize(ROW_HEIGHT);
        double height = visibleRows > 0
                ? HEADER_HEIGHT + visibleRows * ROW_HEIGHT + 2
                : emptyHeight;
        table.setPrefHeight(height);
    }
}
