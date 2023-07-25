package org.dgsob;

import org.controlsfx.control.action.Action;
import qupath.lib.common.Version;
import qupath.lib.gui.ActionTools;
import qupath.lib.gui.ActionTools.ActionDescription;
import qupath.lib.gui.ActionTools.ActionMenu;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.extensions.QuPathExtension;
import qupath.lib.gui.dialogs.Dialogs;
import qupath.lib.objects.PathObject;
import qupath.lib.images.ImageData;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

import static org.dgsob.GeoJSON_to_XML.shapeType.CELL;
import static qupath.lib.scripting.QP.exportObjectsToGeoJson;

public class ExportToLMDExtension implements QuPathExtension {
    @Override
    public void installExtension(QuPathGUI qupath) {
        qupath.installActions(ActionTools.getAnnotatedActions(new ExportToLMDAction(qupath)));
    }

    @Override
    public String getName() {
        return "Export to LMD";
    }

    @Override
    public String getDescription() {
        return "Export QuPath ROIs to an XML file readable by Leica's LMD7 software";
    }

    @Override
    public Version getQuPathVersion() {
        return QuPathExtension.super.getQuPathVersion();
    }

    @ActionMenu("Extensions>Export to LMD")
    public static class ExportToLMDAction {
        private final QuPathGUI qupath;
        @ActionMenu("Export all objects")
        @ActionDescription("Export all objects to LMD format.")
        public final Action actionExportAll;
        public void exportAll(String pathGeoJSON, String pathXML) throws IOException {
            ImageData<BufferedImage> imageData = qupath.getViewer().imageDataProperty().get();
            Collection<PathObject> allObjects = imageData.getHierarchy().getAllObjects(false);
            exportObjectsToGeoJson(allObjects, pathGeoJSON, "FEATURE_COLLECTION");

            GeoJSON_to_XML converter = new GeoJSON_to_XML(pathGeoJSON, pathXML, CELL);
            converter.convertGeoJSONtoXML();
            Dialogs.showInfoNotification("Done","Export all to LMD completed.");
        }

        @ActionMenu("Export selected objects")
        public final Action actionExportSelected;
        public void exportSelected(String pathGeoJSON, String pathXML) throws IOException {
            ImageData<BufferedImage> imageData = qupath.getViewer().imageDataProperty().get();
            Collection<PathObject> selectedObjects = imageData.getHierarchy().getSelectionModel().getSelectedObjects();
            exportObjectsToGeoJson(selectedObjects, pathGeoJSON, "FEATURE_COLLECTION");

            GeoJSON_to_XML converter = new GeoJSON_to_XML(pathGeoJSON, pathXML, CELL);
            converter.convertGeoJSONtoXML();

            Dialogs.showInfoNotification("Done","Export selected to LMD completed.");
        }
        private ExportToLMDAction(QuPathGUI qupath) {
            this.qupath = qupath;
            actionExportAll = new Action(event -> {
                try {
                    // These two should probably be placed somewhere else, but it works for now.
                    final String pathGeoJSON = getProjectDirectory(qupath).resolve("test.geojson").toString();
                    final String pathXML = getProjectDirectory(qupath).resolve("test_output.xml").toString();
                    exportAll(pathGeoJSON, pathXML);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            actionExportSelected = new Action(event -> {
                try {
                    final String pathGeoJSON = getProjectDirectory(qupath).resolve("test.geojson").toString();
                    final String pathXML = getProjectDirectory(qupath).resolve("test_output.xml").toString();
                    exportSelected(pathGeoJSON, pathXML);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

        }
        private Path getProjectDirectory(QuPathGUI qupath) {

            // Get the current project.qpproj file path
            Path projectFilePath = qupath.getProject().getPath();

            // Return the path to the project directory
            if (projectFilePath != null) {
                return projectFilePath.getParent();
            }
            // If the project is null, return the current working directory.
            // This should probably naturally never happen but idk.
            Dialogs.showErrorMessage("Warning","Couldn't access your project location. " +
                    "Check your home directory for the output file.");
            return Paths.get(System.getProperty("user.dir"));
        }
    }

}
