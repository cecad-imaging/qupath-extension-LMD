package org.cecad.lmd.ui;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;
import org.cecad.lmd.commands.MoreOptionsCommand;
import qupath.lib.gui.prefs.PathPrefs;

import java.io.IOException;

import static org.cecad.lmd.common.Constants.EnlargeOptions.*;


public class MoreOptionsPane extends GridPane {

    private final MoreOptionsCommand command;

    public MoreOptionsPane(MoreOptionsCommand command) {
        super();
        this.command = command;

        int BIG_BUTTON_WIDTH = 330;
        int SPACING_BETWEEN_SMALL_BUTTONS = 10;
        int SMALL_BUTTON_WIDTH = (BIG_BUTTON_WIDTH-SPACING_BETWEEN_SMALL_BUTTONS)/2;

        setPadding(new Insets(10));
        setHgap(20);
        setVgap(10);

        Label enlargeSectionLabel = new Label("Expand selected detections:");

        Label radiusLabel = new Label("Expansion radius:");
        Spinner<Integer> radiusSpinner = new Spinner<>(0, 100, 1);
        radiusSpinner.setPrefWidth(70);

        HBox radiusBox = new HBox();
        radiusBox.setSpacing(10);
        radiusBox.getChildren().addAll(radiusLabel, radiusSpinner);

        Label convertLabel = new Label("Convert selected objects:");

        Button enlargeButton = new Button("Expand");
        enlargeButton.setPrefWidth(SMALL_BUTTON_WIDTH);

        Button undoButton = new Button("Undo");
        undoButton.setPrefWidth(SMALL_BUTTON_WIDTH);

        HBox enlargeButtonsBox = new HBox();
        enlargeButtonsBox.setSpacing(SPACING_BETWEEN_SMALL_BUTTONS);
        enlargeButtonsBox.getChildren().addAll(undoButton, enlargeButton);

        Button detToAnnButton = new Button("Detections to annotations");
        detToAnnButton.setPrefWidth(BIG_BUTTON_WIDTH);
        Button annToDetButton = new Button("Annotations to detections");
        annToDetButton.setPrefWidth(BIG_BUTTON_WIDTH);

        Label sameClassLabel = new Label("If two objects of the same class intersect:");
        Label differentClassLabel = new Label("If two objects of different classes intersect:");

        ComboBox<String> sameClassComboBox = new ComboBox<>(FXCollections.observableArrayList(DISCARD_1, MERGE));
        ComboBox<String> differentClassComboBox = new ComboBox<>(FXCollections.observableArrayList(SET_PRIORITY, EXCLUDE_BOTH));
        sameClassComboBox.setPrefWidth(BIG_BUTTON_WIDTH);
        differentClassComboBox.setPrefWidth(BIG_BUTTON_WIDTH);
        sameClassComboBox.getSelectionModel().select(DISCARD_1);
        differentClassComboBox.getSelectionModel().select(SET_PRIORITY);

        enlargeButton.setOnAction(actionEvent -> {
            String sameClassChoice = sameClassComboBox.getSelectionModel().getSelectedItem();
            String diffClassChoice = differentClassComboBox.getSelectionModel().getSelectedItem();

            command.makeSelectedDetectionsBigger(radiusSpinner.getValue(), sameClassChoice, diffClassChoice);

        });

        undoButton.setOnAction(actionEvent -> command.undoEnlargement());

        detToAnnButton.setOnAction(actionEvent -> command.convertSelectedObjects(command.getQupath().getImageData().getHierarchy(),false));
        annToDetButton.setOnAction(actionEvent -> command.convertSelectedObjects(command.getQupath().getImageData().getHierarchy(),true));

        Label simplifyLabel = new Label("Simplify selected detections shapes:");

        HBox altitudeBox = new HBox();
        Label altitudeLabel = new Label("Altitude threshold (px):");
        Spinner<Double> altitudeSpinner = new Spinner<>(1.0, 10.0, 1.0, 0.1);
        altitudeSpinner.setPrefWidth(70);
        setDecimalFormattingForSpinner(altitudeSpinner);
        altitudeBox.setSpacing(10);
        altitudeBox.getChildren().addAll(altitudeLabel, altitudeSpinner);
        Label altitudeDescriptionLabel = new Label("Higher values result in simpler shapes");

        Button simplifyButton = new Button("Simplify shapes");
        simplifyButton.setOnAction(actionEvent -> command.simplifySelectedDetections(command.getQupath().getImageData().getHierarchy(), altitudeSpinner.getValue()));
        simplifyButton.setPrefWidth(BIG_BUTTON_WIDTH);

        Label detectionsBordersLabel = new Label("Visualize the laser (changes all detections border width):");

        HBox laserApertureBox = new HBox();
        Label laserApertureLabel = new Label("Laser's aperture (microns):");
        double defaultAperture = PathPrefs.detectionStrokeThicknessProperty().getValue();
        Spinner<Double> laserApertureSpinner = new Spinner<>(1.0, 50.0, defaultAperture, 0.1);
        laserApertureSpinner.setPrefWidth(70);
        setDecimalFormattingForSpinner(laserApertureSpinner);
        laserApertureBox.setSpacing(10);
        laserApertureBox.getChildren().addAll(laserApertureLabel, laserApertureSpinner);

        Button repaintBordersButton = new Button("Visualize");
        repaintBordersButton.setOnAction(actionEvent -> {
            try {
                command.repaintDetectionsBordersToMatchLaser(laserApertureSpinner.getValue());
            } catch (IOException e) {
                command.getLogger().error("Error while modifying shapes border width: {}", e.getMessage());
            }
        });
        repaintBordersButton.setPrefWidth(BIG_BUTTON_WIDTH);




        GridPane.setConstraints(enlargeSectionLabel, 0, 0);
        GridPane.setConstraints(radiusBox, 0, 1);

        GridPane.setConstraints(sameClassLabel, 0, 2);
        GridPane.setConstraints(sameClassComboBox, 0, 3);

        GridPane.setConstraints(differentClassLabel, 0, 4);
        GridPane.setConstraints(differentClassComboBox, 0, 5);

        GridPane.setConstraints(enlargeButtonsBox, 0, 6);

        GridPane.setConstraints(detectionsBordersLabel, 0, 7);
        GridPane.setConstraints(laserApertureBox, 0, 8);
        GridPane.setConstraints(repaintBordersButton, 0, 9);

        GridPane.setConstraints(convertLabel, 0, 10);
        GridPane.setConstraints(detToAnnButton, 0, 11);
        GridPane.setConstraints(annToDetButton, 0, 12);

        GridPane.setConstraints(simplifyLabel, 0, 13);
        GridPane.setConstraints(altitudeBox, 0, 14);
        GridPane.setConstraints(altitudeDescriptionLabel, 0, 15);
        GridPane.setConstraints(simplifyButton, 0, 16);

        getChildren().addAll(enlargeSectionLabel, radiusBox, sameClassLabel, sameClassComboBox, differentClassLabel, differentClassComboBox,
                enlargeButtonsBox,
                detectionsBordersLabel, laserApertureBox, repaintBordersButton,
                convertLabel, detToAnnButton, annToDetButton,
                simplifyLabel, altitudeBox, altitudeDescriptionLabel, simplifyButton);

    }

    private void setDecimalFormattingForSpinner(Spinner<Double> spinner) {
        SpinnerValueFactory.DoubleSpinnerValueFactory valueFactory = new SpinnerValueFactory.DoubleSpinnerValueFactory(1.0, 10.0, 1.0, 0.1);
        spinner.setValueFactory(valueFactory);

        StringConverter<Double> converter = new StringConverter<>() {
            @Override
            public String toString(Double value) {
                if (value == null) return "";
                return String.format("%.1f", value);
            }

            @Override
            public Double fromString(String string) {
                try {
                    return Double.parseDouble(string);
                } catch (NumberFormatException e) {
                    return spinner.getValueFactory().getValue();
                }
            }
        };
        valueFactory.setConverter(converter);

        TextFormatter<Double> formatter = new TextFormatter<>(converter, valueFactory.getValue(), change -> {
            String newText = change.getControlNewText();
            if (newText.matches("([0-9]+\\.[0-9]*)?")) {
                try {
                    double newValue = Double.parseDouble(newText);
                    if (newValue >= valueFactory.getMin() && newValue <= valueFactory.getMax()) {
                        return change; // Valid value, allow the change
                    }
                } catch (NumberFormatException ignored) {
                    // Ignore parsing errors
                }
            }
            return null; // Invalid value, prevent the change
        });

        spinner.getEditor().setTextFormatter(formatter);
    }
}
