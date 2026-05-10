package com.edwards;

import com.edwards.csvparser.CsvParser;
import com.edwards.demo.Address;
import com.edwards.demo.Course;
import com.edwards.demo.Employee;
import com.edwards.demo.Person;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class Main {

    public static void main(String[] args) throws Exception {
        Path tmp = Files.createTempDirectory("csvparser-demo");
        System.out.println("Demo working directory: " + tmp);

        CsvParser parser = new CsvParser();

        runPersonDemo(parser, tmp);
        runEmployeeDemo(parser, tmp);
        runCourseDemo(parser, tmp);
    }

    private static void runPersonDemo(CsvParser parser, Path dir) throws Exception {
        System.out.println("\n=== People (plain / gzip / zip round-trip) ===");
        List<Person> people = List.of(
                new Person(1, "Ivan Ivanov", 25),
                new Person(2, "Maria Petrova", 30)
        );

        Path csv = dir.resolve("people.csv");
        Path gz  = dir.resolve("people.csv.gz");
        Path zip = dir.resolve("people.zip");

        parser.saveToCsv(csv.toString(), people, Person.class);
        parser.saveToCsv(gz.toString(),  people, Person.class);
        parser.saveToCsv(zip.toString(), people, Person.class);

        System.out.println("plain  -> " + Files.readString(csv).strip());
        System.out.println("from csv: " + parser.parseFromCsv(csv.toString(), Person.class));
        System.out.println("from gz:  " + parser.parseFromCsv(gz.toString(),  Person.class));
        System.out.println("from zip: " + parser.parseFromCsv(zip.toString(), Person.class));
    }

    private static void runEmployeeDemo(CsvParser parser, Path dir) throws Exception {
        System.out.println("\n=== Employees (nested Address) ===");
        List<Employee> employees = List.of(
                new Employee(1, "Ivan Ivanov",   new Address("Lenina 10", 42)),
                new Employee(2, "Maria Petrova", new Address("Pushkina 5", 7))
        );

        Path csv = dir.resolve("employees.csv");
        parser.saveToCsv(csv.toString(), employees, Employee.class);
        System.out.println(Files.readString(csv).strip());
        System.out.println("parsed: " + parser.parseFromCsv(csv.toString(), Employee.class));
    }

    private static void runCourseDemo(CsvParser parser, Path dir) throws Exception {
        System.out.println("\n=== Courses (List fields) ===");
        List<Course> courses = List.of(
                new Course("Java Basics",   List.of("oop", "collections", "streams"), List.of("Gandalf", "Frodo")),
                new Course("Spring Basics", List.of("spring", "boot", "web"),         List.of("Arathorn", "Legolas"))
        );

        Path csv = dir.resolve("courses.csv");
        parser.saveToCsv(csv.toString(), courses, Course.class);
        System.out.println(Files.readString(csv).strip());
        System.out.println("parsed: " + parser.parseFromCsv(csv.toString(), Course.class));
    }
}
