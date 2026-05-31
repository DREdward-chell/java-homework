package com.edwards.orm.di;

import com.edwards.orm.database.Database;
import com.edwards.orm.database.DatabaseConfiguration;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class DatabaseRegistry {
    private static volatile DatabaseRegistry instance;

    private DatabaseRegistry() {
    }

    private final Map<String, Database> registry = new HashMap<>();

    private static DatabaseRegistry getInstance() {
        if (instance == null) {
            synchronized (DatabaseRegistry.class) {
                if (instance == null) {
                    instance = new DatabaseRegistry();
                }
            }
        }
        return instance;
    }

    /**
     * Без IoC, моя бд будет одна на всю программу, т.к. я пока не хочу писать свой DI,
     * так что я просто оставлю такую заглушку
     */
    public static Database shadowInject() {
        return getInstance().registry.computeIfAbsent("main", _ -> {
                    try {
                        return new Database(DatabaseConfiguration.fromProperties("orm.properties"));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
        );
    }
}