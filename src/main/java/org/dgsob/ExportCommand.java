package org.dgsob;

import qupath.lib.gui.QuPathGUI;
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
import java.util.*;

import static qupath.lib.scripting.QP.exportObjectsToGeoJson;
import static org.dgsob.ExportOptions.CollectorTypes.*;
import static org.dgsob.ExportOptions.CapAssignments.*;

public class ExportCommand {
    private ExportCommand(){

    }

    /**
     * Static method responsible for the main export logic: sets GeoJSON and XML paths and names,
     * exports objects from QuPath to GeoJSON, runs export to XML, deletes GeoJSON.
     * @param qupath An instance of qupath needed to access project's directory
     * @param imageData Current imaage's data needed to access hierarchy and thus objects to export
     * @return returned boolean value is used to control the flow of the function
     */
    @SuppressWarnings("UnusedReturnValue")
    public static boolean runExport(QuPathGUI qupath, ImageData<BufferedImage> imageData) throws IOException {
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

        boolean confirmed = Dialogs.showConfirmDialog("Export to LMD", new ParameterPanelFX(exportParams).getPane());

        if (!confirmed) {
            return false;
        }

        // Part of the code responsible for setting objects to export to either selected or all detections.
        Collection<PathObject> chosenObjects;
        var comboChoice = exportParams.getChoiceParameterValue("exportOptions");
        if (comboChoice.equals("Selected detection objects")) {
            if (hierarchy.getSelectionModel().noSelection()) {
                Dialogs.showErrorMessage("No selection detected",
                        "You had chosen to export selected objects but no selection has been detected.");
                return false;
            }
            chosenObjects = new ArrayList<>(hierarchy.getSelectionModel().getSelectedObjects());
            // Automatically include MultiPoint even if not selected, other annotations will be ignored
            chosenObjects.addAll(hierarchy.getAnnotationObjects());
        }
        else {
            chosenObjects = hierarchy.getAllObjects(false);
        }
        //

        var collectorType = exportParams.getChoiceParameterValue("collectorChoice");
        boolean processCollectors = !collectorType.equals(NO_COLLECTOR);
        ParameterList collectorParams = null;
        if (processCollectors){
            collectorParams = createCollectorsParameterList(collectorType, chosenObjects);
            boolean confirmedSecondWindow = Dialogs.showConfirmDialog("Collector Assignment", new ParameterPanelFX(collectorParams).getPane());
            if (!confirmedSecondWindow){
                return false;
            }
        }

        // Set default names for geojson and xml files
        final String DEFAULT_GeoJSON_NAME = "temp.geojson";
        final String DEFAULT_XML_NAME = imageData.getServer().getMetadata().getName().replaceFirst("\\.[^.]+$", ".xml");

        // Get the current project.qpproj file path
        Path projectFilePath = qupath.getProject().getPath();

        // Set files' default paths
        final String pathGeoJSON = getProjectDirectory(projectFilePath, "LMD data" + File.separator + ".temp").resolve(DEFAULT_GeoJSON_NAME).toString();
        final String pathXML = getProjectDirectory(projectFilePath, "LMD data").resolve(DEFAULT_XML_NAME).toString();

        exportObjectsToGeoJson(chosenObjects, pathGeoJSON, "FEATURE_COLLECTION");

        GeojsonToXml xmlConverter = new GeojsonToXml();
        boolean succesfulConversion = xmlConverter.convertGeoJSONtoXML(pathGeoJSON, pathXML, collectorParams);

        if (!succesfulConversion) {
            Dialogs.showErrorMessage("Incorrect Calibration Points",
                    "Please add either a MultiPoint annotation named 'calibration' consisting of exactly 3 points " +
                              "or 3 separate annotations with a single point each, named 'calibration1', 'calibration2' and 'calibration3'.");
            return false;
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
        return true;
    }
        private static Path getProjectDirectory (Path projectFilePath, String subdirectory){
            // Return the path to the project directory, i.e. projectFilePath's parent.
            if (projectFilePath != null) {
                Path projectDirectory = projectFilePath.getParent();
                if (projectDirectory != null) {
                    Path subdirectoryPath = projectDirectory.resolve(subdirectory);
                    try {
                        Files.createDirectories(subdirectoryPath); // Create the directory if it doesn't exist
                    } catch (IOException e) {
                        // I don't know what to do here
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
                // Well, I guess it doesn't matter if it fails or not.
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
