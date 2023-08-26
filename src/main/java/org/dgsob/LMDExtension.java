package org.dgsob;

import org.controlsfx.control.action.Action;
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
        qupath.installActions(ActionTools.getAnnotatedActions(new ExportToLMDAction(qupath)));
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
    public static class ExportToLMDAction {
        @ActionMenu("Utilities>Create Image Copy>Mirror Horizontally")
        public final Action actionMirrorImageX;
        @ActionMenu("Utilities>Create Image Copy>Mirror Vertically")
        public final Action actionMirrorImageY;
        @ActionMenu("Utilities>Create Image Copy>Do Not Mirror")
        public final Action actionMirrorImageNone;
        @ActionMenu("Utilities>Expand selected objects")
        @ActionDescription("Makes objects larger by the provided radius. Annotations not supported.")
        public final Action actionExpandObjects;
        @ActionMenu("Export")
        public final Action actionExport;

        private ExportToLMDAction(QuPathGUI qupath) {

            actionMirrorImageX = new Action(actionEvent -> MirrorImageCommand.mirrorImage(qupath, true, false));

            actionMirrorImageY = new Action(actionEvent -> MirrorImageCommand.mirrorImage(qupath, false, true));

            actionMirrorImageNone = new Action(actionEvent -> MirrorImageCommand.mirrorImage(qupath, false, false));

            actionExpandObjects = qupath.createImageDataAction(ExpandObjectsCommand::runObjectsExpansion);

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
