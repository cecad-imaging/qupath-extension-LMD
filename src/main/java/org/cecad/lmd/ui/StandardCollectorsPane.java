package org.cecad.lmd.ui;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.cecad.lmd.commands.StandardCollectorsCommand;

public class StandardCollectorsPane extends VBox {

    private final StandardCollectorsCommand command;
    private final IntegerProperty numWells;
    private final String[] wellLabels;

    public StandardCollectorsPane(StandardCollectorsCommand command, int numWells) {
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
        cancelButton.setPrefWidth(130);
        Button doneButton = new Button("Done");
        doneButton.setPrefWidth(130);
        controlsButtonsBox.getChildren().addAll(cancelButton, doneButton);

        getChildren().addAll(wellGrid, controlsButtonsBox);
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
        wellLabel.setPrefWidth(70);
        Label classLabel = new Label("Class");
        classLabel.setPrefWidth(70);
        Label percentageLabel = new Label("Percentage");
        percentageLabel.setPrefWidth(70);
        gridPane.addRow(0, wellLabel, classLabel, percentageLabel);

        // Add rows for wells
        for (int i = 0; i < numWells.get(); i++) {
            Label well = new Label(wellLabels[i]);
            ComboBox<String> comboBox = new ComboBox<>(FXCollections.observableArrayList("STROMA", "OTHER"));
            comboBox.setPrefWidth(100);
            Spinner<Integer> spinner = new Spinner<>(0, 100, 0);
            spinner.setPrefWidth(70);
            gridPane.addRow(i + 1, well, comboBox, spinner);
        }

        return gridPane;
    }
}

