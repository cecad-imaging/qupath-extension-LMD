package org.dgsob;

import qupath.lib.objects.PathObject;
import qupath.lib.objects.classes.PathClass;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class ClassUtils {

    static Set<PathClass> getAllClasses(Collection<PathObject> objects){
        Set<PathClass> uniqueClasses = new HashSet<>();
        for (PathObject object : objects) {
            PathClass objectClass = object.getPathClass();
            if (objectClass != null) {
                uniqueClasses.add(objectClass);
            }
        }
        return uniqueClasses;
    }
    static boolean areAllObjectsOfSameClass(Collection<PathObject> objects) {
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
}
