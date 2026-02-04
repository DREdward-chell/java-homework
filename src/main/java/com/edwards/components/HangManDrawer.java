package com.edwards.components;

import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import java.awt.*;

public class HangManDrawer extends JComponent {
    @Getter
    @Setter
    private int remainingAttempts;

    public HangManDrawer(int remainingAttempts) {
        setLayout(new BorderLayout());
        this.remainingAttempts = remainingAttempts;
    }

    public HangManDrawer() {
        this(10);
    }

    @Override
    public void paint(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        int w = getWidth();
        int radius = 50;
        
        g2d.translate(w / 12, 0);

        switch (remainingAttempts) {
            case 0:
                // Нога справа
                g2d.drawLine(w / 3 * 2, w / 12 + 3 * radius, w / 3 * 2 + radius / 2, w / 12 + 9 * radius / 2);
            case 1:
                // Нога слева
                g2d.drawLine(w / 3 * 2, w / 12 + 3 * radius, w / 3 * 2 - radius / 2, w / 12 + 9 * radius / 2);
            case 2:
                // Рука справа
                g2d.drawLine(w / 3 * 2, w / 12 + 3 * radius / 2, w / 3 * 2 + radius / 2, w / 12 + 5 * radius / 2);
            case 3:
                // Рука слева
                g2d.drawLine(w / 3 * 2, w / 12 + 3 * radius / 2, w / 3 * 2 - radius / 2, w / 12 + 5 * radius / 2);
            case 4:
                // Тело
                g2d.drawLine(w / 3 * 2,w / 12 + radius, w / 3 * 2, w / 12 + 3 * radius);
            case 5:
                // Голова
                g2d.drawOval(w / 3 * 2 - radius / 2, w / 12, radius, radius);
            case 6:
                // Верёвка
                g2d.drawLine(w / 3 * 2, 0, w / 3 * 2, w / 12);
            case 7:
                // Кран
                g2d.drawLine(w / 6, 0, w / 3 * 2, 0);
                g2d.drawLine(w / 6, w / 12, w / 12 * 3, 0);
            case 8:
                // Столб
                g2d.drawLine(w / 6, getHeight(), w / 6, 0);
                g2d.drawLine(0, getHeight(), w / 6, getHeight() - w / 6);
                g2d.drawLine(w / 3, getHeight(), w / 6, getHeight() - w / 6);
            case 9:
                // Опора
                g2d.drawLine(0, getHeight(), w / 3, getHeight());
            default:
                break;
        }
    }

    public void setMargin(int top, int left, int bottom, int right) {
        setBorder(BorderFactory.createEmptyBorder(top, left, bottom, right));
    }

    public void setMargin(int m) {
        setMargin(m, m, m, m);
    }

    public void decreaseAttempts() {
        --remainingAttempts;
    }
}
