package org.dgsob.exporting;

import org.dgsob.common.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.lib.gui.dialogs.Dialogs;
import qupath.lib.gui.dialogs.ParameterPanelFX;
import qupath.lib.images.ImageData;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.classes.PathClass;
import qupath.lib.objects.hierarchy.PathObjectHierarchy;
import qupath.lib.plugins.parameters.ParameterList;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

import static qupath.lib.scripting.QP.exportObjectsToGeoJson;
import static org.dgsob.exporting.ExportOptions.CollectorTypes.*;
import static org.dgsob.exporting.ExportOptions.CapAssignments.*;
import static org.dgsob.exporting.ExportOptions.CalibrationPointsNames.*;

public class ExportCommand {

    private static final Logger logger = LoggerFactory.getLogger(ExportCommand.class);

    /**
     * Main export logic: sets GeoJSON and XML paths and names,
     * exports objects from QuPath to GeoJSON, runs export to XML, deletes GeoJSON.
     *
     * @param projectFilePath project.qpproj file location, e.g. /home/user/QuPath/Projects/Project/project.qpproj
     * @param imageData Current image's data needed to access hierarchy and thus objects to export
     */
    public static void runExport(Path projectFilePath, ImageData<BufferedImage> imageData) throws IOException {
        PathObjectHierarchy hierarchy = imageData.getHierarchy();

        String allObjects = "All detection objects";
        String selectedObjects = "Selected detection objects";
        String defaultObjects = hierarchy.getSelectionModel().noSelection() ? allObjects : selectedObjects;

        ParameterList exportParams = new ParameterList()
                .addChoiceParameter("exportOptions", "Export:", defaultObjects,
                        Arrays.asList(allObjects, selectedObjects),
                        "Choose objects to export.")
                .addChoiceParameter("collectorChoice", "Collector type:", NO_COLLECTOR,
                        Arrays.asList(NO_COLLECTOR, PCR_TUBES, _8_FOLD_STRIP, _96_WELL_PLATE, PETRI),
                        "Choose a type of your collector.\n" +
                                  "You will be asked to assign your objects' classes to a specified collector's caps in the next window.");
//                .addBooleanParameter("excludeAnnotations", "Exclude Annotations", true,
//                        "If checked, all annotation objects (except calibration points) won't be exported.");

        boolean confirmed = Dialogs.showConfirmDialog("Export to LMD", new ParameterPanelFX(exportParams).getPane());

        if (!confirmed) {
            return;
        }

        // Part of the code responsible for setting objects to export to either selected or all detections.
        Collection<PathObject> chosenObjects;
        var comboChoice = exportParams.getChoiceParameterValue("exportOptions");
        if (comboChoice.equals("Selected detection objects")) {
            if (hierarchy.getSelectionModel().noSelection()) {
                Dialogs.showErrorMessage("No selection detected",
                        "You had chosen to export selected objects but no selection has been detected.");
                return;
            }
            chosenObjects = new ArrayList<>(hierarchy.getSelectionModel().getSelectedObjects());
            // Include Callibration even if not selected (by adding all annotations, they will be filtered out in GeojsonToXml anyway).
            chosenObjects.addAll(hierarchy.getAnnotationObjects());
        }
        else {
            chosenObjects = hierarchy.getAllObjects(false);
        }


        var collectorType = exportParams.getChoiceParameterValue("collectorChoice");
        boolean processCollectors = !collectorType.equals(NO_COLLECTOR);
        ParameterList collectorParams = null;
        if (processCollectors){
            collectorParams = createCollectorsParameterList(collectorType, chosenObjects);
            boolean confirmedSecondWindow = Dialogs.showConfirmDialog("Collector Assignment", new ParameterPanelFX(collectorParams).getPane());
            if (!confirmedSecondWindow){
                return;
            }
        }

        // Set default names for geojson and xml files
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        String currentTime = dateFormat.format(new Date());
        final String DEFAULT_GeoJSON_NAME = currentTime + ".geojson";
        final String DEFAULT_XML_NAME = imageData.getServer().getMetadata().getName().replaceFirst("\\.[^.]+$", "_" + currentTime + ".xml");

        // Set files' default paths
        final String pathGeoJSON = createSubdirectory(projectFilePath, "LMD data" + File.separator + ".temp").resolve(DEFAULT_GeoJSON_NAME).toString();
        final String pathXML = createSubdirectory(projectFilePath, "LMD data").resolve(DEFAULT_XML_NAME).toString();

        exportObjectsToGeoJson(chosenObjects, pathGeoJSON, "FEATURE_COLLECTION");

        GeojsonToXml xmlConverter = new GeojsonToXml(pathGeoJSON, pathXML);
        boolean succesfulConversion = xmlConverter.createLeicaXML(collectorParams);

        if (!succesfulConversion) {
            Dialogs.showErrorMessage("Incorrect Calibration Points",
                    "Please add 3 'Point' annotations, named " + CP1 + ", " + CP2 + " and " + CP3 + ".");
            return;
        }

        deleteTemporaryGeoJSON(pathGeoJSON);

        int exportedShapesCount = xmlConverter.getShapeCount();

        if (projectFilePath != null && exportedShapesCount == 1){
            Dialogs.showInfoNotification("Export successful",
                    "1 shape succesfully exported. Check 'LMD data' in your project's directory for the output XML file.");
        }
        else if (projectFilePath != null && exportedShapesCount != 0) {
            Dialogs.showInfoNotification("Export successful",
                    exportedShapesCount + " shapes succesfully exported. Check 'LMD data' in your project's directory for the output XML file.");
        }
        else if (projectFilePath != null){
            Dialogs.showErrorNotification("Export completed",
                    "Export completed but the number of exported detections is 0.");
        }
        else {
            Dialogs.showErrorMessage("Warning", "Couldn't access your project's directory. " +
                    "Check your home folder for the output files.");
        }
    }

    /**
     * Creates subdirectories in QuPath project folder.
     *
     * @param projectFilePath project.qpproj file location, e.g. /home/user/QuPath/Projects/Project/project.qpproj
     * @param subdirectory Name of the subdirectory to create
     * @return subdirectory path
     */
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
        private static void deleteTemporaryGeoJSON (String pathGeoJSON){
            try {
                Path geoJSONPath = Path.of(pathGeoJSON);
                Files.deleteIfExists(geoJSONPath);
            } catch (IOException e) {
                logger.warn("Error deleting GeoJSON interim file: " + e.getMessage(), e);
            }
        }
        private static ParameterList createCollectorsParameterList(Object collectorType, Collection<PathObject> chosenObjects){
            ParameterList collectorParams = new ParameterList();
            Set<PathClass> availableClasses = ClassUtils.getAllClasses(chosenObjects);
            List<String> classNames = new ArrayList<>(availableClasses.stream().map(PathClass::getName).toList());

            List<String> collectorOptions = new ArrayList<>();
            collectorOptions.add(NO_ASSIGNMENT);
            collectorOptions.add(ALL_OBJECTS);
            collectorOptions.addAll(classNames);
            if (!classNames.isEmpty())
                collectorOptions.add(REMAINING_OBJECTS);

            String defaultValue = NO_ASSIGNMENT;

            if (collectorType.equals(PCR_TUBES)) {
                collectorParams
                        .addChoiceParameter("A", "A", defaultValue, collectorOptions)
                        .addChoiceParameter("B", "B", defaultValue, collectorOptions)
                        .addChoiceParameter("C", "C", defaultValue, collectorOptions)
                        .addChoiceParameter("D", "D", defaultValue, collectorOptions)
                        .addChoiceParameter("E", "E", defaultValue, collectorOptions);
            }
            else if (collectorType.equals(_8_FOLD_STRIP)) {
                collectorParams
                        .addChoiceParameter("A", "A", defaultValue, collectorOptions)
                        .addChoiceParameter("B", "B", defaultValue, collectorOptions)
                        .addChoiceParameter("C", "C", defaultValue, collectorOptions)
                        .addChoiceParameter("D", "D", defaultValue, collectorOptions)
                        .addChoiceParameter("E", "E", defaultValue, collectorOptions)
                        .addChoiceParameter("F", "F", defaultValue, collectorOptions)
                        .addChoiceParameter("G", "G", defaultValue, collectorOptions)
                        .addChoiceParameter("H", "H", defaultValue, collectorOptions);
            }
//            else if (collectorType.equals(_96_WELL_PLATE)) {
//            }
            else if (collectorType.equals(PETRI)) {
                collectorParams
                        .addChoiceParameter("A", "A", defaultValue, collectorOptions)
                        .addChoiceParameter("B", "B", defaultValue, collectorOptions);
            }
            return collectorParams;
        }
    }
