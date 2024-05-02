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
    public static class LMDSupportCommands{

        public final Action actionLMDSupportCommand;
        private LMDSupportCommands(QuPathGUI qupath) {
            MainCommand lmdsCommand = new MainCommand(qupath);
            actionLMDSupportCommand = new Action("Leica LMD Support for Qupath", event -> lmdsCommand.run());
        }
    }

//    @ActionMenu("Extensions>LMD Support")
//    public static class LMDActions {
//        @ActionMenu("Utilities>Convert Selected Objects>")
//        @ActionConfig(bundle = "strings", value = "Action.convert.toDetections")
//        public final Action convertToDetections;
//        @ActionMenu("Utilities>Convert Selected Objects>")
//        @ActionConfig(bundle = "strings", value = "Action.convert.toAnnotations")
//        public final Action convertToAnnotations;
//        @ActionMenu("Utilities>Create Image Copy>")
//        @ActionConfig(bundle = "strings", value = "Action.mirrorX")
//        public final Action mirrorImageX;
//        @ActionMenu("Utilities>Create Image Copy>")
//        @ActionConfig(bundle = "strings", value = "Action.mirrorY")
//        public final Action mirrorImageY;
//        @ActionMenu("Utilities>")
//        @ActionConfig(bundle = "strings", value = "Action.expand")
//        public final Action expandObjects;
//        @ActionConfig(bundle = "strings", value = "Action.export")
//        public final Action export;
//
////        private LMDActions(QuPathGUI qupath) {
////
////            // Converting
////            // TODO: Add a way to undo converting
////            convertToDetections = qupath.createImageDataAction(imageData -> {
////                ConvertObjectsCommand.convertObjects(imageData, true);
////            });
////
////            convertToAnnotations = qupath.createImageDataAction(imageData -> {
////                ConvertObjectsCommand.convertObjects(imageData, false);
////            });
////
////            // Mirroring
////            mirrorImageX = qupath.createImageDataAction(imageData -> {
////                try {
////                    MirrorImageCommand.mirrorImage(qupath, true, false);
////                } catch (IOException e) {
////                    logger.error("Mirroring failed: " + e.getMessage());
////                }
////            });
////
////            mirrorImageY = qupath.createImageDataAction(imageData -> {
////                try {
////                    MirrorImageCommand.mirrorImage(qupath, false, true);
////                } catch (IOException e) {
////                    logger.error("Mirroring failed: " + e.getMessage());
////                }
////            });
////
////            // Expanding
////            // TODO: Add a way to undo expanding
////            expandObjects = qupath.createImageDataAction(imageData -> {
////                ExpandDetectionsCommand expanding = new ExpandDetectionsCommand(imageData);
////                // TODO: Display progress bar in case there is a lot of detections to process
////                Thread expandingObjectThread = new Thread(expanding);
////                expandingObjectThread.start();
////            });
////
////            // Exporting
////            export = qupath.createImageDataAction(imageData -> {
////                try {
////                    ExportCommand.runExport(qupath.getProject().getPath(), imageData);
////                } catch (IOException e) {
////                    logger.error("Export failed: " + e.getMessage());
////                }
////            });
////
////        }
//    }

}
