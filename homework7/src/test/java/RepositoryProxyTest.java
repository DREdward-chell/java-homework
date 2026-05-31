import com.edwards.orm.annotation.Param;
import com.edwards.orm.annotation.Query;
import com.edwards.orm.core.RepositoryProxy;
import com.edwards.orm.database.Database;
import com.edwards.orm.repository.EntityManager;
import com.edwards.orm.repository.Repository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import support.Book;
import support.H2Support;

import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RepositoryProxyTest {

    public interface BookRepository extends Repository<Book> {
        @Query("SELECT * FROM :tableName WHERE id = :id")
        Optional<Book> findById(@Param("id") Long id);

        @Query("SELECT * FROM :tableName")
        List<Book> findAll();

        @Query("SELECT * FROM :tableName WHERE author = :author")
        List<Book> findByAuthor(@Param("author") String author);

        @Query("SELECT * FROM :tableName WHERE title = :title")
        Book findOneByTitle(@Param("title") String title);

        @Query("SELECT title FROM :tableName")
        List<String> allTitles();

        @Query("SELECT COUNT(*) FROM :tableName")
        long total();

        @Query("SELECT 1 FROM :tableName WHERE id = :id LIMIT 1")
        boolean existsById(@Param("id") Long id);

        @Query("DELETE FROM :tableName WHERE id = :id")
        int deleteById(@Param("id") Long id);

        @Query("UPDATE :tableName SET available = :available WHERE id = :id")
        void setAvailability(@Param("id") Long id, @Param("available") boolean available);

        default Book firstByAuthor(String author) {
            List<Book> hits = findByAuthor(author);
            return hits.isEmpty() ? null : hits.get(0);
        }

        default void makeUnavailable(Book book) {
            setAvailability(book.getId(), false);
        }
    }

    private Database db;
    private EntityManager em;
    private BookRepository repo;

    @BeforeEach
    void setUp() {
        db = H2Support.freshDatabase();
        em = new EntityManager(db);
        em.createTable(Book.class);
        repo = (BookRepository) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[]{BookRepository.class},
                new RepositoryProxy(db, BookRepository.class)
        );
    }

    @AfterEach
    void tearDown() throws SQLException {
        db.close();
    }

    private Book seed(String title, String author, int year) {
        Book b = new Book(title, author, year, true, 4.0, LocalDate.of(year, 1, 1));
        em.save(b);
        return b;
    }

    @Test
    void optionalEntityMapping() {
        Book b = seed("Dune", "Herbert", 1965);
        Optional<Book> hit = repo.findById(b.getId());
        assertTrue(hit.isPresent());
        assertEquals("Dune", hit.get().getTitle());
        assertEquals(LocalDate.of(1965, 1, 1), hit.get().getReleaseDate());
    }

    @Test
    void optionalEmptyWhenMissing() {
        assertTrue(repo.findById(999L).isEmpty());
    }

    @Test
    void listEntityMapping() {
        seed("A", "x", 2001);
        seed("B", "x", 2002);
        seed("C", "y", 2003);
        List<Book> rows = repo.findAll();
        assertEquals(3, rows.size());
    }

    @Test
    void listFilteredByParameter() {
        seed("A", "x", 2001);
        seed("B", "x", 2002);
        seed("C", "y", 2003);
        List<Book> rows = repo.findByAuthor("x");
        assertEquals(2, rows.size());
    }

    @Test
    void singleEntityReturnsFirstRow() {
        Book b = seed("Solo", "z", 2010);
        Book result = repo.findOneByTitle("Solo");
        assertNotNull(result);
        assertEquals(b.getId(), result.getId());
    }

    @Test
    void scalarListMapping() {
        seed("A", "x", 2001);
        seed("B", "x", 2002);
        List<String> titles = repo.allTitles();
        assertEquals(2, titles.size());
        assertTrue(titles.contains("A"));
        assertTrue(titles.contains("B"));
    }

    @Test
    void scalarCount() {
        seed("A", "x", 2001);
        seed("B", "x", 2002);
        seed("C", "y", 2003);
        assertEquals(3L, repo.total());
    }

    @Test
    void booleanExists() {
        Book b = seed("A", "x", 2001);
        assertTrue(repo.existsById(b.getId()));
        assertFalse(repo.existsById(b.getId() + 1000));
    }

    @Test
    void updateReturnsAffectedRows() {
        Book b = seed("A", "x", 2001);
        int affected = repo.deleteById(b.getId());
        assertEquals(1, affected);
        assertEquals(0, repo.total());
    }

    @Test
    void defaultMethodComposesQueryMethods() {
        seed("A", "x", 2001);
        seed("B", "x", 2002);
        Book result = repo.firstByAuthor("x");
        assertNotNull(result);
        assertEquals("x", result.getAuthor());
    }

    @Test
    void defaultMethodCanIssueWrites() {
        Book b = seed("A", "x", 2001);
        repo.makeUnavailable(b);
        assertFalse(em.findById(Book.class, b.getId()).orElseThrow().isAvailable());
    }

    @Test
    void voidUpdateExecutes() {
        Book b = seed("A", "x", 2001);
        repo.setAvailability(b.getId(), false);
        Book reloaded = em.findById(Book.class, b.getId()).orElseThrow();
        assertFalse(reloaded.isAvailable());
    }
}
