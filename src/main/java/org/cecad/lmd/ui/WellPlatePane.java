package org.cecad.lmd.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.cecad.lmd.commands.WellPlateCommand;

public class WellPlatePane extends VBox {

    private final WellPlateCommand command;

    public WellPlatePane(WellPlateCommand command) {
        super();
        this.command = command;

        setPadding(new Insets(10)); // Set padding around the entire pane
        setSpacing(5); // Set spacing between elements

        // SubPane (initially empty)
        WellPlateSubPane wellSubPane = new WellPlateSubPane();

        // Buttons
        Button addWellButton = new Button("+");
        HBox addWellBox = new HBox();
        addWellBox.setSpacing(92);
        addWellBox.getChildren().addAll(new Label(""), addWellButton);
        HBox controlsButtonsBox = new HBox(); // Container for Cancel and Done buttons
        controlsButtonsBox.setSpacing(10);
        Button cancelButton = new Button("Cancel");
        cancelButton.setPrefWidth(100);
        Button doneButton = new Button("Done");
        doneButton.setPrefWidth(100);
        controlsButtonsBox.getChildren().addAll(cancelButton, doneButton);

        // Ensure proper order of elements
        getChildren().addAll(wellSubPane, addWellBox, controlsButtonsBox);

        // Add button handler for "+" button (example)
        addWellButton.setOnAction(event -> {
            addWellSection(wellSubPane); // Replaced wellGrid with wellSubPane here
        });
    }

    private void addWellSection(WellPlateSubPane wellSubPane) {
        // Create a new well section pane
        WellPlateSubPane newSubPane = new WellPlateSubPane();
        newSubPane.setPrefHeight(30);

        // Insert the new subPane before the "+" button
        getChildren().add(getChildren().indexOf(wellSubPane), newSubPane);

        Stage stage = (Stage) getScene().getWindow();
        stage.sizeToScene();
    }
}
