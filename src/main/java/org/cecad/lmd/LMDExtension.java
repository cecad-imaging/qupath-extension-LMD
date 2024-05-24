package org.cecad.lmd;

import org.cecad.lmd.commands.MainCommand;
import org.controlsfx.control.action.Action;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.lib.common.Version;
import qupath.lib.gui.actions.ActionTools;
import qupath.lib.gui.actions.annotations.ActionMenu;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.extensions.QuPathExtension;

public class LMDExtension implements QuPathExtension {
    private static final Logger logger = LoggerFactory.getLogger(LMDExtension.class);

    @Override
    public void installExtension(QuPathGUI qupath) {
        qupath.installActions(ActionTools.getAnnotatedActions(new LMDSupportCommands(qupath)));
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

    @ActionMenu("Extensions")
    public static class LMDSupportCommands {

        public final Action actionLMDSupportCommand;
        private LMDSupportCommands(QuPathGUI qupath) {
            actionLMDSupportCommand = new Action("Leica LMD Support for QuPath", event -> {
                MainCommand lmdsCommand = new MainCommand(qupath);
                lmdsCommand.run();
            });
        }
    }
}
