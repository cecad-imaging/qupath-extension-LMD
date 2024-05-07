package org.cecad.lmd.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import org.cecad.lmd.commands.SetCollectorCommand;

public class SetCollectorPane extends GridPane {

    private final SetCollectorCommand command;


    public SetCollectorPane(SetCollectorCommand command, ControlsInterface controls) {
        super();
        this.command = command;

        setPadding(new Insets(10)); // Set padding around the entire pane
        setHgap(2); // Set horizontal spacing between elements
        setVgap(10); // Set vertical spacing between elements

        Button pcrTubesButton = new Button("PCR Tubes");
        pcrTubesButton.setPrefWidth(200);
        pcrTubesButton.setOnAction(event -> {
            command.openStandardCollectorsPane(5);
            command.closeStage();
        });
        Button _8FoldStripButton = new Button("8-Fold Strip");
        _8FoldStripButton.setPrefWidth(200);
        _8FoldStripButton.setOnAction(event -> {
            command.openStandardCollectorsPane(8);
            command.closeStage();
        });

        Button _12FoldStripButton = new Button("12-Fold Strip");
        _12FoldStripButton.setPrefWidth(200);
        _12FoldStripButton.setOnAction(event -> {
            command.openStandardCollectorsPane(12);
            command.closeStage();
        });

        Button petriDishesButton = new Button("Petri Dishes");
        petriDishesButton.setPrefWidth((200));
        petriDishesButton.setOnAction(event -> {
            command.openStandardCollectorsPane(2);
            command.closeStage();
        });

        Button _96WellPlateButton = new Button("96-Well Plate");
        _96WellPlateButton.setPrefWidth(200);
        _96WellPlateButton.setOnAction(event -> {
            command.openWellPlatePane();
            command.closeStage();
        });

        Button NoneButton = new Button("None");
        NoneButton.setPrefWidth(200);
        NoneButton.setOnAction(event -> {
            controls.updateCollectorLabel("None");
            command.closeStage();
        });


        GridPane.setConstraints(pcrTubesButton, 0, 0);
        GridPane.setConstraints(_8FoldStripButton, 0, 1);
        GridPane.setConstraints(_12FoldStripButton, 0, 2);
        GridPane.setConstraints(petriDishesButton, 0, 3);
        GridPane.setConstraints(_96WellPlateButton, 0, 4);
        GridPane.setConstraints(NoneButton, 0, 5);

        GridPane.setHgrow(pcrTubesButton, Priority.ALWAYS);
        GridPane.setHgrow(_8FoldStripButton, Priority.ALWAYS);

        getChildren().addAll(pcrTubesButton, _8FoldStripButton, _12FoldStripButton,
                petriDishesButton, _96WellPlateButton, NoneButton);
    }


}
