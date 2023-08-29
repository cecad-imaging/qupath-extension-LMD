package org.dgsob;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import qupath.lib.geom.Point2;
import qupath.lib.gui.dialogs.Dialogs;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.PathObjects;
import qupath.lib.objects.classes.PathClass;
import qupath.lib.objects.hierarchy.PathObjectHierarchy;
import qupath.lib.roi.RoiTools;
import qupath.lib.roi.interfaces.ROI;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ObjectUtils {
    static PathObject mergeObjects(final Collection<PathObject> objects, final PathClass objectClass) {
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

    static Polygon convertRoiToGeometry(PathObject object){
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

    static List<PathObject> sortObjectsByPriority(final Collection<PathObject> objects, List<String> priorityRanking) {
        List<PathObject> sortedObjects = new ArrayList<>(objects);

        sortedObjects.sort((obj1, obj2) -> {
            PathClass class1 = obj1.getPathClass();
            PathClass class2 = obj2.getPathClass();

            String class1Name = class1 != null ? class1.getName() : null;
            String class2Name = class2 != null ? class2.getName() : null;

            int index1 = priorityRanking.indexOf(class1Name);
            int index2 = priorityRanking.indexOf(class2Name);

            if (index1 != -1 && index2 == -1) {
                return -1; // obj1 comes before obj2
            } else if (index1 == -1 && index2 != -1) {
                return 1; // obj2 comes before obj1
            }

            return Integer.compare(index1, index2);
        });

        return sortedObjects;
    }

    /**
     * Mirrors an object.
     *
     * @param object Object to mirror.
     * @param scaleX Horizontal scale value.
     * @param scaleY Vertical scale value.
     * @param translateX Horizontal translation.
     * @param translateY Vertical translation.
     * @return The mirrored object.
     */
    static PathObject mirrorObject(PathObject object, int scaleX, int scaleY, int translateX, int translateY){
        ROI roi = object.getROI();
        roi = roi.scale(scaleX, scaleY);
        roi = roi.translate(-translateX, -translateY);
        PathClass objectClass = object.getPathClass();
        String objectName = object.getName();
        PathObject newObject = null;

        if (object.isDetection()) {
            if (objectClass != null)
                newObject = PathObjects.createDetectionObject(roi, objectClass);
            else
                newObject = PathObjects.createDetectionObject(roi);

            if (objectName != null)
                newObject.setName(objectName);
        }
        else if (object.isAnnotation()) {
            if (objectClass != null)
                newObject = PathObjects.createAnnotationObject(roi, objectClass);
            else
                newObject = PathObjects.createAnnotationObject(roi);

            if (objectName != null)
                newObject.setName(objectName);
        }
        return newObject;
    }

    /**
     * Adds an object to the hierarchy or inserts an object as a child of the provided parent object.
     *
     * @param hierarchy Current hierarchy to add object to.
     * @param object Object to add.
     * @param parent Already existing in hierarchy parent object.
     */
    static void addObjectAccountingForParent(PathObjectHierarchy hierarchy, PathObject object, PathObject parent) {
        if (parent != null)
            parent.addChildObject(object);
        else
            hierarchy.addObject(object);
    }

    /**
     * Iterates over provided PathObject collection and looks for objects which are not annotations. Returns them.
     *
     * @param objects Collection of PathObjects.
     * @return The collection of non-annotation objects.
     */
    static Collection<PathObject> getDetectionObjects(Collection<PathObject> objects){
        Collection<PathObject> detectionObjects = new ArrayList<>();
        for (PathObject object : objects){
            if (!object.isAnnotation()){
                detectionObjects.add(object);
            }
        }
        return detectionObjects;
    }

    static void convertToDetections(PathObjectHierarchy hierarchy, Collection<PathObject> objects){
        Collection<PathObject> detectionObjects = new ArrayList<>();
        for (PathObject object : objects){
            PathClass pathClass = object.getPathClass();
            if (pathClass != null)
                detectionObjects.add(PathObjects.createDetectionObject(object.getROI(), pathClass));
            else
                detectionObjects.add(PathObjects.createDetectionObject(object.getROI()));
        }
        hierarchy.removeObjects(objects, true);
        hierarchy.addObjects(detectionObjects);
    }

    static void convertToAnnotations(PathObjectHierarchy hierarchy, Collection<PathObject> objects){
        Collection<PathObject> annotationObjects = new ArrayList<>();
        for (PathObject object : objects){
            PathClass pathClass = object.getPathClass();
            if (pathClass != null)
                annotationObjects.add(PathObjects.createAnnotationObject(object.getROI(), pathClass));
            else
                annotationObjects.add(PathObjects.createAnnotationObject(object.getROI()));
        }
        hierarchy.removeObjects(objects, true);
        hierarchy.addObjects(annotationObjects);
    }

}
