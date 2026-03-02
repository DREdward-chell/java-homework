package com.edwards.server.manager;

import com.edwards.server.connection.Connection;
import com.edwards.server.connection.FaultyConnection;
import com.edwards.server.connection.StableConnection;

import java.util.Random;

public class DefaultConnectionManager implements ConnectionManager {
    private final int stability = 90;

    @Override
    public Connection getConnection() {
        Random random = new Random();
        int res = 1 + random.nextInt(100);
        if (res <= stability) {
            return new StableConnection();
        }
        return new FaultyConnection(100 - (res - stability) * 2);
    }
}
