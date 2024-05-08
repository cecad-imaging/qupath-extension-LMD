package org.cecad.lmd.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import org.cecad.lmd.commands.MainCommand;
import qupath.lib.images.ImageData;
import org.cecad.lmd.common.Constants;

import java.awt.image.BufferedImage;

public class MainPane extends GridPane implements ControlsInterface {

    private final MainCommand command;
    private Label collectorChosenLabel;
    ComboBox<String> detectionsComboBox;
    private final String SELECTED = Constants.Detections.SELECTED;
    private final String ALL = Constants.Detections.ALL;

    public MainPane(MainCommand command) {
        super();
        this.command = command;

        setPadding(new Insets(10));
        setHgap(2);
        setVgap(10);

        // Labels
        Label detectionsLabel = new Label("Detections to export:");
        detectionsLabel.setPrefWidth(144);

        String defaultDetections = "All";

        ImageData<BufferedImage> imageData = command.getQuPath().getImageData();

        if (imageData != null)
            defaultDetections = imageData.getHierarchy().getSelectionModel().noSelection() ? "All" : "Selected";

        // Dropdown Menu (assuming String options)
        detectionsComboBox = new ComboBox<>();
        detectionsComboBox.getItems().addAll(SELECTED, ALL);
        detectionsComboBox.setPrefWidth(144);
        detectionsComboBox.getSelectionModel().select(defaultDetections);

        Label collectorOptionLabel = new Label("Collector is set to:");
        collectorOptionLabel.setPrefWidth(144);
        collectorChosenLabel = new Label("None");
        collectorOptionLabel.setPrefWidth(144);

        // Buttons
        Button setCollectorButton = new Button("Set Collector");
        setCollectorButton.setPrefWidth(290);
        setCollectorButton.setOnAction(command.openCollectorsPane(this));

        Button moreOptionsButton = new Button("More Options");
        moreOptionsButton.setPrefWidth(290);
        moreOptionsButton.setOnAction(command.openMoreOptionsPane());

        Button exportButton = new Button("Export");
        exportButton.setPrefWidth(130);
        // TODO: add action

        Button cancelButton = new Button("Cancel");
        cancelButton.setPrefWidth(130);
        cancelButton.setOnAction(actionEvent -> command.closeStage());

        HBox controlsButtonsBox = new HBox();
        controlsButtonsBox.setSpacing(30);
        controlsButtonsBox.getChildren().addAll(cancelButton, exportButton);

        // GridPane constraints
        GridPane.setConstraints(detectionsLabel, 0, 0);
        GridPane.setConstraints(detectionsComboBox, 1, 0);

        GridPane.setConstraints(collectorOptionLabel, 0, 1);
        GridPane.setConstraints(collectorChosenLabel, 1, 1);

        // Buttons
        GridPane.setColumnSpan(setCollectorButton, 2);
        GridPane.setConstraints(setCollectorButton, 0, 2);

        GridPane.setColumnSpan(moreOptionsButton, 2);
        GridPane.setConstraints(moreOptionsButton, 0, 3);

        GridPane.setColumnSpan(controlsButtonsBox, 2);
        GridPane.setConstraints(controlsButtonsBox, 0, 5);

        // Make buttons grow horizontally
        GridPane.setHgrow(setCollectorButton, Priority.ALWAYS);
        GridPane.setHgrow(controlsButtonsBox, Priority.ALWAYS);

        // Add elements to the grid
        getChildren().addAll(detectionsLabel, detectionsComboBox, collectorOptionLabel, collectorChosenLabel,
                setCollectorButton, moreOptionsButton, controlsButtonsBox);
    }

    @Override
    public void updateCollectorLabel(String collectorName) {
        collectorChosenLabel.setText(collectorName);
    }

    public String getDetectionsToExport(){
        return detectionsComboBox.getSelectionModel().getSelectedItem();
    }

    public String getCollector(){
        return collectorChosenLabel.toString();
    }
}


