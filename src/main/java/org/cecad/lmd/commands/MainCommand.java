package org.cecad.lmd.commands;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import org.cecad.lmd.ui.MainPane;
import org.controlsfx.control.action.Action;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.lib.gui.QuPathGUI;

public class MainCommand implements Runnable {

    private final static Logger logger = LoggerFactory.getLogger(MainCommand.class);
    private final String TITLE = "LMD Support";
    private Stage stage;
    private final QuPathGUI qupath;

    public QuPathGUI getQuPath() {
        return qupath;
    }

    public MainCommand(QuPathGUI qupath) {
        this.qupath = qupath;
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
        Stage stage = new Stage();
        Pane pane = new MainPane(this);
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
//        this.liveModeProperty.set(false);
//        this.imageDataProperty.unbind();
//        this.imageDataProperty.removeListener(imageDataListener); // To be sure...
        stage.hide();
    }

    public EventHandler<ActionEvent> openCollectorsPane(MainPane mainPane) {
        SetCollectorCommand setCollectorCommand = new SetCollectorCommand(qupath, mainPane);
        return new Action(event -> setCollectorCommand.run());
    }

    public EventHandler<ActionEvent> openMoreOptionsPane() {
        MoreOptionsCommand moCommand = new MoreOptionsCommand(qupath);
        return new Action(event -> moCommand.run());
    }
}
