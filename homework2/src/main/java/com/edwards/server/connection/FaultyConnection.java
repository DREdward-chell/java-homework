package com.edwards.server.connection;

import com.edwards.server.ConnectionException;

import java.util.Random;

public class FaultyConnection implements Connection {
    private final int stability;

    public FaultyConnection(int stability) {
        this.stability = stability;
    }

    @Override
    public String execute(String command) throws ConnectionException {
        Random random = new Random();
        var res = 1 + random.nextInt(100);
        if (res <= stability) {
            return "";
        }
        throw new ConnectionException("Connection timed out");
    }

    @Override
    public void close() {
        System.out.println("Connection closed");
    }
}
