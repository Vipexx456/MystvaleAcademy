package com.ror;

import com.ror.engine.GameWindow;
import com.ror.engine.LoadingSplashScreen;

public class Main {
    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            LoadingSplashScreen.showThenLaunch(() -> {
                GameWindow window = new GameWindow();
                return window::launchGame;
            });
        });
    }
}
