package com.edwards.orm.database;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public record DatabaseConfiguration(String url, String username, String password) {
    public static DatabaseConfiguration fromProperties(String propertiesFile) throws IOException {
        try (InputStream inputStream = new FileInputStream(propertiesFile)) {
            Properties properties = new Properties();
            properties.load(inputStream);

            String url = properties.getProperty("database.url");
            String username = properties.getProperty("database.username");
            String password = properties.getProperty("database.password");

            return new DatabaseConfiguration(
                    url,
                    username,
                    password
            );
        }
    }
}