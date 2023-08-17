package org.dgsob;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
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

public class GeojsonToXml {
    public static class shapeType {
        public static final String CELL = "cell";
        public static final String DETECTION = "detection";
        public static final String ANNOTATION = "annotation";
    }
//    private static String featureType;
//    public class featureType {
//        public static final String POLYGON = "Polygon";
//        public static final String POINT = "MultiPoint";
//    }
    public GeojsonToXml(){
    }
    private int shapeCount = 0;

    /**
     * Reads a GeoJSON file from a specified location and converts to the XML format required by Leica's LMD software.
     * The saved image data is: calibration points, shapes' count and coordinates and optionally collector's cap ID for each shape.
     * @param inputPath Path to GeoJSON file
     * @param outputPath Path where the output XML file will be saved
     * @param notShapeType An objectType property in GeoJSON file which will NOT be counted as 'shape' in the output XML file
     * @param collectorParams Classes which user assigned to collectors symbols (A, B, C, etc.), if null - collectors won't be assigned to a shape in the output XML file
     */
    public boolean convertGeoJSONtoXML(String inputPath, String outputPath, String notShapeType, ParameterList collectorParams) {
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

            // Extract calibration points from GeoJSON and add to XML
            JsonNode calibrationPoints = new ObjectMapper().createArrayNode();
            JsonNode features = jsonNode.get("features");
            for (JsonNode feature : features) {
                JsonNode geometry = feature.get("geometry");
                String geometryType = geometry.get("type").asText();
                JsonNode properties = feature.get("properties");
                String featureName = properties.has("name") ? properties.get("name").asText() : "Unnamed Feature";
                // If one annotation of type 'MultiPoint'
                if ("MultiPoint".equals(geometryType) && "calibration".equalsIgnoreCase(featureName)) {
                    calibrationPoints = geometry.path("coordinates");
                    break;
                }
                // If 3 annotations of type 'Point'
                else if ("Point".equals(geometryType)){
                    switch (featureName.toLowerCase()) {
                        case "calibration1", "calibration2", "calibration3" -> ((ArrayNode) calibrationPoints).add(geometry.path("coordinates"));
                    }
                }
            }

            // Abort if calibration data is invalid
            if (calibrationPoints == null || calibrationPoints.size() != 3){
                return false;
            }

            for (int i = 0; i < calibrationPoints.size(); i++) {
                double x = calibrationPoints.get(i).get(0).asDouble();
                double y = calibrationPoints.get(i).get(1).asDouble();
                Element xElement = createTextElement(xmlDoc, "X_CalibrationPoint_" + (i + 1), String.valueOf(x));
                imageDataElement.appendChild(xElement);
                Element yElement = createTextElement(xmlDoc, "Y_CalibrationPoint_" + (i + 1), String.valueOf(y));
                imageDataElement.appendChild(yElement);
            }

            // Count shapes in GeoJSON and add ShapeCount element to XML
            for (JsonNode feature : features) {
                String objectType = feature.path("properties").path("objectType").asText();
                if (!notShapeType.equals(objectType)) {
                    shapeCount++;
                }
            }
            Element shapeCountElement = createTextElement(xmlDoc, "ShapeCount", String.valueOf(shapeCount));
            imageDataElement.appendChild(shapeCountElement);

            // Handle each shape: PointCount, CapID, coordinates
            int shapeIndex = 1;
            for (JsonNode feature : features) {
                String objectType = feature.path("properties").path("objectType").asText();
                if (!notShapeType.equals(objectType)) {
                    Element shapeElement = xmlDoc.createElement("Shape_" + shapeIndex);
                    imageDataElement.appendChild(shapeElement);

                    JsonNode geometry = feature.get("geometry");
                    JsonNode coordinates = geometry.get("coordinates").get(0);

                    int pointCount = coordinates.size();
                    Element pointCountElement = createTextElement(xmlDoc, "PointCount", String.valueOf(pointCount));
                    shapeElement.appendChild(pointCountElement);

                    if (collectorParams != null) {
                        JsonNode featureClassificationNode = feature.path("properties").path("classification");
                        addCupID(xmlDoc, shapeElement, featureClassificationNode, collectorParams);
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
    private boolean addCupID(Document doc, Element parentShape, JsonNode classificationNode, ParameterList paramsSetByUser){
        if (!classificationNode.isMissingNode()) {
            String featureClassName = classificationNode.path("name").asText();
            for (String paramKey : paramsSetByUser.getParameters().keySet()){
                Object paramValue = paramsSetByUser.getChoiceParameterValue(paramKey);
                if (paramValue.equals("All objects")){
                    parentShape.appendChild(createTextElement(doc, "CupID", paramKey));
                    return true;
                }
                if (featureClassName.equals(paramValue)){
                    parentShape.appendChild(createTextElement(doc, "CupID", paramKey));
                    return true;
                }
                if (paramValue.equals("Remaining objects")){
                    parentShape.appendChild(createTextElement(doc, "CupID", paramKey));
                    return true;
                }
                if (paramValue.equals("None")){
                    return false;
                }

            }
        }
        else {
            for (String paramKey : paramsSetByUser.getParameters().keySet()){
                Object paramValue = paramsSetByUser.getChoiceParameterValue(paramKey);
                if (paramValue.equals("All objects")){
                    parentShape.appendChild(createTextElement(doc, "CupID", paramKey));
                    return true;
                }
                if (paramValue.equals("Stroma") || paramValue.equals("Tumor") || paramValue.equals("Positive") || paramValue.equals("Negative")){
                    continue;
                }
                if (paramValue.equals("Remaining objects")){
                    parentShape.appendChild(createTextElement(doc, "CupID", paramKey));
                    return true;
                }
                if (paramValue.equals("None")){
                    return false;
                }

            }
        }
        return false;
    }
    public int getShapeCount(){
        return shapeCount;
    }
}
