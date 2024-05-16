package org.cecad.lmd.ui;

import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.HBox;
import static org.cecad.lmd.common.Constants.CapAssignments.*;
import static org.cecad.lmd.common.Constants.WellDataFileFields.*;

import java.util.*;

public class WellPlateSubPane extends HBox {
    public WellPlateSubPane(List<String> allClasses, Map<String, Integer> classesCounts){
        setPadding(new Insets(0));
        setSpacing(10);

        Spinner<Integer> wellNumSpinner = new Spinner<>(0, 96, 0);
        wellNumSpinner.setPrefWidth(85);

        ComboBox<String> classComboBox = new ComboBox<>();
        if (!allClasses.isEmpty())
            classComboBox.getItems().addAll(allClasses);
        else
            classComboBox.getItems().add(ALL_OBJECTS);
        classComboBox.getItems().add(NO_ASSIGNMENT);
        classComboBox.setPrefWidth(100);

        Spinner<Integer> percentageSpinner = new Spinner<>(0, 0, 0);
        percentageSpinner.setPrefWidth(85);

        Label countLabel = new Label("/ 0");
        countLabel.setPrefWidth(45);

        classComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && classesCounts.containsKey(newValue)) {
                int count = classesCounts.get(newValue);
                percentageSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, count, count));
                countLabel.setText("/ " + count);
            }
        });

        getChildren().addAll(wellNumSpinner, classComboBox, percentageSpinner, countLabel);
    }

    public Map<String, Object> getSubPaneWellData() {
        Map<String, Object> wellData = new HashMap<>();

        int wellCount = ((Spinner<Integer>) getChildren().get(0)).getValue();
        String objectType = ((ComboBox<String>) getChildren().get(1)).getValue();
        int objectQty = ((Spinner<Integer>) getChildren().get(2)).getValue();

        int objectsPerWell = 0;
        if (wellCount > 0) {
            objectsPerWell = objectQty / wellCount;
        }

        int redundantObjects = objectQty - wellCount*objectsPerWell;

        Set<String> wellLabels = generateRandomLabels(wellCount);

        wellData.put("wellLabels", wellLabels);
        wellData.put(WELL_COUNT, wellCount);
        wellData.put(OBJECT_CLASS_TYPE, objectType);
        wellData.put(OBJECT_QTY, objectQty);
        wellData.put("objectsPerWell", objectsPerWell);
        wellData.put("redundantObjects", redundantObjects);
        wellData.put("objectsPerWellAtTheBeginning", objectsPerWell);

        return wellData;
    }

    public Set<String> generateRandomLabels(int labelsQty) {
        Set<String> uniqueLabels = new HashSet<>();
        while (uniqueLabels.size() < labelsQty) {
            String wellLabel;
            do {
                int row = (int) Math.floor(Math.random() * 8) + 1; // Random row (1-8)
                int col = (int) Math.floor(Math.random() * 12) + 1; // Random column (1-12)
                wellLabel = Character.toString((char) (row + 64)) + col; // Convert row number to uppercase letter (A-H)
            } while (uniqueLabels.contains(wellLabel));
            uniqueLabels.add(wellLabel);
        }
        return uniqueLabels;
    }
}
