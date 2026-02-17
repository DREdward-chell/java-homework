package com.edwards.server.connection;

public class StableConnection implements Connection {
    public StableConnection() {}

    @Override
    public String execute(String command) {
        return "";
    }

    @Override
    public void close() {
        System.out.println("Connection closed");
    }
}
