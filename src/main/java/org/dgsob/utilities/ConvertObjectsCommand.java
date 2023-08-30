package org.dgsob.utilities;

import org.dgsob.common.ObjectUtils;
import qupath.lib.gui.dialogs.Dialogs;
import qupath.lib.images.ImageData;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.hierarchy.PathObjectHierarchy;

import java.awt.image.BufferedImage;
import java.util.Collection;

public class ConvertObjectsCommand {
    public static boolean convertObjects(ImageData<BufferedImage> imageData, boolean toDetections){
        PathObjectHierarchy hierarchy = imageData.getHierarchy();
        if (hierarchy.getSelectionModel().noSelection()){
            Dialogs.showErrorMessage("Selection Required", "Please select objects to convert.");
            return false;
        }
        Collection<PathObject> objects = hierarchy.getSelectionModel().getSelectedObjects();
        if (toDetections){
            ObjectUtils.convertToDetections(hierarchy, objects);
        }
        else {
            ObjectUtils.convertToAnnotations(hierarchy, objects);
        }
        return true;
    }
}
