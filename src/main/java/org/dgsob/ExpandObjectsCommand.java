package org.dgsob;

import org.locationtech.jts.geom.*;
import org.locationtech.jts.operation.buffer.BufferOp;
import org.locationtech.jts.operation.buffer.BufferParameters;

import qupath.lib.common.GeneralTools;
import qupath.lib.geom.Point2;
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
import qupath.lib.roi.RoiTools;

import java.awt.image.BufferedImage;
import java.util.*;

//import static qupath.lib.analysis.DistanceTools.computeDistance;

public class ExpandObjectsCommand {
    private ExpandObjectsCommand(){

    }

    public static void runObjectsExpansion(ImageData<BufferedImage> imageData){

        ImageServer<BufferedImage> server = imageData.getServer();

        PathObjectHierarchy hierarchy = imageData.getHierarchy();

        Collection<PathObject> pathObjects = getSelected(hierarchy);

        assert pathObjects != null;

        Collection<PathObject> newObjects = new ArrayList<>();

        int objectsNumber = pathObjects.size();
        if(objectsNumber == 1)
            Dialogs.showInfoNotification("LMD Notification", "You have chosen " + objectsNumber + " object to expand.");
        else
            Dialogs.showInfoNotification("LMD Notification", "You have chosen " + objectsNumber + " objects to expand.");

        ParameterList params = new ParameterList()
                .addDoubleParameter("radiusMicrons", "Expansion radius", 3, GeneralTools.micrometerSymbol(),
                        "Distance to expand ROI")
                .addChoiceParameter("priorityClass",
                        "Set object of which class to keep if two diffrent overlap or exlude both",
                        "Positive", Arrays.asList("Exclude both", "Positive", "Negative"));

        boolean confirmed = Dialogs.showConfirmDialog("Expand selected", new ParameterPanelFX(params).getPane());

        if(confirmed) {
            double radiusPixels;
            PixelCalibration cal = server.getPixelCalibration();
            if (cal.hasPixelSizeMicrons())
                radiusPixels = params.getDoubleParameterValue("radiusMicrons") / cal.getAveragedPixelSizeMicrons();
            else
                radiusPixels = params.getDoubleParameterValue("radiusMicrons");

            for (PathObject pathObject : pathObjects) {

                ROI roi = pathObject.getROI();
                Geometry geometry = roi.getGeometry();
                Geometry geometry2 = BufferOp.bufferOp(geometry, radiusPixels, BufferParameters.DEFAULT_QUADRANT_SEGMENTS);
                ROI roi2 = GeometryTools.geometryToROI(geometry2, ImagePlane.getPlane(roi));
                PathObject detection2 = PathObjects.createDetectionObject(roi2, pathObject.getPathClass());
                detection2.setName(pathObject.getName());
                detection2.setColor(pathObject.getColor());
                newObjects.add(detection2);
            }
            hierarchy.removeObjects(pathObjects, false);
            hierarchy.getSelectionModel().clearSelection();

            // 1. -> 2.
            // We should iterate over newObjets here, get each objects ROI + a little extra,
            // add all background objects which intersect an object to newObjects and probably remove them from heirarchy at this point
            // as there is only once scenario - it is a priority class object - when it should remain in the heirarchy
            // so we will add it back later when we actually check for class priority - here we have no idea

            // 2.a. -> Unimplemented logic
            // Now when we have enhanced newObjects, we should check if they ALL are of the same class, if they are -> we should process
            // them just merging overlapping

            // If they are not all the same class we should check what the user wants us to do when different classes overlap
            // If the user wants to exclude both -> we should process objects merging overlapping same class and deleting overlapping different class
            // If the user wants priority class set -> we should sort newObjects so that priority class is first, then process them deleting all objecs
            // intersecting priority class objects and merging the rest





            // 2.b. -> 3.
            // Or we can just check if priorityClass is not exclude both and if !all objects have same class, if both are true -> sort newObjects

            Object priorityClass = params.getChoiceParameterValue("priorityClass");
            if (!areAllObjectsOfSameClass(newObjects) && !priorityClass.equals("Exclude both")){
                newObjects = sortObjectsByPriority(newObjects, priorityClass);
            }

            // 3.
            // Process overlapping objects: merge, exclude both or exclude one of the two overlapping depending on their class
            Collection<PathObject> objectsToRemove = new ArrayList<>();
            Collection<PathObject> objectsToAdd = new ArrayList<>();
            while(!newObjects.isEmpty()) {
                newObjects = processOverlappingObjects(hierarchy, newObjects, objectsToAdd, objectsToRemove, priorityClass);
            }
//            hierarchy.removeObjects(objectsToRemove, false);
            hierarchy.addObjects(objectsToAdd);
        }

    }

    private static Collection<PathObject> processOverlappingObjects(final PathObjectHierarchy hierarchy,
                                                                           Collection<PathObject> newObjects,
                                                                           Collection<PathObject> objectsToAdd,
                                                                           Collection<PathObject> objectsToRemoveFromHierarchy,
                                                                           Object priorityClass){
        Collection<PathObject> remainingObjects = new ArrayList<>(newObjects);
        Collection<PathObject> objectsToMerge = new ArrayList<>();
        Collection<PathObject> objectsToRemoveFromProcessed = new ArrayList<>();
        Collection<PathObject> objectToAddBackToProcessed = new ArrayList<>();
        boolean isOverlapping = false;
        boolean isSameClass = true;

        for (PathObject object : newObjects){
            PathClass objectClass = object.getPathClass();
            Polygon polygon = convertRoiToGeometry(object);
            remainingObjects.remove(object);

            for (PathObject otherObject : remainingObjects){
                PathClass otherObjectClass = otherObject.getPathClass();
                Polygon otherPolygon = convertRoiToGeometry(otherObject);
                if (polygon.intersects(otherPolygon)){
                    isOverlapping = true;
                    if (objectClass == otherObjectClass){
                        objectsToMerge.add(object);
                        objectsToMerge.add(otherObject);
                        break;
                    }
                    else{
                        isSameClass = false;
                        if (priorityClass.equals("Exclude both")){
                            objectsToRemoveFromProcessed.add(object);
                            objectsToRemoveFromProcessed.add(otherObject);
                            break;
                        }
                        else{
                            // assumes max 2 class scenario
                            if (Objects.equals(priorityClass.toString(), objectClass != null ? objectClass.toString() : null)) {
//                                Dialogs.showConfirmDialog("","I am priority class object, I'm removing the other intersecting me rn");
                                objectsToRemoveFromProcessed.add(otherObject);
                                objectToAddBackToProcessed.add(object);
                            } else {
//                                Dialogs.showConfirmDialog("","I am not a priority object, I am getting removed for intersecting one rn." +
//                                        "Btw, this should never occur if the object were actually sorted, which they should've been.");
//                                Dialogs.showConfirmDialog("","Let's check if you don't lie, your class is " + objectClass + " and priority class is " + priorityClass);
                                // we should remove object from the remaining here, but it was done earlier
                            }
                            break;
                        }
                    }
                }
            }
            if (isOverlapping) {
                if (isSameClass) {
                    remainingObjects.removeAll(objectsToMerge); //we already removed object earlier,
                    // but I don't see intuitive other option to remove otherObject as well other than this for now
                    remainingObjects.add(mergeObjects(objectsToMerge, objectClass));
                }
                else{
//                    Dialogs.showConfirmDialog("","Size of the remaining: " + remainingObjects.size());
                    remainingObjects.removeAll(objectsToRemoveFromProcessed);
                    remainingObjects.addAll(objectToAddBackToProcessed);
//                    Dialogs.showConfirmDialog("","Size of the remaining after removal of peasants: " + remainingObjects.size());
                }
            }
            else{
//                Dialogs.showConfirmDialog("","This should fire only once at the end");
                objectsToAdd.add(object);
            }
            break;
        }
        return remainingObjects;
    }

            // ---------------------------------------------------------------------------------------------------------
            // Handle objects that are not selected to be expanded but intersect our object in the result of its expansion.
            // These are already in hierarchy, newObjects are not.
            // It is for sure an improvement from iterating over all objects but the problem is it relies on objects centroids, not intersection checking.
            // We could try to somehow enlarge the ROI and iterate over these objects identically to newObjects above.
//            Collection<PathObject> alreadyInHierarchy = hierarchy.getObjectsForROI(null, object.getROI());
//            if (!alreadyInHierarchy.isEmpty()){
//                for (PathObject otherObject : alreadyInHierarchy){
//                    if (objectClass == null && otherObject.getPathClass() != null ||
//                            objectClass != null && otherObject.getPathClass() == null ||
//                            objectClass != null && !objectClass.equals(otherObject.getPathClass())){
//
//                        areAllTheSameClass = false;
//                    }
//                    // We could either add each object to objectsToMerge here or all at once below, doesn't matter I guess
//                }
//                objectsToMerge.addAll(alreadyInHierarchy);
//                objectsToRemove.addAll(alreadyInHierarchy);
//                isOverlapping = true;
//            }
            // ---------------------------------------------------------------------------------------------------------
    private static Polygon convertRoiToGeometry(PathObject object){
        List<Point2> points = object.getROI().getAllPoints();

        Coordinate[] coords = new Coordinate[points.size() + 1]; // +1 to close the polygon
        for (int i = 0; i < points.size(); i++) {
            Point2 point = points.get(i);
            coords[i] = new Coordinate(point.getX(), point.getY());
        }
        coords[points.size()] = coords[0]; // Close the polygon

        GeometryFactory geomFactory = new GeometryFactory();
        LinearRing linearRing = geomFactory.createLinearRing(coords);
        return geomFactory.createPolygon(linearRing, null);
    }
    private static PathObject mergeObjects(final Collection<PathObject> objects, final PathClass objectClass) {
        ROI shapeNew = null;
        for (PathObject object : objects) {
            if (shapeNew == null)
                shapeNew = object.getROI();
            else if (shapeNew.getImagePlane().equals(object.getROI().getImagePlane()))
                shapeNew = RoiTools.combineROIs(shapeNew, object.getROI(), RoiTools.CombineOp.ADD);
            else {
                Dialogs.showErrorMessage("Error", "It seems as if the processed objects were from different image planes. " +
                        "Please reload the image and try again.");
            }
        }
        assert shapeNew != null;
        if (objectClass != null)
            return PathObjects.createDetectionObject(shapeNew, objectClass);
        else
            return PathObjects.createDetectionObject(shapeNew);
    }

    public static boolean areAllObjectsOfSameClass(Collection<PathObject> objects) {
        PathClass commonClass = null;

        for (PathObject object : objects) {
            if (object != null) {
                PathClass currentClass = object.getPathClass();
                if (currentClass != null) {
                    if (commonClass == null) {
                        commonClass = currentClass;
                    } else if (!commonClass.equals(currentClass)) {
                        return false; // Different classes found
                    }
                }
            }
        }

        return true;
    }

    public static List<PathObject> sortObjectsByPriority(Collection<PathObject> objects, Object priorityClass) {
        List<PathObject> sortedObjects = new ArrayList<>(objects);

        sortedObjects.sort((obj1, obj2) -> {
            PathClass class1 = obj1.getPathClass();
            PathClass class2 = obj2.getPathClass();

            boolean isClass1Priority = Objects.equals(class1 != null ? class1.toString() : null, priorityClass.toString());
            boolean isClass2Priority = Objects.equals(class2 != null ? class2.toString() : null, priorityClass.toString());

            if (isClass1Priority && !isClass2Priority) {
                return -1; // obj1 comes before obj2
            } else if (!isClass1Priority && isClass2Priority) {
                return 1; // obj2 comes before obj1
            }

            return 0;
        });

        return sortedObjects;
    }

    private static Collection<PathObject> getSelected(PathObjectHierarchy hierarchy){
        if (hierarchy.getSelectionModel().noSelection()) {
            Dialogs.showErrorMessage("Error", "No selection. Please, select detections to expand.");
            return null;
        }
        return hierarchy.getSelectionModel().getSelectedObjects();
    }
}
