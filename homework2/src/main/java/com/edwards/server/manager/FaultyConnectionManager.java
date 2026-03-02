package com.edwards.server.manager;

import com.edwards.server.connection.Connection;
import com.edwards.server.connection.FaultyConnection;

import java.util.Random;

public class FaultyConnectionManager implements ConnectionManager {
    @Override
    public Connection getConnection() {
        Random random = new Random();
        var res = 50 + random.nextInt(41);
        return new FaultyConnection(res);
    }
}
