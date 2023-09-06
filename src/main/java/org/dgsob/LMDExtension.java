package org.dgsob;

import org.controlsfx.control.action.Action;
import org.dgsob.exporting.ExportCommand;
import org.dgsob.utilities.ConvertObjectsCommand;
import org.dgsob.utilities.ExpandDetectionsCommand;
import org.dgsob.utilities.MirrorImageCommand;
import qupath.lib.common.Version;
import qupath.lib.gui.ActionTools;
import qupath.lib.gui.ActionTools.ActionMenu;
import qupath.lib.gui.ActionTools.ActionDescription;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.extensions.QuPathExtension;
import java.io.IOException;

public class LMDExtension implements QuPathExtension {
    @Override
    public void installExtension(QuPathGUI qupath) {
        qupath.installActions(ActionTools.getAnnotatedActions(new LMDActions(qupath)));
    }

    @Override
    public String getName() {
        return "LMD Support for QuPath";
    }

    @Override
    public String getDescription() {
        return "Export QuPath ROIs to an XML file readable by Leica's LMD7 software";
    }

    @Override
    public Version getQuPathVersion() {
        return QuPathExtension.super.getQuPathVersion();
    }

    @ActionMenu("Extensions>LMD Support")
    public static class LMDActions {
        @ActionMenu("Utilities>Convert Selected Objects>To Detections")
        @ActionDescription("Converts any object which encloses an area to a detection object.")
        public final Action convertToDetections;
        @ActionMenu("Utilities>Convert Selected Objects>To Annotations")
        @ActionDescription("Converts any object which encloses an area to an annotation object.")
        public final Action convertToAnnotations;
        @ActionMenu("Utilities>Create Image Copy>Mirror Horizontally")
        @ActionDescription("Creates a new image with the objects from the original image, mirrored along horizontal axis.")
        public final Action mirrorImageX;
        @ActionMenu("Utilities>Create Image Copy>Mirror Vertically")
        @ActionDescription("Creates a new image with the objects from the original image, mirrored along vertical axis.")
        public final Action mirrorImageY;
        @ActionMenu("Utilities>Expand Selected Detections")
        @ActionDescription("Makes objects larger by the provided radius. Annotations not supported.")
        public final Action expandObjects;
        @ActionMenu("Export Detections to LMD")
        @ActionDescription("Exports detections to an XML file. Annotations not supported.")
        public final Action export;

        private LMDActions(QuPathGUI qupath) {

            // Converting
            // TODO: Add a way to undo converting
            convertToDetections = qupath.createImageDataAction(imageData -> {
                ConvertObjectsCommand.convertObjects(imageData, true);
            });

            convertToAnnotations = qupath.createImageDataAction(imageData -> {
                ConvertObjectsCommand.convertObjects(imageData, false);
            });

            // Mirroring
            mirrorImageX = qupath.createImageDataAction(imageData -> {
                MirrorImageCommand.mirrorImage(qupath, true, false);
            });

            mirrorImageY = qupath.createImageDataAction(imageData -> {
                MirrorImageCommand.mirrorImage(qupath, false, true);
            });

            // Expanding
            // TODO: Add a way to undo expanding
            expandObjects = qupath.createImageDataAction(imageData -> {
                ExpandDetectionsCommand expanding = new ExpandDetectionsCommand(imageData);
                // TODO: Display progress bar in case there is a lot of detections to process
                Thread expandingObjectThread = new Thread(expanding);
                expandingObjectThread.start();
            });

            // Exporting
            export = qupath.createImageDataAction(imageData -> {
                try {
                    ExportCommand.runExport(qupath.getProject().getPath(), imageData);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

        }
    }

}
