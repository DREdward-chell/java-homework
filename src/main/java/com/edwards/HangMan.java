package com.edwards;

import com.edwards.management.GameManager;
import javax.swing.*;
import java.io.IOException;

public class HangMan {

    public void run(String[] args) {
        try {
            new GameManager();
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new HangMan().run(args));
    }
}