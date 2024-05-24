package org.cecad.lmd.ui;

import com.google.gson.Gson;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.cecad.lmd.common.Constants.WellDataFileNames.*;
import static org.cecad.lmd.common.Constants.CollectorTypes.*;

public class IOUtils {

    public static Path createSubdirectory(Path projectFilePath, String subdirectory, Logger logger){
        if (projectFilePath != null) {
            Path projectDirectory = projectFilePath.getParent();
            if (projectDirectory != null) {
                Path subdirectoryPath = projectDirectory.resolve(subdirectory);
                try {
                    Files.createDirectories(subdirectoryPath); // Create the directory if it doesn't exist
                } catch (IOException e) {
                    logger.error("Error creating subdirectories: {}", e.getMessage());
                }
                return subdirectoryPath;
            }
        }
        // If the project is null, return the current working directory.
        // This should probably naturally never happen but idk.
        return Paths.get(System.getProperty("user.dir"));
    }

    public static void saveWellsToFile(Path dirPath, List<Map<String, Object>> wellDataList, String fileName, Logger logger) {
        String filePath = dirPath.resolve(fileName).toString();
        try (Writer writer = new FileWriter(filePath)) {
            Gson gson = new Gson();  // Add Gson dependency
            gson.toJson(wellDataList, writer);
        } catch (IOException e) {
            logger.error("Error while saving wells assignment: {}", e.getMessage());
        }
    }

    public static void clearJsonFiles(String directoryPath, Logger logger) {
        File directory = new File(directoryPath);

        if (!directory.exists() || !directory.isDirectory()) {
            logger.error("Invalid directory path: {}", directoryPath);
        }

        if (directory.listFiles() == null)
            return;

        for (File file : Objects.requireNonNull(directory.listFiles())) {
            if (file.isFile() && file.getName().endsWith(".json")) {
                if (file.delete()) {
                    logger.info("Well Data cleared."); // only well data files use .json format
                } else {
                    logger.warn("Failed to delete file: {}", file.getName());
                }
            }
        }
    }

    public static void clearGeoJsonFile (String pathGeoJSON, Logger logger) {
        try {
            Path geoJSONPath = Path.of(pathGeoJSON);
            Files.deleteIfExists(geoJSONPath);
        } catch (IOException e) {
            logger.warn("Error deleting GeoJSON interim file: {}", e.getMessage(), e);
        }
    }

    public static String genWellDataFileNameFromWellsNum(int numWells){
        if (numWells == 8)
            return _8_FOLD_STRIP_DATA;
        else if (numWells == 12)
            return _12_FOLD_STRIP_DATA;
        else if (numWells == 5)
            return PCR_TUBES_DATA;
        else if (numWells == 2)
            return PETRI_DISHES_DATA;
        return "";
    }

    public static String genWellDataFileNameFromCollectorName(String collectorName, Logger logger){
        if (Objects.equals(collectorName, _8_FOLD_STRIP))
            return _8_FOLD_STRIP_DATA;
        else if (Objects.equals(collectorName, _12_FOLD_STRIP))
            return _12_FOLD_STRIP_DATA;
        else if (Objects.equals(collectorName, PCR_TUBES))
            return PCR_TUBES_DATA;
        else if (Objects.equals(collectorName, PETRI_DISHES))
            return PETRI_DISHES_DATA;
        else if (Objects.equals(collectorName, _96_WELL_PLATE))
            return _96_WELL_PLATE_DATA;
        logger.warn("Provided collector doesn't much any of the available collectors!");
        return "";
    }
}
