package org.cecad.lmd.common;

import qupath.lib.objects.PathObject;
import qupath.lib.objects.classes.PathClass;

import java.util.*;

public class ClassUtils {

    public static Set<PathClass> getAllClasses(Collection<PathObject> objects){
        Set<PathClass> uniqueClasses = new HashSet<>();
        for (PathObject object : objects) {
            PathClass objectClass = object.getPathClass();
            if (objectClass != null) {
                uniqueClasses.add(objectClass);
            }
        }
        return uniqueClasses;
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
                        return false;
                    }
                }
            }
        }

        return true;
    }

    public static Map<String, Integer> getObjectsCountByClass(Collection<PathObject> objects) {
        Map<String, Integer> classesCount = new HashMap<>();
        for (PathObject object : objects) {
            PathClass objectClass = object.getPathClass();
            if (objectClass != null) {
                String className = objectClass.getName();
                classesCount.put(className, classesCount.getOrDefault(className, 0) + 1);
            }
        }
        return classesCount;
    }

    public static Map<String, Double> getObjectsAreaByClass(Collection<PathObject> objects) {
        Map<String, Double> classesAreas = new HashMap<>();
        for (PathObject object : objects) {
            PathClass objectClass = object.getPathClass();
            if (objectClass != null) {
                String className = objectClass.getName();
                classesAreas.put(className, classesAreas.getOrDefault(className, 0.0) + object.getROI().getArea());
            }
        }
        return classesAreas;
    }
}
