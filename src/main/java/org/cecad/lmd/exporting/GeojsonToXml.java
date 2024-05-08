package org.cecad.lmd.exporting;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;
import qupath.lib.plugins.parameters.ParameterList;

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
import java.util.Set;

import static org.cecad.lmd.common.Constants.CapAssignments.*;
import static org.cecad.lmd.common.Constants.ObjectTypes.*;
import static org.cecad.lmd.common.Constants.FeatureGeoTypes.*;
import static org.cecad.lmd.common.Constants.CalibrationPointsNames.*;

public class GeojsonToXml {
    private int shapeCount = 0;
    private final String inputPath;
    private final String outputPath;

    /**
     *
     * @param inputPath Path to the input GeoJSON file.
     * @param outputPath Path where the output XML file will be saved.
     */
    public GeojsonToXml(String inputPath, String outputPath){
        this.inputPath = inputPath;
        this.outputPath = outputPath;
    }

    /**
     * Reads a GeoJSON file from a specified location and converts to the XML format required by Leica's LMD software.
     * The saved image data is: calibration points, shapes' count and coordinates and optionally collector's cap ID for each shape taken from collectorParams.
     * It filters out annotations from GeoJSON and doesn't count them as shapes in the XML, so the two files actually contain different objects.
     *
     * @param collectorParams Classes which user assigned to collectors symbols (A, B, C, etc.), if null - collectors won't be assigned to a shape in the output XML file
     */
    boolean createLeicaXML(ParameterList collectorParams) {
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
            for (JsonNode feature : features) {
                String objectType = feature.path("properties").path("objectType").asText();
                if (!ANNOTATION.equals(objectType)) {
                    shapeCount++;
                }
            }
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

                    if (collectorParams != null) {
                        JsonNode featureClassificationNode = feature.path("properties").path("classification");
                        addCapID(xmlDoc, shapeElement, featureClassificationNode, collectorParams);
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
    @SuppressWarnings("UnusedReturnValue")
    private boolean addCapID(Document doc, Element parentShape, JsonNode classificationNode, ParameterList paramsSetByUser){

        Set<String> paramKeys = paramsSetByUser.getParameters().keySet();
        String[] paramKeysPriorities = new String[paramKeys.size()];

        if (!classificationNode.isMissingNode()) {
            String featureClassName = classificationNode.path("name").asText();
            for (String paramKey : paramKeys){
                Object paramValue = paramsSetByUser.getChoiceParameterValue(paramKey);
                if (paramValue.equals(NO_ASSIGNMENT)){
                    continue;
                }
                if (paramValue.equals(ALL_OBJECTS)){
                    paramKeysPriorities[0] = paramKey;
                }
                if (featureClassName.equals(paramValue)){
                    paramKeysPriorities[1] = paramKey;
                }
                if (paramValue.equals(REMAINING_OBJECTS)){
                    paramKeysPriorities[2] = paramKey;
                }

            }
            for (String paramKey : paramKeysPriorities) {
                if (paramKey != null) {
                    parentShape.appendChild(createTextElement(doc, "CapID", paramKey));
                    return true;
                }
            }
        }
        else {
            for (String paramKey : paramKeys){
                Object paramValue = paramsSetByUser.getChoiceParameterValue(paramKey);
                if (paramValue.equals(NO_ASSIGNMENT)){
                    continue;
                }
                if (paramValue.equals("Stroma") || paramValue.equals("Tumor") || paramValue.equals("Positive") || paramValue.equals("Negative")){
                    continue;
                }
                if (paramValue.equals(ALL_OBJECTS)){
                    paramKeysPriorities[0] = paramKey;

                }
                if (paramValue.equals(REMAINING_OBJECTS)){
                    paramKeysPriorities[1] = paramKey;
                }
            }
            for (String paramKey : paramKeysPriorities) {
                if (paramKey != null) {
                    parentShape.appendChild(createTextElement(doc, "CapID", paramKey));
                    return true;
                }
            }
        }
        return false;
    }
    public int getShapeCount(){
        return shapeCount;
    }
}
