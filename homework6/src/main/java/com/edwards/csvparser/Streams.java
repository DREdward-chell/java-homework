package com.edwards.csvparser;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

final class Streams {

    private Streams() {}

    static InputStream openInput(String filename) throws IOException {
        InputStream in = new BufferedInputStream(new FileInputStream(filename));
        String lower = filename.toLowerCase();
        if (lower.endsWith(".zip")) {
            ZipInputStream zin = new ZipInputStream(in);
            ZipEntry entry = zin.getNextEntry();
            if (entry == null) {
                zin.close();
                throw new CsvParserException("ZIP archive is empty: " + filename);
            }
            return zin;
        }
        if (lower.endsWith(".gz")) {
            return new GZIPInputStream(in);
        }
        return in;
    }

    static OutputStream openOutput(String filename) throws IOException {
        OutputStream out = new BufferedOutputStream(new FileOutputStream(filename));
        String lower = filename.toLowerCase();
        if (lower.endsWith(".zip")) {
            ZipOutputStream zout = new ZipOutputStream(out);
            zout.putNextEntry(new ZipEntry(zipEntryName(filename)));
            return zout;
        }
        if (lower.endsWith(".gz")) {
            return new GZIPOutputStream(out);
        }
        return out;
    }

    private static String zipEntryName(String filename) {
        String name = new File(filename).getName();
        String base = name.substring(0, name.length() - 4);
        if (base.isEmpty()) base = "data";
        if (!base.toLowerCase().endsWith(".csv")) base += ".csv";
        return base;
    }
}
