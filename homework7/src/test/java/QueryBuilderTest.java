import com.edwards.orm.annotation.Param;
import com.edwards.orm.core.QueryBuilder;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class QueryBuilderTest {

    @SuppressWarnings("unused")
    static class Methods {
        public void byId(@Param("id") Long id) {}
        public void byNameAndAge(@Param("name") String name, @Param("age") int age) {}
        public void noBinding(@Param("id") Long id) {}
    }

    private static Parameter[] paramsOf(String method, Class<?>... types) throws NoSuchMethodException {
        Method m = Methods.class.getDeclaredMethod(method, types);
        return m.getParameters();
    }

    @Test
    void substitutesTableNameInline() throws Exception {
        QueryBuilder.ParsedQuery q = QueryBuilder.queryFromTemplate(
                "SELECT * FROM :tableName WHERE id = :id",
                new Object[]{42L},
                paramsOf("byId", Long.class),
                "books"
        );
        assertEquals("SELECT * FROM books WHERE id = ?", q.sql());
        assertArrayEquals(new Object[]{42L}, q.params());
    }

    @Test
    void preservesPositionalOrderForMultipleParams() throws Exception {
        QueryBuilder.ParsedQuery q = QueryBuilder.queryFromTemplate(
                "SELECT * FROM :tableName WHERE name = :name AND age >= :age",
                new Object[]{"Alice", 21},
                paramsOf("byNameAndAge", String.class, int.class),
                "users"
        );
        assertEquals("SELECT * FROM users WHERE name = ? AND age >= ?", q.sql());
        assertArrayEquals(new Object[]{"Alice", 21}, q.params());
    }

    @Test
    void throwsOnUnboundPlaceholder() throws Exception {
        Parameter[] params = paramsOf("noBinding", Long.class);
        assertThrows(IllegalArgumentException.class, () ->
                QueryBuilder.queryFromTemplate(
                        "SELECT * FROM :tableName WHERE foo = :missing",
                        new Object[]{1L},
                        params,
                        "t"
                )
        );
    }
}
