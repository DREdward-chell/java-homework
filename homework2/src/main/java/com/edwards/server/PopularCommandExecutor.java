package com.edwards.server;

import com.edwards.server.connection.Connection;
import com.edwards.server.manager.ConnectionManager;

public final class PopularCommandExecutor {
    private final ConnectionManager connectionManager;
    private final int maxAttempts;

    public PopularCommandExecutor(ConnectionManager connectionManager, int maxAttempts) {
        this.connectionManager = connectionManager;
        this.maxAttempts = maxAttempts;
    }

    public void updatePackages() {
        System.out.println(tryExecute("update"));
    }

    public void systemReboot() {
        System.out.println(tryExecute("systemReboot"));
    }

    public void dir() {
        System.out.println(tryExecute("dir /s"));
    }

    public String tryExecute(String command) {
        Connection connection = connectionManager.getConnection();
        ConnectionException connectionException = null;
        for (int i = 0; i < maxAttempts; i++) {
            try {
                return connection.execute(command);
            } catch (ConnectionException e) {
                connectionException = e;
                System.err.println(e.getMessage());
            }
        }
        throw new ConnectionException("Attempts limit exceeded!", connectionException);
    }
}
