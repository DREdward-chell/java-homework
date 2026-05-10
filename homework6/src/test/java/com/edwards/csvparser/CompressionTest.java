package com.edwards.csvparser;

import com.edwards.csvparser.fixtures.Person;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import static org.junit.jupiter.api.Assertions.*;

class CompressionTest {

    private final CsvParser parser = new CsvParser();
    private final List<Person> sample = List.of(new Person(1, "Ivan", 25), new Person(2, "Maria", 30));

    @Test
    void gzipDotCsvDotGz(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("people.csv.gz");
        parser.saveToCsv(file.toString(), sample, Person.class);

        try (var gz = new GZIPInputStream(Files.newInputStream(file))) {
            String text = new String(gz.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(text.startsWith("id,full_name,age"));
        }
        assertEquals(sample, parser.parseFromCsv(file.toString(), Person.class));
    }

    @Test
    void gzipDotGzAlias(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("people.gz");
        parser.saveToCsv(file.toString(), sample, Person.class);

        byte[] head = Files.readAllBytes(file);
        assertEquals((byte) 0x1f, head[0]);
        assertEquals((byte) 0x8b, head[1]);
        assertEquals(sample, parser.parseFromCsv(file.toString(), Person.class));
    }

    @Test
    void zipRoundTrip(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("people.zip");
        parser.saveToCsv(file.toString(), sample, Person.class);

        try (var zin = new ZipInputStream(Files.newInputStream(file))) {
            ZipEntry e = zin.getNextEntry();
            assertNotNull(e);
            assertEquals("people.csv", e.getName());

            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            zin.transferTo(buf);
            assertTrue(buf.toString(StandardCharsets.UTF_8).contains("Ivan"));

            assertNull(zin.getNextEntry(), "ZIP should contain exactly one entry");
        }

        assertEquals(sample, parser.parseFromCsv(file.toString(), Person.class));
    }
}
