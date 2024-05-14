package org.cecad.lmd.ui;

import qupath.lib.objects.PathObject;
import qupath.lib.objects.classes.PathClass;

import java.util.Collection;
import java.util.Set;

public interface ControlsInterface {
    void updateCollectorLabel(String collectorName);
    Set<PathClass> getAllClasses();
    Collection<PathObject> getDetectionsToExport();
}
