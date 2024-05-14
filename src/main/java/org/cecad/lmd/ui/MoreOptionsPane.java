package org.cecad.lmd.ui;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import org.cecad.lmd.commands.MoreOptionsCommand;
import static org.cecad.lmd.common.Constants.EnlargeOptions.*;


public class MoreOptionsPane extends GridPane {

    private final MoreOptionsCommand command;

    public MoreOptionsPane(MoreOptionsCommand command) {
        super();
        this.command = command;

        setPadding(new Insets(10));
        setHgap(20);
        setVgap(10);

        Label radiusLabel = new Label("Enlarge Selected Detections by a Radius:");
        radiusLabel.setPrefWidth(240);
        Spinner<Integer> radiusSpinner = new Spinner<>(0, 100, 1);
        radiusSpinner.setPrefWidth(70);

        HBox radiusBox = new HBox();
        radiusBox.setSpacing(1);
        radiusBox.getChildren().addAll(radiusLabel, radiusSpinner);

        Label convertLabel = new Label("Convert objects:");

        Button enlargeButton = new Button("Enlarge");
        enlargeButton.setPrefWidth(120);

        Button undoButton = new Button("Undo");
        undoButton.setPrefWidth(120);

        HBox enlargeButtonsBox = new HBox();
        enlargeButtonsBox.setSpacing(30);
        enlargeButtonsBox.getChildren().addAll(undoButton, enlargeButton);

        Button detToAnnButton = new Button("Detections to Annotations");
        detToAnnButton.setPrefWidth(270);
        Button annToDetButton = new Button("Annotations to Detections");
        annToDetButton.setPrefWidth(270);

        Label sameClassLabel = new Label("If 2 objects of the same class intersect:");
        Label differentClassLabel = new Label("If 2 objects of different classes intersect:");

        ComboBox<String> sameClassComboBox = new ComboBox<>(FXCollections.observableArrayList(MERGE, DISCARD_1));
        ComboBox<String> differentClassComboBox = new ComboBox<>(FXCollections.observableArrayList(EXCLUDE_BOTH, SET_PRIORITY));

        enlargeButton.setOnAction(actionEvent -> {
            String sameClassChoice = sameClassComboBox.getSelectionModel().getSelectedItem();
            String diffClassChoice = differentClassComboBox.getSelectionModel().getSelectedItem();

            command.makeSelectedDetectionsBigger(radiusSpinner.getValue(), sameClassChoice, diffClassChoice);

        });

        undoButton.setOnAction(actionEvent -> {

        });


        GridPane.setConstraints(radiusBox, 0, 0);

        GridPane.setConstraints(sameClassLabel, 0, 1);
        GridPane.setConstraints(sameClassComboBox, 0, 2);

        GridPane.setConstraints(differentClassLabel, 0, 3);
        GridPane.setConstraints(differentClassComboBox, 0, 4);

        GridPane.setConstraints(enlargeButtonsBox, 0, 5);

        GridPane.setConstraints(convertLabel, 0, 6);
        GridPane.setConstraints(detToAnnButton, 0, 7);
        GridPane.setConstraints(annToDetButton, 0, 8);

        getChildren().addAll(radiusBox, sameClassLabel, sameClassComboBox, differentClassLabel, differentClassComboBox,
                enlargeButtonsBox,
                convertLabel, detToAnnButton, annToDetButton);

    }
}
