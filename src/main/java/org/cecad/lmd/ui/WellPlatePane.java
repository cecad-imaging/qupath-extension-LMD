package org.cecad.lmd.ui;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.cecad.lmd.commands.WellPlateCommand;
import qupath.fx.dialogs.Dialogs;

import java.util.*;

import static org.cecad.lmd.common.Constants.Paths.TEMP_SUBDIRECTORY;
import static org.cecad.lmd.common.Constants.WellDataFileNames._96_WELL_PLATE_DATA;

public class WellPlatePane extends VBox {

    private final WellPlateCommand command;
    private final String AREA_TEXT = "Area (%)           ";
    private final String NUMBER_TEXT = "Objects (qty)   ";
    List<String> allClasses;
    Map<String, Integer> classesCounts;

    public WellPlatePane(WellPlateCommand command, ControlsInterface controls) {
        super();
        this.command = command;
        this.allClasses = command.getAllClassesNames();
        this.classesCounts = command.getAllClassesCounts();

        boolean isClassification = !allClasses.isEmpty();

        setPadding(new Insets(10)); // Set padding around the entire pane
        setSpacing(5); // Set spacing between elements

        // Initial SubPane
        WellPlateSubPane wellSubPane = new WellPlateSubPane(allClasses, classesCounts, command.getAllDetectionsCount());
        wellSubPane.setPrefHeight(30);

        Label wellLabel = new Label("Well");
        wellLabel.setPrefWidth(85);
        Label classLabel = new Label("Objects (type)");
        classLabel.setPrefWidth(100);

        Button percentageLabel = new Button(NUMBER_TEXT);
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
        Tooltip wellTooltip = new Tooltip("Standard 96-Well Plate well label");
        wellLabel.setTooltip(wellTooltip);
        wellTooltip.setShowDuration(new Duration(30000));
        Tooltip percentageTooltip = new Tooltip("Amount of detection objects to assign to wells in this batch, \nspecify either percentage of total area of detections or a specific number of detections");
        percentageLabel.setTooltip(percentageTooltip);
        percentageTooltip.setShowDuration(new Duration(30000));
        HBox headerLabelsBox = new HBox();
        headerLabelsBox.setSpacing(10);
        if (isClassification)
            headerLabelsBox.getChildren().addAll(wellLabel, classLabel, percentageLabel);
        else
            headerLabelsBox.getChildren().addAll(wellLabel, percentageLabel);

        // Buttons
        Button addWellButton = new Button("+");
        addWellButton.setPrefSize(40, 25);
        HBox addWellBox = new HBox();
        if (isClassification)
            addWellBox.setSpacing(125);
        else
            addWellBox.setSpacing(92);
        addWellBox.getChildren().addAll(new Label(""), addWellButton);

        HBox controlsButtonsBox = new HBox();
        controlsButtonsBox.setSpacing(10);

        int BUTTON_WIDTH = 140;
        if (!isClassification)
            BUTTON_WIDTH = 105;
        int BUTTON_HEIGHT = 25;

        Button cancelButton = new Button("Cancel");
        cancelButton.setPrefSize(BUTTON_WIDTH, BUTTON_HEIGHT);
        cancelButton.setOnAction(actionEvent -> command.closeStage());

        Button doneButton = new Button("Save");
        doneButton.setPrefSize(BUTTON_WIDTH, BUTTON_HEIGHT);
        doneButton.setOnAction(event -> {
            if (isWellDataValid()){
                List<Map<String, Object>> wellDataList = getWellDataFromSubPanes(isClassification);
                if (TEMP_SUBDIRECTORY == null)
                    command.getLogger().error("'LMD Data/.temp' subdirectory doesn't exist! Please restart the extension.");
                IOUtils.saveWellsToFile(TEMP_SUBDIRECTORY, wellDataList, _96_WELL_PLATE_DATA, command.getLogger());

                controls.updateCollectorLabel("96-Well Plate");
                command.closeStage();
            }
        });

        controlsButtonsBox.getChildren().addAll(cancelButton, doneButton);

        getChildren().addAll(headerLabelsBox, wellSubPane, addWellBox, controlsButtonsBox);

        addWellButton.setOnAction(event -> {
            addWellSection(getChildren().indexOf(addWellBox));
        });
    }

    private void addWellSection(int index) {
        // Create a new well section pane
        WellPlateSubPane newSubPane = new WellPlateSubPane(allClasses, classesCounts, command.getAllDetectionsCount());
        newSubPane.setPrefHeight(30);

        // Insert the new subPane before the "+" button
        getChildren().add(index, newSubPane);

        Stage stage = (Stage) getScene().getWindow();
        stage.sizeToScene();
    }

    private boolean isWellDataValid(){
        boolean isClassification = !allClasses.isEmpty();

        if (isClassification) {
            Map<String, Integer> actualClassesCounts = new HashMap<>();

            for (int i = 0; i < getChildren().size(); i++) {

                if (getChildren().get(i) instanceof WellPlateSubPane subPane) {
                    if (!subPane.isSubPaneDataValid(true))
                        return false;

                    Map<String, Integer> subPaneCounts = subPane.getObjectsForClassCount();
                    for (Map.Entry<String, Integer> entry : subPaneCounts.entrySet()) {
                        actualClassesCounts.merge(entry.getKey(), entry.getValue(), Integer::sum);
                    }
                }

            }
            boolean areCountsEqual = classesCounts.equals(actualClassesCounts);
            if (!areCountsEqual) {
                Dialogs.showErrorMessage("Invalid Data", "The number of detections of each class can't exceed the total number of processed detections of this class.");
                return false;
            }
        }
        else {
            int referenceAllDetectionsCount = command.getAllDetectionsCount();
            int actualAllDetectionsCount = 0;

            for (int i = 0; i < getChildren().size(); i++) {
                if (getChildren().get(i) instanceof WellPlateSubPane subPane) {
                    actualAllDetectionsCount += subPane.getBasicCount();
                }
            }
            if (referenceAllDetectionsCount != actualAllDetectionsCount){
                Dialogs.showErrorMessage("Invalid Data", "The assigned number of detections can't exceed the total number of processed detections.");
                return false;
            }
        }
        return true;
    }

    private List<Map<String, Object>> getWellDataFromSubPanes(boolean isClassification) {
        List<Map<String, Object>> wellDataList = new ArrayList<>();
        Set<String> usedLabels = new HashSet<>();
        for (int i = 0; i < getChildren().size(); i++) {
            if (getChildren().get(i) instanceof WellPlateSubPane subPane) {
                Map<String, Object> wellData = subPane.getSubPaneWellData(usedLabels, isClassification);
                wellDataList.add(wellData);
            }
        }
        return wellDataList;
    }
}
