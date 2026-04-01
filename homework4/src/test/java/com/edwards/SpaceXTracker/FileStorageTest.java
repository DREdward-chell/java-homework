package com.edwards.SpaceXTracker;

import com.edwards.SpaceXTracker.cache.FileStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

public class FileStorageTest {

    @TempDir
    Path tempDir;

    private FileStorage storage(Duration ttl) {
        return new FileStorage(tempDir.toString(), ttl);
    }

    private FileStorage storage() {
        return storage(Duration.ofMinutes(5));
    }

    @Test
    public void saveAndLoadReturnsData() {
        FileStorage fs = storage();
        fs.save("test.json", "{\"name\":\"hello\"}");
        String loaded = fs.load("test.json");
        assertEquals("{\"name\":\"hello\"}", loaded);
    }

    @Test
    public void loadReturnsNullWhenFileDoesNotExist() {
        FileStorage fs = storage();
        assertNull(fs.load("nonexistent.json"));
    }

    @Test
    public void loadReturnsNullWhenTtlExpired() {
        FileStorage fs = storage(Duration.ZERO);
        fs.save("expired.json", "{\"expired\":true}");
        assertNull(fs.load("expired.json"));
    }

    @Test
    public void isValidReturnsTrueForFreshCache() {
        FileStorage fs = storage();
        fs.save("fresh.json", "{\"ok\":true}");
        assertTrue(fs.isValid("fresh.json"));
    }

    @Test
    public void isValidReturnsFalseForExpiredCache() {
        FileStorage fs = storage(Duration.ZERO);
        fs.save("old.json", "{\"ok\":true}");
        assertFalse(fs.isValid("old.json"));
    }

    @Test
    public void clearAllDeletesEverything() {
        FileStorage fs = storage();
        fs.save("a.json", "{\"a\":1}");
        fs.save("b.json", "{\"b\":2}");
        fs.clearAll();
        assertNull(fs.load("a.json"));
        assertNull(fs.load("b.json"));
    }

    @Test
    public void overwriteExistingFile() {
        FileStorage fs = storage();
        fs.save("key.json", "{\"v\":1}");
        fs.save("key.json", "{\"v\":2}");
        assertEquals("{\"v\":2}", fs.load("key.json"));
    }

    @Test
    public void getTimestampReturnsCorrectTime() {
        FileStorage fs = storage();
        Instant before = Instant.now().minusSeconds(1);
        fs.save("ts.json", "{\"ts\":true}");
        Instant after = Instant.now().plusSeconds(1);

        Instant ts = fs.getTimestamp("ts.json");
        assertNotNull(ts);
        assertTrue(ts.isAfter(before) && ts.isBefore(after));
    }
}
