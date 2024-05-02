package org.cecad.lmd.ui;

import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.layout.GridPane;

public class WellPlateSubPane extends GridPane {
    public WellPlateSubPane(){
        setPadding(new Insets(2)); // Set some padding
        setHgap(10); // Set horizontal spacing between elements
        setVgap(10); // Set vertical spacing between elements

        int LABEL_WIDTH = 100;
        // Labels
        Label classLabel = new Label("Class:");
        classLabel.setPrefWidth(LABEL_WIDTH);
        Label percentageLabel = new Label(("Percentage:"));
        percentageLabel.setPrefWidth(LABEL_WIDTH);
        Label wellsNumLabel = new Label("Number of wells:");
        wellsNumLabel.setPrefWidth(LABEL_WIDTH);

        // Other elements
        ComboBox<String> classComboBox = new ComboBox<>();
        classComboBox.getItems().addAll("STROMA", "OTHER");
        classComboBox.setPrefWidth(LABEL_WIDTH);

        Spinner<Integer> percentageSpinner = new Spinner<>(0, 100, 1);
        percentageSpinner.setPrefWidth(LABEL_WIDTH);
        Spinner<Integer> wellNumSpinner = new Spinner<>(1, 96, 1);
        wellNumSpinner.setPrefWidth(LABEL_WIDTH);

        GridPane.setConstraints(classLabel, 0, 0);
        GridPane.setConstraints(classComboBox, 1, 0);
        GridPane.setConstraints(percentageLabel, 0, 1);
        GridPane.setConstraints(percentageSpinner, 1, 1);
        GridPane.setConstraints(wellsNumLabel, 0, 2);
        GridPane.setConstraints(wellNumSpinner, 1, 2);




        getChildren().addAll(classLabel, percentageLabel, wellsNumLabel, classComboBox,
                percentageSpinner, wellNumSpinner);
    }
}
