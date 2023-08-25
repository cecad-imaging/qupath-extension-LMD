package org.dgsob;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.Collection;

import qupath.lib.gui.QuPathGUI;
import qupath.lib.images.ImageData;
import qupath.lib.images.servers.ImageServer;
import qupath.lib.images.servers.TransformedServerBuilder;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.PathObjects;
import qupath.lib.objects.classes.PathClass;
import qupath.lib.objects.hierarchy.PathObjectHierarchy;
import qupath.lib.roi.interfaces.ROI;

public class MirrorImageCommand {
    private MirrorImageCommand(){

    }
    public static void mirrorImage(QuPathGUI qupath, boolean mirrorHorizontally, boolean mirrorVertically){

        // Collect all data from original image
        ImageData<BufferedImage> imageData = qupath.getImageData();
        ImageServer<BufferedImage> server = imageData.getServer();
        PathObjectHierarchy hierarchy = imageData.getHierarchy();
        ImageData.ImageType imageType = imageData.getImageType();
        Collection<PathObject> allObjects = hierarchy.getAllObjects(false);
        int imageWidth = server.getWidth();
        int imageHeight = server.getHeight();

        // Initialize factors to no tranformation
        int scaleX = 1;
        int scaleY = 1;
        int translateX = 0;
        int translateY = 0;

        // Set factors according to requested transformation
        if (mirrorHorizontally){
            scaleX = -1;
            translateX = -imageWidth;
        }
        if (mirrorVertically){
            scaleY = -1;
            translateY = -imageHeight;
        }

        // Perfor transformation of the original image
        AffineTransform transform = new AffineTransform();
        transform.scale(scaleX, scaleY);
        transform.translate(translateX, translateY);

        // Build a new server
        TransformedServerBuilder builder = new TransformedServerBuilder(server);
        builder.transform(transform);
        ImageServer<BufferedImage> newServer = builder.build();

        // Use the newly built server to create new imageData
        ImageData<BufferedImage> newImageData = new ImageData<>(newServer);
        newImageData.setImageType(imageType);

        // Get its hierarchy to append transformed objects
        PathObjectHierarchy newHierarchy = newImageData.getHierarchy();

        // Perform transformation on all objects from the original image and add each transformed object to the new hierarchy
        for (PathObject object : allObjects){
            ROI roi = object.getROI();
            roi = roi.scale(scaleX, scaleY);
            roi = roi.translate(-translateX, -translateY);
            PathClass objectClass = object.getPathClass();
            String objectName = object.getName();
            // TODO: Check if object has a parent and deal with it somehow
            if (object.isDetection()) {
                PathObject newObject;

                if (objectClass != null)
                    newObject = PathObjects.createDetectionObject(roi, objectClass);
                else
                    newObject = PathObjects.createDetectionObject(roi);

                if (objectName != null)
                    newObject.setName(objectName);

                newHierarchy.addObject(newObject);

            }
            else if (object.isAnnotation()) {
                PathObject newObject;

                if (objectClass != null)
                    newObject = PathObjects.createAnnotationObject(roi, objectClass);
                else
                    newObject = PathObjects.createAnnotationObject(roi);

                if (objectName != null)
                    newObject.setName(objectName);

                newHierarchy.addObject(newObject);

            }
        }

        // TODO: If the two TODOs above work out, maybe change the behaviour from adding new image to changing the existing one?
        qupath.getViewer().setImageData(newImageData);
        qupath.refreshProject();


    }

}
