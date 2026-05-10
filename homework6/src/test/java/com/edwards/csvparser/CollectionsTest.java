package com.edwards.csvparser;

import com.edwards.csvparser.fixtures.Course;
import com.edwards.csvparser.fixtures.Tagged;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CollectionsTest {

    private final CsvParser parser = new CsvParser();

    @Test
    void customAndDefaultDelimiters(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("courses.csv");
        List<Course> input = List.of(
                new Course("Java",   List.of("oop","streams"),     List.of("Gandalf","Frodo")),
                new Course("Spring", List.of("spring","boot"),     List.of("Arathorn","Legolas"))
        );

        parser.saveToCsv(file.toString(), input, Course.class);

        assertEquals("""
                name,tags,authors
                Java,oop;streams,Gandalf|Frodo
                Spring,spring;boot,Arathorn|Legolas
                """, Files.readString(file));

        assertEquals(input, parser.parseFromCsv(file.toString(), Course.class));
    }

    @Test
    void defaultPipeDelimiter(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("tagged.csv");
        Tagged input = new Tagged("a", List.of("x", "y", "z"));

        parser.saveToCsv(file.toString(), List.of(input), Tagged.class);

        assertTrue(Files.readString(file).contains("a,x|y|z\n"));
        assertEquals(List.of(input), parser.parseFromCsv(file.toString(), Tagged.class));
    }

    @Test
    void emptyListRoundTrips(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("empty-list.csv");
        Tagged input = new Tagged("nothing", List.of());

        parser.saveToCsv(file.toString(), List.of(input), Tagged.class);

        assertEquals("name,tags\nnothing,\n", Files.readString(file));
        assertEquals(List.of(input), parser.parseFromCsv(file.toString(), Tagged.class));
    }
}
