package org.dgsob;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import org.controlsfx.control.action.Action;
import qupath.lib.common.Version;
import qupath.lib.gui.ActionTools;
import qupath.lib.gui.ActionTools.ActionDescription;
import qupath.lib.gui.ActionTools.ActionMenu;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.extensions.QuPathExtension;
import qupath.lib.gui.dialogs.Dialogs;
import qupath.lib.gui.viewer.QuPathViewer;
import qupath.lib.objects.PathObject;
import qupath.lib.images.ImageData;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collection;

import static java.awt.SystemColor.infoText;
import static org.dgsob.GeoJSON_to_XML.shapeType.CELL;
import static qupath.lib.scripting.QP.exportObjectsToGeoJson;

public class ExportToLMDExtension implements QuPathExtension {
    private final String pathGeoJSON = "./test.geojson";
    private final String pathXML = "./test_output.xml";
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
    public class ExportToLMDAction {
        private final QuPathGUI qupath;

        @ActionMenu("Export all objects")
        @ActionDescription("Export all objects to LMD format.")
        public final Action actionExportAll;
        public void exportAll() throws IOException {
            // 1. Convert all to GeoJSON and save it to temp location.
            // 2. Run GeoJSON_to_XML.
            ImageData<BufferedImage> imageData = qupath.getViewer().imageDataProperty().get();
            Collection<PathObject> allObjects = imageData.getHierarchy().getAllObjects(false);
            exportObjectsToGeoJson(allObjects, pathGeoJSON, "FEATURE_COLLECTION");

            GeoJSON_to_XML converter = new GeoJSON_to_XML(pathGeoJSON, pathXML, CELL);
            converter.convertGeoJSONtoXML();
            Dialogs.showConfirmDialog("Done","Export all to LMD completed.");
        }

        @ActionMenu("Export selected objects")
        public final Action actionExportSelected;
        public void exportSelected() throws IOException {
            // 1. Convert selections to GeoJSON and save it to temp location with exportObjectsToGeoJson(selections, path, "FEATURE_COLLECTION")
            // 2. Run GeoJSON_to_XML(inputPath, outputPath, CELL); //CELL is not important right now
            ImageData<BufferedImage> imageData = qupath.getViewer().imageDataProperty().get();
            Collection<PathObject> selectedObjects = imageData.getHierarchy().getSelectionModel().getSelectedObjects();
            exportObjectsToGeoJson(selectedObjects, pathGeoJSON, "FEATURE_COLLECTION");

            GeoJSON_to_XML converter = new GeoJSON_to_XML(pathGeoJSON, pathXML, CELL);
            converter.convertGeoJSONtoXML();

            Dialogs.showConfirmDialog("Done","Export selected to LMD completed.");
        }
        private ExportToLMDAction(QuPathGUI qupath) {
            actionExportAll = new Action(event -> {
                try {
                    exportAll();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            actionExportSelected = new Action(event -> {
                try {
                    exportSelected();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            this.qupath = qupath;

        }
    }

}
