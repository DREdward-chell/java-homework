package support;

import com.edwards.orm.database.Database;
import com.edwards.orm.database.DatabaseConfiguration;

import java.util.concurrent.atomic.AtomicInteger;

public final class H2Support {
    private static final AtomicInteger COUNTER = new AtomicInteger();

    private H2Support() {}

    public static Database freshDatabase() {
        String name = "miniorm_test_" + COUNTER.incrementAndGet() + "_" + System.nanoTime();
        String url = "jdbc:h2:mem:" + name + ";DB_CLOSE_DELAY=-1";
        return new Database(new DatabaseConfiguration(url, "sa", ""));
    }
}
