package com.edwards.components;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.List;

public class HiddenWord extends JPanel {
    private final String initialWord;
    private final JPanel lettersPanel;

    public HiddenWord(String initialWord, int gap) {
        setLayout(new BorderLayout());
        this.initialWord = initialWord;
        lettersPanel = new JPanel(new GridLayout(1, initialWord.length(), gap, gap));

        for (int i = 0; i < initialWord.length(); i++) {
            JLabel label = new JLabel("_", SwingConstants.CENTER);
            label.setFont(new Font("Arial", Font.BOLD, 20));
            lettersPanel.add(label);
        }

        add(lettersPanel, BorderLayout.CENTER);
    }

    public boolean unleashLetter(char letter) {
        boolean unleashed = false;
        for (int i = 0; i < initialWord.length(); i++) {
            if (initialWord.charAt(i) == letter) {
                JLabel label = (JLabel) lettersPanel.getComponent(i);
                label.setText(initialWord.substring(i, i + 1));
                unleashed = true;
            }
        }

        return unleashed;
    }

    public void unleashWord() {
        for (int i = 0; i < initialWord.length(); i++) {
            JLabel label = (JLabel) lettersPanel.getComponent(i);
            label.setText(initialWord.substring(i, i + 1));
        }
    }

    public boolean wholeWordWasUnleashed() {
        return Arrays.stream(lettersPanel.getComponents())
                .noneMatch(c -> ((JLabel)c).getText().equals("_"));
    }
}
