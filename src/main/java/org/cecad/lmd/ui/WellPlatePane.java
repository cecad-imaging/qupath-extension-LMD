package org.cecad.lmd.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.cecad.lmd.commands.WellPlateCommand;

import java.util.Objects;

public class WellPlatePane extends VBox {

    private final WellPlateCommand command;
    private final String AREA_TEXT = "Area (%)           ";
    private final String NUMBER_TEXT = "Objects (qty)   ";

    public WellPlatePane(WellPlateCommand command, ControlsInterface controls) {
        super();
        this.command = command;

        setPadding(new Insets(10)); // Set padding around the entire pane
        setSpacing(5); // Set spacing between elements

        // Initial SubPane
        WellPlateSubPane wellSubPane = new WellPlateSubPane();
        wellSubPane.setPrefHeight(30);

        Label wellLabel = new Label("Wells Number");
        wellLabel.setPrefWidth(85);
        Label classLabel = new Label("Class");
        classLabel.setPrefWidth(100);

        Button percentageLabel = new Button(AREA_TEXT);
        percentageLabel.setPrefWidth(85);
        percentageLabel.setPadding(new Insets(0));
        percentageLabel.setOnAction(event -> {
            String text = percentageLabel.getText();
            if (Objects.equals(text, AREA_TEXT))
                percentageLabel.setText(NUMBER_TEXT);
            else if (Objects.equals(text, NUMBER_TEXT))
                percentageLabel.setText(AREA_TEXT);
        });

        Tooltip classTooltip = new Tooltip("Classes of detections obtained from segmentation step");
        classLabel.setTooltip(classTooltip);
        classTooltip.setShowDuration(new Duration(30000));
        Tooltip wellTooltip = new Tooltip("Number of wells (0-96) among which specified amount and type of detections will be randomly distributed");
        wellLabel.setTooltip(wellTooltip);
        wellTooltip.setShowDuration(new Duration(30000));
        Tooltip percentageTooltip = new Tooltip("Amount of detection objects to assign to wells in this batch, \nspecify either percentage of total area of detections or a specific number of detections");
        percentageLabel.setTooltip(percentageTooltip);
        percentageTooltip.setShowDuration(new Duration(30000));
        HBox headerLabelsBox = new HBox();
        headerLabelsBox.setSpacing(10);
        headerLabelsBox.getChildren().addAll(wellLabel, classLabel, percentageLabel);

        // Buttons
        Button addWellButton = new Button("+");
        addWellButton.setPrefSize(40, 25);
        HBox addWellBox = new HBox();
        addWellBox.setSpacing(125);
        addWellBox.getChildren().addAll(new Label(""), addWellButton);
        HBox controlsButtonsBox = new HBox();
        controlsButtonsBox.setSpacing(10);
        Button cancelButton = new Button("Cancel");
        cancelButton.setPrefSize(140, 25);
        cancelButton.setOnAction(actionEvent -> command.closeStage());
        Button doneButton = getDoneButton(controls, command);
        doneButton.setPrefSize(140, 25);
        controlsButtonsBox.getChildren().addAll(cancelButton, doneButton);

        getChildren().addAll(headerLabelsBox, wellSubPane, addWellBox, controlsButtonsBox);

        addWellButton.setOnAction(event -> {
            addWellSection(getChildren().indexOf(addWellBox));
        });
    }

    private static Button getDoneButton(ControlsInterface controls, WellPlateCommand command) {
        Button doneButton = new Button("Done");
        doneButton.setPrefWidth(120);

        doneButton.setOnAction(event -> {
            controls.updateCollectorLabel("96-Well Plate");
            command.closeStage();
        });
        return doneButton;
    }

    private void addWellSection(int index) {
        // Create a new well section pane
        WellPlateSubPane newSubPane = new WellPlateSubPane();
        newSubPane.setPrefHeight(30);

        // Insert the new subPane before the "+" button
        getChildren().add(index, newSubPane);

        Stage stage = (Stage) getScene().getWindow();
        stage.sizeToScene();
    }
}
