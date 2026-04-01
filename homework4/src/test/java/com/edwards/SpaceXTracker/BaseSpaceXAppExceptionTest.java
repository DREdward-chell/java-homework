package com.edwards.SpaceXTracker;

import com.edwards.SpaceXTracker.exceptions.BaseSpaceXAppException;
import com.edwards.SpaceXTracker.exceptions.SpaceXIOException;
import com.edwards.SpaceXTracker.exceptions.SpaceXMalformedUrl;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

public class BaseSpaceXAppExceptionTest {

    @Test
    public void printMessageWritesToStderr() {
        PrintStream original = System.err;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        System.setErr(new PrintStream(captured));
        try {
            new SpaceXIOException("test error").printMessage();
            assertTrue(captured.toString().contains("test error"));
        } finally {
            System.setErr(original);
        }
    }

    @Test
    public void spaceXIOExceptionHasMessage() {
        SpaceXIOException ex = new SpaceXIOException("io problem");
        assertEquals("io problem", ex.getMessage());
    }

    @Test
    public void spaceXMalformedUrlHasMessage() {
        SpaceXMalformedUrl ex = new SpaceXMalformedUrl("bad url");
        assertEquals("bad url", ex.getMessage());
    }

    @Test
    public void spaceXIOExceptionWithCause() {
        RuntimeException cause = new RuntimeException("root");
        SpaceXIOException ex = new SpaceXIOException("wrapped", cause);
        assertEquals(cause, ex.getCause());
    }
}
