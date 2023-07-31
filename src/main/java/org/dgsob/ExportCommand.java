package org.dgsob;

import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.dialogs.Dialogs;
import qupath.lib.gui.dialogs.ParameterPanelFX;
import qupath.lib.images.ImageData;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.hierarchy.PathObjectHierarchy;
import qupath.lib.plugins.parameters.ParameterList;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;

import static org.dgsob.GeoJSON_to_XML.shapeType.CELL;
import static qupath.lib.scripting.QP.exportObjectsToGeoJson;

public class ExportCommand {
    private ExportCommand(){

    }

    public static void runExport(QuPathGUI qupath, ImageData<BufferedImage> imageData) throws IOException {
        PathObjectHierarchy hierarchy = imageData.getHierarchy();

        // Provide exactly the same options of export as native exporting to geojson
        String allObjects = "All objects";
        String selectedObjects = "Selected objects";
        String defaultObjects = hierarchy.getSelectionModel().noSelection() ? allObjects : selectedObjects;

        var parametersList = new ParameterList()
                .addChoiceParameter("exportOptions", "Export", defaultObjects, Arrays.asList(allObjects, selectedObjects),
                        "Choose objects to export.");

        try{
            Dialogs.showConfirmDialog("Export to LMD", new ParameterPanelFX(parametersList).getPane());
        } catch (Exception e){
            Dialogs.showErrorMessage("Error", e);
        }

        // The user chooses objects
        Collection<PathObject> chosenObjects;
        var comboChoice = parametersList.getChoiceParameterValue("exportOptions");
        if (comboChoice.equals("Selected objects")) {
            if (hierarchy.getSelectionModel().noSelection()) {
                Dialogs.showErrorMessage("No selection", "No selection detected!");
            }
            chosenObjects = hierarchy.getSelectionModel().getSelectedObjects();
        } else
            chosenObjects = hierarchy.getAllObjects(false);

        // Set default names for geojson and xml files
        String default_GeoJSON_Name = "temp.geojson";
        String default_XML_Name = imageData.getServer().getMetadata().getName().replaceFirst("\\.[^.]+$", ".xml");

        // Get the current project.qpproj file path
        Path projectFilePath = qupath.getProject().getPath();

        // Set files' default paths
        final String pathGeoJSON = getProjectDirectory(qupath, projectFilePath, ".temp").resolve(default_GeoJSON_Name).toString();
        final String pathXML = getProjectDirectory(qupath, projectFilePath, "LMD data").resolve(default_XML_Name).toString();

        exportObjectsToGeoJson(chosenObjects, pathGeoJSON, "FEATURE_COLLECTION");

        // TODO: This might as well be static.
        GeoJSON_to_XML converter = new GeoJSON_to_XML(pathGeoJSON, pathXML, CELL);
        converter.convertGeoJSONtoXML();

        deleteTemporaryGeoJSON(pathGeoJSON);

        if(projectFilePath != null) {
            Dialogs.showInfoNotification("Export successful", "Check for 'LMD data' in your project's directory");
        }
        else {
            Dialogs.showErrorMessage("Warning", "Couldn't access your project's directory. " +
                    "Check your home folder for the output files.");
        }
    }
    private static Path getProjectDirectory(QuPathGUI qupath, Path projectFilePath, String subdirectory) {
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
    private static void deleteTemporaryGeoJSON(String pathGeoJSON) {
        try {
            Path geoJSONPath = Path.of(pathGeoJSON);
            Files.deleteIfExists(geoJSONPath);
        } catch (IOException e) {
            // Well, I guess it doesn't matter if it fails or not.
        }
    }
}
