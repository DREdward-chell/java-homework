package com.edwards.csvparser;

import com.edwards.csvparser.fixtures.Person;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BasicRoundTripTest {

    private final CsvParser parser = new CsvParser();

    @Test
    void plainCsvRoundTrip(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("people.csv");
        List<Person> people = List.of(new Person(1, "Ivan", 25), new Person(2, "Maria", 30));

        parser.saveToCsv(file.toString(), people, Person.class);

        assertEquals("id,full_name,age\n1,Ivan,25\n2,Maria,30\n", Files.readString(file));
        assertEquals(people, parser.parseFromCsv(file.toString(), Person.class));
    }

    @Test
    void parsesShuffledHeaderOrder(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("shuffled.csv");
        Files.writeString(file, "age,full_name,id\n25,Ivan,1\n30,Maria,2\n");

        assertEquals(
                List.of(new Person(1, "Ivan", 25), new Person(2, "Maria", 30)),
                parser.parseFromCsv(file.toString(), Person.class));
    }

    @Test
    void extraColumnsAreIgnored(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("extra.csv");
        Files.writeString(file, "id,full_name,age,note\n1,Ivan,25,hello\n");

        assertEquals(List.of(new Person(1, "Ivan", 25)),
                parser.parseFromCsv(file.toString(), Person.class));
    }

    @Test
    void savingEmptyCollectionWritesHeaderOnly(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("empty.csv");
        parser.saveToCsv(file.toString(), List.of(), Person.class);

        assertEquals("id,full_name,age\n", Files.readString(file));
    }

    @Test
    void acceptsAnyCollection(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("set.csv");
        Set<Person> input = new java.util.LinkedHashSet<>(List.of(
                new Person(1, "A", 10), new Person(2, "B", 20)));

        parser.saveToCsv(file.toString(), input, Person.class);

        String text = Files.readString(file);
        assertTrue(text.contains("1,A,10"));
        assertTrue(text.contains("2,B,20"));
    }
}
