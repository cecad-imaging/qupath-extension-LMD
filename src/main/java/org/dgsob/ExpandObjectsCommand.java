package org.dgsob;

import org.locationtech.jts.geom.Geometry;
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
import qupath.lib.objects.hierarchy.PathObjectHierarchy;
import qupath.lib.plugins.parameters.ParameterList;
import qupath.lib.roi.ROIs;
import qupath.lib.roi.interfaces.ROI;
import qupath.lib.regions.ImagePlane;
import qupath.lib.roi.GeometryTools;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Collection;

public class ExpandObjectsCommand {
    private ExpandObjectsCommand(){

    }

    public static void runObjectsExpansion(ImageData<BufferedImage> imageData){

        ImageServer<BufferedImage> server = imageData.getServer();

        Rectangle bounds = new Rectangle(0, 0, server.getWidth(), server.getHeight());

        PathObjectHierarchy hierarchy = imageData.getHierarchy();

        Collection<PathObject> pathObjects = getSelected(hierarchy);

        assert pathObjects != null;

        int objectsNumber = pathObjects.size();
        if(objectsNumber>100)
            Dialogs.showInfoNotification("EtLMD notification", "You've chosen " + objectsNumber + " objects. This may take a while.");

        ParameterList params = new ParameterList()
                .addDoubleParameter("radiusMicrons", "Expansion radius", 3, GeneralTools.micrometerSymbol(), "Distance to expand ROI")
                .addBooleanParameter("removeOriginal", "Delete original object", false, "Create annotation containing only the expanded region, with the original ROI removed")
                .addBooleanParameter("constrainToParent", "Constrain to parent", false, "Constrain ROI to fit inside the ROI of the parent object")
                ;

        boolean confirmed = Dialogs.showConfirmDialog("Expand selected", new ParameterPanelFX(params).getPane());

        if(confirmed) {
            boolean constrainToParent = params.getBooleanParameterValue("constrainToParent");
            boolean removeOriginal = params.getBooleanParameterValue("removeOriginal");

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

                // If the radius is negative (i.e. an erosion), then the parent will be the original object itself
                boolean isErosion = radiusPixels < 0;
                PathObject parent = isErosion ? pathObject : pathObject.getParent();
                if (constrainToParent && !isErosion) {
                    Geometry parentShape;
                    if (parent == null || parent.getROI() == null)
                        parentShape = ROIs.createRectangleROI(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight(), ImagePlane.getPlane(roi)).getGeometry();
                    else
                        parentShape = parent.getROI().getGeometry();
                    geometry2 = geometry2.intersection(parentShape);
                }

                ROI roi2 = GeometryTools.geometryToROI(geometry2, ImagePlane.getPlane(roi));

                // Create a new object, with properties based on the original
                // TODO: check in geojson properties of DetectionObject, maybe change createDetectionObject to CellObject with null nuclei or something else
                PathObject detection2 = PathObjects.createDetectionObject(roi2, pathObject.getPathClass());
                detection2.setName(pathObject.getName());
                detection2.setColor(pathObject.getColor());

                if (constrainToParent || isErosion)
                    hierarchy.addObjectBelowParent(parent, detection2, true);
                else
                    hierarchy.addObject(detection2);
            }
            if (removeOriginal) {
                hierarchy.removeObjects(pathObjects, false);
                hierarchy.getSelectionModel().clearSelection();
            } else {
                hierarchy.getSelectionModel().clearSelection();
            }
        }

    }
    private static Collection<PathObject> getSelected(PathObjectHierarchy hierarchy){
        if (hierarchy.getSelectionModel().noSelection()) {
            Dialogs.showErrorMessage("Error", "No selection. Please, select detections to expand.");
            return null;
        }
        return hierarchy.getSelectionModel().getSelectedObjects();
    }
}
