package com.edwards.utilities;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public abstract class Utilities {
    public static String readWholeFile(String path) throws IOException {
        return Files.readString(Paths.get(path));
    }
}
