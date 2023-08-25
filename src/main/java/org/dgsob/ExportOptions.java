package org.dgsob;

public class ExportOptions {
    /*
    Collectors have caps. We assign objects of which class should be collected to which cap.
    Besides the classes we allow for an assignment of these optons.
    */
    public static class CapAssignments {
        public static final String NO_ASSIGNMENT = "None";
        public static final String ALL_OBJECTS = "All Objects";
        public static final String REMAINING_OBJECTS = "Remaining Objects";
    }
    public static class CollectorTypes {
        public static final String NO_COLLECTOR = "None";
        public static final String PCR_TUBES = "PCR Tubes";
        public static final String _8_FOLD_STRIP = "8-fold-Strip";
        public static final String _96_WELL_PLATE = "96-Wellplate";
        public static final String PETRI = "Petri";

    }
    /*
    In QuPath and GeoJSON it is called 'Object' (properties.objectType for each feature in GeoJSON file).
    In Leica's LMD software and XML it is called 'Shape'.
    */
    public static class ObjectTypes {
        public static final String CELL = "cell";
        public static final String DETECTION = "detection";
        public static final String ANNOTATION = "annotation";
    }
    /*
    These are the types geometries of features (geometry.type for each feature in GeoJSON file).
    */
    public static class FeatureGeoTypes {
        public static final String POLYGON = "Polygon";
        public static final String MULTIPOINT = "MultiPoint";
        public static final String POINT = "Point";

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
        },
        {
          "type": "Feature",
          "id": "2776da45-a948-4d11-8f31-e3bcd955ab5f",
          "geometry": {
            "type": "Polygon",
            "coordinates": [
              [
                [10141, 26552],
                [12087, 26552],
                [12087, 27880],
                [10141, 27880],
                [10141, 26552]
              ]
            ]
          },
          "properties": {
            "objectType": "annotation",
            "isLocked": true
          }
        }
      ]
    }
    */
}
