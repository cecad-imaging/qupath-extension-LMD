package org.cecad.lmd.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import org.cecad.lmd.commands.MainCommand;

public class MainPane extends GridPane {

    private final MainCommand command;

    public MainPane(MainCommand command) {
        super();
        this.command = command;

        setPadding(new Insets(10));
        setHgap(2);
        setVgap(10);

        // Labels
        Label detectionsLabel = new Label("Detections to export:");
        detectionsLabel.setPrefWidth(144);

        // Dropdown Menu (assuming String options)
        ComboBox<String> detectionsComboBox = new ComboBox<>();
        detectionsComboBox.getItems().addAll("Selected", "All");
        detectionsComboBox.setPrefWidth(144);

        Label collectorOptionLabel = new Label("Collector is set to:");
        collectorOptionLabel.setPrefWidth(144);
        Label collectorChosenLabel = new Label("96-well plate");
        collectorOptionLabel.setPrefWidth(144);

        // Buttons
        Button setCollectorButton = new Button("Set Collector");
        setCollectorButton.setPrefWidth(290);
        setCollectorButton.setOnAction(command.openCollectorsPane());

        Button moreOptionsButton = new Button("More Options");
        moreOptionsButton.setPrefWidth(290);
        moreOptionsButton.setOnAction(command.openMoreOptionsPane());

        Button exportButton = new Button("Export");
        exportButton.setPrefWidth(130);
        // TODO: add action

        Button cancelButton = new Button("Cancel");
        cancelButton.setPrefWidth(130);

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
}


