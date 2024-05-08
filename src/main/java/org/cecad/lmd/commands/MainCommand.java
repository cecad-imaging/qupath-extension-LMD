package org.cecad.lmd.commands;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.cecad.lmd.ui.MainPane;
import org.cecad.lmd.common.Constants;
import org.controlsfx.control.action.Action;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.fx.dialogs.Dialogs;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.images.ImageData;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.hierarchy.PathObjectHierarchy;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import static org.cecad.lmd.common.Constants.CollectorTypes.NONE;
import static qupath.lib.scripting.QP.exportObjectsToGeoJson;

public class MainCommand implements Runnable {

    private final static Logger logger = LoggerFactory.getLogger(MainCommand.class);
    private final String TITLE = "LMD Support";
    private final String SELECTED = Constants.Detections.SELECTED;
    private final String ALL = Constants.Detections.ALL;
    private Stage stage;
    private final QuPathGUI qupath;
    ImageData<BufferedImage> imageData;
    MainPane mainPane;

    public MainCommand(QuPathGUI qupath) {
        this.qupath = qupath;
    }

    public QuPathGUI getQuPath() {
        return qupath;
    }

    @Override
    public void run() {
        if (qupath.getImageData() == null){
            Dialogs.showErrorNotification("No Image", "Please load an image first.");
            return;
        }
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
        mainPane = new MainPane(this);
        Scene scene = new Scene(mainPane);
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

    public void runExport() throws IOException {
        imageData = qupath.getImageData();
        if (imageData == null){
            Dialogs.showErrorNotification("No Image", "Please load an image to perform this action.");
            return;
        }
        PathObjectHierarchy hierarchy = imageData.getHierarchy();
        Path projectFilePath = qupath.getProject().getPath();

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        String currentTime = dateFormat.format(new Date());
        final String DEFAULT_GeoJSON_NAME = currentTime + ".geojson";
        final String DEFAULT_XML_NAME = imageData.getServer().getMetadata().getName().replaceFirst("\\.[^.]+$", "_" + currentTime + ".xml");

        final String pathGeoJSON = createSubdirectory(projectFilePath, "LMD data" + File.separator + ".temp").resolve(DEFAULT_GeoJSON_NAME).toString();
        final String pathXML = createSubdirectory(projectFilePath, "LMD data").resolve(DEFAULT_XML_NAME).toString();

        String comboBoxChoice = mainPane.getDetectionsToExport();

        Collection<PathObject> detectionsToExport;

        if (comboBoxChoice.equals(SELECTED)){
            if (hierarchy.getSelectionModel().noSelection()) {
                Dialogs.showErrorNotification("No selection detected",
                        "You had chosen to export selected objects but no selection has been detected.");
                return;
            }
            detectionsToExport = new ArrayList<>(hierarchy.getSelectionModel().getSelectedObjects());
            // Include Calibration even if not selected (by adding all annotations, they will be filtered out in GeojsonToXml anyway).
            detectionsToExport.addAll(hierarchy.getAnnotationObjects());
        }
        else {
            detectionsToExport = hierarchy.getAllObjects(false);
        }

        String collectorType = mainPane.getCollector();
        boolean processCollectors = !collectorType.equals(NONE);

        // TODO We have to:
        // 1. On doneButton pressed in any Collector Pane send all of the info form there somewhere
        // 2. E.g. create an object of a new class that will store these options or save them to temporary file
        // 3. Then we need to use them and send to GeoJsonToXml object to precess
        // (bcs it's actually a processor, not a simple converter - should change the name prolly)

        exportObjectsToGeoJson(detectionsToExport, pathGeoJSON, "FEATURE_COLLECTION");

    }

    private static Path createSubdirectory(Path projectFilePath, String subdirectory){
        if (projectFilePath != null) {
            Path projectDirectory = projectFilePath.getParent();
            if (projectDirectory != null) {
                Path subdirectoryPath = projectDirectory.resolve(subdirectory);
                try {
                    Files.createDirectories(subdirectoryPath); // Create the directory if it doesn't exist
                } catch (IOException e) {
                    logger.error("Error creating subdirectories: " + e.getMessage(), e);
                }
                return subdirectoryPath;
            }
        }
        // If the project is null, return the current working directory.
        // This should probably naturally never happen but idk.
        return Paths.get(System.getProperty("user.dir"));
    }
}
