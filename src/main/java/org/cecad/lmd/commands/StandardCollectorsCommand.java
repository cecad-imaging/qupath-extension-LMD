package org.cecad.lmd.commands;

import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import org.cecad.lmd.ui.StandardCollectorsPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.lib.gui.QuPathGUI;

public class StandardCollectorsCommand implements Runnable {
    private final static Logger logger = LoggerFactory.getLogger(SetCollectorCommand.class);
    private Stage stage;
    private final QuPathGUI qupath;
    private final int numWells;

    public StandardCollectorsCommand(QuPathGUI qupath, int numWells) {
        this.qupath = qupath;
        this.numWells = numWells;
    }

    @Override
    public void run() {
        showStage();
    }
    private void showStage(){
        boolean creatingStage = stage == null;
        if (creatingStage)
            stage = createStage();
        if (stage.isShowing())
            return;
        stage.show();
    }

    private Stage createStage(){
        String TITLE = "";
        if (numWells == 8 || numWells == 12)
            TITLE = numWells + "-Fold Strip";
        else if (numWells == 5)
            TITLE = "PCR Tubes";
        else if (numWells == 2)
            TITLE = "Petri Dishes";

        Stage stage = new Stage();
        Pane pane = new StandardCollectorsPane(this, numWells);
        Scene scene = new Scene(pane);
        stage.setScene(scene);
        stage.setResizable(false);
        stage.setTitle(TITLE);
        stage.initOwner(qupath.getStage());
        stage.setOnCloseRequest(event -> {
            hideStage();
            event.consume();
        });
        return stage;
    }

    private void hideStage() {
        stage.hide();
    }
}
