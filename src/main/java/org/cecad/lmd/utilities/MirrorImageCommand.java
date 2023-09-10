package org.cecad.lmd.utilities;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.Collection;

import org.cecad.lmd.common.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.images.ImageData;
import qupath.lib.images.servers.ImageServer;
import qupath.lib.images.servers.TransformedServerBuilder;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.hierarchy.PathObjectHierarchy;

public class MirrorImageCommand {

    private static final Logger logger = LoggerFactory.getLogger(MirrorImageCommand.class);

    public static void mirrorImage(QuPathGUI qupath, boolean mirrorX, boolean mirrorY){

        // Collect all data from original image
        ImageData<BufferedImage> imageData = qupath.getImageData();
        ImageServer<BufferedImage> server = imageData.getServer();
        PathObjectHierarchy hierarchy = imageData.getHierarchy();
        ImageData.ImageType imageType = imageData.getImageType();
        Collection<PathObject> allParentObjects = hierarchy.getRootObject().getChildObjects();
        int imageWidth = server.getWidth();
        int imageHeight = server.getHeight();

        // Initialize factors to no tranformation
        int scaleX = 1;
        int scaleY = 1;
        int translateX = 0;
        int translateY = 0;

        // Set factors according to requested transformation
        if (mirrorX){
            scaleX = -1;
            translateX = -imageWidth;
        }
        if (mirrorY){
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

//        logger.trace("newServer metadata: " + newServer.getMetadata());
//        logger.trace("original server metadata: " + server.getMetadata());
//        newServer.setMetadata(server.getMetadata());

        // Use the newly built server to create new imageData
        ImageData<BufferedImage> newImageData = new ImageData<>(newServer);
        newImageData.setImageType(imageType);

        // Get its hierarchy to append transformed objects
        PathObjectHierarchy newHierarchy = newImageData.getHierarchy();

        // Perform transformation on all objects from the original image and add each transformed object to the new hierarchy
        for (PathObject parent : allParentObjects){
            PathObject mirroredParent = ObjectUtils.mirrorObject(parent, scaleX, scaleY, translateX, translateY);
            ObjectUtils.addObjectAccountingForParent(newHierarchy, mirroredParent, null);
            if (parent.hasChildObjects()){
                for (PathObject child : parent.getChildObjects()){
                    PathObject mirroredChild = ObjectUtils.mirrorObject(child, scaleX, scaleY, translateX, translateY);
                    ObjectUtils.addObjectAccountingForParent(newHierarchy, mirroredChild, mirroredParent);
                }
            }
        }

        qupath.getViewer().setImageData(newImageData);
        qupath.refreshProject();
    }
}
