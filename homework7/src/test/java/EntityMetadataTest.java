import com.edwards.orm.annotation.Column;
import com.edwards.orm.annotation.Id;
import com.edwards.orm.annotation.Table;
import com.edwards.orm.core.EntityMetadata;
import com.edwards.orm.core.OrmException;
import org.junit.jupiter.api.Test;
import support.Book;

import java.sql.Date;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EntityMetadataTest {

    @Table
    static class NoIdEntity {
        @Column private String name;
        public NoIdEntity() {}
    }

    static class NoTableEntity {
        @Id private Long id;
        public NoTableEntity() {}
    }

    @Table(name = "renamed")
    static class CustomName {
        @Id private Long id;
        @Column(name = "renamed_col") private String value;
        public CustomName() {}
    }

    @Test
    void parsesTableAndColumns() {
        EntityMetadata m = EntityMetadata.of(Book.class);
        assertEquals("books", m.tableName());
        assertEquals("id", m.id().columnName());
        assertEquals(6, m.columns().size());
        assertEquals(7, m.allFields().size());
    }

    @Test
    void throwsWhenTableMissing() {
        assertThrows(OrmException.class, () -> EntityMetadata.of(NoTableEntity.class));
    }

    @Test
    void throwsWhenIdMissing() {
        assertThrows(OrmException.class, () -> EntityMetadata.of(NoIdEntity.class));
    }

    @Test
    void respectsCustomColumnNames() {
        EntityMetadata m = EntityMetadata.of(CustomName.class);
        assertEquals("renamed", m.tableName());
        assertEquals("renamed_col", m.byFieldName("value").columnName());
    }

    @Test
    void columnValuesSkipsIdAndConvertsLocalDate() {
        Book b = new Book("t", "a", 2024, true, 4.5, LocalDate.of(2024, 1, 2));
        EntityMetadata m = EntityMetadata.of(Book.class);
        Object[] values = m.columnValues(b);
        assertEquals(6, values.length);
        assertEquals("t", values[0]);
        assertTrue(values[5] instanceof Date);
        assertEquals(LocalDate.of(2024, 1, 2), ((Date) values[5]).toLocalDate());
    }

    @Test
    void fromRowRebuildsEntityAndConvertsDate() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", 7L);
        row.put("title", "x");
        row.put("author", "y");
        row.put("publicationYear", 2020);
        row.put("available", true);
        row.put("rating", 3.5);
        row.put("releaseDate", Date.valueOf(LocalDate.of(2020, 5, 1)));

        Book b = EntityMetadata.of(Book.class).fromRow(row);
        assertEquals(7L, b.getId());
        assertEquals("x", b.getTitle());
        assertEquals(LocalDate.of(2020, 5, 1), b.getReleaseDate());
    }

    @Test
    void checkNotNullableEnforcesColumns() {
        Book b = new Book(null, "a", 2024, true, 4.0, LocalDate.now());
        assertThrows(OrmException.class, () -> EntityMetadata.of(Book.class).checkNotNullable(b));
    }

    @Test
    void isCompatibleValue() {
        EntityMetadata m = EntityMetadata.of(Book.class);
        assertTrue(m.isCompatibleValue("title", "anything"));
        assertTrue(m.isCompatibleValue("title", null));
        assertFalse(m.isCompatibleValue("publicationYear", "not a number"));
        assertTrue(m.isCompatibleValue("publicationYear", 1999));
    }

    @Test
    void caches() {
        EntityMetadata first = EntityMetadata.of(Book.class);
        EntityMetadata second = EntityMetadata.of(Book.class);
        assertNotNull(first);
        assertEquals(first, second);
    }

    @Test
    void fromRowHandlesNullDate() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", 1L);
        row.put("title", "x");
        row.put("author", "y");
        row.put("publicationYear", 2000);
        row.put("available", false);
        row.put("rating", 0.0);
        row.put("releaseDate", null);
        Book b = EntityMetadata.of(Book.class).fromRow(row);
        assertNull(b.getReleaseDate());
    }
}
