package org.cecad.lmd.commands;


import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import org.cecad.lmd.ui.ControlsInterface;
import org.cecad.lmd.ui.SetCollectorPane;
import org.controlsfx.control.action.Action;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.lib.gui.QuPathGUI;

public class SetCollectorCommand implements Runnable {
    private final static Logger logger = LoggerFactory.getLogger(SetCollectorCommand.class);
    private final String TITLE = "Set Collector";
    private Stage stage;
    private final QuPathGUI qupath;
    private final ControlsInterface mainPane;

    public SetCollectorCommand(QuPathGUI qupath, ControlsInterface mainPane) {
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
        Pane pane = new SetCollectorPane(this, mainPane);
        Scene scene = new Scene(pane);
        stage.setScene(scene);
        stage.setResizable(false);
        stage.setTitle(TITLE);
        stage.initOwner(qupath.getStage());
        stage.setOnCloseRequest(event -> {
            stage.close();
            event.consume();
        });
        return stage;
    }

    public void openWellPlatePane() {
        WellPlateCommand wpCommand = new WellPlateCommand(qupath, mainPane);
        wpCommand.run();
    }

    public void openStandardCollectorsPane(int numWells) {
        StandardCollectorsCommand fsCommand = new StandardCollectorsCommand(qupath, numWells, mainPane);
        fsCommand.run();
    }

    private void hideStage() {
        stage.hide();
    }
}
