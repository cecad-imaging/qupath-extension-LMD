package org.cecad.lmd.ui;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
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

//        List<String> detectionClasses = command.getAllClassesNames();

        boolean isClassification = !command.getAllClassesNames().isEmpty();

        setPadding(new Insets(10));
        setSpacing(10);

        Map<String, Integer> classesCounts = command.getAllClassesCounts();

        GridPane wellGrid = createWellGrid(command.getAllClassesNames());

        HBox controlsButtonsBox = new HBox();
        controlsButtonsBox.setSpacing(10);
        Button cancelButton = new Button("Cancel");
        int BUTTON_WIDTH = 152;
        if (!isClassification)
            BUTTON_WIDTH = 82;
        int BUTTON_HEIGHT = 25;
        cancelButton.setPrefSize(BUTTON_WIDTH, BUTTON_HEIGHT);
        Button doneButton = new Button("Save");
        doneButton.setPrefSize(BUTTON_WIDTH, BUTTON_HEIGHT);

        controlsButtonsBox.getChildren().addAll(cancelButton, doneButton);

        // DONE:
        // Before getChildren().addAll(wellGrid, controlsButtonsBox); look for a file
        // if numWells and the label from controls correspond and add well grid from there
        // if the file exists
        File wellDataFile = findWellDataFile(TEMP_SUBDIRECTORY.toString(), IOUtils.genWellDataFileNameFromWellsNum(numWells));
        if (wellDataFile == null)
            getChildren().addAll(wellGrid, controlsButtonsBox);
        else {
            getChildren().addAll(readWellGridDataFromFile(wellGrid, wellDataFile), controlsButtonsBox);
        }

        if (isClassification)
            updateSpinnersMaxValues(wellGrid, classesCounts);

        doneButton.setOnAction(event -> {
            // Save wellGrid to a file:
            List<Map<String, Object>> wellDataList = getWellData(wellGrid, isClassification);
            if (TEMP_SUBDIRECTORY == null)
                command.getLogger().error("'LMD Data/.temp' subdirectory doesn't exist!");
            IOUtils.saveWellsToFile(TEMP_SUBDIRECTORY, wellDataList, IOUtils.genWellDataFileNameFromWellsNum(numWells), command.getLogger());

            controls.updateCollectorLabel(getCollectorName(numWells));
            command.hideStage();
        });

        cancelButton.setOnAction(actionEvent -> {
            // DONE:
            // 1. Don't save anything to a file | done lol
            // 2. Look for a file with the title corresponding to wellDataFileName | Done on line 63
            // 3. If it doesn't exist simply close the stage
            // 4. If it does, delete current wellGrid and add one with data from the file
            if (wellDataFile == null)
                command.closeStage();
            else{
                command.hideStage();
                getChildren().removeAll(wellGrid, controlsButtonsBox);
                getChildren().addAll(readWellGridDataFromFile(wellGrid, wellDataFile), controlsButtonsBox);
            }

            // 5. Don't update the controls label ever I guess | yeah I guess
            // 6. Show the user notification | nah
        });
    }

    private GridPane readWellGridDataFromFile(GridPane wellGrid, File wellData) {

        try (Reader reader = new FileReader(wellData)) {
            if (wellData.getName().endsWith(".json")) {
                // Read JSON data
                Gson gson = new Gson();  // Add Gson dependency
                List<Map<String, Object>> dataList = gson.fromJson(reader, new TypeToken<List<Map<String, Object>>>(){}.getType());
                populateGridFromList(wellGrid, dataList);
            } else {
                // Handle unsupported file format
                System.out.println("Unsupported file format: " + wellData.getName());
            }
        } catch (IOException e) {
            command.getLogger().error(e.getMessage());
        }

        return wellGrid;
    }

    private void populateGridFromList(GridPane wellGrid, List<Map<String, Object>> dataList) {
        int expectedRows = wellGrid.getChildren().size() - 1; // Assuming header row is not created dynamically
        if (dataList.size() != expectedRows) {
            System.out.println("Warning: Data list size (" + dataList.size() + ") doesn't match grid rows (" + expectedRows + ").");
        }

        int rowIndex = 1;  // Assuming row 0 is the header
        for (Map<String, Object> dataMap : dataList) {
            if (rowIndex > expectedRows) {
                break;  // Stop if data list has more entries than grid rows
            }

            String wellLabel = (String) dataMap.get(WELL_LABEL);
            String objectType = (String) dataMap.get(OBJECT_CLASS_TYPE);
            int objectQty = (int) (double) dataMap.get(OBJECT_QTY);

            Label wellLabelElement = (Label) wellGrid.getChildren().get(rowIndex * 3);
            ComboBox<String> classComboBox = (ComboBox<String>) wellGrid.getChildren().get(rowIndex * 3 + 1);
            Spinner<Integer> spinner = (Spinner<Integer>) wellGrid.getChildren().get(rowIndex * 3 + 2);

            wellLabelElement.setText(wellLabel);
            classComboBox.setValue(objectType);
            spinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 0, objectQty));

            rowIndex++;
        }
    }

    private File findWellDataFile(String tempSubdirectory, String wellDataFileName) {
        File tempDir = new File(tempSubdirectory);

        if (!tempDir.exists() || !tempDir.isDirectory()) {
            command.getLogger().error("'LMD Data/.temp' subdirectory doesn't exist!");
            return null;
        }

        for (File file : Objects.requireNonNull(tempDir.listFiles())) {
            if (file.isFile() && file.getName().equals(wellDataFileName)) {
                return file;
            }
        }
        return null;
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
}

