package org.cecad.lmd.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Spinner;
import javafx.scene.layout.GridPane;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import org.cecad.lmd.commands.MoreOptionsCommand;


public class MoreOptionsPane extends GridPane {

    private final MoreOptionsCommand command;

    public MoreOptionsPane(MoreOptionsCommand command) {
        super();
        this.command = command;

        setPadding(new Insets(10));
        setHgap(20);
        setVgap(10);

        Label radiusLabel = new Label("Enlarge detections by a radius:");
        radiusLabel.setPrefWidth(180);
//        radiusLabel.setFont(new Font(18));
        Label convertLabel = new Label("Convert objects:");

        Button enlargeButton = new Button("Enlarge");
        enlargeButton.setPrefWidth(270);
        Button detToAnnButton = new Button("Detections to Annotations");
        detToAnnButton.setPrefWidth(270);
        Button annToDetButton = new Button("Annotations to Detections");
        annToDetButton.setPrefWidth(270);
        Button undoButton = new Button("Undo");
        undoButton.setPrefWidth(120);
        Button saveButton = new Button("Save");
        saveButton.setPrefWidth(120);
        HBox controlsButtonsBox = new HBox();
        controlsButtonsBox.setSpacing(30);
        controlsButtonsBox.getChildren().addAll(undoButton, saveButton);

        Spinner<Integer> radiusSpinner = new Spinner<>(0, 100, 1);
        radiusSpinner.setPrefWidth(70);

        GridPane.setConstraints(radiusLabel, 0, 0);
        GridPane.setConstraints(radiusSpinner, 1, 0);
        GridPane.setColumnSpan(enlargeButton, 2);
        GridPane.setConstraints(enlargeButton, 0, 1);
        GridPane.setColumnSpan(convertLabel, 2);
        GridPane.setConstraints(convertLabel, 0, 2);
        GridPane.setColumnSpan(detToAnnButton, 2);
        GridPane.setConstraints(detToAnnButton, 0, 3);
        GridPane.setColumnSpan(annToDetButton, 2);
        GridPane.setConstraints(annToDetButton, 0, 4);
        GridPane.setColumnSpan(controlsButtonsBox, 2);
        GridPane.setConstraints(controlsButtonsBox, 0, 6);

        getChildren().addAll(radiusLabel, radiusSpinner, enlargeButton, convertLabel,
                detToAnnButton, annToDetButton, controlsButtonsBox);

    }
}
