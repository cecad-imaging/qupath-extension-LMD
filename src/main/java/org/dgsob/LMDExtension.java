package org.dgsob;

import org.controlsfx.control.action.Action;
import org.dgsob.exporting.ExportCommand;
import org.dgsob.utilities.ConvertObjectsCommand;
import org.dgsob.utilities.ExpandObjectsCommand;
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
        return "LMD Export Extension for QuPath";
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
        @ActionMenu("Utilities>Create Mirrored Image>Horizontal")
        public final Action mirrorImageX;
        @ActionMenu("Utilities>Create Mirrored Image>Vertical")
        public final Action mirrorImageY;
        @ActionMenu("Utilities>Expand Selected Detections")
        @ActionDescription("Makes objects larger by the provided radius. Annotations not supported.")
        public final Action expandObjects;
        @ActionMenu("Export Detections to LMD")
        @ActionDescription("Exports detections to an XML file. Annotations not supported.")
        public final Action export;

        private LMDActions(QuPathGUI qupath) {

            convertToDetections = qupath.createImageDataAction(imageData -> {
                ConvertObjectsCommand converting = new ConvertObjectsCommand(imageData, true);
                converting.run();
            });

            convertToAnnotations = qupath.createImageDataAction(imageData -> {
                ConvertObjectsCommand converting = new ConvertObjectsCommand(imageData, false);
                converting.run();
            });

            mirrorImageX = qupath.createImageDataAction(imageData -> {
                MirrorImageCommand mirroring = new MirrorImageCommand(qupath, true, false);
                mirroring.run();
            });

            mirrorImageY = qupath.createImageDataAction(imageData -> {
                MirrorImageCommand mirroring = new MirrorImageCommand(qupath, false, true);
                mirroring.run();
            });

            expandObjects = qupath.createImageDataAction(imageData -> {
                ExpandObjectsCommand expanding = new ExpandObjectsCommand(imageData);
                expanding.run();
            });

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
