package org.cecad.lmd.utilities;

import qupath.lib.gui.dialogs.Dialogs;
import qupath.lib.images.ImageData;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.PathObjects;
import qupath.lib.objects.classes.PathClass;
import qupath.lib.objects.hierarchy.PathObjectHierarchy;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;

public class ConvertObjectsCommand {

    /**
     * Operates on hierarchy. Deletes selected objects and inserts objects of chosen type with the same ROI,
     * preserving old objects' PathClass and name.
     *
     * @param imageData
     * @param toDetections
     */
    public static void convertObjects(ImageData<BufferedImage> imageData, boolean toDetections){
        PathObjectHierarchy hierarchy = imageData.getHierarchy();
        if (hierarchy.getSelectionModel().noSelection()){
            Dialogs.showErrorMessage("Selection Required", "Please select objects to convert.");
            return;
        }
        Collection<PathObject> objects = hierarchy.getSelectionModel().getSelectedObjects();
        Collection<PathObject> onlyAreasObjects = new ArrayList<>(objects);
        Collection<PathObject> convertedObjects = new ArrayList<>();
        for (PathObject object : objects) {
            if (!object.getROI().isArea()) {
                onlyAreasObjects.remove(object);
                continue;
            }
            PathClass pathClass = object.getPathClass();
            String objectName = object.getName();
            PathObject convertedObject;

            if (toDetections) {
                if (pathClass != null)
                    convertedObject = PathObjects.createDetectionObject(object.getROI(), pathClass);
                else
                    convertedObject = PathObjects.createDetectionObject(object.getROI());
            }
            else {
                if (pathClass != null)
                    convertedObject = PathObjects.createAnnotationObject(object.getROI(), pathClass);
                else
                    convertedObject = PathObjects.createAnnotationObject(object.getROI());
            }
            if (objectName != null)
                convertedObject.setName(objectName);
            convertedObjects.add(convertedObject);
        }
        hierarchy.getSelectionModel().clearSelection();
        hierarchy.removeObjects(onlyAreasObjects, true);
        hierarchy.addObjects(convertedObjects);
    }
}
