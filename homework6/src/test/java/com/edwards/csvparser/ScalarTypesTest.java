package com.edwards.csvparser;

import com.edwards.csvparser.fixtures.AllScalars;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ScalarTypesTest {

    private final CsvParser parser = new CsvParser();

    @Test
    void allScalarTypesRoundTrip(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("scalars.csv");
        List<AllScalars> input = List.of(
                new AllScalars(-7, 42, 9_000_000_000L, -1L, 3.14, -2.5, true, false, "hello"),
                new AllScalars(0, 0, 0L, 0L, 0.0, 0.0, false, true, "")
        );

        parser.saveToCsv(file.toString(), input, AllScalars.class);
        assertEquals(input, parser.parseFromCsv(file.toString(), AllScalars.class));
    }

    @Test
    void emptyCellOnWrappers(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("empties.csv");
        Files.writeString(file,
                """
                        i,iBoxed,l,lBoxed,d,dBoxed,b,bBoxed,s
                        1,,2,,3.0,,true,,
                        """);

        List<AllScalars> result = parser.parseFromCsv(file.toString(), AllScalars.class);
        assertEquals(1, result.size());
        AllScalars row = result.getFirst();
        assertNull(row.getIBoxed());
        assertNull(row.getLBoxed());
        assertNull(row.getDBoxed());
        assertNull(row.getBBoxed());
        assertEquals("", row.getS());
    }

    @Test
    void emptyCellOnPrimitiveFails(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("bad.csv");
        Files.writeString(file,
                """
                        i,iBoxed,l,lBoxed,d,dBoxed,b,bBoxed,s
                        ,,,,,,,,\s
                        """);

        CsvParserException ex = assertThrows(CsvParserException.class,
                () -> parser.parseFromCsv(file.toString(), AllScalars.class));
        assertTrue(ex.getMessage().toLowerCase().contains("primitive"), ex.getMessage());
    }

    @Test
    void badNumericReportsValue(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("bad-int.csv");
        Files.writeString(file,
                """
                        i,iBoxed,l,lBoxed,d,dBoxed,b,bBoxed,s
                        abc,1,2,3,4.0,5.0,true,false,x
                        """);

        CsvParserException ex = assertThrows(CsvParserException.class,
                () -> parser.parseFromCsv(file.toString(), AllScalars.class));
        assertTrue(ex.getMessage().contains("abc"), ex.getMessage());
    }
}
