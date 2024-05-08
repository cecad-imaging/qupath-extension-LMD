package org.cecad.lmd.commands;

import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import org.cecad.lmd.ui.ControlsInterface;
import org.cecad.lmd.ui.WellPlatePane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.lib.gui.QuPathGUI;

public class WellPlateCommand implements Runnable {
    private final static Logger logger = LoggerFactory.getLogger(SetCollectorCommand.class);
    private final String TITLE = "96-Well Plate";
    private Stage stage;
    private final QuPathGUI qupath;
    private final ControlsInterface mainPane;

    public WellPlateCommand(QuPathGUI qupath, ControlsInterface mainPane) {
        this.qupath = qupath;
        this.mainPane = mainPane;
    }

    @Override
    public void run() {
        showStage();
    }

    public void closeStage(){
        if (stage.isShowing())
            stage.close();
    }

    public void hideStage() {
        stage.hide();
    }

    public void revokeStage(){
        stage.show();
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
        Stage stage = new Stage();
        Pane pane = new WellPlatePane(this, mainPane);
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
}
