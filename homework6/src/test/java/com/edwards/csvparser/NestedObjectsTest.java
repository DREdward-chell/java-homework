package com.edwards.csvparser;

import com.edwards.csvparser.fixtures.Address;
import com.edwards.csvparser.fixtures.City;
import com.edwards.csvparser.fixtures.Country;
import com.edwards.csvparser.fixtures.Employee;
import com.edwards.csvparser.fixtures.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NestedObjectsTest {

    private final CsvParser parser = new CsvParser();

    @Test
    void singleLevelNesting(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("employees.csv");
        List<Employee> input = List.of(
                new Employee(1, "Ivan",  new Address("Lenina 10", 42)),
                new Employee(2, "Maria", new Address("Pushkina 5", 7))
        );

        parser.saveToCsv(file.toString(), input, Employee.class);

        assertEquals("""
                id,full_name,address.street,address.flat
                1,Ivan,Lenina 10,42
                2,Maria,Pushkina 5,7
                """, Files.readString(file));

        assertEquals(input, parser.parseFromCsv(file.toString(), Employee.class));
    }

    @Test
    void deepNesting(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("users.csv");
        List<User> input = List.of(
                new User(10L, new City("Moscow", new Country("RU")), List.of(1, 2, 3)),
                new User(20L, new City("Berlin", new Country("DE")), List.of())
        );

        parser.saveToCsv(file.toString(), input, User.class);

        String content = Files.readString(file);
        assertTrue(content.startsWith("id,city.name,city.country.code,scores\n"), content);
        assertEquals(input, parser.parseFromCsv(file.toString(), User.class));
    }

    @Test
    void nullNestedWritesEmptyCells(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("null-nested.csv");
        Employee employee = new Employee(1, "Ivan", null);

        parser.saveToCsv(file.toString(), List.of(employee), Employee.class);

        assertEquals("""
                id,full_name,address.street,address.flat
                1,Ivan,,
                """, Files.readString(file));
    }
}
