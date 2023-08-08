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

import static org.dgsob.GeojsonToXml.shapeType.ANNOTATION;
import static qupath.lib.scripting.QP.exportObjectsToGeoJson;

public class ExportCommand {
    private ExportCommand(){

    }

    public static boolean runExport(QuPathGUI qupath, ImageData<BufferedImage> imageData) throws IOException {
        PathObjectHierarchy hierarchy = imageData.getHierarchy();

        String allObjects = "All objects";
        String selectedObjects = "Selected objects";
        String defaultObjects = hierarchy.getSelectionModel().noSelection() ? allObjects : selectedObjects;

        ParameterList parametersList = new ParameterList()
                .addChoiceParameter("exportOptions", "Export", defaultObjects,
                        Arrays.asList(allObjects, selectedObjects),
                        "Choose objects to export.")
                .addChoiceParameter("collectorChoice", "Choose the type of collector", "None",
                        Arrays.asList("4 tube cap holder","8-strip holder", "96-well collector"));

        boolean confirmed = Dialogs.showConfirmDialog("Export to LMD", new ParameterPanelFX(parametersList).getPane());

        if (confirmed) {
            boolean confirmedSecondWindow = true;
            if (!parametersList.getChoiceParameterValue("collectorChoice").equals("None")){
                confirmedSecondWindow = Dialogs.showConfirmDialog("","");
            }

            if (!confirmedSecondWindow){
                return false;
            }

            Collection<PathObject> chosenObjects;
            var comboChoice = parametersList.getChoiceParameterValue("exportOptions");
            if (comboChoice.equals("Selected objects")) {
                if (hierarchy.getSelectionModel().noSelection()) {
                    Dialogs.showErrorMessage("No selection", "No selection detected!");
                }
                chosenObjects = new ArrayList<>(hierarchy.getSelectionModel().getSelectedObjects());
                // Automatically include MultiPoint even if not selected, other annotations will be ignored
                chosenObjects.addAll(hierarchy.getAnnotationObjects());
            } else
                chosenObjects = hierarchy.getAllObjects(false);

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
                Dialogs.showInfoNotification("Export successful", "Check 'LMD data' in your project's directory for the output file.");
            } else {
                Dialogs.showErrorMessage("Warning", "Couldn't access your project's directory. " +
                        "Check your home folder for the output files.");
            }
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
    }
