package com.edwards.SpaceXTracker.exceptions;

import lombok.experimental.StandardException;

@StandardException
public class BaseSpaceXAppException extends Exception {
    public void printMessage() {
        System.err.println(getMessage());
    }

    public void easyExit() {
        printMessage();
        System.exit(-1);
    }
}
