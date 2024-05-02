package org.cecad.lmd.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import org.cecad.lmd.commands.MainCommand;

public class MainPane extends GridPane {

    private final MainCommand command;

    public MainPane(MainCommand command) {
        super();
        this.command = command;

        setPadding(new Insets(10)); // Set padding around the entire pane
        setHgap(2); // Set horizontal spacing between elements
        setVgap(10); // Set vertical spacing between elements

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
        // TODO: add action

        Button exportButton = new Button("Export");
        exportButton.setPrefWidth(290);
        // TODO: add action

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

        GridPane.setColumnSpan(exportButton, 2);
        GridPane.setConstraints(exportButton, 0, 4);

        // Make buttons grow horizontally
        GridPane.setHgrow(setCollectorButton, Priority.ALWAYS);
        GridPane.setHgrow(exportButton, Priority.ALWAYS);

        // Add elements to the grid
        getChildren().addAll(detectionsLabel, detectionsComboBox, collectorOptionLabel, collectorChosenLabel,
                setCollectorButton, moreOptionsButton, exportButton);
    }
}


