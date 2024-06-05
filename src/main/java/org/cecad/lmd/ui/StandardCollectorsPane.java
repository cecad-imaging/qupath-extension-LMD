package org.cecad.lmd.ui;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.cecad.lmd.commands.StandardCollectorsCommand;
import org.cecad.lmd.common.Constants;
import qupath.fx.dialogs.Dialogs;

import java.util.*;

import static org.cecad.lmd.common.Constants.CollectorTypes.*;
import static org.cecad.lmd.common.Constants.Paths.*;
import static org.cecad.lmd.common.Constants.WellDataFileFields.*;

public class StandardCollectorsPane extends VBox {

    private final StandardCollectorsCommand command;
    private final IntegerProperty numWells;
    private final String[] wellLabels;
    private final String AREA_TEXT = "Area (%)             ";
    private final String NUMBER_TEXT = "Objects (count) ";

    public StandardCollectorsPane(StandardCollectorsCommand command, int numWells, ControlsInterface controls) {
        super();
        this.command = command;
        this.numWells = new SimpleIntegerProperty(numWells);
        this.wellLabels = generateWellLabels(numWells);

        boolean isClassification = !command.getAllClassesNames().isEmpty();

        setPadding(new Insets(10));
        setSpacing(10);

        Map<String, Integer> classesCounts = command.getAllClassesCounts();

        GridPane wellGrid = createWellGrid(command.getAllClassesNames());

        HBox controlsButtonsBox = new HBox();
        controlsButtonsBox.setSpacing(10);
        Button cancelButton = new Button("Cancel");
        int BUTTON_WIDTH = 150;
        if (!isClassification)
            BUTTON_WIDTH = 93;
        int BUTTON_HEIGHT = 25;
        cancelButton.setPrefSize(BUTTON_WIDTH, BUTTON_HEIGHT);
        Button doneButton = new Button("Save");
        doneButton.setPrefSize(BUTTON_WIDTH, BUTTON_HEIGHT);

        controlsButtonsBox.getChildren().addAll(cancelButton, doneButton);

        if (isClassification)
            updateSpinnersMaxValues(wellGrid, classesCounts);

        getChildren().addAll(wellGrid, controlsButtonsBox);

        doneButton.setOnAction(event -> {
            if (isWellDataValid(wellGrid, isClassification)) {
                // Save wellGrid to a file:
                List<Map<String, Object>> wellDataList = getWellData(wellGrid, isClassification);
                if (TEMP_SUBDIRECTORY == null)
                    command.getLogger().error("'LMD Data/.temp' subdirectory doesn't exist!");
                IOUtils.saveWellsToFile(TEMP_SUBDIRECTORY, wellDataList, IOUtils.genWellDataFileNameFromWellsNum(numWells), command.getLogger());

                controls.updateCollectorLabel(getCollectorName(numWells));
                command.closeStage();
            }
        });

        cancelButton.setOnAction(actionEvent -> command.closeStage());
    }

    private String[] generateWellLabels(int numWells) {
        String[] labels = new String[numWells];
        for (int i = 0; i < numWells; i++) {
            labels[i] = Character.toString('A' + i);
        }
        return labels;
    }

    private GridPane createWellGrid(List<String> allClasses) {
        boolean isClassification = !allClasses.isEmpty();

        GridPane gridPane = new GridPane();
        gridPane.setPadding(new Insets(5));
        gridPane.setHgap(10);
        gridPane.setVgap(5);

        // Header row
        Label wellLabel = new Label("Well");
        wellLabel.setPrefWidth(40);
        Label classLabel = new Label("Objects (type)");
        classLabel.setPrefWidth(85);
        Button percentageLabel = new Button(NUMBER_TEXT);
        percentageLabel.setPrefWidth(95);
        percentageLabel.setPadding(new Insets(0));
        GridPane.setColumnSpan(percentageLabel, 2);
        percentageLabel.setOnAction(event -> {
            String text = percentageLabel.getText();
            if (Objects.equals(text, AREA_TEXT))
                percentageLabel.setText(NUMBER_TEXT);
            else if (Objects.equals(text, NUMBER_TEXT))
                percentageLabel.setText(AREA_TEXT);
        });
        if (isClassification)
            gridPane.addRow(0, wellLabel, classLabel, percentageLabel);
        else
            gridPane.addRow(0, wellLabel, percentageLabel);

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

            Spinner<Integer> spinner = new Spinner<>(0, 0, 0);
            spinner.setPrefWidth(90);
            Label maxCount = new Label("/ 0");
            maxCount.setPrefWidth(45);

            if (isClassification) {
                ComboBox<String> comboBox = new ComboBox<>(FXCollections.observableArrayList(/* here we can maybe add all objects idk */));
                comboBox.getItems().addAll(allClasses);
                comboBox.getItems().add(Constants.CapAssignments.NO_ASSIGNMENT);
                comboBox.setPrefWidth(100);

                gridPane.addRow(i + 1, well, comboBox, spinner, maxCount);
            }
            else {
                int count = command.getAllDetectionsCount();
                spinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, count, 0));
                maxCount.setText("/ " + count);
                gridPane.addRow(i + 1, well, spinner, maxCount);
            }
        }

        return gridPane;
    }

    public List<Map<String, Object>> getWellData(GridPane gridPane, boolean isClassification) {
        List<Map<String, Object>> wellDataList = new ArrayList<>();
        for (int i = 1; i <= numWells.get(); i++) {
            int columnsCount = gridPane.getColumnCount();
            Map<String, Object> wellData = new HashMap<>();

            Label wellLabel = (Label) gridPane.getChildren().get(i * columnsCount - 1); // index is row*columns
            wellData.put(WELL_LABEL, wellLabel.getText());

            if (isClassification){
                ComboBox<String> classComboBox = (ComboBox<String>) gridPane.getChildren().get(i * columnsCount);
                wellData.put(OBJECT_CLASS_TYPE, classComboBox.getValue());

                Spinner<Integer> spinner = (Spinner<Integer>) gridPane.getChildren().get(i * columnsCount + 1);
                wellData.put(OBJECT_QTY, spinner.getValue());
            }
            else{
                Spinner<Integer> spinner = (Spinner<Integer>) gridPane.getChildren().get(i * columnsCount);
                wellData.put(OBJECT_QTY, spinner.getValue());
            }

            wellDataList.add(wellData);
        }
        return wellDataList;
    }

    private String getCollectorName(int numWells){
        if (numWells == 8)
            return _8_FOLD_STRIP;
        else if (numWells == 12)
            return _12_FOLD_STRIP;
        else if (numWells == 5)
            return PCR_TUBES;
        else if (numWells == 2)
            return PETRI_DISHES;
        return "";
    }

    private void updateSpinnersMaxValues(GridPane wellGrid, Map<String, Integer> classesCounts){
        List<ComboBox<String>> comboBoxes = new ArrayList<>();
        for (Node child : wellGrid.getChildren()) {
            if (child instanceof ComboBox) {
                comboBoxes.add((ComboBox<String>) child);
            }
        }
        for (ComboBox<String> comboBox : comboBoxes) {
            comboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null && classesCounts.containsKey(newValue)) {
                    int count = classesCounts.get(newValue);

                    int rowIndex = GridPane.getRowIndex(comboBox);
                    int spinnerIndex = rowIndex * (wellGrid.getColumnCount()) + 1;
                    int labelIndex = rowIndex * (wellGrid.getColumnCount()) + 2;

                    Spinner<Integer> spinner = (Spinner<Integer>) wellGrid.getChildren().get(spinnerIndex);
                    spinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, count, count));

                    Label label = (Label) wellGrid.getChildren().get(labelIndex);
                    label.setText("/ " + count);
                }
            });
        }
    }

    private boolean isWellDataValid(GridPane gridPane, boolean isClassification){
        Map<String, Integer> referenceClassesCounts = command.getAllClassesCounts();
        Map<String, Integer> actualClassesCounts = new HashMap<>();
        int referenceAllDetectionsCounts = command.getAllDetectionsCount();
        int actualAllDetectionsCounts = 0;

        for (int i = 1; i <= numWells.get(); i++) {
            int columnsCount = gridPane.getColumnCount();
            if (isClassification){
                ComboBox<String> classComboBox = (ComboBox<String>) gridPane.getChildren().get(i * columnsCount);
                Spinner<Integer> spinner = (Spinner<Integer>) gridPane.getChildren().get(i * columnsCount + 1);

                String selectedClass = classComboBox.getValue();
                if (selectedClass == null)
                    continue;
                int objectCount = spinner.getValue();
                // Add or update the count for the selected class in the actualClassesCounts map
                actualClassesCounts.merge(selectedClass, objectCount, Integer::sum);

                command.getLogger().debug("REF: {}", referenceClassesCounts.get(selectedClass));
                command.getLogger().debug("ACTUAL: {}", actualClassesCounts.get(selectedClass));
            }
            else{
                Spinner<Integer> spinner = (Spinner<Integer>) gridPane.getChildren().get(i * columnsCount);
                int objectCount = spinner.getValue();
                actualAllDetectionsCounts += objectCount;
            }
        }

        if (isClassification){
            boolean areCountsEqual = referenceClassesCounts.equals(actualClassesCounts);
            if (!areCountsEqual) {
                Dialogs.showErrorMessage("Invalid Data", "The number of detections of each class can't exceed the total number of processed detections for this class.");
                return false;
            }
        }
        else{
            if (referenceAllDetectionsCounts != actualAllDetectionsCounts) {
                Dialogs.showErrorMessage("Invalid Data", "The assigned number of detections can't exceed the total number of processed detections.");
                return false;
            }
        }
        return true;
    }
}

