package org.cecad.lmd.ui;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.cecad.lmd.commands.StandardCollectorsCommand;

import java.util.Objects;

public class StandardCollectorsPane extends VBox {

    private final StandardCollectorsCommand command;
    private final IntegerProperty numWells;
    private final String[] wellLabels;
    private final String AREA_TEXT = "Area (%)           ";
    private final String NUMBER_TEXT = "Objects (qty)   ";

    public StandardCollectorsPane(StandardCollectorsCommand command, int numWells, ControlsInterface controls) {
        super();
        this.command = command;
        this.numWells = new SimpleIntegerProperty(numWells);
        this.wellLabels = generateWellLabels(numWells);

        setPadding(new Insets(10));
        setSpacing(10);

        // GridPane for wells
        GridPane wellGrid = createWellGrid();

        HBox controlsButtonsBox = new HBox(); // Container for Cancel and Done buttons
        controlsButtonsBox.setSpacing(10);
        Button cancelButton = new Button("Cancel");
        cancelButton.setPrefSize(120, 25);
        cancelButton.setOnAction(actionEvent -> command.closeStage());


        Button doneButton = getDoneButton(numWells, controls, command);

        controlsButtonsBox.getChildren().addAll(cancelButton, doneButton);

        getChildren().addAll(wellGrid, controlsButtonsBox);
    }

    private static Button getDoneButton(int numWells, ControlsInterface controls, StandardCollectorsCommand command) {
        Button doneButton = new Button("Done");
        doneButton.setPrefSize(120, 25);

        doneButton.setOnAction(event -> {
            String collectorName = "";
            if (numWells == 8 || numWells == 12)
                collectorName = numWells + "-Fold Strip";
            else if (numWells == 5)
                collectorName = "PCR Tubes";
            else if (numWells == 2)
                collectorName = "Petri Dishes";
            controls.updateCollectorLabel(collectorName);
            command.closeStage();
        });
        return doneButton;
    }

    private String[] generateWellLabels(int numWells) {
        String[] labels = new String[numWells];
        for (int i = 0; i < numWells; i++) {
            labels[i] = Character.toString('A' + i);
        }
        return labels;
    }

    private GridPane createWellGrid() {
        GridPane gridPane = new GridPane();
        gridPane.setPadding(new Insets(5));
        gridPane.setHgap(10);
        gridPane.setVgap(5);

        // Header row
        Label wellLabel = new Label("Well");
        wellLabel.setPrefWidth(40);
        Label classLabel = new Label("Class");
        classLabel.setPrefWidth(70);
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
        gridPane.addRow(0, wellLabel, classLabel, percentageLabel);

        Tooltip classTooltip = new Tooltip("Classes of detections obtained from segmentation step");
        classLabel.setTooltip(classTooltip);
        classTooltip.setShowDuration(new Duration(30000));
        Tooltip wellTooltip = new Tooltip("Label of each well available in the chosen collector");
        wellLabel.setTooltip(wellTooltip);
        wellTooltip.setShowDuration(new Duration(30000));
        Tooltip percentageTooltip = new Tooltip("Amount of detection objects to assign to the well, \nspecify either percentage of total area of detections or a specific number of detections");
        percentageLabel.setTooltip(percentageTooltip);
        percentageTooltip.setShowDuration(new Duration(30000));

        // Add rows for wells
        for (int i = 0; i < numWells.get(); i++) {
            Label well = new Label(wellLabels[i]);
            ComboBox<String> comboBox = new ComboBox<>(FXCollections.observableArrayList("STROMA", "OTHER"));
            comboBox.setPrefWidth(100);
            Spinner<Integer> spinner = new Spinner<>(0, 100, 0);
            spinner.setPrefWidth(85);
            gridPane.addRow(i + 1, well, comboBox, spinner);
        }

        return gridPane;
    }
}

