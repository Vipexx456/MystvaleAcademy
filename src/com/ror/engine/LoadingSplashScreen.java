package com.ror.engine;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.IllegalComponentStateException;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JWindow;
import javax.swing.Timer;

public final class LoadingSplashScreen {
    private static final int SPLASH_MINIMUM_MS = 1300;
    private static final int SPLASH_FADE_MS = 450;
    private static final int ANIMATION_DELAY_MS = 16;
    private static final float LOADING_PROGRESS_CAP = 0.92f;
    private static final float PROGRESS_EASING = 0.12f;
    private static final String[] SPLASH_IMAGE_LOCATIONS = {
            "/com/ror/models/assets/images/loadscreen/load-screen-background.png",
            "src/com/ror/models/assets/images/loadscreen/load-screen-background.png",
            "assets/images/loadscreen/load-screen-background.png"
    };
    private static final String[] SPLASH_FONT_LOCATIONS = {
            "/com/ror/models/assets/fonts/Cinzel-VariableFont_wght.ttf",
            "src/com/ror/models/assets/fonts/Cinzel-VariableFont_wght.ttf"
    };
    private static final Color BAR_OUTLINE = new Color(130, 245, 255, 220);
    private static final Color BAR_FILL = new Color(84, 240, 255, 200);
    private static final Color BAR_FILL_GLOW = new Color(150, 255, 255, 120);
    private static final Color TEXT_COLOR = new Color(230, 250, 255, 235);
    private static final Color SHADOW_COLOR = new Color(0, 18, 28, 200);

    private LoadingSplashScreen() {
    }

    public static void showThenLaunch(Callable<Runnable> startupTask) {
        BufferedImage splashImage = loadSplashImage();
        if (splashImage == null) {
            try {
                Runnable completion = startupTask.call();
                if (completion != null) {
                    completion.run();
                }
            } catch (Exception exception) {
                throw new IllegalStateException("Unable to start game.", exception);
            }
            return;
        }

        Font splashFont = loadSplashFont();
        JWindow splashWindow = new JWindow();
        splashWindow.setBackground(Color.BLACK);
        SplashPanel splashPanel = new SplashPanel(splashImage, splashFont);
        splashWindow.setContentPane(splashPanel);
        splashWindow.setSize(resolveWindowSize(splashImage));
        splashWindow.setLocationRelativeTo(null);
        setWindowOpacity(splashWindow, 0f);
        splashWindow.setVisible(true);

        AtomicReference<Runnable> completionAction = new AtomicReference<>();
        AtomicReference<Throwable> startupFailure = new AtomicReference<>();
        Thread startupThread = new Thread(() -> {
            try {
                completionAction.set(startupTask.call());
            } catch (Throwable throwable) {
                startupFailure.set(throwable);
            }
        }, "startup-loader");
        startupThread.start();

        long startTime = System.currentTimeMillis();
        final float[] displayedProgress = { 0f };
        Timer timer = new Timer(ANIMATION_DELAY_MS, event -> {
            long elapsed = System.currentTimeMillis() - startTime;
            boolean loadingFinished = completionAction.get() != null || startupFailure.get() != null;

            float targetProgress;
            if (loadingFinished) {
                targetProgress = 1f;
            } else {
                float timeBasedProgress = Math.min(LOADING_PROGRESS_CAP, elapsed / 2200f);
                targetProgress = Math.min(LOADING_PROGRESS_CAP, Math.max(displayedProgress[0], timeBasedProgress));
            }

            displayedProgress[0] += (targetProgress - displayedProgress[0]) * PROGRESS_EASING;
            if (loadingFinished && Math.abs(1f - displayedProgress[0]) < 0.01f) {
                displayedProgress[0] = 1f;
            }

            boolean readyToClose = loadingFinished
                    && displayedProgress[0] >= 0.999f
                    && elapsed >= SPLASH_MINIMUM_MS;

            splashPanel.setState(elapsed, displayedProgress[0]);
            setWindowOpacity(splashWindow, calculateOpacity(elapsed, readyToClose));

            if (readyToClose) {
                ((Timer) event.getSource()).stop();
                Throwable failure = startupFailure.get();
                if (failure != null) {
                    splashWindow.dispose();
                    throw new IllegalStateException("Unable to start game.", failure);
                }
                Runnable completion = completionAction.get();
                if (completion != null) {
                    completion.run();
                }
                splashWindow.dispose();
            }
        });
        timer.start();
    }

    private static Dimension resolveWindowSize(BufferedImage image) {
        Rectangle screenBounds = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getMaximumWindowBounds();
        int maxWidth = Math.max(900, screenBounds.width - 120);
        int maxHeight = Math.max(540, screenBounds.height - 120);

        double widthScale = maxWidth / (double) image.getWidth();
        double heightScale = maxHeight / (double) image.getHeight();
        double scale = Math.min(1.0, Math.min(widthScale, heightScale));

        int width = Math.max(640, (int) Math.round(image.getWidth() * scale));
        int height = Math.max(360, (int) Math.round(image.getHeight() * scale));
        return new Dimension(width, height);
    }

    private static BufferedImage loadSplashImage() {
        for (String location : SPLASH_IMAGE_LOCATIONS) {
            BufferedImage image = loadImage(location);
            if (image != null) {
                return image;
            }
        }
        return null;
    }

    private static BufferedImage loadImage(String location) {
        if (location.startsWith("/")) {
            try (InputStream input = LoadingSplashScreen.class.getResourceAsStream(location)) {
                return input == null ? null : ImageIO.read(input);
            } catch (IOException exception) {
                return null;
            }
        }

        try {
            Path path = Path.of(location);
            if (!Files.isRegularFile(path)) {
                return null;
            }
            return ImageIO.read(path.toFile());
        } catch (IOException exception) {
            return null;
        }
    }

    private static Font loadSplashFont() {
        for (String location : SPLASH_FONT_LOCATIONS) {
            Font font = loadFont(location);
            if (font != null) {
                return font.deriveFont(Font.BOLD, 24f);
            }
        }
        return new Font("Serif", Font.BOLD, 24);
    }

    private static Font loadFont(String location) {
        if (location.startsWith("/")) {
            try (InputStream input = LoadingSplashScreen.class.getResourceAsStream(location)) {
                return input == null ? null : Font.createFont(Font.TRUETYPE_FONT, input);
            } catch (IOException | FontFormatException exception) {
                return null;
            }
        }

        try (InputStream input = Files.newInputStream(Path.of(location))) {
            return Font.createFont(Font.TRUETYPE_FONT, input);
        } catch (IOException | FontFormatException exception) {
            return null;
        }
    }

    private static float calculateOpacity(long elapsed, boolean fadeOut) {
        if (elapsed <= SPLASH_FADE_MS) {
            return clamp01(elapsed / (float) SPLASH_FADE_MS);
        }
        return 1f;
    }

    private static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private static void setWindowOpacity(JWindow window, float opacity) {
        try {
            window.setOpacity(clamp01(opacity));
        } catch (UnsupportedOperationException | IllegalComponentStateException exception) {
            // Fall back silently when per-window opacity is unavailable.
        }
    }

    private static final class SplashPanel extends JComponent {
        private final BufferedImage image;
        private final Font labelFont;
        private long elapsed;
        private float progress;

        private SplashPanel(BufferedImage image, Font labelFont) {
            this.image = image;
            this.labelFont = labelFont;
        }

        private void setState(long elapsed, float progress) {
            this.elapsed = elapsed;
            this.progress = clamp01(progress);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Color.BLACK);
            g2.fillRect(0, 0, getWidth(), getHeight());

            double scale = Math.max(
                    getWidth() / (double) image.getWidth(),
                    getHeight() / (double) image.getHeight());
            int drawWidth = (int) Math.ceil(image.getWidth() * scale);
            int drawHeight = (int) Math.ceil(image.getHeight() * scale);
            int drawX = (getWidth() - drawWidth) / 2;
            int drawY = (getHeight() - drawHeight) / 2;
            g2.drawImage(image, drawX, drawY, drawWidth, drawHeight, null);
            paintFooter(g2);
            g2.dispose();
        }

        private void paintFooter(Graphics2D g2) {
            int footerHeight = Math.max(124, getHeight() / 5);
            int footerY = getHeight() - footerHeight;
            g2.setComposite(AlphaComposite.SrcOver);
            g2.setColor(new Color(0, 6, 12, 110));
            g2.fillRect(0, footerY, getWidth(), footerHeight);

            int barWidth = Math.min(520, getWidth() - 120);
            int barHeight = 18;
            int barX = (getWidth() - barWidth) / 2;
            int barY = footerY + 34;

            g2.setStroke(new BasicStroke(2.2f));
            g2.setColor(BAR_OUTLINE);
            g2.drawRoundRect(barX, barY, barWidth, barHeight, 18, 18);

            paintFloatingProgress(g2, barX, barY, barWidth, barHeight);

            String label = "Loading " + Math.round(progress * 100f) + "%";
            g2.setFont(labelFont);
            int labelWidth = g2.getFontMetrics().stringWidth(label);
            int labelX = (getWidth() - labelWidth) / 2;
            int labelY = barY + 52;
            g2.setColor(SHADOW_COLOR);
            g2.drawString(label, labelX + 2, labelY + 2);
            g2.setColor(TEXT_COLOR);
            g2.drawString(label, labelX, labelY);
        }

        private void paintFloatingProgress(Graphics2D g2, int barX, int barY, int barWidth, int barHeight) {
            Graphics2D barGraphics = (Graphics2D) g2.create();
            barGraphics.setClip(barX + 2, barY + 2, Math.max(1, barWidth - 3), Math.max(1, barHeight - 3));

            float normalizedTime = (elapsed % 1400L) / 1400f;
            int segmentWidth = Math.max(100, barWidth / 4);
            int travelWidth = barWidth + segmentWidth;
            int segmentX = barX - segmentWidth + Math.round(travelWidth * normalizedTime);

            barGraphics.setColor(BAR_FILL_GLOW);
            barGraphics.fillRoundRect(segmentX - 18, barY + 1, segmentWidth + 36, barHeight - 1, 18, 18);
            barGraphics.setColor(BAR_FILL);
            barGraphics.fillRoundRect(segmentX, barY + 3, segmentWidth, Math.max(8, barHeight - 5), 16, 16);

            int fillWidth = Math.max(10, Math.round((barWidth - 8) * progress));
            barGraphics.setColor(new Color(84, 240, 255, 118));
            barGraphics.fillRoundRect(barX + 4, barY + 4, fillWidth, Math.max(6, barHeight - 7), 14, 14);
            barGraphics.dispose();
        }

    }
}
