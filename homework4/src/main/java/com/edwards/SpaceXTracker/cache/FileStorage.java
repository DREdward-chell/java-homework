package com.edwards.SpaceXTracker.cache;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import org.springframework.stereotype.Component;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * File-based cache with TTL support.
 * Each cached response is stored as a JSON file under the cache directory.
 * Timestamps are tracked in a separate cache_meta.json file.
 */
@Component
public class FileStorage {

    private static final String META_FILE = "cache_meta.json";
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(5);

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Instant.class, (JsonSerializer<Instant>) (src, type, ctx) ->
                    new JsonPrimitive(src.toEpochMilli()))
            .registerTypeAdapter(Instant.class, (JsonDeserializer<Instant>) (json, type, ctx) ->
                    Instant.ofEpochMilli(json.getAsLong()))
            .create();

    private final String cacheDir;
    private final Duration ttl;

    public FileStorage() {
        this.cacheDir = "cache";
        this.ttl = DEFAULT_TTL;
        ensureCacheDir();
    }

    public FileStorage(String cacheDir, Duration ttl) {
        this.cacheDir = cacheDir;
        this.ttl = ttl;
        ensureCacheDir();
    }

    private void ensureCacheDir() {
        File dir = new File(cacheDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    /**
     * Save raw JSON string to cache file and update the meta timestamp.
     */
    public void save(String cacheKey, String jsonData) {
        File file = cacheFile(cacheKey);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(jsonData);
        } catch (IOException e) {
            System.err.println("[cache] Failed to write cache for key: " + cacheKey);
            return;
        }

        Map<String, Instant> meta = loadMeta();
        meta.put(cacheKey, Instant.now());
        saveMeta(meta);
    }

    /**
     * Load cached JSON string. Returns null if file doesn't exist or TTL has expired.
     */
    public String load(String cacheKey) {
        File file = cacheFile(cacheKey);
        if (!file.exists()) {
            return null;
        }

        Map<String, Instant> meta = loadMeta();
        Instant savedAt = meta.get(cacheKey);
        if (savedAt != null && Duration.between(savedAt, Instant.now()).compareTo(ttl) > 0) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            System.err.println("[cache] Failed to read cache for key: " + cacheKey);
            return null;
        }
        return sb.toString();
    }

    /**
     * Returns true if a valid (non-expired) cache entry exists for this key.
     */
    public boolean isValid(String cacheKey) {
        File file = cacheFile(cacheKey);
        if (!file.exists()) {
            return false;
        }
        Map<String, Instant> meta = loadMeta();
        Instant savedAt = meta.get(cacheKey);
        if (savedAt == null) {
            return false;
        }
        return Duration.between(savedAt, Instant.now()).compareTo(ttl) <= 0;
    }

    /**
     * Delete all cache files and meta.
     */
    public void clearAll() {
        File dir = new File(cacheDir);
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                f.delete();
            }
        }
    }

    /**
     * Get the timestamp when a key was cached (for display in fallback messages).
     */
    public Instant getTimestamp(String cacheKey) {
        Map<String, Instant> meta = loadMeta();
        return meta.get(cacheKey);
    }

    private File cacheFile(String cacheKey) {
        return Path.of(cacheDir, cacheKey).toFile();
    }

    private File metaFile() {
        return Path.of(cacheDir, META_FILE).toFile();
    }

    private Map<String, Instant> loadMeta() {
        File file = metaFile();
        if (!file.exists()) {
            return new HashMap<>();
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            Type type = new TypeToken<Map<String, Instant>>() {}.getType();
            Map<String, Instant> meta = GSON.fromJson(reader, type);
            return meta != null ? meta : new HashMap<>();
        } catch (IOException e) {
            return new HashMap<>();
        }
    }

    private void saveMeta(Map<String, Instant> meta) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(metaFile()))) {
            writer.write(GSON.toJson(meta));
        } catch (IOException e) {
            System.err.println("[cache] Failed to write cache meta");
        }
    }
}
