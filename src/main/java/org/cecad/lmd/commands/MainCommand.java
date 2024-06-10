package org.cecad.lmd.commands;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.cecad.lmd.common.ClassUtils;
import org.cecad.lmd.common.ObjectUtils;
import org.cecad.lmd.ui.IOUtils;
import org.cecad.lmd.ui.MainPane;
import org.cecad.lmd.common.Constants;
import org.controlsfx.control.action.Action;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.fx.dialogs.Dialogs;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.classes.PathClass;
import qupath.lib.objects.hierarchy.PathObjectHierarchy;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.cecad.lmd.common.Constants.CalibrationPointsNames.*;
import static org.cecad.lmd.common.Constants.CollectorTypes.NONE;
import static org.cecad.lmd.common.Constants.CollectorTypes._96_WELL_PLATE;
import static org.cecad.lmd.common.Constants.Paths.*;
import static org.cecad.lmd.common.Constants.WellDataFileFields.OBJECT_CLASS_TYPE;
import static org.cecad.lmd.ui.IOUtils.createSubdirectory;
import static qupath.lib.scripting.QP.exportObjectsToGeoJson;

public class MainCommand implements Runnable {

    private final static Logger logger = LoggerFactory.getLogger(MainCommand.class);
    private final String TITLE = "QuCut";
    private final String SELECTED = Constants.Detections.SELECTED;
    private final String ALL = Constants.Detections.ALL;
    private Stage stage;
    private final QuPathGUI qupath;
    MainPane mainPane;
    private Collection<PathObject> detectionsToExport; // contains detections + annotations for calibration points

    public MainCommand(QuPathGUI qupath) {
        this.qupath = qupath;
    }

    public QuPathGUI getQuPath() {
        return qupath;
    }

    @Override
    public void run() {
        if (qupath.getProject() == null){
            Dialogs.showInfoNotification("No open project detected", "Please create a project or open existing one.");
            return;
        }
        Path PROJECT_FILE_PATH = qupath.getProject().getPath();
        DATA_SUBDIRECTORY = createSubdirectory(PROJECT_FILE_PATH, "LMD data", logger);
        TEMP_SUBDIRECTORY = createSubdirectory(PROJECT_FILE_PATH, "LMD data" + File.separator + ".temp", logger);

        if (qupath.getImageData() == null){
            Dialogs.showInfoNotification("No open image detected", "Please open an image.");
            return;
        }
        clearWellData();
        showStage();
        updateDetectionsToExport();
    }

    public void closeStage(){
        if (stage.isShowing())
            stage.close();
    }

    public void clearWellData(){
        IOUtils.clearJsonFiles(TEMP_SUBDIRECTORY.toString(), logger);
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
            closeStage();
            event.consume();
            clearWellData();
        });
        return stage;
    }

    public void updateDetectionsToExport(){
        String comboBoxChoice = mainPane.getSelectedOrAll();
        PathObjectHierarchy hierarchy = qupath.getImageData().getHierarchy();

        if (comboBoxChoice.equals(SELECTED)){
            if (hierarchy.getSelectionModel().noSelection()) {
                Dialogs.showWarningNotification("No selection detected",
                        "No selection has been detected yet. Please select detections to process.");
                return;
            }
            detectionsToExport = new ArrayList<>(hierarchy.getSelectionModel().getSelectedObjects());
            ObjectUtils.filterOutAnnotations(detectionsToExport);
        }
        else {
            detectionsToExport = hierarchy.getDetectionObjects();
        }
    }

    public void openCollectorsPane(MainPane mainPane) {
        SetCollectorCommand setCollectorCommand = new SetCollectorCommand(qupath, mainPane);
        setCollectorCommand.run();
    }

    public EventHandler<ActionEvent> openMoreOptionsPane() {
        MoreOptionsCommand moCommand = new MoreOptionsCommand(qupath);
        return new Action(event -> moCommand.run());
    }

    public void runExport() throws IOException {

        if (qupath.getImageData().getHierarchy().getSelectionModel().noSelection()
                && mainPane.getSelectedOrAll().equals(SELECTED)) {
            Dialogs.showErrorNotification("No selection detected",
                    "You had chosen to export selected objects but no selection has been detected.");
            return;
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        String currentTime = dateFormat.format(new Date());
        final String DEFAULT_GeoJSON_NAME = currentTime + ".geojson";
        final String DEFAULT_NAME = qupath.getImageData().getServer().getMetadata().getName().replaceFirst("\\.[^.]+$", "_" + currentTime);
        final String DEFAULT_XML_NAME = DEFAULT_NAME + ".xml";

        final String pathGeoJSON = getTempSubdirectory().resolve(DEFAULT_GeoJSON_NAME).toString();
        final String pathXML = getDataSubdirectory().resolve(DEFAULT_XML_NAME).toString();

        // TODO: test if optimizeDetectionsOrder works
        updateDetectionsToExport();
        optimizeDetectionsOrder(detectionsToExport);
        Collection<PathObject> objectsToExport = ObjectUtils.getCalibrationPoints(qupath.getImageData().getHierarchy().getAnnotationObjects(), CP1, CP2, CP3);
        if (objectsToExport.isEmpty()){
            Dialogs.showErrorNotification("Missing Calibration Points",
                    "Please add 3 'Point' annotations, named " + CP1 + ", " + CP2 + " and " + CP3 + ".");
            return;
        }
        objectsToExport.addAll(detectionsToExport);
        exportObjectsToGeoJson(objectsToExport, pathGeoJSON, "FEATURE_COLLECTION");

        String collectorType = mainPane.getCollector();

        // Read file data
        String wellDataFilePath = TEMP_SUBDIRECTORY.resolve(IOUtils.genWellDataFileNameFromCollectorName(collectorType, logger)).toString();
        Map<String, Object>[] wellData = null;
        if (!Objects.equals(collectorType, NONE))
            wellData = getWellDataFromFile(wellDataFilePath);

        // Run BuildXmlCommand
        BuildXmlCommand xmlBuilder = new BuildXmlCommand(pathGeoJSON, pathXML, collectorType);
        boolean isXmlCreationSuccessful = xmlBuilder.createLeicaXML(wellData);

        if (wellData != null && wellData[0].containsKey(OBJECT_CLASS_TYPE)) {

            List<Map<String, Object>> wellLabels = getWellLabelsByClass(wellData, collectorType);
            Map<String, Integer> wellCounts = getCountsByClass(detectionsToExport);
            Map<String, Double> wellAreas = getAreasByClass(detectionsToExport);

            final String collectorName = DEFAULT_NAME + IOUtils.genWellDataFileNameFromCollectorName(collectorType, logger);

            createAuxiliaryFile(wellLabels, wellCounts, wellAreas, DATA_SUBDIRECTORY.resolve(collectorName).toString());
        }

        if (!isXmlCreationSuccessful) {
            Dialogs.showErrorNotification("XML Build Failed",
                    "Failed to build XML file.");
            return;
        }

        IOUtils.clearGeoJsonFile(pathGeoJSON, logger);

        int exportedShapesCount = xmlBuilder.getShapeCount();
        if (exportedShapesCount == 1){
            Dialogs.showInfoNotification("Export successful",
                    "1 shape successfully exported. Check 'LMD data' in your project's directory for the output Leica XML file and the JSON file with export details.");
        }
        else if (exportedShapesCount != 0) {
            Dialogs.showInfoNotification("Export successful",
                    exportedShapesCount + " shapes successfully exported. Check 'LMD data' in your project's directory for the output Leica XML file and the JSON file with export details.");
        }
        else{
            Dialogs.showWarningNotification("Export completed",
                    "The number of exported detections is 0.");
        }
    }

    public Path getDataSubdirectory(){
        if (DATA_SUBDIRECTORY == null)
            logger.error("'LMD Data' subdirectory doesn't exist!");
        return DATA_SUBDIRECTORY;
    }

    public Path getTempSubdirectory(){
        if (TEMP_SUBDIRECTORY == null)
            logger.error("'LMD Data/.temp' subdirectory doesn't exist!");
        return TEMP_SUBDIRECTORY;
    }

    public Logger getLogger(){
        return logger;
    }

    public Set<PathClass> getAllClasses(){
        return ClassUtils.getAllClasses(detectionsToExport);
    }

    public Collection<PathObject> getDetectionsToExport(){
        return detectionsToExport;
    }

    private Map<String, Object>[] getWellDataFromFile(String filePath) {
        try {
            File file = new File(filePath);
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(file, new TypeReference<Map<String, Object>[]>() {
            });
        } catch (IOException e) {
            logger.error("Error reading collector params from file{}", e.getMessage());
        }
        return null;
    }

    private void createAuxiliaryFile(List<Map<String, Object>> wellLabels,
                                     Map<String, Integer> wellCounts,
                                     Map<String, Double> wellAreas,
                                     String filePath) throws IOException {
        // Combine labels into a single map
        Map<String, List<String>> combinedWellLabels = new HashMap<>();
        for (Map<String, Object> labelMap : wellLabels) {
            labelMap.forEach((className, wells) -> {
                combinedWellLabels.computeIfAbsent(className, k -> new ArrayList<>()).addAll((List<String>) wells);
            });
        }

        // Create a map to hold all data with headers
        Map<String, Object> structuredData = new LinkedHashMap<>(); // LinkedHashMap preserves insertion order
        structuredData.put("Well labels by class", combinedWellLabels);
        structuredData.put("Shapes count by class", wellCounts);
        structuredData.put("Shapes area by class", wellAreas);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.writeValue(new File(filePath), structuredData);
    }


    private List<Map<String, Object>> getWellLabelsByClass(Map<String, Object>[] wellData, String collectorType){
        List<Map<String, Object>> wellDataList = new ArrayList<>();
        for (Map<String, Object> assignment : wellData) {

            String objectClass = (String) assignment.get(OBJECT_CLASS_TYPE);

            if (objectClass == null || objectClass.equals(NONE))
                continue;

            if (Objects.equals(collectorType, _96_WELL_PLATE)) {
                List<String> wellLabels = (List<String>) assignment.get("wellLabels");
                Map<String, Object> wellDataEntry = new HashMap<>();
                wellDataEntry.put(objectClass, wellLabels);

                wellDataList.add(wellDataEntry);
            }
            else {
                List<String> wellLabels = new ArrayList<>();
                String wellLabel = (String) assignment.get("wellLabel");
                wellLabels.add(wellLabel);
                Map<String, Object> wellDataEntry = new HashMap<>();
                wellDataEntry.put(objectClass, wellLabels);

                wellDataList.add(wellDataEntry);
            }
        }
        return wellDataList;
    }

    private Map<String, Integer> getCountsByClass(Collection<PathObject> objects){
        return ClassUtils.getObjectsCountByClass(objects);
    }

    private Map<String, Double> getAreasByClass(Collection<PathObject> objects){
        return ClassUtils.getObjectsAreaByClass(objects);
    }

    // Function that optimizes shapes order and thus minimizes laser's travel, only detections correspond to shapes,
    // annotations are either junk or calibration points filtered and used later on, respectively.
    private void optimizeDetectionsOrder(Collection<PathObject> detections){
        if (detections == null || detections.isEmpty()) {
            return;
        }

        List<PathObject> orderedDetections = new ArrayList<>(detections);
        List<PathObject> unvisitedDetections = new ArrayList<>(detections);

        // Start with an arbitrary detection object
        PathObject currentObject = unvisitedDetections.remove(0);
        orderedDetections.set(0, currentObject);

        // Greedy nearest-neighbor approach
        for (int i = 1; i < detections.size(); i++) {
            PathObject nearestObject = findNearestObject(currentObject, unvisitedDetections);
            orderedDetections.set(i, nearestObject);
            unvisitedDetections.remove(nearestObject);
            currentObject = nearestObject;
        }

        // Update the original collection
        detections.clear();
        detections.addAll(orderedDetections);
    }

    private PathObject findNearestObject(PathObject currentObject, List<PathObject> unvisitedObjects) {
        return Collections.min(unvisitedObjects, Comparator.comparingDouble(obj ->
                distance(currentObject.getROI().getCentroidX(), currentObject.getROI().getCentroidY(),
                        obj.getROI().getCentroidX(), obj.getROI().getCentroidY())
        ));
    }

    private double distance(double x1, double y1, double x2, double y2) {
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }

}
