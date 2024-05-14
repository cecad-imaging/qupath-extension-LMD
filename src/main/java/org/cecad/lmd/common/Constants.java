package org.cecad.lmd.common;

import java.nio.file.Path;

public class Constants {

    public static class EnlargeOptions {
        public static final String MERGE = "Merge Objects";
        public static final String DISCARD_1 = "Discard 1 of the Objects";
        public static final String EXCLUDE_BOTH = "Discard Both";
        public static final String SET_PRIORITY = "Set Priority for Each Class";
    }

    public static class CapAssignments {
        public static final String NO_ASSIGNMENT = "None";
        public static final String ALL_OBJECTS = "All";
        public static final String REMAINING_OBJECTS = "Remaining";
    }

    public static class CollectorTypes {
        public static final String NONE = "None";
        public static final String PCR_TUBES = "PCR Tubes";
        public static final String _8_FOLD_STRIP = "8-Fold Strip";
        public static final String _12_FOLD_STRIP = "12-Fold Strip";
        public static final String _96_WELL_PLATE = "96-Well Plate";
        public static final String PETRI_DISHES = "Petri Dishes";

    }

    public static class WellDataFileNames {
        public static final String PCR_TUBES_DATA = "well_data_pcr_tubes.json";
        public static final String _8_FOLD_STRIP_DATA = "well_data_8_fold_strip.json";
        public static final String _12_FOLD_STRIP_DATA = "well_data_12_fold_strip.json";
        public static final String _96_WELL_PLATE_DATA = "well_data_96_well_plate.json";
        public static final String PETRI_DISHES_DATA = "well_data_petri_dishes.json";

    }

    public static class ObjectTypes {
        public static final String CELL = "cell";
        public static final String DETECTION = "detection";
        public static final String ANNOTATION = "annotation";
    }

    public static class FeatureGeoTypes {
        public static final String POLYGON = "Polygon";
        public static final String MULTIPOINT = "MultiPoint";
        public static final String POINT = "Point";

    }

    public static class Detections {
        public static final String ALL = "All";
        public static final String SELECTED = "Selected";
    }
    
    public static class Paths {
        public static Path DATA_SUBDIRECTORY;
        public static Path TEMP_SUBDIRECTORY;
    }

    /* Example of Pretty JSON file structure relevant for classes: ObjectTypes, FeatureGeoTypes:
    {
      "type": "FeatureCollection",
      "features": [
        {
          "type": "Feature",
          "id": "d45ec4dc-b4cc-44ba-a5d5-47cca579f2ef",
          "geometry": {
            "type": "MultiPoint",
            "coordinates": [
              [36425.21, 33205.61],
              [56491.38, 23111.9],
              [43903.19, 5644.47]
            ]
          },
          "properties": {
            "objectType": "annotation"
          }
        }
      ]
    }
    */

    public static class CalibrationPointsNames {
        public static final String CP1 = "calibration1";
        public static final String CP2 = "calibration2";
        public static final String CP3 = "calibration3";

    }
}
