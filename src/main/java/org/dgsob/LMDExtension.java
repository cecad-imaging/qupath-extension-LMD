package org.dgsob;

import org.controlsfx.control.action.Action;
import qupath.lib.common.Version;
import qupath.lib.gui.ActionTools;
import qupath.lib.gui.ActionTools.ActionMenu;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.extensions.QuPathExtension;
import java.io.IOException;

public class LMDExtension implements QuPathExtension {
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
        @ActionMenu("Expand selected objects")
        public final Action actionExpandObjects;
        @ActionMenu("Create New Mirrored Image>Mirrored Horizontally")
        public final Action actionMirrorImageX;
        @ActionMenu("Create New Mirrored Image>Mirrored Vertically")
        public final Action actionMirrorImageY;
        @ActionMenu("Export")
        public final Action actionExport;

        private ExportToLMDAction(QuPathGUI qupath) {

            actionExpandObjects = qupath.createImageDataAction(imageData -> ExpandObjectsCommand.runObjectsExpansion(imageData));

            actionMirrorImageX = new Action(actionEvent -> MirrorImageCommand.mirrorImage(qupath, true, false));

            actionMirrorImageY = new Action(actionEvent -> MirrorImageCommand.mirrorImage(qupath, false, true));

            actionExport = qupath.createImageDataAction(imageData -> {
                try {
                    ExportCommand.runExport(qupath, imageData);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

        }
    }

}
