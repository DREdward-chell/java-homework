package com.edwards.management;

import com.edwards.components.GameScreen;
import com.edwards.components.SelectorScreen;
import com.edwards.entities.Theme;
import com.edwards.utilities.Utilities;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

public class GameManager {
    private final JFrame frame;
    private GameScreen gameScreen;
    private SelectorScreen selectorScreen;
    private final String wordsJson;

    public GameManager() throws IOException {
        wordsJson = Utilities.readWholeFile(Objects.requireNonNull(getClass().getClassLoader().getResource("words.json")).getPath());

        frame = new JFrame("Hang Man");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        initSelectorScreen();

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setMinimumSize(new Dimension(1200, 800));
        frame.setVisible(true);
    }

    private void initSelectorScreen() {
        selectorScreen = new SelectorScreen(this, wordsJson);
        selectorScreen.setBorder(BorderFactory.createEmptyBorder(100, 100, 100, 100));
        frame.add(selectorScreen, BorderLayout.CENTER);
    }

    public void initGameScreen(Theme.Word word) {
        selectorScreen.setVisible(false);

        gameScreen = new GameScreen(this, word);
        gameScreen.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

        frame.add(gameScreen, BorderLayout.CENTER);
    }

    public void process(boolean unleashed) {
        if (!unleashed) {
            gameScreen.getHangManDrawer().decreaseAttempts();
        }

        gameScreen.getAttemptsLabel().setText("Осталось попыток: " + getRemainingAttempts());
//        gameScreen.getHangManDrawer().repaint();
        repaint();

        if (gameScreen.getHiddenWord().wholeWordWasUnleashed()) {
            displayRestartDialog("Победа! Вы бы хотели сыграть ещё одну игру?");
        }

        if (noRemainingAttempts()) {
            gameScreen.getHiddenWord().unleashWord();
            displayRestartDialog("Вы проиграли! Может попробуете ещё один раз?");
        }
    }

    public void restart() {
        frame.remove(gameScreen);
        selectorScreen.setVisible(true);
        frame.pack();
    }

    private void displayRestartDialog(String message) {
        int res = JOptionPane.showConfirmDialog(frame, message, "", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (res == JOptionPane.OK_OPTION) {
            restart();
        } else {
            System.exit(0);
        }
    }

    private boolean noRemainingAttempts() {
        return gameScreen.getHangManDrawer().getRemainingAttempts() == 0;
    }

    private int getRemainingAttempts() {
        return gameScreen.getHangManDrawer().getRemainingAttempts();
    }

    private void repaint() {
        gameScreen.repaint();
    }
}
