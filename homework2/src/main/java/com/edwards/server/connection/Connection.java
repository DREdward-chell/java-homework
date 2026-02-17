package com.edwards.server.connection;

import com.edwards.server.ConnectionException;

public interface Connection extends AutoCloseable {
    String execute(String command);
}
