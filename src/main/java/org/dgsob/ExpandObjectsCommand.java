package org.dgsob;

import org.locationtech.jts.geom.*;
import org.locationtech.jts.operation.buffer.BufferOp;
import org.locationtech.jts.operation.buffer.BufferParameters;

import qupath.lib.common.GeneralTools;
import qupath.lib.gui.dialogs.Dialogs;
import qupath.lib.gui.dialogs.ParameterPanelFX;
import qupath.lib.images.ImageData;
import qupath.lib.images.servers.ImageServer;
import qupath.lib.images.servers.PixelCalibration;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.PathObjects;
import qupath.lib.objects.classes.PathClass;
import qupath.lib.objects.hierarchy.PathObjectHierarchy;
import qupath.lib.plugins.parameters.ParameterList;
import qupath.lib.roi.interfaces.ROI;
import qupath.lib.regions.ImagePlane;
import qupath.lib.roi.GeometryTools;

import java.awt.image.BufferedImage;
import java.util.*;

public class ExpandObjectsCommand {
    private ExpandObjectsCommand(){

    }
    @SuppressWarnings("UnusedReturnValue")
    public static boolean runObjectsExpansion(ImageData<BufferedImage> imageData){

        ImageServer<BufferedImage> server = imageData.getServer();

        PathObjectHierarchy hierarchy = imageData.getHierarchy();

        if (hierarchy.getSelectionModel().noSelection()) {
            Dialogs.showErrorMessage("Selection Required", "Please select detection objects to expand.");
            return false;
        }

        Collection<PathObject> pathObjects = hierarchy.getSelectionModel().getSelectedObjects();
        pathObjects = ObjectUtils.getDetectionObjects(pathObjects);

        Collection<PathObject> newObjects = new ArrayList<>();

        int objectsNumber = pathObjects.size();
        if(objectsNumber == 1)
            Dialogs.showInfoNotification("LMD Notification", "You have chosen " + objectsNumber + " object to expand.");
        else
            Dialogs.showInfoNotification("LMD Notification", "You have chosen " + objectsNumber + " objects to expand.");

        ParameterList params = new ParameterList()
                .addDoubleParameter("radiusMicrons", "Expansion radius:", 3, GeneralTools.micrometerSymbol(),
                        "Distance to expand ROI")
                .addChoiceParameter("differentClassesChoice",
                        "If objects of two different classes intersect:",
                        "Exclude Both", Arrays.asList("Exclude Both", "Set priority for each class"),
                        """
                                'Exclude Both' option will remove objects which intersect each other regardless of their class.
                                'Set priority for each class' option keeps an object with the higher-priority class. You will be prompted to set priorities after confirming your choice.""");

        boolean confirmed = Dialogs.showConfirmDialog("Expand selected", new ParameterPanelFX(params).getPane());

        if(!confirmed) {
            return false;
        }

        Object differentClassesChoice = params.getChoiceParameterValue("differentClassesChoice");
        List<String> priorityRanking = new ArrayList<>();

        // Creating second window if the user wants to set priorities for classes
        if (differentClassesChoice.equals("Set priority for each class")){
            Set<PathClass> availableClasses = ClassUtils.getAllClasses(pathObjects);
            ParameterList priorityRankingParams = createPriorityRankingParameterList(availableClasses);
            boolean confirmedPriorityRanking = Dialogs.showConfirmDialog("Set priorities for classes", new ParameterPanelFX(priorityRankingParams).getPane());

            if (!confirmedPriorityRanking){
                return false;
            }

            for (int i = 1; i <= availableClasses.size(); i++){
                String priorityClass = priorityRankingParams.getChoiceParameterValue("priorityClass_" + i).toString();
                if (priorityClass != null) {
                    priorityRanking.add(priorityClass);
                }
            }
        }

        // Enlarging the objects by creating new ones and deleting old ones
        double radiusPixels;
        PixelCalibration calibration = server.getPixelCalibration();
        if (calibration.hasPixelSizeMicrons())
            radiusPixels = params.getDoubleParameterValue("radiusMicrons") / calibration.getAveragedPixelSizeMicrons();
        else
            radiusPixels = params.getDoubleParameterValue("radiusMicrons");
        // Iterate over old objects and create a new one for each
        for (PathObject pathObject : pathObjects) {

            ROI roi = pathObject.getROI();
            Geometry geometry = roi.getGeometry();
            Geometry geometry2 = BufferOp.bufferOp(geometry, radiusPixels, BufferParameters.DEFAULT_QUADRANT_SEGMENTS);
            ROI roi2 = GeometryTools.geometryToROI(geometry2, ImagePlane.getPlane(roi));
            PathObject detection2 = PathObjects.createDetectionObject(roi2, pathObject.getPathClass());
            detection2.setName(pathObject.getName());
            detection2.setColor(pathObject.getColor());
            newObjects.add(detection2); // append newly created objects for processing instead of adding them to hierarchy
        }
        hierarchy.removeObjects(pathObjects, false); // remove old objects
        hierarchy.getSelectionModel().clearSelection(); // the selection is no longer necessary

        //Steps for processing overlapping objects:

        // 1. Add 'background', i.e. already existing in hierarchy, not selected, objects to newObjects.
        newObjects = addOverlappingBackroundObjects(hierarchy, newObjects, radiusPixels);

        // 2. Check if differentClassesChoice is not 'Exclude Both' and if !all objects have same class,
        // if both are true -> sort newObjects
        if (!ClassUtils.areAllObjectsOfSameClass(newObjects) && !differentClassesChoice.equals("Exclude Both")){
            newObjects = ObjectUtils.sortObjectsByPriority(newObjects, priorityRanking);
        }

        // 3. Process overlapping objects: merge, exclude both or exclude one of the two overlapping depending on their class
        Collection<PathObject> objectsToAddToHierarchy = new ArrayList<>();
        while(!newObjects.isEmpty()) {
            newObjects = processOverlappingObjects(newObjects, objectsToAddToHierarchy, priorityRanking);
        }
        hierarchy.addObjects(objectsToAddToHierarchy);
        return true;
    }
    private static Collection<PathObject> processOverlappingObjects(Collection<PathObject> newObjects,
                                                                    Collection<PathObject> objectsToAddToHierarchy,
                                                                    List<String> priorityRanking){
        Collection<PathObject> remainingObjects = new ArrayList<>(newObjects);
        Collection<PathObject> objectsToMerge = new ArrayList<>();
        Collection<PathObject> objectsToRemoveFromProcessed = new ArrayList<>();
        Collection<PathObject> objectsToAddToProcessed = new ArrayList<>();
        boolean isOverlapping = false;
        boolean isSameClass = true;

        for (PathObject object : newObjects){
            PathClass objectClass = object.getPathClass();
            Polygon polygon = ObjectUtils.convertRoiToGeometry(object);
            remainingObjects.remove(object); // remove from processed now so there will be no intersection with itself check

            for (PathObject otherObject : remainingObjects){
                PathClass otherObjectClass = otherObject.getPathClass();
                Polygon otherPolygon = ObjectUtils.convertRoiToGeometry(otherObject);
                if (polygon.intersects(otherPolygon)){
                    isOverlapping = true;
                    if (objectClass == otherObjectClass){
                        objectsToMerge.add(object);
                        objectsToMerge.add(otherObject);
                        break;
                    }
                    else{
                        isSameClass = false;
                        if (priorityRanking.isEmpty()){
                            objectsToRemoveFromProcessed.add(object);
                            objectsToRemoveFromProcessed.add(otherObject);
                            break;
                        }
                        else{
                            int objectIndex = priorityRanking.indexOf(objectClass != null ? objectClass.getName() : null);
                            int otherObjectIndex = priorityRanking.indexOf(otherObjectClass != null ? otherObjectClass.getName() : null);
                            // Since when not in the list -1 is returned, we make sure it doesn't become the highest priority
                            objectIndex = objectIndex != -1 ? objectIndex : Integer.MAX_VALUE;
                            otherObjectIndex = otherObjectIndex != -1 ? otherObjectIndex : Integer.MAX_VALUE;


                            if (objectIndex < otherObjectIndex) { // lower index -> higher priority
                                objectsToRemoveFromProcessed.add(otherObject); // deleting other, non-priority, intersecting object
                                /*
                                This is the only case when we don't break the loop, after it is finished,
                                we are sure the object doesn't intersect diffrent class object,
                                still may intersect same class object though, so we add it back to processed.
                                */
                                if (!objectsToAddToProcessed.contains(object)) {
                                    objectsToAddToProcessed.add(object);
                                }
                            }
                            else if (otherObjectIndex < objectIndex){
                                /*
                                Here the object is non-priority, and it intersects priority otherObject (assuming two classes),
                                the object has been removed from remaining objects before the loop started, so we don't really do anything here.
                                I do not really understand why this actually ever happens as the collection should be sorted and all the
                                priority objects should delete other non-priority intersecting them objects. But it does happen sometimes for some reason.
                                */
                                break;
                            }
                        }
                    }
                }
            }
            if (!isOverlapping) {
                objectsToAddToHierarchy.add(object);
                break;
            }

            if (isSameClass) {
                remainingObjects.removeAll(objectsToMerge);
                remainingObjects.add(ObjectUtils.mergeObjects(objectsToMerge, objectClass));
            } else {
                remainingObjects.removeAll(objectsToRemoveFromProcessed);
                remainingObjects.addAll(objectsToAddToProcessed);
            }
            break;
        }
        return remainingObjects;
    }
    private static Collection<PathObject> addOverlappingBackroundObjects(PathObjectHierarchy hierarchy, final Collection<PathObject> objects, double radius){
        Collection<PathObject> enhancedObjects = new ArrayList<>(objects);
        for (PathObject object : objects){
            ROI roi = object.getROI();
            Geometry geometry = roi.getGeometry();
            Geometry geometry2 = BufferOp.bufferOp(geometry, radius*10, BufferParameters.DEFAULT_QUADRANT_SEGMENTS);
            ROI roi2 = GeometryTools.geometryToROI(geometry2, ImagePlane.getPlane(roi));

            Collection<PathObject> objectsInROI = hierarchy.getObjectsForROI(null, roi2);
            hierarchy.removeObjects(objectsInROI, false);
            // this loop might be unnecessary and replaced with objects.addAll(objectsInROI) but idk, should be tested
            for (PathObject roiObject : objectsInROI){
                if (!enhancedObjects.contains(roiObject)){
                    enhancedObjects.add(roiObject);
                }
            }
            //
        }
        return enhancedObjects;
    }
    private static ParameterList createPriorityRankingParameterList(Set<PathClass> availableClasses){
        List<String> classNames = new ArrayList<>(availableClasses.stream()
                .map(PathClass::getName)
                .toList());
        ParameterList priorityRankingParams = new ParameterList();
        priorityRankingParams.addEmptyParameter("""
                Class 1 corresponds to the highest priority.
                Lower prority object will be deleted if intersecting object of a higher priority.
                
                """);
        for (int i = 1; i <= classNames.size(); i++){
            priorityRankingParams.addChoiceParameter("priorityClass_" + i, "Class " + i, classNames.get(i-1), classNames);
        }
        return priorityRankingParams;
    }
}
