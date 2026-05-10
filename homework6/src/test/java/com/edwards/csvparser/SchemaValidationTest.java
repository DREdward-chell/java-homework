package com.edwards.csvparser;

import com.edwards.csvparser.fixtures.Cyclic;
import com.edwards.csvparser.fixtures.NoDefaultCtor;
import com.edwards.csvparser.fixtures.Person;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchemaValidationTest {

    private final CsvParser parser = new CsvParser();

    @Test
    void rejectsCycles(@TempDir Path dir) {
        CsvParserException ex = assertThrows(CsvParserException.class,
                () -> parser.saveToCsv(dir.resolve("c.csv").toString(), List.of(), Cyclic.class));
        assertTrue(ex.getMessage().toLowerCase().contains("cycl"), ex.getMessage());
    }

    @Test
    void missingColumnReportsName(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("missing.csv");
        Files.writeString(file, "id,age\n1,25\n");

        CsvParserException ex = assertThrows(CsvParserException.class,
                () -> parser.parseFromCsv(file.toString(), Person.class));
        assertTrue(ex.getMessage().contains("full_name"), ex.getMessage());
    }

    @Test
    void missingNoArgCtorReported(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("nodefault.csv");
        Files.writeString(file, "id\n1\n");

        CsvParserException ex = assertThrows(CsvParserException.class,
                () -> parser.parseFromCsv(file.toString(), NoDefaultCtor.class));
        assertTrue(ex.getMessage().toLowerCase().contains("no-arg"), ex.getMessage());
    }

    @Test
    void missingFileFails(@TempDir Path dir) {
        CsvParserException ex = assertThrows(CsvParserException.class,
                () -> parser.parseFromCsv(dir.resolve("does-not-exist.csv").toString(), Person.class));
        assertTrue(ex.getMessage().toLowerCase().contains("i/o"), ex.getMessage());
    }
}
