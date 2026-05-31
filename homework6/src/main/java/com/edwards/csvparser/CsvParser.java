package com.edwards.csvparser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CsvParser {

    private static final String SEPARATOR = ",";

    public <T> List<T> parseFromCsv(String filename, Class<T> klass) {
        List<FieldPath> paths = Schema.resolve(klass);
        Constructor<T> ctor = noArgConstructor(klass);

        try (InputStream raw = Streams.openInput(filename);
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(raw, StandardCharsets.UTF_8))) {

            String headerLine = reader.readLine();
            if (headerLine == null) {
                return new ArrayList<>();
            }
            int[] columns = matchColumns(paths, headerLine.split(SEPARATOR, -1));

            List<T> result = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) continue;
                String[] cells = line.split(SEPARATOR, -1);
                T instance = newInstance(ctor);
                for (int i = 0; i < paths.size(); i++) {
                    int col = columns[i];
                    if (col >= cells.length) {
                        throw new CsvParserException(
                                "Row has fewer columns than header: '" + line + "'");
                    }
                    paths.get(i).write(instance, cells[col]);
                }
                result.add(instance);
            }
            return result;
        } catch (IOException e) {
            throw new CsvParserException("I/O error reading " + filename, e);
        }
    }

    public <T> void saveToCsv(String filename, Collection<T> collection, Class<T> klass) {
        List<FieldPath> paths = Schema.resolve(klass);

        try (OutputStream raw = Streams.openOutput(filename);
             BufferedWriter writer = new BufferedWriter(
                     new OutputStreamWriter(raw, StandardCharsets.UTF_8))) {

            writer.write(paths.stream()
                    .map(FieldPath::header)
                    .collect(Collectors.joining(SEPARATOR)));
            writer.newLine();

            for (T item : collection) {
                StringBuilder row = new StringBuilder();
                for (int i = 0; i < paths.size(); i++) {
                    if (i > 0) row.append(SEPARATOR);
                    row.append(paths.get(i).read(item));
                }
                writer.write(row.toString());
                writer.newLine();
            }
        } catch (IOException e) {
            throw new CsvParserException("I/O error writing " + filename, e);
        }
    }

    private static int[] matchColumns(List<FieldPath> paths, String[] headers) {
        Map<String, Integer> index = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            index.put(headers[i], i);
        }
        int[] columns = new int[paths.size()];
        for (int i = 0; i < paths.size(); i++) {
            Integer col = index.get(paths.get(i).header());
            if (col == null) {
                throw new CsvParserException(
                        "CSV is missing column for field '" + paths.get(i).header() + "'");
            }
            columns[i] = col;
        }
        return columns;
    }

    private static <T> Constructor<T> noArgConstructor(Class<T> klass) {
        try {
            Constructor<T> ctor = klass.getDeclaredConstructor();
            ctor.setAccessible(true);
            return ctor;
        } catch (NoSuchMethodException e) {
            throw new CsvParserException(
                    "Class " + klass.getName() + " must have a no-arg constructor", e);
        }
    }

    private static <T> T newInstance(Constructor<T> ctor) {
        try {
            return ctor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new CsvParserException(
                    "Failed to instantiate " + ctor.getDeclaringClass().getName(), e);
        }
    }
}
