package org.cecad.lmd.ui;

import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.layout.HBox;

public class WellPlateSubPane extends HBox {
    public WellPlateSubPane(){
        setPadding(new Insets(0)); // Set some padding
        setSpacing(10); // Set spacing between elements

        Spinner<Integer> wellNumSpinner = new Spinner<>(1, 96, 1);
        wellNumSpinner.setPrefWidth(70);

        ComboBox<String> classComboBox = new ComboBox<>();
        classComboBox.getItems().addAll("STROMA", "OTHER");
        classComboBox.setPrefWidth(100);

        Spinner<Integer> percentageSpinner = new Spinner<>(0, 100, 1);
        percentageSpinner.setPrefWidth(70);

        Label spacerLabel = new Label("");
        spacerLabel.setPrefWidth(5);

        getChildren().addAll(wellNumSpinner, spacerLabel, classComboBox, percentageSpinner);
    }
}
