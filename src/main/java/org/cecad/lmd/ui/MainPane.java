package org.cecad.lmd.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import org.cecad.lmd.commands.MainCommand;
import org.slf4j.LoggerFactory;
import qupath.lib.images.ImageData;
import org.cecad.lmd.common.Constants;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.classes.PathClass;

public class MainPane extends GridPane implements ControlsInterface {

    private static final Logger log = LoggerFactory.getLogger(MainPane.class);
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

        // Dropdown Menu
        detectionsComboBox = new ComboBox<>();
        detectionsComboBox.getItems().addAll(SELECTED, ALL);
        detectionsComboBox.setPrefWidth(144);
        detectionsComboBox.getSelectionModel().select(defaultDetections);
        detectionsComboBox.getSelectionModel().selectedItemProperty().addListener(event -> command.updateDetectionsToExport());

        Label collectorOptionLabel = new Label("Collector is set to:");
        collectorOptionLabel.setPrefWidth(144);
        collectorChosenLabel = new Label("None");
        collectorOptionLabel.setPrefWidth(144);

        // Buttons
        Button setCollectorButton = new Button("Set Collector");
        setCollectorButton.setPrefWidth(290);
        setCollectorButton.setOnAction(actionEvent -> {
            command.updateDetectionsToExport();
            command.openCollectorsPane(this);
        });

        Button moreOptionsButton = new Button("More Options");
        moreOptionsButton.setPrefWidth(290);
        moreOptionsButton.setOnAction(command.openMoreOptionsPane());

        Button exportButton = new Button("Export");
        exportButton.setPrefWidth(130);
        exportButton.setOnAction(actionEvent -> {
            try {
                command.runExport();
            } catch (IOException e) {
                Logger logger = command.getLogger();
                logger.error(e.getMessage());
            }
            command.closeStage();
            command.clearWellData();
        });

        Button cancelButton = new Button("Cancel");
        cancelButton.setPrefWidth(130);
        cancelButton.setOnAction(actionEvent -> {
            command.closeStage();
            command.clearWellData();
        });

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

    @Override
    public Set<PathClass> getAllClasses() {
        return command.getAllClasses();
    }

    @Override
    public Collection<PathObject> getDetectionsToExport() {
        return command.getDetectionsToExport();
    }

    public String getSelectedOrAll(){
        return detectionsComboBox.getSelectionModel().getSelectedItem();
    }

    public String getCollector(){
        return collectorChosenLabel.getText();
    }
}


