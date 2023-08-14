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
            // Adding 'background', i.e. already existing in hierarchy, not selected, objects.
            newObjects = addOverlappingBackroundObjects(hierarchy, newObjects);

            // 2.a. -> Unimplemented logic
            // Now when we have enhanced newObjects, we should check if they ALL are of the same class, if they are -> we should process
            // them just merging overlapping

            // If they are not all the same class we should check what the user wants us to do when different classes overlap
            // If the user wants to exclude both -> we should process objects merging overlapping same class and deleting overlapping different class
            // If the user wants priority class set -> we should sort newObjects so that priority class is first, then process them deleting all objecs
            // intersecting priority class objects and merging the rest





            // 2.b. -> 3.
            // Check if priorityClass is not exclude both and if !all objects have same class, if both are true -> sort newObjects

            Object priorityClass = params.getChoiceParameterValue("priorityClass");
            if (!areAllObjectsOfSameClass(newObjects) && !priorityClass.equals("Exclude both")){
                newObjects = sortObjectsByPriority(newObjects, priorityClass);
            }

            // 3.
            // Process overlapping objects: merge, exclude both or exclude one of the two overlapping depending on their class
            Collection<PathObject> objectsToAddToHierarchy = new ArrayList<>();
            while(!newObjects.isEmpty()) {
                newObjects = processOverlappingObjects(newObjects, objectsToAddToHierarchy, priorityClass);
            }
            hierarchy.addObjects(objectsToAddToHierarchy);
        }

    }

    private static Collection<PathObject> addOverlappingBackroundObjects(PathObjectHierarchy hierarchy, final Collection<PathObject> objects){
        Collection<PathObject> enhancedObjects = new ArrayList<>(objects);
        for (PathObject object : objects){
            ROI roi = object.getROI();
            Geometry geometry = roi.getGeometry();
            Geometry geometry2 = BufferOp.bufferOp(geometry, 10, BufferParameters.DEFAULT_QUADRANT_SEGMENTS);
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

    private static Collection<PathObject> processOverlappingObjects(Collection<PathObject> newObjects,
                                                                    Collection<PathObject> objectsToAddToHierarchy, Object priorityClass){
        Collection<PathObject> remainingObjects = new ArrayList<>(newObjects);
        Collection<PathObject> objectsToMerge = new ArrayList<>();
        Collection<PathObject> objectsToRemoveFromProcessed = new ArrayList<>();
        Collection<PathObject> objectsToAddToProcessed = new ArrayList<>();
        boolean isOverlapping = false;
        boolean isSameClass = true;

        for (PathObject object : newObjects){
            PathClass objectClass = object.getPathClass();
            Polygon polygon = convertRoiToGeometry(object);
            remainingObjects.remove(object); // remove from processed now so there will be no intersection with itself check

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
                        else{ // else, i.e. two diffrent classes intersecting and priority class is set by the user.
                            // Assumes max 2 class scenario.
                            if (Objects.equals(priorityClass.toString(), objectClass != null ? objectClass.toString() : null)) {
                                objectsToRemoveFromProcessed.add(otherObject); // other, non-priority object intersects object, so will be deleted
                                // This is the only case when we don't break the loop.
                                // After it is finished, we are sure the object doesn't intersect diffrent class object,
                                // still may intersect same class object though.
                                if (!objectsToAddToProcessed.contains(object)) {
                                    objectsToAddToProcessed.add(object);
                                }
                            }
                            else {
                                // Here the object is non-priority, and it intersects priority otherObject (assuming two classes),
                                // the object has been removed from remaining objects before the loop started, so we don't really do anything here.
                                // I do not really understand why this actually ever happens as the collection should be sorted and all the
                                // priority objects should delete other non-priority intersecting them objects. But it does happen sometimes for some reason.
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
                remainingObjects.add(mergeObjects(objectsToMerge, objectClass));
            } else {
                remainingObjects.removeAll(objectsToRemoveFromProcessed);
                remainingObjects.addAll(objectsToAddToProcessed);
            }
            break;
        }
        return remainingObjects;
    }

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

    public static List<PathObject> sortObjectsByPriority(final Collection<PathObject> objects, Object priorityClass) {
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
