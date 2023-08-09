package org.dgsob;

import org.controlsfx.control.action.Action;
import qupath.lib.common.Version;
import qupath.lib.gui.ActionTools;
import qupath.lib.gui.ActionTools.ActionMenu;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.extensions.QuPathExtension;
import java.io.IOException;

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
        @ActionMenu("Utilities>Expand selected objects (merges overlapping)")
        public final Action actionExpandObjects;
        @ActionMenu("Export")
        public final Action actionExport;

        private ExportToLMDAction(QuPathGUI qupath) {
            this.qupath = qupath;

            actionExpandObjects = qupath.createImageDataAction(imageData -> {
                ExpandObjectsCommand.runObjectsExpansion(imageData);
            });

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
