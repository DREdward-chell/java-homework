import com.edwards.orm.core.OrmException;
import com.edwards.orm.database.Database;
import com.edwards.orm.repository.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import support.Book;
import support.H2Support;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EntityManagerTest {

    private Database db;
    private EntityManager em;

    @BeforeEach
    void setUp() {
        db = H2Support.freshDatabase();
        em = new EntityManager(db);
        em.createTable(Book.class);
    }

    @AfterEach
    void tearDown() throws SQLException {
        db.close();
    }

    private static Book sample(String title, String author, int year) {
        return new Book(title, author, year, true, 4.0, LocalDate.of(year, 1, 1));
    }

    @Test
    void saveAssignsGeneratedId() {
        Book b = sample("Dune", "Herbert", 1965);
        Long id = em.save(b);
        assertNotNull(id);
        assertEquals(id, b.getId());
    }

    @Test
    void findByIdReturnsSavedEntity() {
        Book b = sample("Dune", "Herbert", 1965);
        Long id = em.save(b);

        Optional<Book> loaded = em.findById(Book.class, id);
        assertTrue(loaded.isPresent());
        assertEquals("Dune", loaded.get().getTitle());
        assertEquals("Herbert", loaded.get().getAuthor());
        assertEquals(1965, loaded.get().getPublicationYear());
        assertTrue(loaded.get().isAvailable());
        assertEquals(LocalDate.of(1965, 1, 1), loaded.get().getReleaseDate());
    }

    @Test
    void findByIdMissingReturnsEmpty() {
        assertTrue(em.findById(Book.class, 999L).isEmpty());
    }

    @Test
    void findAllReturnsAllRows() {
        em.save(sample("A", "x", 2001));
        em.save(sample("B", "x", 2002));
        em.save(sample("C", "y", 2003));

        List<Book> all = em.findAll(Book.class);
        assertEquals(3, all.size());
    }

    @Test
    void updateChangesColumns() {
        Book b = sample("Dune", "Herbert", 1965);
        em.save(b);
        b.setTitle("Dune Messiah");
        b.setAvailable(false);

        int affected = em.update(b);
        assertEquals(1, affected);

        Book reloaded = em.findById(Book.class, b.getId()).orElseThrow();
        assertEquals("Dune Messiah", reloaded.getTitle());
        assertFalse(reloaded.isAvailable());
    }

    @Test
    void deleteRemovesRow() {
        Book b = sample("Dune", "Herbert", 1965);
        em.save(b);
        int affected = em.delete(b);
        assertEquals(1, affected);
        assertTrue(em.findById(Book.class, b.getId()).isEmpty());
    }

    @Test
    void deleteByIdNonExistentReturnsZero() {
        assertEquals(0, em.deleteById(Book.class, 12345L));
    }

    @Test
    void saveAllInsertsBatchAndAssignsIds() {
        List<Book> batch = List.of(
                sample("A", "x", 2001),
                sample("B", "x", 2002),
                sample("C", "y", 2003)
        );
        em.saveAll(batch);
        for (Book b : batch) assertNotNull(b.getId());
        assertEquals(3, em.count(Book.class));
    }

    @Test
    void saveAllRollsBackWhenNullableViolated() {
        Book good = sample("A", "x", 2001);
        Book bad = sample("B", null, 2002);
        bad.setAuthor(null);
        long before = em.count(Book.class);
        assertThrows(OrmException.class, () -> em.saveAll(List.of(good, bad)));
        assertEquals(before, em.count(Book.class));
    }

    @Test
    void findAllWhereFiltersByField() {
        em.save(sample("A", "x", 2001));
        em.save(sample("B", "x", 2002));
        em.save(sample("C", "y", 2003));

        List<Book> hits = em.findAllWhere(Book.class, "author", "x");
        assertEquals(2, hits.size());
        for (Book b : hits) assertEquals("x", b.getAuthor());
    }

    @Test
    void findOneWhereSingleHit() {
        em.save(sample("Solo", "z", 2010));
        Optional<Book> hit = em.findOneWhere(Book.class, "title", "Solo");
        assertTrue(hit.isPresent());
    }

    @Test
    void findOneWhereThrowsOnMultiple() {
        em.save(sample("A", "x", 2001));
        em.save(sample("B", "x", 2002));
        assertThrows(OrmException.class, () -> em.findOneWhere(Book.class, "author", "x"));
    }

    @Test
    void findAllWhereRejectsIncompatibleValueType() {
        assertThrows(OrmException.class, () ->
                em.findAllWhere(Book.class, "publicationYear", "not-a-number"));
    }

    @Test
    void countMatchesRows() {
        assertEquals(0, em.count(Book.class));
        em.save(sample("A", "x", 2001));
        em.save(sample("B", "x", 2002));
        assertEquals(2, em.count(Book.class));
    }

    @Test
    void existsById() {
        Book b = sample("A", "x", 2001);
        em.save(b);
        assertTrue(em.existsById(Book.class, b.getId()));
        assertFalse(em.existsById(Book.class, b.getId() + 1000));
    }

    @Test
    void notNullableViolationOnSave() {
        Book b = sample("A", null, 2001);
        b.setAuthor(null);
        assertThrows(OrmException.class, () -> em.save(b));
    }

    @Test
    void localDateRoundTrips() {
        Book b = sample("A", "x", 2024);
        em.save(b);
        Book reloaded = em.findById(Book.class, b.getId()).orElseThrow();
        assertEquals(LocalDate.of(2024, 1, 1), reloaded.getReleaseDate());
    }
}
