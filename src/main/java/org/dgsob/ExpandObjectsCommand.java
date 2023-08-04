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
import qupath.lib.objects.hierarchy.PathObjectHierarchy;
import qupath.lib.plugins.parameters.ParameterList;
import qupath.lib.roi.interfaces.ROI;
import qupath.lib.regions.ImagePlane;
import qupath.lib.roi.GeometryTools;
import qupath.lib.roi.RoiTools;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
        if(objectsNumber>100)
            Dialogs.showInfoNotification("LMD Notification", "You've chosen " + objectsNumber + " objects.");

        ParameterList params = new ParameterList()
                .addDoubleParameter("radiusMicrons", "Expansion radius", 3, GeneralTools.micrometerSymbol(),
                        "Distance to expand ROI");

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

                // Merge overlapping objects
            Collection<PathObject> objectsToAdd = new ArrayList<>();
            while(!newObjects.isEmpty()) {
                newObjects = detectAndMergeOverlappingObjects(hierarchy, newObjects, hierarchy.getDetectionObjects(), objectsToAdd);
            }
            hierarchy.addObjects(objectsToAdd);
        }

    }
    private static Collection<PathObject> getSelected(PathObjectHierarchy hierarchy){
        if (hierarchy.getSelectionModel().noSelection()) {
            Dialogs.showErrorMessage("Error", "No selection. Please, select detections to expand.");
            return null;
        }
        return hierarchy.getSelectionModel().getSelectedObjects();
    }

    /**
     *
     * @param hierarchy
     * @param newObjects
     * @param alreadyExistingObjects
     * @param objectsToAdd
     * @return
     */
    private static Collection<PathObject> detectAndMergeOverlappingObjects(final PathObjectHierarchy hierarchy,
                                                                           Collection<PathObject> newObjects,
                                                                           Collection<PathObject> alreadyExistingObjects,
                                                                           Collection<PathObject> objectsToAdd){
        Collection<PathObject> remainingObjects = new ArrayList<>(newObjects);
        Collection<PathObject> objectsToMerge = new ArrayList<>();
        boolean isOverlapping = false;
        for (PathObject object : newObjects){
            Polygon polygon = convertRoiToGeometry(object);
            remainingObjects.remove(object);
            for (PathObject otherObject : remainingObjects){
                Polygon otherPolygon = convertRoiToGeometry(otherObject);
                if (polygon.intersects(otherPolygon)){
                    objectsToMerge.add(otherObject);
                    isOverlapping = true;
                }
            }
            remainingObjects.removeAll(objectsToMerge);
            if (isOverlapping){
                objectsToMerge.add(object);
            }
            else{
                objectsToAdd.add(object);
            }
            break;
        }
        if (!objectsToMerge.isEmpty()){
            remainingObjects.add(mergeObjects(hierarchy, objectsToMerge));
        }
        return remainingObjects;
    }

    /**
     *
     * @param object
     * @return
     */
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

    /**
     *
     * @param hierarchy
     * @param objects
     * @return
     */
    private static PathObject mergeObjects(final PathObjectHierarchy hierarchy, final Collection<PathObject> objects) {
        ROI shapeNew = null;
        List<PathObject> merged = new ArrayList<>();
        for (PathObject object : objects) {
            if (object.hasROI() && object.getROI().isArea()) {
                if (shapeNew == null)
                    shapeNew = object.getROI();
                else if (shapeNew.getImagePlane().equals(object.getROI().getImagePlane()))
                    shapeNew = RoiTools.combineROIs(shapeNew, object.getROI(), RoiTools.CombineOp.ADD);
                else {
                    Dialogs.showErrorMessage("", "");
                }
                merged.add(object);
            }
        }
        assert shapeNew != null;
        PathObject pathObjectNew = PathObjects.createDetectionObject(shapeNew);
        hierarchy.removeObjects(merged, false);
        return pathObjectNew;
    }
}
