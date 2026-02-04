package com.edwards.components;

import java.awt.*;
import java.util.Arrays;
import java.util.stream.Stream;
import javax.swing.*;

public class Alphabet extends JPanel {
    private final JPanel buttonPanel;

    public Alphabet(int gap) {
        setLayout(new BorderLayout());
        buttonPanel = new JPanel(new GridLayout(4, 8, gap, gap));

        for (int i = 0; i < 32; i++) {
            JButton button = new JButton(String.valueOf((char) ('А' + i)));
            button.setFont(new Font("Arial", Font.BOLD, 20));
            button.setFocusable(false);
            buttonPanel.add(button);
        }

        add(buttonPanel, BorderLayout.CENTER);
    }

    public Stream<JButton> getButtons() {
        return Arrays.stream(buttonPanel.getComponents()).map(c -> (JButton) c);
    }
}
