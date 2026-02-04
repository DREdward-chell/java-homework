package com.edwards.components;

import com.edwards.entities.Theme;
import com.edwards.management.GameManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Random;

public class SelectorScreen extends JPanel {
    @Getter
    private final GameManager gameManager;
    private final List<Theme> themes;
    private final JComboBox<String> difficultySelector;
    private final JComboBox<String> themeSelector;

    public SelectorScreen(GameManager srcManager, String wordsJson) {
        this.gameManager = srcManager;

        setLayout(new BorderLayout());

        Gson gson = new Gson();
        Type jsonType = new TypeToken<List<Theme>>(){}.getType();

        themes = gson.fromJson(wordsJson, jsonType);
        themes.add(new Theme("Случайно", List.of(), List.of(), List.of()));

        themeSelector = new JComboBox<>();

        themes.forEach(theme -> themeSelector.addItem(theme.getTheme()));

        themeSelector.setFont(new Font("Arial", Font.PLAIN, 12));

        String[] difficulties = {"Легко", "Средне", "Тяжело", "Случайно"};

        difficultySelector = new JComboBox<>(difficulties);

        difficultySelector.setFont(new Font("Arial", Font.PLAIN, 12));

        JButton startGameButton = new JButton("Играть");
        startGameButton.setFocusable(false);
        startGameButton.setFont(new Font("Arial", Font.BOLD, 20));
        startGameButton.addActionListener(_ -> gameManager.initGameScreen(getWord()));
        startGameButton.setMargin(new Insets(10, 5, 10, 5));

        JPanel selectorPanel = new JPanel();
        selectorPanel.setLayout(new BoxLayout(selectorPanel, BoxLayout.Y_AXIS));

        selectorPanel.add(themeSelector, Component.CENTER_ALIGNMENT);
        selectorPanel.add(difficultySelector, Component.CENTER_ALIGNMENT);
        selectorPanel.add(startGameButton, Component.CENTER_ALIGNMENT);

        add(selectorPanel, BorderLayout.CENTER);
    }

    public Theme.Word getWord() {
        Random random = new Random();

        Theme selectedTheme = null;
        String selectedThemeName = themeSelector.getSelectedItem().toString();

        if (selectedThemeName.equals("Случайно")) {
            selectedThemeName = themeSelector.getItemAt(random.nextInt(themeSelector.getItemCount() - 1)).toString();
        }

        for (Theme theme : themes) {
            if (theme.getTheme().equals(selectedThemeName)) {
                selectedTheme = theme;
            }
        }

        if (selectedTheme == null) {
            throw new RuntimeException("Unexpected theme selection error");
        }

        List<Theme.Word> wordList = switch (difficultySelector.getSelectedItem().toString()) {
            case "Легко" -> selectedTheme.getEasy();
            case "Средне" -> selectedTheme.getMedium();
            case "Тяжело" -> selectedTheme.getHard();
            case "Случайно" -> switch (random.nextInt(3)) {
                case 1 -> selectedTheme.getMedium();
                case 2 -> selectedTheme.getHard();
                default -> selectedTheme.getEasy();
            };
            default ->
                    throw new IllegalStateException("Unexpected value: " + difficultySelector.getSelectedItem().toString());
        };

        return wordList.get(random.nextInt(wordList.size()));
    }
}
