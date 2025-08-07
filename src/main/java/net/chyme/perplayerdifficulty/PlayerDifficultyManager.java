package net.chyme.perplayerdifficulty;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.Difficulty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerDifficultyManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("per-player-difficulty");
    private static final Map<UUID, Difficulty> playerDifficulties = new HashMap<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static File dataFile;
    private static MinecraftServer server;

    public static void initialize(MinecraftServer minecraftServer) {
        server = minecraftServer;
        dataFile = new File(server.getSavePath(net.minecraft.util.WorldSavePath.ROOT).toFile(), "player_difficulties.json");
        loadData();
    }

    public static Difficulty getServerDifficulty() {
        return server.getOverworld().getDifficulty();
    }

    public static Difficulty getPlayerDifficulty(ServerPlayerEntity player) {
        return playerDifficulties.getOrDefault(player.getUuid(), getServerDifficulty());
    }

    public static void setPlayerDifficulty(ServerPlayerEntity player, Difficulty difficulty) {
        playerDifficulties.put(player.getUuid(), difficulty);
        saveData();
        LOGGER.info("Set difficulty for player {} to {}", player.getName().getString(), difficulty.getName());
    }


    private static void loadData() {
        if (!dataFile.exists()) {
            LOGGER.info("No existing player difficulty data found, starting fresh");
            return;
        }

        try (FileReader reader = new FileReader(dataFile)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

            for (String uuidString : json.keySet()) {
                try {
                    UUID uuid = UUID.fromString(uuidString);
                    String difficultyName = json.get(uuidString).getAsString();
                    Difficulty difficulty = Difficulty.byName(difficultyName);

                    if (difficulty != null) {
                        playerDifficulties.put(uuid, difficulty);
                    }
                } catch (Exception e) {
                    LOGGER.warn("Failed to parse difficulty data for UUID: {}", uuidString, e);
                }
            }

            LOGGER.info("Loaded difficulty settings for {} players", playerDifficulties.size());
        } catch (IOException e) {
            LOGGER.error("Failed to load player difficulty data", e);
        }
    }

    public static void saveData() {
        if (dataFile == null) return;

        try {
            dataFile.getParentFile().mkdirs();

            JsonObject json = new JsonObject();
            for (Map.Entry<UUID, Difficulty> entry : playerDifficulties.entrySet()) {
                json.addProperty(entry.getKey().toString(), entry.getValue().getName());
            }

            try (FileWriter writer = new FileWriter(dataFile)) {
                GSON.toJson(json, writer);
            }

            LOGGER.debug("Saved difficulty settings for {} players", playerDifficulties.size());
        } catch (IOException e) {
            LOGGER.error("Failed to save player difficulty data", e);
        }
    }

    public static Map<UUID, Difficulty> getAllPlayerDifficulties() {
        return new HashMap<>(playerDifficulties);
    }
}