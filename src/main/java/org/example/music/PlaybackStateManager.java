package org.example.music;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class PlaybackStateManager {
    private static final String FILE_PATH = "playback.json";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static void saveState(String guildId, PlaybackState state) {
        Map<String, PlaybackState> stateMap = loadStateMap();
        stateMap.put(guildId, state);
        try (FileWriter writer = new FileWriter(FILE_PATH)) {
            gson.toJson(new StateWrapper(stateMap), writer);
            System.out.println("Saved playback state for guild: " + guildId);
        } catch (IOException e) {
            System.err.println("Failed to save playback state for guild " + guildId + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static PlaybackState loadState(String guildId) {
        Map<String, PlaybackState> stateMap = loadStateMap();
        return stateMap.get(guildId);
    }

    public static void clearState(String guildId) {
        Map<String, PlaybackState> stateMap = loadStateMap();
        stateMap.remove(guildId);
        try (FileWriter writer = new FileWriter(FILE_PATH)) {
            gson.toJson(new StateWrapper(stateMap), writer);
            System.out.println("Cleared playback state for guild: " + guildId);
        } catch (IOException e) {
            System.err.println("Failed to clear playback state for guild " + guildId + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static Map<String, PlaybackState> loadStateMap() {
        try (FileReader reader = new FileReader(FILE_PATH)) {
            StateWrapper wrapper = gson.fromJson(reader, StateWrapper.class);
            return wrapper != null && wrapper.guilds != null ? wrapper.guilds : new HashMap<>();
        } catch (IOException e) {
            System.err.println("No saved playback state found or failed to read: " + e.getMessage());
            try (FileReader reader = new FileReader(FILE_PATH)) {
                PlaybackState legacyState = gson.fromJson(reader, PlaybackState.class);
                if (legacyState != null && legacyState.guildId != null) {
                    Map<String, PlaybackState> stateMap = new HashMap<>();
                    stateMap.put(legacyState.guildId, legacyState);
                    saveStateMap(stateMap);
                    return stateMap;
                }
            } catch (IOException ex) {
                System.err.println("Failed to read legacy playback state: " + ex.getMessage());
            }
            return new HashMap<>();
        }
    }

    private static void saveStateMap(Map<String, PlaybackState> stateMap) {
        try (FileWriter writer = new FileWriter(FILE_PATH)) {
            gson.toJson(new StateWrapper(stateMap), writer);
        } catch (IOException e) {
            System.err.println("Failed to save state map: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static class StateWrapper {
        Map<String, PlaybackState> guilds;

        StateWrapper(Map<String, PlaybackState> guilds) {
            this.guilds = guilds;
        }
    }
}