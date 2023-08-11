package org.dgsob;

import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.dialogs.Dialogs;
import qupath.lib.gui.dialogs.ParameterPanelFX;
import qupath.lib.images.ImageData;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.hierarchy.PathObjectHierarchy;
import qupath.lib.plugins.parameters.ParameterList;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.dgsob.GeojsonToXml.shapeType.ANNOTATION;
import static qupath.lib.scripting.QP.exportObjectsToGeoJson;

public class ExportCommand {
    private ExportCommand(){

    }

    public static boolean runExport(QuPathGUI qupath, ImageData<BufferedImage> imageData) throws IOException {
        PathObjectHierarchy hierarchy = imageData.getHierarchy();

        String allObjects = "All detection objects";
        String selectedObjects = "Selected detection objects";
        String defaultObjects = hierarchy.getSelectionModel().noSelection() ? allObjects : selectedObjects;

        ParameterList exportParams = new ParameterList()
                .addChoiceParameter("exportOptions", "Export", defaultObjects,
                        Arrays.asList(allObjects, selectedObjects),
                        "Choose objects to export.")
                .addChoiceParameter("collectorChoice", "Choose the type of collector", "None",
                        Arrays.asList("None", "PCR Tubes","8-fold-Strip", "96-Wellplate", "Petri"));

        boolean confirmed = Dialogs.showConfirmDialog("Export to LMD", new ParameterPanelFX(exportParams).getPane());

        if (!confirmed) {
            return false;
        }

        // Part of the code responsible for setting objects to export to either selected or all detections.
        Collection<PathObject> chosenObjects;
        var comboChoice = exportParams.getChoiceParameterValue("exportOptions");
        if (comboChoice.equals("Selected detection objects")) {
            if (hierarchy.getSelectionModel().noSelection()) {
                Dialogs.showErrorMessage("Error", "No selection detected!");
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
        boolean processCollectors = !collectorType.equals("None");
        ParameterList collectorParams = setCollectorsParameterList(collectorType, chosenObjects);
        if (processCollectors){
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
        xmlConverter.convertGeoJSONtoXML(pathGeoJSON, pathXML, ANNOTATION, collectorType, collectorParams);

        deleteTemporaryGeoJSON(pathGeoJSON);

        if (projectFilePath != null) {
            Dialogs.showInfoNotification("Export successful",
                    xmlConverter.getShapeCount() + " shapes succesfully exported. Check 'LMD data' in your project's directory for the output XML file.");
        } else {
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
        private static ParameterList setCollectorsParameterList(Object collectorType, Collection<PathObject> chosenObjects){
            // TODO: Add enum with values of collectorOptions, it should probably also be renamed to smth like classificationOptions
            ParameterList collectorParams = new ParameterList();
            List<String> collectorOptions = Arrays.asList("None", "All objects", "Stroma", "Tumor", "Positive", "Negative", "Remaining objects");
            if (collectorType.equals("PCR Tubes")) {
                collectorParams
                        .addChoiceParameter("A", "A", "None", collectorOptions)
                        .addChoiceParameter("B", "B", "None", collectorOptions)
                        .addChoiceParameter("C", "C", "None", collectorOptions)
                        .addChoiceParameter("D", "D", "None", collectorOptions)
                        .addChoiceParameter("E", "E", "None", collectorOptions)
                        .addChoiceParameter("No Cap", "No Cap", "None", collectorOptions); // TODO: No Cap ID is not No Cap, check in the LMD
            }
            else if (collectorType.equals("8-fold-Strip")) {
                collectorParams
                        .addChoiceParameter("A", "A", "None", collectorOptions)
                        .addChoiceParameter("B", "B", "None", collectorOptions)
                        .addChoiceParameter("C", "C", "None", collectorOptions)
                        .addChoiceParameter("D", "D", "None", collectorOptions)
                        .addChoiceParameter("E", "E", "None", collectorOptions)
                        .addChoiceParameter("F", "F", "None", collectorOptions)
                        .addChoiceParameter("G", "G", "None", collectorOptions)
                        .addChoiceParameter("H", "H", "None", collectorOptions)
                        .addChoiceParameter("No Cap", "No Cap", "None", collectorOptions); // TODO: No Cap ID is not No Cap, check in the LMD
            }
            else if (collectorType.equals("96-Wellplate")) {
            }
            else if (collectorType.equals("Petri")) {
                collectorParams
                        .addChoiceParameter("A", "A", "None", collectorOptions)
                        .addChoiceParameter("B", "B", "None", collectorOptions)
                        .addChoiceParameter("No Cap", "No Cap", "None", collectorOptions); // TODO: No Cap ID is not No Cap, check in the LMD
            }
            return collectorParams;
        }
    }
