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

    @ActionMenu("Extensions>Export to LMD")
    public static class LMDActions {
        @ActionMenu("Utilities>Convert Selected Objects>To Detections")
        public final Action convertToDetections;
        @ActionMenu("Utilities>Convert Selected Objects>To Annotations")
        public final Action convertToAnnotations;
        @ActionMenu("Utilities>Create Mirrored Image>Horizontal")
        public final Action mirrorImageX;
        @ActionMenu("Utilities>Create Mirrored Image>Vertical")
        public final Action mirrorImageY;
        @ActionMenu("Utilities>Expand Selected Detections")
        @ActionDescription("Makes objects larger by the provided radius. Annotations not supported.")
        public final Action expandObjects;
        @ActionMenu("Export")
        @ActionDescription("Exports objects to an XML file. Annotations not supported.")
        public final Action export;

        private LMDActions(QuPathGUI qupath) {

            convertToDetections = qupath.createImageDataAction(imageData -> ConvertObjectsCommand.convertObjects(imageData, true));

            convertToAnnotations = qupath.createImageDataAction(imageData -> ConvertObjectsCommand.convertObjects(imageData, false));

            mirrorImageX = qupath.createImageDataAction(imageData -> MirrorImageCommand.mirrorImage(qupath, true, false));

            mirrorImageY = qupath.createImageDataAction(imageData -> MirrorImageCommand.mirrorImage(qupath, false, true));

            expandObjects = qupath.createImageDataAction(ExpandObjectsCommand::runObjectsExpansion);

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
