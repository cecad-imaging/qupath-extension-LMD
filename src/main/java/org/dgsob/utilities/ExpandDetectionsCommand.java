package org.dgsob.utilities;

import org.dgsob.common.ClassUtils;
import org.dgsob.common.ObjectUtils;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.operation.buffer.BufferOp;
import org.locationtech.jts.operation.buffer.BufferParameters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class ExpandDetectionsCommand implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ExpandDetectionsCommand.class);
    ImageData<BufferedImage> imageData;
    ImageServer<BufferedImage> server;
    PathObjectHierarchy hierarchy;
    Collection<PathObject> selectedDetections;
    Collection<PathObject> expandedDetections;
    private int selectedDetectionsNumber;
    private boolean noSelection = false;
    public ExpandDetectionsCommand(ImageData<BufferedImage> imageData){
        this.imageData = imageData;
        this.server = imageData.getServer();
        this.hierarchy = imageData.getHierarchy();

        if (hierarchy.getSelectionModel().noSelection()) {
            Dialogs.showErrorNotification("Selection Required", "Please select detection objects to expand.");
            noSelection = true;
            return;
        }

        this.selectedDetections = ObjectUtils.filterOutAnnotations(hierarchy.getSelectionModel().getSelectedObjects());

        if (selectedDetections.isEmpty()) {
            Dialogs.showErrorNotification("Annotations not supported", "Please select a detection object.");
            noSelection = true;
            return;
        }

        this.selectedDetectionsNumber = selectedDetections.size();
    }

    @Override
    public void run() {
        if (noSelection)
            return;
        runObjectsExpansion();
    }

    /**
     * In first stage collects selected detection objects in pathObjects, creates new bigger ones based on the selected objects ROIs and provided by the user radius,
     * appends them to newObjects and removes the smaller ones from hierarchy.
     * In second stage it processes overlapping objects.
     */
    private void runObjectsExpansion(){

        Collection<PathObject> newObjects = new ArrayList<>();

        if(selectedDetectionsNumber == 1)
            Dialogs.showInfoNotification("LMD Notification", "You have chosen " + selectedDetectionsNumber + " object to expand.");
        else if(selectedDetectionsNumber > 5000)
            Dialogs.showWarningNotification("LMD Warning", "The number of selected objects is large: " + selectedDetectionsNumber + ". Consider processing less objects at once.");
        else
            Dialogs.showInfoNotification("LMD Notification", "You have chosen " + selectedDetectionsNumber + " objects to expand.");

        // TODO: Separate GUI dialogs creation from expansion logic.
        ParameterList params = new ParameterList()
                .addDoubleParameter("radiusMicrons", "Expansion radius:", 10, GeneralTools.micrometerSymbol(),
                        "Distance to expand ROI")
                .addChoiceParameter("sameClassChoice",
                        "If objects of the same class intersect:",
                        "Merge objects", Arrays.asList("Merge objects", "Exclude one, keep the other"),
                        "Either merge the intersecting objects or randomly delete one of them")
                .addChoiceParameter("differentClassesChoice",
                        "If objects of different classes intersect:",
                        "Exclude Both", Arrays.asList("Exclude Both", "Set priority for each class"),
                        "Either remove intersecting or choose a class of which object should be preserved when two objects intersect\n" +
                                "You will be prompted to set priorities after confirming your choice");

        boolean confirmed = Dialogs.showConfirmDialog("Expand selected", new ParameterPanelFX(params).getPane());

        if(!confirmed) {
            return;
        }

        boolean mergeSameClass = params.getChoiceParameterValue("sameClassChoice").equals("Merge objects");
        Object differentClassesChoice = params.getChoiceParameterValue("differentClassesChoice");
        List<String> priorityRanking = new ArrayList<>();

        // Creating second window if the user wants to set priorities for classes
        if (differentClassesChoice.equals("Set priority for each class")){
            Set<PathClass> availableClasses = ClassUtils.getAllClasses(hierarchy.getAllObjects(false));
            ParameterList priorityRankingParams = createPriorityRankingParameterList(availableClasses);
            boolean confirmedPriorityRanking = Dialogs.showConfirmDialog("Set priorities for classes", new ParameterPanelFX(priorityRankingParams).getPane());

            if (!confirmedPriorityRanking){
                return;
            }

            for (int i = 1; i <= availableClasses.size(); i++){
                String priorityClass = priorityRankingParams.getChoiceParameterValue("priorityClass_" + i).toString();
                if (priorityClass != null) {
                    priorityRanking.add(priorityClass);
                }
            }
        }

        long startTime = System.nanoTime();

        // Enlarging the objects by creating new ones and deleting old ones
        double radiusPixels;
        PixelCalibration calibration = server.getPixelCalibration();
        if (calibration.hasPixelSizeMicrons())
            radiusPixels = params.getDoubleParameterValue("radiusMicrons") / calibration.getAveragedPixelSizeMicrons();
        else
            radiusPixels = params.getDoubleParameterValue("radiusMicrons");
        // Iterate over old objects and create a new one for each
        for (PathObject pathObject : selectedDetections) {

            ROI roi = pathObject.getROI();
            Geometry geometry = roi.getGeometry();
            Geometry geometry2 = BufferOp.bufferOp(geometry, radiusPixels, BufferParameters.DEFAULT_QUADRANT_SEGMENTS);
            ROI roi2 = GeometryTools.geometryToROI(geometry2, ImagePlane.getPlane(roi));
            PathObject detection2 = PathObjects.createDetectionObject(roi2, pathObject.getPathClass());
            detection2.setName(pathObject.getName());
            detection2.setColor(pathObject.getColor());
            newObjects.add(detection2); // append newly created objects for processing instead of adding them to hierarchy
        }
        hierarchy.removeObjects(selectedDetections, false); // remove old objects
        hierarchy.getSelectionModel().clearSelection(); // the selection is no longer necessary
        try {
            // Steps for processing overlapping objects:

            // 1. Add 'background', i.e. already existing in hierarchy, not selected, detection objects to newObjects.
            newObjects = addOverlappingBackroundObjects(hierarchy, newObjects, radiusPixels);

            // 2. Check if differentClassesChoice is not 'Exclude Both' and if !all objects have same class,
            // if both are true -> sort newObjects
            if (!ClassUtils.areAllObjectsOfSameClass(newObjects) && !differentClassesChoice.equals("Exclude Both")) {
                newObjects = ObjectUtils.sortObjectsByPriority(newObjects, priorityRanking);
            }

            // 3. Process overlapping objects: merge, exclude both or exclude one of the two overlapping depending on their class
            Collection<PathObject> objectsToAddToHierarchy = new ArrayList<>();
            while (!newObjects.isEmpty()) {
                newObjects = processOverlappingObjects(newObjects, objectsToAddToHierarchy, mergeSameClass, priorityRanking);
            }

            hierarchy.addObjects(objectsToAddToHierarchy);
            this.expandedDetections = objectsToAddToHierarchy;
            long endTime = System.nanoTime();
            long duration = endTime - startTime;
            double seconds = (double) duration / 1_000_000_000.0;
            Dialogs.showInfoNotification("Operation Succesful", selectedDetectionsNumber + " objects processed in " + seconds + " seconds.\n"
                    + objectsToAddToHierarchy.size() + " output objects.");
        } catch (Throwable t){
            hierarchy.addObjects(selectedDetections);
            logger.error("Error processing overlapping objects: " + t.getMessage());
            Dialogs.showErrorNotification("Operation Failed", "Expanding objects failed. Please, try again.");
        }
    }

    /**
     * Should be called recursively. It appends an object to objectsToAddToHierarchy
     * if it doesn't intersect any other object.
     *
     * @param newObjects Collection of objects to process. Only one is processed witch each call of the function.
     * @param objectsToAddToHierarchy Collection of objects to add to hierarchy.
     * @param priorityRanking List of calsses names as strings which is the order of priority.
     * @return newObjects - one object
     */
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

    /**
     * For each object of the provided objects collection takes its ROI, multiplies by the given radius, and collects all
     * non-annotation objects in such enlarged ROI, adding them to the copy of provided objects collection and deleting from hierarchy.
     *
     * @param hierarchy A hierarchy to delete the objectsfrom.
     * @param objects Original collection of objects of interest.
     * @param radius Int value to enlarge each object's ROI. 'Background' objects within this ROI are added to the collection.
     * @return Provided objects + collected 'background' objects as one collection.
     */
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

    /**
     * Creates a list of parameters from provided classes.
     *
     * @param availableClasses Set of classes, their names will be options for a user to choose from while assigning priorities to the classes.
     * @return ParameterList of classes names or a ParameterList with an empty parameter message if availableClasses were empty.
     */
    private static ParameterList createPriorityRankingParameterList(Set<PathClass> availableClasses){
        List<String> classNames = new ArrayList<>(availableClasses.stream()
                .map(PathClass::getName)
                .toList());
        ParameterList priorityRankingParams = new ParameterList();

        if (!classNames.isEmpty()) {
            priorityRankingParams.addEmptyParameter("""
                    Class 1 corresponds to the highest priority.
                    Lower prority object will be deleted if intersecting object of a higher priority.
                                        
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

    public int getSelectedDetectionsNumber(){
        return selectedDetectionsNumber;
    }
}
