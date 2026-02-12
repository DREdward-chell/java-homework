package com.edwards.components;

import com.edwards.entities.Theme;
import com.edwards.management.GameManager;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;

public class GameScreen extends JPanel {
    @Getter
    private final JLabel attemptsLabel;

    @Getter
    private final HangManDrawer hangManDrawer;

    @Getter
    private final GameManager gameManager;

    public GameScreen(GameManager srcManager, Theme.Word word) {
        setLayout(new GridLayout(1, 2, 50, 0));

        gameManager = srcManager;
        attemptsLabel = new JLabel("Осталось попыток: " + 10);
        attemptsLabel.setFont(new Font("Arial", Font.PLAIN, 20));
        hangManDrawer = new HangManDrawer();

        initializeWordAndAlphabet(word.getWord());
        initializeDrawer();
        emplaceHint(word.getHint());
    }

    public void initializeWordAndAlphabet(String initialWord) {
        final int gap = 5;
        Alphabet alphabet = new Alphabet(gap);
        HiddenWord hiddenWord = new HiddenWord(initialWord, gap);

        alphabet.getButtons().forEach(button ->
            button.addActionListener(e -> {
                JButton jbutton = (JButton) e.getSource();
                jbutton.setVisible(false);
                refresh(hiddenWord.unleashLetter(jbutton.getText().charAt(0)));
            })
        );

        JPanel panel = new JPanel(new BorderLayout(0, 50));

        panel.add(hiddenWord, BorderLayout.NORTH);
        panel.add(alphabet, BorderLayout.CENTER);

        add(panel, 0);
    }

    public void initializeDrawer() {
        JPanel panel = new JPanel(new BorderLayout(0, 25));

        hangManDrawer.setMargin(25);

        panel.add(attemptsLabel, BorderLayout.NORTH);
        panel.add(hangManDrawer, BorderLayout.CENTER);

        add(panel, 1);
    }

    public void emplaceHint(String hint) {
        JButton button = new JButton("Подсказка");
        button.setFocusable(false);
        button.setFont(new Font("Arial", Font.PLAIN, 20));
        button.addActionListener(e -> {
            JButton jbutton = (JButton) e.getSource();
            jbutton.setText(hint);
            jbutton.addActionListener(_ -> {});
            jbutton.setBackground(new Color(255, 255, 255, 0));
            jbutton.setBorderPainted(false);
        });

        JPanel rightPanel = (JPanel) getComponent(1);
        rightPanel.add(button, BorderLayout.SOUTH);
    }

    public void refresh(boolean unleashed) {
        gameManager.process(unleashed);
    }

    public HiddenWord getHiddenWord() {
        return (HiddenWord) getComponent(0).getComponentAt(0, 1);
    }
}