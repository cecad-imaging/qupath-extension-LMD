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
}
