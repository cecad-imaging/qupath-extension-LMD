package org.dgsob;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Objects;

public class GeoJSON_to_XML {
    private final String inputPath;
    private final String outputPath;
    private final String shapeType;
    // 'objectType' in GeoJSON from feature's properties
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

    public GeoJSON_to_XML(String inputPath, String outputPath, String shapeType){
        this.outputPath = outputPath;
        this.inputPath = inputPath;
        this.shapeType = shapeType;
    }
    public void convertGeoJSONtoXML() {
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
            JsonNode calibrationPoints = null;
            JsonNode features = jsonNode.get("features");
            for (JsonNode feature : features) {
                JsonNode geometry = feature.get("geometry");
                String geometryType = geometry.get("type").asText();
                if ("MultiPoint".equals(geometryType)) {
                    calibrationPoints = geometry.path("coordinates");
                    break;
                }
            }
            // TODO: add proper handling of checking if calibrationPoints are not null in try catch block
            for (int i = 0; i < Objects.requireNonNull(calibrationPoints).size(); i++) {
                double x = calibrationPoints.get(i).get(0).asDouble();
                double y = calibrationPoints.get(i).get(1).asDouble();
                Element xElement = createTextElement(xmlDoc, "X_CalibrationPoint_" + (i + 1), String.valueOf(x));
                imageDataElement.appendChild(xElement);
                Element yElement = createTextElement(xmlDoc, "Y_CalibrationPoint_" + (i + 1), String.valueOf(y));
                imageDataElement.appendChild(yElement);
            }

            // Extract shapes from GeoJSON and add to XML
            int shapeCount = 0;
            for (JsonNode feature : features) {
                String objectType = feature.path("properties").path("objectType").asText();
                if (!shapeType.equals(objectType)) {
                    shapeCount++;
                }
            }
            Element shapeCountElement = createTextElement(xmlDoc, "ShapeCount", String.valueOf(shapeCount));
            imageDataElement.appendChild(shapeCountElement);

            int shapeIndex = 1;
            for (JsonNode feature : features) {
                String objectType = feature.path("properties").path("objectType").asText();
                if (!shapeType.equals(objectType)) {
                    Element shapeElement = xmlDoc.createElement("Shape_" + shapeIndex);
                    imageDataElement.appendChild(shapeElement);

                    JsonNode geometry = feature.get("geometry");
                    JsonNode coordinates = geometry.get("coordinates").get(0);

                    int pointCount = coordinates.size();
                    Element pointCountElement = createTextElement(xmlDoc, "PointCount", String.valueOf(pointCount));
                    shapeElement.appendChild(pointCountElement);

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
            e.printStackTrace();
        }
    }

    private static Element createTextElement(Document doc, String tagName, String textContent) {
        Element element = doc.createElement(tagName);
        Text textNode = doc.createTextNode(textContent);
        element.appendChild(textNode);
        return element;
    }
}
