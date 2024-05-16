package org.cecad.lmd.commands;

import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import org.cecad.lmd.common.ClassUtils;
import org.cecad.lmd.ui.ControlsInterface;
import org.cecad.lmd.ui.StandardCollectorsPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.objects.classes.PathClass;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StandardCollectorsCommand implements Runnable {
    private final static Logger logger = LoggerFactory.getLogger(SetCollectorCommand.class);
    private Stage stage;
    private final QuPathGUI qupath;
    private final int numWells;
    private final ControlsInterface mainPane;

    public StandardCollectorsCommand(QuPathGUI qupath, int numWells, ControlsInterface mainPane) {
        this.qupath = qupath;
        this.numWells = numWells;
        this.mainPane = mainPane;
    }

    @Override
    public void run() {
        showStage();
    }

    public void closeStage(){
        if (stage.isShowing())
            stage.close();
        stage = null;
    }

    public void hideStage() {
        stage.hide();
    }

//    public void revokeStage(){
//        showStage();
//    }

    public int getNumWells() {
        return numWells;
    }

    public Set<PathClass> getAllClasses(){
        return mainPane.getAllClasses();
    }

    public List<String> getAllClassesNames(){
        return new ArrayList<>(mainPane.getAllClasses().stream().map(PathClass::getName).toList());
    }

    public Map<String, Integer> getAllClassesCounts(){
        return ClassUtils.countObjectsOfAllClasses(mainPane.getDetectionsToExport());
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
        Pane pane = new StandardCollectorsPane(this, numWells, mainPane);
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

    public Logger getLogger(){
        return logger;
    }
}
