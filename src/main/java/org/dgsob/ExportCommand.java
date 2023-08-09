package org.dgsob;

import javafx.scene.layout.Pane;
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

        ParameterList parametersList = new ParameterList()
                .addChoiceParameter("exportOptions", "Export", defaultObjects,
                        Arrays.asList(allObjects, selectedObjects),
                        "Choose objects to export.")
                .addChoiceParameter("collectorChoice", "Choose the type of collector", "None",
                        Arrays.asList("None", "PCR Tubes","8-fold-Strip", "96-Wellplate", "Petri"));

        boolean confirmed = Dialogs.showConfirmDialog("Export to LMD", new ParameterPanelFX(parametersList).getPane());

        if (!confirmed) {
            return false;
        }

        // Part of the code responsible for setting objects to export to either selected or all detections.
        Collection<PathObject> chosenObjects;
        var comboChoice = parametersList.getChoiceParameterValue("exportOptions");
        if (comboChoice.equals("Selected detection objects")) {
            if (hierarchy.getSelectionModel().noSelection()) {
                Dialogs.showErrorMessage("Error", "No selection detected!");
            }
            chosenObjects = new ArrayList<>(hierarchy.getSelectionModel().getSelectedObjects());
            // Automatically include MultiPoint even if not selected, other annotations will be ignored
            chosenObjects.addAll(hierarchy.getAnnotationObjects());
        } else
            chosenObjects = hierarchy.getAllObjects(false);
        //

        boolean confirmedSecondWindow = true;
        var collectorType = parametersList.getChoiceParameterValue("collectorChoice");
        if (!collectorType.equals("None")){
            confirmedSecondWindow = Dialogs.showConfirmDialog("Collector Assignment", setCollectorsPane(collectorType, chosenObjects));
        }


        if (!confirmedSecondWindow){
            return false;
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

        GeojsonToXml.convertGeoJSONtoXML(pathGeoJSON, pathXML, ANNOTATION);

        deleteTemporaryGeoJSON(pathGeoJSON);

        if (projectFilePath != null) {
            Dialogs.showInfoNotification("Export successful", "Objects succesfully exported. Check 'LMD data' in your project's directory for the output XML file.");
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
                        // Handle the exception if necessary
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
        private static Pane setCollectorsPane(Object collectorType, Collection<PathObject> chosenObjects){
            // PCR A B C D E No Cap
            // 8-strip A B C D E F G H
            // 96 A-H, 1-12
            // Petri A B
            ParameterList parametersList = new ParameterList();
            List<String> collectorOptions = Arrays.asList("None", "All objects", "Stroma", "Tumor", "Positive", "Negative", "Remaining objects", "Other");
            if (collectorType.equals("PCR Tubes")) {
                parametersList
                        .addChoiceParameter("eppA", "A", "None", collectorOptions)
                        .addChoiceParameter("eppB", "B", "None", collectorOptions)
                        .addChoiceParameter("eppC", "C", "None", collectorOptions)
                        .addChoiceParameter("eppD", "D", "None", collectorOptions);
            }
            else if (collectorType.equals("8-fold-Strip")) {
                parametersList
                        .addChoiceParameter("capA", "A", "None", collectorOptions)
                        .addChoiceParameter("capB", "B", "None", collectorOptions)
                        .addChoiceParameter("capC", "C", "None", collectorOptions)
                        .addChoiceParameter("capD", "D", "None", collectorOptions)
                        .addChoiceParameter("capE", "E", "None", collectorOptions)
                        .addChoiceParameter("capF", "F", "None", collectorOptions)
                        .addChoiceParameter("capG", "G", "None", collectorOptions)
                        .addChoiceParameter("capH", "H", "None", collectorOptions);
            }
            else if (collectorType.equals("96-Wellplate")) {
            }
            else if (collectorType.equals("Petri")) {
                parametersList
                        .addChoiceParameter("dishA", "A", "None", collectorOptions)
                        .addChoiceParameter("dishB", "B", "None", collectorOptions);
            }
            return new ParameterPanelFX(parametersList).getPane();
        }
    }
