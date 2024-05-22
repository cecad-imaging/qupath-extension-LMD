package org.cecad.lmd.commands;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.cecad.lmd.common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.cecad.lmd.common.Constants.ObjectTypes.*;
import static org.cecad.lmd.common.Constants.FeatureGeoTypes.*;
import static org.cecad.lmd.common.Constants.CalibrationPointsNames.*;
import static org.cecad.lmd.common.Constants.WellDataFileFields.*;
import static org.cecad.lmd.common.Constants.CollectorTypes.*;

public class BuildXmlCommand {
    private static final Logger logger = LoggerFactory.getLogger(BuildXmlCommand.class);
    private int shapeCount = 0;
    private final String inputPath;
    private final String outputPath;
    private final String collectorName;

    public BuildXmlCommand(String inputPath, String outputPath, String collectorName){
        this.inputPath = inputPath;
        this.outputPath = outputPath;
        this.collectorName = collectorName;
    }

    boolean createLeicaXML(Map<String, Object>[] collectorParams) {
        try {
            // Read GeoJSON file
            File geojsonFile = new File(inputPath);
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(geojsonFile);

            // Create XML document
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document xmlDoc = docBuilder.newDocument();

            // Create root element
            Element imageDataElement = xmlDoc.createElement("ImageData");
            xmlDoc.appendChild(imageDataElement);

            // Extract calibration points from GeoJSON and save in an array
            JsonNode[] calibrationPoints = new JsonNode[3];
            JsonNode features = jsonNode.get("features");
            for (JsonNode feature : features) {
                JsonNode geometry = feature.get("geometry");
                String geometryType = geometry.get("type").asText();
                JsonNode properties = feature.get("properties");
                String featureName = properties.has("name") ? properties.get("name").asText() : "Unnamed Feature";
                // If 3 objects of type 'Point' (their objectType doesn't matter)
                if (POINT.equals(geometryType)){
                    switch (featureName.toLowerCase()) {
                        case CP1 -> calibrationPoints[0] = geometry.path("coordinates");
                        case CP2 -> calibrationPoints[1] = geometry.path("coordinates");
                        case CP3 -> calibrationPoints[2] = geometry.path("coordinates");
                    }
                }
            }

            // Abort if calibration data is invalid
            if (calibrationPoints[0] == null || calibrationPoints[1] == null || calibrationPoints[2] == null){
                return false;
            }

            // Add calibration points to the XML
            for (int i = 0; i < calibrationPoints.length; i++) {
                double x = calibrationPoints[i].get(0).asDouble();
                double y = calibrationPoints[i].get(1).asDouble();
                Element xElement = createTextElement(xmlDoc, "X_CalibrationPoint_" + (i + 1), String.valueOf(x));
                imageDataElement.appendChild(xElement);
                Element yElement = createTextElement(xmlDoc, "Y_CalibrationPoint_" + (i + 1), String.valueOf(y));
                imageDataElement.appendChild(yElement);
            }

            // Count shapes in GeoJSON and add ShapeCount element to XML
            // DONE: We can get all of the counts of all classes here. Also, make this a function.
            for (JsonNode feature : features) {
                String objectType = feature.path("properties").path("objectType").asText();
                if (!ANNOTATION.equals(objectType)) {
                    shapeCount++;
                }
            }

//            Map<String, Integer> classesCounts = countObjects(features); //this also updates shapeCount - more elegant solution later


            Element shapeCountElement = createTextElement(xmlDoc, "ShapeCount", String.valueOf(shapeCount));
            imageDataElement.appendChild(shapeCountElement);

            // Handle each shape: PointCount, CapID, coordinates
            int shapeIndex = 1;
            for (JsonNode feature : features) {
                String objectType = feature.path("properties").path("objectType").asText();
                if (!ANNOTATION.equals(objectType)) {
                    Element shapeElement = xmlDoc.createElement("Shape_" + shapeIndex);
                    imageDataElement.appendChild(shapeElement);

                    JsonNode geometry = feature.get("geometry");
                    JsonNode coordinates = geometry.get("coordinates").get(0);

                    int pointCount = coordinates.size();
                    Element pointCountElement = createTextElement(xmlDoc, "PointCount", String.valueOf(pointCount));
                    shapeElement.appendChild(pointCountElement);

                    if (collectorParams != null && !Objects.equals(collectorName, NONE)) {
                        JsonNode classificationNode = feature.path("properties").path("classification");
                        if (!classificationNode.isMissingNode()) {
                            String featureClassName = classificationNode.path("name").asText();
                            if (Objects.equals(collectorName, _96_WELL_PLATE)) {
                                addCapIDForClasses_96Well(xmlDoc, shapeElement, featureClassName, collectorParams);
                            }
                            else
                                addCapIDForClasses(xmlDoc, shapeElement, featureClassName, collectorParams);
                        }
                        else{
                            logger.warn("Classification is missing.");
                            addCapIDBasic(xmlDoc, shapeElement, collectorParams);
                            // TODO: addCapIDBasic_96Well
                        }
                    }
                    else{
                        logger.error("Collector Params is null!");
                    }

                    int pointIndex = 1;
                    for (JsonNode point : coordinates) {
                        double x = point.get(0).asDouble();
                        double y = point.get(1).asDouble();
                        Element xElement = createTextElement(xmlDoc, "X_" + pointIndex, String.valueOf(x));
                        shapeElement.appendChild(xElement);
                        Element yElement = createTextElement(xmlDoc, "Y_" + pointIndex, String.valueOf(y));
                        shapeElement.appendChild(yElement);
                        pointIndex++;
                    }

                    shapeIndex++;
                }
            }

            // Write XML to file
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            DOMSource source = new DOMSource(xmlDoc);
            StreamResult result = new StreamResult(new File(outputPath));
            transformer.transform(source, result);

            System.out.println("Conversion completed successfully.");

        } catch (IOException | ParserConfigurationException | TransformerException e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    private Element createTextElement(Document doc, String tagName, String textContent) {
        Element element = doc.createElement(tagName);
        Text textNode = doc.createTextNode(textContent);
        element.appendChild(textNode);
        return element;
    }

    private void addCapIDForClasses(Document doc, Element parentShape,
                                    String featureClassName,
                                    Map<String, Object>[] wellToClassAssignments){

        for (Map<String, Object> assignment : wellToClassAssignments) {
            String objectClass = (String) assignment.get(OBJECT_CLASS_TYPE);
            String wellLabel = (String) assignment.get(WELL_LABEL);
            int objectQty = (int) assignment.get(OBJECT_QTY);

            if (objectClass == null)
                continue;

            if (objectClass.equals(Constants.CapAssignments.NO_ASSIGNMENT) || objectQty == 0)
                continue;

            if (objectClass.equals(featureClassName)) {
                parentShape.appendChild(createTextElement(doc, "CapID", wellLabel));
                assignment.put(OBJECT_QTY, objectQty - 1);
                break;
            }
        }
    }

    private void addCapIDForClasses_96Well(Document doc, Element parentShape,
                                           String featureClassName,
                                           Map<String, Object>[] wellsCountToClassAssignments) {
        // TODO: Check if the number of objects > the number of wells to assign them to | UPON SAVING WELL DATA
        for (Map<String, Object> assignment : wellsCountToClassAssignments) {
            String objectClass = (String) assignment.get(OBJECT_CLASS_TYPE);
            List<String> wellLabels = (List<String>) assignment.get("wellLabels");
            int objectQty = (int) assignment.get(OBJECT_QTY);
            int objectsPerWell = (int) assignment.get("objectsPerWell");
            int redundantObjects = (int) assignment.get("redundantObjects");
            int wellCount = (int)  assignment.get(WELL_COUNT);

            if (objectClass == null)
                return;

            if (objectClass.equals(Constants.CapAssignments.NO_ASSIGNMENT) || !objectClass.equals(featureClassName)
                    || objectQty == 0 || wellCount == 0)
                continue;

            if (objectsPerWell == 0) {
                assignment.put(WELL_COUNT, wellCount - 1);
                wellCount = wellCount - 1;
                int objectsPerWellAtTheBeginning = (int) assignment.get("objectsPerWellAtTheBeginning");
                assignment.put("objectsPerWell", objectsPerWellAtTheBeginning);
                objectsPerWell = objectsPerWellAtTheBeginning;
            }

            String wellLabel = wellLabels.get(wellCount - 1);

            // put wellLabel in the xml
            parentShape.appendChild(createTextElement(doc, "CapID", wellLabel));
            assignment.put(OBJECT_QTY, objectQty - 1);
            assignment.put("objectsPerWell", objectsPerWell - 1);
            break;
        }
    }


    private void addCapIDBasic(Document doc, Element parentShape,
                               Map<String, Object>[] wellToClassAssignments){

        for (Map<String, Object> assignment : wellToClassAssignments) {
            String objectClass = (String) assignment.get(OBJECT_CLASS_TYPE);
            String wellLabel = (String) assignment.get(WELL_LABEL);
            int objectQty = (int) assignment.get(OBJECT_QTY);

            if (objectClass == null)
                return;

            if (objectClass.equals(Constants.CapAssignments.NO_ASSIGNMENT))
                continue;

            if (objectQty == 0)
                continue;

            if (objectClass.equals(Constants.CapAssignments.ALL_OBJECTS)) {
                parentShape.appendChild(createTextElement(doc, "CapID", wellLabel));
                assignment.put(OBJECT_QTY, objectQty - 1);
                break;
            }
        }
    }

    public int getShapeCount(){
        return shapeCount;
    }
}
