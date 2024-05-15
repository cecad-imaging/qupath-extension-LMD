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

        Label enlargeSectionLabel = new Label("Expand selected detections:");

        Label radiusLabel = new Label("Expansion radius:");
        radiusLabel.setPrefWidth(120);
        Spinner<Integer> radiusSpinner = new Spinner<>(0, 100, 1);
        radiusSpinner.setPrefWidth(70);

        HBox radiusBox = new HBox();
        radiusBox.setSpacing(1);
        radiusBox.getChildren().addAll(radiusLabel, radiusSpinner);

        Label convertLabel = new Label("Convert selected objects:");

        Button enlargeButton = new Button("Expand");
        enlargeButton.setPrefWidth(130);

        Button undoButton = new Button("Undo");
        undoButton.setPrefWidth(130);

        HBox enlargeButtonsBox = new HBox();
        enlargeButtonsBox.setSpacing(10);
        enlargeButtonsBox.getChildren().addAll(undoButton, enlargeButton);

        Button detToAnnButton = new Button("Detections to annotations");
        detToAnnButton.setPrefWidth(270);
        Button annToDetButton = new Button("Annotations to detections");
        annToDetButton.setPrefWidth(270);

        Label sameClassLabel = new Label("If two objects of the same class intersect:");
        Label differentClassLabel = new Label("If two objects of different classes intersect:");

        ComboBox<String> sameClassComboBox = new ComboBox<>(FXCollections.observableArrayList(MERGE, DISCARD_1));
        ComboBox<String> differentClassComboBox = new ComboBox<>(FXCollections.observableArrayList(EXCLUDE_BOTH, SET_PRIORITY));
        sameClassComboBox.setPrefWidth(270);
        differentClassComboBox.setPrefWidth(270);

        enlargeButton.setOnAction(actionEvent -> {
            String sameClassChoice = sameClassComboBox.getSelectionModel().getSelectedItem();
            String diffClassChoice = differentClassComboBox.getSelectionModel().getSelectedItem();

            command.makeSelectedDetectionsBigger(radiusSpinner.getValue(), sameClassChoice, diffClassChoice);

        });

        undoButton.setOnAction(actionEvent -> command.undoEnlargement());

        detToAnnButton.setOnAction(actionEvent -> command.convertObjects(command.getQupath().getImageData(),false));
        annToDetButton.setOnAction(actionEvent -> command.convertObjects(command.getQupath().getImageData(),true));



        GridPane.setConstraints(enlargeSectionLabel, 0, 0);
        GridPane.setConstraints(radiusBox, 0, 1);

        GridPane.setConstraints(sameClassLabel, 0, 2);
        GridPane.setConstraints(sameClassComboBox, 0, 3);

        GridPane.setConstraints(differentClassLabel, 0, 4);
        GridPane.setConstraints(differentClassComboBox, 0, 5);

        GridPane.setConstraints(enlargeButtonsBox, 0, 6);

        GridPane.setConstraints(convertLabel, 0, 7);
        GridPane.setConstraints(detToAnnButton, 0, 8);
        GridPane.setConstraints(annToDetButton, 0, 9);

        getChildren().addAll(enlargeSectionLabel, radiusBox, sameClassLabel, sameClassComboBox, differentClassLabel, differentClassComboBox,
                enlargeButtonsBox,
                convertLabel, detToAnnButton, annToDetButton);

    }
}
