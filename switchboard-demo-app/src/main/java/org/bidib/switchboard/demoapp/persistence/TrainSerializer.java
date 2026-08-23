package org.bidib.switchboard.demoapp.persistence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.bidib.switchboard.component.model.Train;
import org.bidib.switchboard.component.model.TrainListModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Handles persistence of train data to JSON files.
 */
public class TrainSerializer {

    private static final Logger LOG = LoggerFactory.getLogger(TrainSerializer.class);

    private static final Path DATA_DIR = Paths.get(System.getProperty("user.home"), "switchboard-demo-1");

    private static final ObjectMapper MAPPER = JsonMapper.builder()
        .enable(SerializationFeature.INDENT_OUTPUT)
        .build();

    /**
     * Loads trains from the given file path into the model.
     * If path is null, loads from the default location.
     */
    public void loadInto(TrainListModel model, Path path) {
        List<TrainData> data = load(path);
        List<Train> trains = new ArrayList<>();
        for (TrainData td : data) {
            trains.add(new Train(td.id, td.name, null));
        }
        model.setTrains(trains);
    }

    /**
     * Saves the current trains from the model to the given file path.
     * Returns the path the file was saved to.
     */
    public Path saveFrom(TrainListModel model, Path path) {
        List<TrainData> data = new ArrayList<>();
        for (Train train : model.getTrains()) {
            TrainData td = new TrainData();
            td.id = train.getId();
            td.name = train.getName();
            data.add(td);
        }
        return save(data, path);
    }

    /**
     * Resolves a trains file path relative to the layout file.
     * If trainsFile is null, returns the default path.
     */
    public static Path resolveTrainsPath(Path layoutFile, String trainsFile) {
        if (trainsFile != null && !trainsFile.isBlank()) {
            Path layoutDir = layoutFile.getParent();
            if (layoutDir != null) {
                return layoutDir.resolve(trainsFile);
            }
            return Paths.get(trainsFile);
        }
        return DATA_DIR.resolve("trains.json");
    }

    // --- DTO ---

    static class TrainData {
        public String id;
        public String name;
    }

    // --- I/O ---

    private static List<TrainData> load(Path path) {
        if (path != null && path.toFile().exists()) {
            try {
                LOG.info("Loading trains from {}", path);
                return MAPPER.readValue(path.toFile(), new TypeReference<List<TrainData>>() {});
            }
            catch (Exception e) {
                LOG.warn("Failed to load trains from {}", path, e);
            }
        }
        return List.of();
    }

    private Path save(List<TrainData> data, Path path) {
        Path target = path != null ? path : DATA_DIR.resolve("trains.json");
        try {
            Files.createDirectories(target.getParent());
            MAPPER.writeValue(target.toFile(), data);
            LOG.info("Saved trains to {}", target);
        }
        catch (IOException e) {
            LOG.warn("Failed to save trains to {}", target, e);
        }
        return target;
    }
}
