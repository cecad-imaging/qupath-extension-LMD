package org.cecad.lmd.commands;

import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import org.cecad.lmd.common.ClassUtils;
import org.cecad.lmd.common.ObjectUtils;
import org.cecad.lmd.ui.MoreOptionsPane;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.operation.buffer.BufferOp;
import org.locationtech.jts.operation.buffer.BufferParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.fx.dialogs.Dialogs;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.tools.GuiTools;
import qupath.lib.gui.viewer.OverlayOptions;
import qupath.lib.images.servers.ImageServer;
import qupath.lib.images.servers.PixelCalibration;
import qupath.lib.objects.PathDetectionObject;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.PathObjects;
import qupath.lib.objects.classes.PathClass;
import qupath.lib.objects.hierarchy.PathObjectHierarchy;
import qupath.lib.objects.hierarchy.events.PathObjectSelectionModel;
import qupath.lib.plugins.parameters.ParameterList;
import qupath.lib.regions.ImagePlane;
import qupath.lib.roi.GeometryTools;
import qupath.lib.roi.PolygonROI;
import qupath.lib.roi.ShapeSimplifier;
import qupath.lib.roi.interfaces.ROI;

import static org.cecad.lmd.common.Constants.EnlargeOptions.*;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class MoreOptionsCommand implements Runnable {
    private final static Logger logger = LoggerFactory.getLogger(SetCollectorCommand.class);
    private final String TITLE = "More Options";
    private Stage stage;
    private final QuPathGUI qupath;
    private final PathObjectHierarchy hierarchy;
    Collection<PathObject> oldObjects = null;
    Collection<PathObject> expandedDetections = null;

    public MoreOptionsCommand(QuPathGUI qupath) {
        this.qupath = qupath;
        this.hierarchy = qupath.getImageData().getHierarchy();
    }

    @Override
    public void run() {
        showStage();
    }

    private void showStage(){
        boolean creatingStage = stage == null;
        if (creatingStage)
            stage = createStage();
        if (stage.isShowing())
            return;
        stage.show();
    }

    private Stage createStage(){
        Stage stage = new Stage();
        Pane pane = new MoreOptionsPane(this);
        Scene scene = new Scene(pane);
        stage.setScene(scene);
        stage.setResizable(false);
        stage.setTitle(TITLE);
        stage.initOwner(qupath.getStage());
        stage.setOnCloseRequest(event -> {
            hideStage();
            event.consume();
        });
        return stage;
    }

    private void hideStage() {
        stage.hide();
    }

    public void makeSelectedDetectionsBigger(int radius, String sameClass, String diffClass){
        if (isNoSelection(hierarchy.getSelectionModel(), true))
            return;

        Collection<PathObject> selectedDetections = ObjectUtils.filterOutAnnotations(hierarchy.getSelectionModel().getSelectedObjects());

        if (!wereSelectedObjectsDetections(selectedDetections))
            return;

        oldObjects = selectedDetections;

        int selectedDetectionsNumber = selectedDetections.size();
        showEnlargingNotification(selectedDetectionsNumber);

        boolean mergeSameClass = Objects.equals(sameClass, MERGE);

        List<String> priorityRanking = new ArrayList<>();

        if (Objects.equals(diffClass, SET_PRIORITY)){
            Set<PathClass> availableClasses = ClassUtils.getAllClasses(hierarchy.getAllObjects(false));
            ParameterList priorityRankingParams = createPriorityRankingParameterList(availableClasses);
            boolean confirmedPriorityRanking = GuiTools.showParameterDialog("Set Priorities", priorityRankingParams);

            if (!confirmedPriorityRanking)
                return;

            for (int i = 1; i <= availableClasses.size(); i++){
                String priorityClass = priorityRankingParams.getChoiceParameterValue("priorityClass_" + i).toString();
                if (priorityClass != null) {
                    priorityRanking.add(priorityClass);
                }
            }

        }

        long startTime = System.nanoTime();

        double radiusPixels;
        PixelCalibration calibration = qupath.getImageData().getServer().getPixelCalibration();
        if (calibration.hasPixelSizeMicrons())
            radiusPixels = radius / calibration.getAveragedPixelSizeMicrons();
        else
            radiusPixels = radius;

        Collection<PathObject> newObjects = new ArrayList<>();

        for (PathObject pathObject : selectedDetections) {

            ROI roi = pathObject.getROI();
            Geometry geometry = roi.getGeometry();
            Geometry geometry2 = BufferOp.bufferOp(geometry, radiusPixels, BufferParameters.DEFAULT_QUADRANT_SEGMENTS);
            ROI roi2 = GeometryTools.geometryToROI(geometry2, ImagePlane.getPlane(roi));
            PathObject detection2 = PathObjects.createDetectionObject(roi2, pathObject.getPathClass());
            detection2.setName(pathObject.getName());
            detection2.setColor(pathObject.getColor());
            newObjects.add(detection2);
        }

        Collection<PathObject> enlargedWithoutBackground = newObjects;

        hierarchy.removeObjects(selectedDetections, false);
        hierarchy.getSelectionModel().clearSelection();

        try {
            // Steps for processing overlapping objects:

            // 1. Add 'background', i.e. already existing in hierarchy, not selected, detection objects to newObjects.
            newObjects = addOverlappingBackroundObjects(hierarchy, newObjects, radiusPixels);

                // Just update the oldObjects for Undo action
            Collection<PathObject> overlappingBackgroundObjects = getOverlappingBackground(newObjects, enlargedWithoutBackground);
            oldObjects.addAll(overlappingBackgroundObjects);

            // 2. Check if differentClassesChoice is not 'Exclude Both' and if !all objects have same class,
            // if both are true -> sort newObjects
            if (!ClassUtils.areAllObjectsOfSameClass(newObjects) && !Objects.equals(diffClass, EXCLUDE_BOTH)) {
                newObjects = ObjectUtils.sortObjectsByPriority(newObjects, priorityRanking);
            }

            // 3. Process overlapping objects: merge, exclude both or exclude one of the two overlapping depending on their class
            Collection<PathObject> objectsToAddToHierarchy = new ArrayList<>();
            while (!newObjects.isEmpty()) {
                newObjects = processOverlappingObjects(newObjects, objectsToAddToHierarchy, mergeSameClass, priorityRanking);
            }

            hierarchy.addObjects(objectsToAddToHierarchy);
            expandedDetections = objectsToAddToHierarchy;
            long endTime = System.nanoTime();
            long duration = endTime - startTime;
            double seconds = (double) duration / 1_000_000_000.0;
            Dialogs.showInfoNotification("Operation Successful", selectedDetectionsNumber + " objects processed in " + seconds + " seconds.\n"
                    + objectsToAddToHierarchy.size() + " remaining objects.");
        } catch (Throwable t){
            hierarchy.addObjects(selectedDetections);
            logger.error("Error processing overlapping objects: {}", t.getMessage());
            Dialogs.showErrorNotification("Operation Failed", "Expanding objects failed. Please, try again.");
        }
    }

    public void undoEnlargement(){
        if (expandedDetections == null || oldObjects == null)
            return;
        hierarchy.removeObjects(expandedDetections, false);
        hierarchy.addObjects(oldObjects);
        expandedDetections = null;
        oldObjects = null;
    }

    private Collection<PathObject> getOverlappingBackground(Collection<PathObject> allObjects, Collection<PathObject> enlargedObjects) {
        Collection<PathObject> backgroundObjects = new HashSet<>(allObjects);
        backgroundObjects.removeAll(enlargedObjects);
        return backgroundObjects;
    }

    private static Collection<PathObject> processOverlappingObjects(Collection<PathObject> newObjects,
                                                                    Collection<PathObject> objectsToAddToHierarchy,
                                                                    boolean mergeSameClass,
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
                        if (mergeSameClass) {
                            objectsToMerge.add(object);
                            objectsToMerge.add(otherObject);
                        }
                        else{
                            if (new Random().nextBoolean()) {
                                objectsToRemoveFromProcessed.add(object);
                                objectsToAddToProcessed.add(otherObject);
                            } else {
                                objectsToRemoveFromProcessed.add(otherObject);
                                objectsToAddToProcessed.add(object);
                            }
                        }
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
                                we are sure the object doesn't intersect different class object,
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
                if (mergeSameClass) {
                    remainingObjects.removeAll(objectsToMerge);
                    remainingObjects.add(ObjectUtils.mergeObjects(objectsToMerge, objectClass));
                }
                else {
                    remainingObjects.removeAll(objectsToRemoveFromProcessed);
                    remainingObjects.addAll(objectsToAddToProcessed);
                }
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
            objectsInROI = ObjectUtils.filterOutAnnotations(objectsInROI); // remove all annotations from the collection
            for (PathObject roiObject : objectsInROI){
                if (!enhancedObjects.contains(roiObject)/*&& !roiObject.isAnnotation()*/){
                    enhancedObjects.add(roiObject);
                }
            }
            hierarchy.removeObjects(objectsInROI, true);
        }
        return enhancedObjects;
    }

    private boolean isNoSelection(PathObjectSelectionModel selectionModel, boolean modifyingDetections){
        if (selectionModel.noSelection()) {
            if (modifyingDetections)
                Dialogs.showWarningNotification("Selection Required", "Please select detections to modify (Ctrl+Alt+D for all).");
            else
                Dialogs.showWarningNotification("Selection Required", "Please select annotations to modify.");

            return true;
        }
        return false;
    }
    private boolean wereSelectedObjectsDetections(Collection<PathObject> selected){
        if (selected.isEmpty()) {
            Dialogs.showErrorNotification("Annotations not supported", "Please select a detection object.");
            return false;
        }
        return true;
    }
    private void showEnlargingNotification(int selectedDetectionsNumber){
        if(selectedDetectionsNumber == 1)
            Dialogs.showInfoNotification("LMD Notification", "You have chosen " + selectedDetectionsNumber + " object to expand.");
        else if(selectedDetectionsNumber > 5000)
            Dialogs.showWarningNotification("LMD Warning", "The number of selected objects is large: " + selectedDetectionsNumber + ". Depending on your resources this may take a long time and may or may not crash. Consider processing less objects at once.");
        else
            Dialogs.showInfoNotification("LMD Notification", "You have chosen " + selectedDetectionsNumber + " objects to expand.");

    }

    private static ParameterList createPriorityRankingParameterList(Set<PathClass> availableClasses){
        List<String> classNames = new ArrayList<>(availableClasses.stream()
                .map(PathClass::getName)
                .toList());
        ParameterList priorityRankingParams = new ParameterList();

        if (!classNames.isEmpty()) {
            priorityRankingParams.addEmptyParameter("""
                    Class 1 corresponds to the highest priority.
                    Lower priority object will be deleted if intersecting object of a higher priority.
                                        
                    """);
            for (int i = 1; i <= classNames.size(); i++) {
                priorityRankingParams.addChoiceParameter("priorityClass_" + i, "Class " + i, classNames.get(i - 1), classNames);
            }
        }
        else{
            priorityRankingParams.addEmptyParameter("""
                    No classifications detected.
                                        
                    """);
        }
        return priorityRankingParams;
    }

    public void convertSelectedObjects(PathObjectHierarchy hierarchy, boolean toDetections){

        if (!toDetections && isNoSelection(hierarchy.getSelectionModel(), true))
            return;
        else if (toDetections && isNoSelection(hierarchy.getSelectionModel(), false)) {
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

    public void simplifySelectedDetections(PathObjectHierarchy hierarchy, Double altitudeThreshold){

        if (isNoSelection(hierarchy.getSelectionModel(), true))
            return;

        Collection<PathObject> objects = hierarchy.getSelectionModel().getSelectedObjects();
        for (PathObject object : objects){
            ROI pathROI = object.getROI();
            if (pathROI instanceof PolygonROI polygonROI) {
                pathROI = ShapeSimplifier.simplifyPolygon(polygonROI, altitudeThreshold);
            } else {
                pathROI = ShapeSimplifier.simplifyShape(pathROI, altitudeThreshold);
            }
            ((PathDetectionObject)object).setROI(pathROI);
        }
        hierarchy.fireObjectsChangedEvent(hierarchy, objects);
    }

    public void repaintDetectionsBordersToMatchLaser(double customStrokeMicrons) throws IOException {
        PathObjectSelectionModel selectionModel = this.qupath.getImageData().getHierarchy().getSelectionModel();

//        if (isNoSelection(selectionModel, true)) // modify selected
//            return;
//        Collection<PathObject> objects = selectionModel.getSelectedObjects();

        /* We repaint all detections instead of selected because the way we set the new stroke thickness
           combined with the use of PathObjectPainter.paintSpecifiedObjects triggers the change for all detections
           regardless of what we pass as objects :/ for now it'll do tho */

        Collection<PathObject> objects = this.qupath.getImageData().getHierarchy().getDetectionObjects(); // modify all

        ImageServer<BufferedImage> server = qupath.getViewer().getImageData().getServer();
        OverlayOptions overlay = qupath.getOverlayOptions();
        double downsample = qupath.getViewer().getDownsampleFactor();
        ObjectUtils.repaintDetectionsWithCustomStroke(objects, customStrokeMicrons, server, overlay, selectionModel, downsample);
        Dialogs.showInfoNotification("Action successful", objects.size() + " detections modified");
    }

    public QuPathGUI getQupath(){
        return qupath;
    }

    public Logger getLogger(){
        return logger;
    }

}
