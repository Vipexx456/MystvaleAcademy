package com.ror.engine;

import com.ror.models.Entity;
import com.ror.models.Gunner;
import com.ror.models.Hero;
import com.ror.models.Boss.Azrael;
import com.ror.models.Boss.Elderthorn;
import com.ror.models.Boss.Kim;
import com.ror.models.Boss.Morgrath;
import com.ror.models.Mage;
import com.ror.models.Mobs.FadingWarden;
import com.ror.models.Mobs.Goblin;
import com.ror.models.Mobs.HollowKing;
import com.ror.models.Mobs.MudLurker;
import com.ror.models.Mobs.ShadowAbyss;
import com.ror.models.Mobs.Slime;
import com.ror.models.Mobs.SwampRat;
import com.ror.models.Mobs.VeilSerpent;
import com.ror.models.Mobs.VoidBeast;
import com.ror.models.Swordsman;
import com.ror.utils.sounds.SoundManager;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicSliderUI;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.plaf.FontUIResource;

final class GameWindowGraphics {
    private static final String UI_IMAGE_DIRECTORY = "assets/images/ui/";
    private static final String ENEMY_IMAGE_DIRECTORY = "assets/images/enemies/";
    private static final String HERO_IMAGE_DIRECTORY = "assets/images/heroes/";
    private static final int LANDING_SECONDARY_BUTTON_WIDTH = 170;
    private static final int LANDING_SECONDARY_BUTTON_HEIGHT = 78;
    private static final Color BUTTON_PRIMARY = new Color(0x0B1033);
    private static final Color BUTTON_HOVER = new Color(0x1A2159);
    private static final Color BUTTON_PRESSED = new Color(0x06081F);
    private static final Color BUTTON_BORDER = new Color(0x2B356E);
    private static final int[] STORY_WALK_X_OFFSETS_NEO = { 0, -12, -4, -14, -8 };
    private static final int[] STORY_WALK_Y_OFFSETS_NEO = { 0, 0, 0, 0, 0 };
    private static final int[] STORY_WALK_X_OFFSETS_MLEUX = { 0, -10, -2, -12, -6 };
    private static final int[] STORY_WALK_Y_OFFSETS_MLEUX = { 0, 0, 0, 0, 0 };
    private static final int[] STORY_WALK_X_OFFSETS_FEHLD = { 0, -8, -2, -10 };
    private static final int[] STORY_WALK_Y_OFFSETS_FEHLD = { 0, 0, 0, 0 };

    private Font headingFont = new Font("Serif", Font.BOLD, 28);
    private Font bodyFont = new Font("SansSerif", Font.PLAIN, 14);
    private float headingFontSize = 28f;
    private float bodyFontSize = 14f;

    private final BufferedImage[] landingBackgroundFrames;
    private int landingBackgroundIndex = 0;
    private final ButtonSkin primaryButtonSkin;
    private final ButtonSkin secondaryButtonSkin;
    private final BufferedImage landingPlayButtonImage;
    private final BufferedImage landingAboutButtonImage;
    private final BufferedImage landingLoadButtonImage;
    private final BufferedImage landingOptionsButtonImage;
    private final BufferedImage landingExitButtonImage;
    private final BufferedImage landingAboutOverlayImage;
    private final BufferedImage landingOptionsOverlayImage;

    GameWindowGraphics() {
        loadFonts();
        landingBackgroundFrames = loadLandingBackgroundFrames();
        primaryButtonSkin = loadButtonSkin("button-primary");
        secondaryButtonSkin = loadButtonSkin("button-secondary");
        landingPlayButtonImage = loadLandingButtonImage("play-button.png");
        landingAboutButtonImage = loadLandingButtonImage("about-button.png");
        landingLoadButtonImage = loadLandingButtonImage("load-button.png");
        landingOptionsButtonImage = loadLandingButtonImage("options-button.png");
        landingExitButtonImage = loadLandingButtonImage("exit-button.png");
        landingAboutOverlayImage = loadAboutOverlayImage();
        landingOptionsOverlayImage = loadOptionsOverlayImage();
    }

    JPanel createCardPanel(Color borderColor, Color panelColor) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borderColor, 1),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)));
        panel.setBackground(panelColor);
        panel.setOpaque(true);
        return panel;
    }

    Font getHeadingFont(float size) {
        if (headingFont != null) return headingFont.deriveFont(Font.BOLD, size);
        return new Font("Serif", Font.BOLD, Math.round(size));
    }

    Font getBodyFont(float size) {
        if (bodyFont != null) return bodyFont.deriveFont(Font.PLAIN, size);
        return new Font("SansSerif", Font.PLAIN, Math.round(size));
    }

    String styleHtml(String htmlBody) {
        String family = bodyFont != null ? bodyFont.getFamily() : "SansSerif";
        int size = Math.round(bodyFontSize);
        String cleaned = htmlBody
                .replaceAll("^<html>", "")
                .replaceAll("</html>$", "");
        return "<html><div style='font-family: " + family + "; font-size: " + size + "pt;'>" + cleaned + "</div></html>";
    }

    String styleRaw(String text) {
        String family = bodyFont != null ? bodyFont.getFamily() : "SansSerif";
        int size = Math.round(bodyFontSize);
        return "<html><div style='font-family: " + family + "; font-size: " + size + "pt;'>" + text + "</div></html>";
    }

    JLabel createHeading(String text, Color textColor) {
        JLabel label = new JLabel(text);
        label.putClientProperty("fontRole", "heading");
        label.setFont(getHeadingFont(headingFontSize));
        label.setForeground(textColor);
        return label;
    }

    JLabel createBody(String text, Color textColor) {
        JLabel label = new JLabel(styleRaw(text));
        label.putClientProperty("fontRole", "body");
        label.setFont(getBodyFont(bodyFontSize));
        label.setForeground(textColor);
        return label;
    }

    JButton createPrimaryButton(String text) {
        return createStyledButton(text, primaryButtonSkin, BUTTON_PRIMARY,
                Color.WHITE, getBodyFont(14f).deriveFont(Font.BOLD, 14f), 42);
    }

    JButton createSecondaryButton(String text) {
        return createStyledButton(text, secondaryButtonSkin, BUTTON_PRIMARY,
                Color.WHITE, getBodyFont(13f).deriveFont(Font.BOLD, 13f), 40);
    }

    JButton createBattleButton(String text, Color fallbackColor) {
        return createStyledButton(text, null, fallbackColor,
                Color.WHITE, getBodyFont(15f).deriveFont(Font.BOLD, 15f), 58);
    }

    JButton createCharacterSelectButton(String text) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g2 = (Graphics2D) graphics.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                ButtonModel model = getModel();
                Color fill = BUTTON_PRIMARY;
                if (model.isPressed()) fill = BUTTON_PRESSED;
                else if (model.isRollover()) fill = BUTTON_HOVER;
                g2.setColor(fill);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.dispose();
                super.paintComponent(graphics);
            }

            @Override
            protected void paintBorder(Graphics graphics) {
                Graphics2D g2 = (Graphics2D) graphics.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BUTTON_BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.dispose();
            }
        };
        button.setFont(getBodyFont(14f).deriveFont(Font.BOLD, 14f));
        button.setForeground(Color.WHITE);
        button.setBackground(BUTTON_PRIMARY);
        button.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        button.setRolloverEnabled(true);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setFocusPainted(false);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        return button;
    }

    JPanel buildLandingScreen(JFrame frame, Runnable onPlay, Runnable onLoad, Runnable onExit) {
        final int playButtonWidth = 448;
        final int playButtonHeight = 133;
        final int secondaryButtonWidth = 225;
        final int secondaryButtonHeight = 110;
        final int secondaryButtonGap = 18;
        final int buttonBlockWidth = (secondaryButtonWidth * 2) + secondaryButtonGap;

        JPanel backgroundPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                Graphics2D g2 = (Graphics2D) graphics.create();
                g2.setColor(new Color(18, 12, 10));
                g2.fillRect(0, 0, getWidth(), getHeight());

                BufferedImage landingBackground = getCurrentLandingBackground();
                if (landingBackground != null) {
                    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                    double scale = Math.max(
                            getWidth() / (double) landingBackground.getWidth(),
                            getHeight() / (double) landingBackground.getHeight());
                    int drawWidth = (int) Math.ceil(landingBackground.getWidth() * scale);
                    int drawHeight = (int) Math.ceil(landingBackground.getHeight() * scale);
                    int drawX = (getWidth() - drawWidth) / 2;
                    int drawY = (getHeight() - drawHeight) / 2;
                    g2.drawImage(landingBackground, drawX, drawY, drawWidth, drawHeight, null);
                    g2.setColor(new Color(6, 8, 18, 70));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                }
                g2.dispose();
            }
        };

        backgroundPanel.setLayout(new GridBagLayout());
        if (landingBackgroundFrames.length > 1) {
            Timer frameTimer = new Timer(280, event -> {
                landingBackgroundIndex = (landingBackgroundIndex + 1) % landingBackgroundFrames.length;
                backgroundPanel.repaint();
            });
            frameTimer.start();
        }

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(12, 0, 12, 0));
        content.setMaximumSize(new Dimension(buttonBlockWidth, Integer.MAX_VALUE));

        JPanel topMenuRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        topMenuRow.setOpaque(false);
        topMenuRow.setMaximumSize(new Dimension(buttonBlockWidth, playButtonHeight));
        topMenuRow.setPreferredSize(new Dimension(buttonBlockWidth, playButtonHeight));
        topMenuRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton playButton = landingPlayButtonImage != null
                ? createLandingImageButton(landingPlayButtonImage, playButtonWidth, playButtonHeight, "PLAY")
                : createLandingPlaceholderButton("PLAY", 220, 48);
        playButton.addActionListener(event -> onPlay.run());
        topMenuRow.add(playButton);

        JPanel middleMenuRow = new JPanel(new GridLayout(1, 2, 1, 0));
        middleMenuRow.setOpaque(false);
        middleMenuRow.setMaximumSize(new Dimension(buttonBlockWidth, secondaryButtonHeight));
        middleMenuRow.setPreferredSize(new Dimension(buttonBlockWidth, secondaryButtonHeight));
        middleMenuRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton aboutButton = landingAboutButtonImage != null
                ? createLandingImageButton(landingAboutButtonImage,
                        secondaryButtonWidth, secondaryButtonHeight, "ABOUT")
                : createLandingPlaceholderButton("ABOUT");
        JButton loadButton = landingLoadButtonImage != null
                ? createLandingImageButton(landingLoadButtonImage,
                        secondaryButtonWidth, secondaryButtonHeight, "LOAD")
                : createLandingPlaceholderButton("LOAD");
        loadButton.addActionListener(event -> onLoad.run());
        middleMenuRow.add(aboutButton);
        middleMenuRow.add(loadButton);

        JPanel bottomMenuRow = new JPanel(new GridLayout(1, 2, 1, 0));
        bottomMenuRow.setOpaque(false);
        bottomMenuRow.setMaximumSize(new Dimension(buttonBlockWidth, secondaryButtonHeight));
        bottomMenuRow.setPreferredSize(new Dimension(buttonBlockWidth, secondaryButtonHeight));
        bottomMenuRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton optionsButton = landingOptionsButtonImage != null
                ? createLandingImageButton(landingOptionsButtonImage,
                        secondaryButtonWidth, secondaryButtonHeight, "OPTIONS")
                : createLandingPlaceholderButton("OPTIONS");

        JButton exitButton = landingExitButtonImage != null
                ? createLandingImageButton(landingExitButtonImage,
                        secondaryButtonWidth, secondaryButtonHeight, "EXIT")
                : createLandingPlaceholderButton("EXIT");

        exitButton.addActionListener(event -> System.exit(0));

        bottomMenuRow.add(optionsButton);
        bottomMenuRow.add(exitButton);

        content.add(topMenuRow);
        content.add(Box.createVerticalStrut(-20));
        content.add(middleMenuRow);
        content.add(Box.createVerticalStrut(-10));
        content.add(bottomMenuRow);
        content.add(Box.createVerticalGlue());

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        constraints.anchor = GridBagConstraints.SOUTHWEST;
        constraints.insets = new Insets(0, 70, 35, 0);
        backgroundPanel.add(content, constraints);

        AboutOverlayPanel aboutOverlayPanel = new AboutOverlayPanel(landingAboutOverlayImage);
        OptionsOverlayPanel optionsOverlayPanel = new OptionsOverlayPanel(landingOptionsOverlayImage);
        JLayeredPane layeredPane = new JLayeredPane() {
            @Override
            public void doLayout() {
                backgroundPanel.setBounds(0, 0, getWidth(), getHeight());
                aboutOverlayPanel.setBounds(0, 0, getWidth(), getHeight());
                optionsOverlayPanel.setBounds(0, 0, getWidth(), getHeight());
            }
        };
        layeredPane.add(backgroundPanel, JLayeredPane.DEFAULT_LAYER);
        layeredPane.add(aboutOverlayPanel, JLayeredPane.PALETTE_LAYER);
        layeredPane.add(optionsOverlayPanel, JLayeredPane.PALETTE_LAYER);

        aboutButton.addActionListener(event -> aboutOverlayPanel.showOverlay());
        optionsButton.addActionListener(event -> optionsOverlayPanel.showOverlay());

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(layeredPane, BorderLayout.CENTER);
        return wrapper;
    }

    private BufferedImage loadAboutOverlayImage() {
        return loadFirstAvailableImage(
                "/com/ror/models/assets/images/ui/About.png",
                "src/com/ror/models/assets/images/ui/About.png",
                "assets/images/ui/About.png",
                "C:/Users/Shayndel Mizy Amaga/Downloads/About.png");
    }

    private BufferedImage loadOptionsOverlayImage() {
        return loadFirstAvailableImage(
                "/com/ror/models/assets/images/ui/Option.png",
                "src/com/ror/models/assets/images/ui/Option.png",
                "assets/images/ui/Option.png",
                "C:/Users/Shayndel Mizy Amaga/Downloads/Option.png");
    }

    private final class AboutOverlayPanel extends JComponent {
        private static final double MAX_WIDTH_RATIO = 1.0d;
        private static final double MAX_HEIGHT_RATIO = 1.0d;
        private static final int ABOUT_IMAGE_BASE_WIDTH = 1280;
        private static final int ABOUT_IMAGE_BASE_HEIGHT = 720;
        private static final Rectangle ABOUT_CLOSE_BOX = new Rectangle(1000, 108, 54, 56);

        private final BufferedImage aboutImage;
        private final Rectangle closeBounds = new Rectangle();
        private boolean hoveringClose;

        private AboutOverlayPanel(BufferedImage aboutImage) {
            this.aboutImage = aboutImage;
            setVisible(false);
            setOpaque(false);

            MouseAdapter mouseHandler = new MouseAdapter() {
                @Override
                public void mouseMoved(MouseEvent event) {
                    updateHoverState(event.getPoint());
                }

                @Override
                public void mouseExited(MouseEvent event) {
                    hoveringClose = false;
                    setCursor(Cursor.getDefaultCursor());
                    repaint();
                }

                @Override
                public void mouseClicked(MouseEvent event) {
                    if (closeBounds.contains(event.getPoint())) {
                        hideOverlay();
                    }
                }
            };
            addMouseMotionListener(mouseHandler);
            addMouseListener(mouseHandler);
        }

        private void showOverlay() {
            setVisible(true);
            requestFocusInWindow();
            repaint();
        }

        private void hideOverlay() {
            setVisible(false);
            hoveringClose = false;
            setCursor(Cursor.getDefaultCursor());
        }

        private void updateHoverState(Point point) {
            boolean hovering = closeBounds.contains(point);
            if (hoveringClose == hovering) {
                return;
            }
            hoveringClose = hovering;
            setCursor(hovering ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            if (!isVisible()) {
                return;
            }

            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setColor(new Color(0, 0, 0, 185));
            g2.fillRect(0, 0, getWidth(), getHeight());

            if (aboutImage == null) {
                closeBounds.setBounds(0, 0, 0, 0);
                g2.setColor(Color.WHITE);
                g2.setFont(getHeadingFont(24f));
                g2.drawString("About image not found.", Math.max(30, getWidth() / 2 - 120), getHeight() / 2);
                g2.dispose();
                return;
            }

            double scale = Math.min(
                    Math.min((getWidth() * MAX_WIDTH_RATIO) / aboutImage.getWidth(),
                            (getHeight() * MAX_HEIGHT_RATIO) / aboutImage.getHeight()),
                    1d);
            int drawWidth = Math.max(1, (int) Math.round(aboutImage.getWidth() * scale));
            int drawHeight = Math.max(1, (int) Math.round(aboutImage.getHeight() * scale));
            int drawX = (getWidth() - drawWidth) / 2;
            int drawY = (getHeight() - drawHeight) / 2;

            g2.drawImage(aboutImage, drawX, drawY, drawWidth, drawHeight, null);

            double scaleX = drawWidth / (double) ABOUT_IMAGE_BASE_WIDTH;
            double scaleY = drawHeight / (double) ABOUT_IMAGE_BASE_HEIGHT;
            int closeX = drawX + (int) Math.round(ABOUT_CLOSE_BOX.x * scaleX);
            int closeY = drawY + (int) Math.round(ABOUT_CLOSE_BOX.y * scaleY);
            int closeWidth = Math.max(20, (int) Math.round(ABOUT_CLOSE_BOX.width * scaleX));
            int closeHeight = Math.max(20, (int) Math.round(ABOUT_CLOSE_BOX.height * scaleY));
            closeBounds.setBounds(closeX, closeY, closeWidth, closeHeight);

            g2.dispose();
        }
    }

    private final class OptionsOverlayPanel extends JComponent {
        private static final double MAX_WIDTH_RATIO = 1.0d;
        private static final double MAX_HEIGHT_RATIO = 1.0d;
        private static final int OPTIONS_IMAGE_BASE_WIDTH = 1280;
        private static final int OPTIONS_IMAGE_BASE_HEIGHT = 720;
        private static final Rectangle OPTIONS_CLOSE_BOX = new Rectangle(1000, 108, 54, 56);
        private static final Rectangle OPTIONS_SLIDER_BOX = new Rectangle(455, 234, 440, 40);
        private static final Rectangle OPTIONS_VALUE_BOX = new Rectangle(912, 228, 96, 48);

        private final BufferedImage optionsImage;
        private final Rectangle closeBounds = new Rectangle();
        private final JSlider volumeSlider;
        private final JLabel volumeValueLabel;

        private OptionsOverlayPanel(BufferedImage optionsImage) {
            this.optionsImage = optionsImage;
            setVisible(false);
            setOpaque(false);
            setLayout(null);
            setCursor(Cursor.getDefaultCursor());

            volumeSlider = createVolumeSlider();
            volumeValueLabel = new JLabel("", SwingConstants.CENTER);
            volumeValueLabel.setOpaque(false);
            volumeValueLabel.setForeground(new Color(210, 226, 255));
            volumeValueLabel.setFont(getBodyFont(15f).deriveFont(Font.BOLD, 15f));

            ChangeListener sliderChangeListener = event -> {
                int volume = volumeSlider.getValue();
                SoundManager.setMasterVolume(volume);
                updateVolumeLabel(volume);
            };
            volumeSlider.addChangeListener(sliderChangeListener);

            add(volumeSlider);
            add(volumeValueLabel);

            MouseAdapter mouseHandler = new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent event) {
                    if (closeBounds.contains(event.getPoint())) {
                        hideOverlay();
                    }
                }
            };
            addMouseMotionListener(mouseHandler);
            addMouseListener(mouseHandler);
            updateVolumeLabel(SoundManager.getMasterVolume());
        }

        private JSlider createVolumeSlider() {
            JSlider slider = new JSlider(0, 100, SoundManager.getMasterVolume());
            slider.setOpaque(false);
            slider.setFocusable(false);
            slider.setPaintTicks(false);
            slider.setPaintLabels(false);
            slider.setBorder(BorderFactory.createEmptyBorder());
            slider.setUI(new BasicSliderUI(slider) {
                @Override
                public void paintTrack(Graphics graphics) {
                    Graphics2D g2 = (Graphics2D) graphics.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    int trackY = trackRect.y + (trackRect.height / 2) - 4;
                    g2.setColor(new Color(57, 74, 132, 170));
                    g2.fillRoundRect(trackRect.x, trackY, trackRect.width, 8, 8, 8);

                    int filledWidth = thumbRect.x + (thumbRect.width / 2) - trackRect.x;
                    g2.setColor(new Color(140, 184, 255, 230));
                    g2.fillRoundRect(trackRect.x, trackY, Math.max(0, filledWidth), 8, 8, 8);
                    g2.dispose();
                }

                @Override
                public void paintThumb(Graphics graphics) {
                    Graphics2D g2 = (Graphics2D) graphics.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(228, 243, 255, 245));
                    g2.fillOval(thumbRect.x, thumbRect.y + 1, thumbRect.width - 1, thumbRect.height - 1);
                    g2.setColor(new Color(89, 122, 208, 255));
                    g2.drawOval(thumbRect.x, thumbRect.y + 1, thumbRect.width - 1, thumbRect.height - 1);
                    g2.dispose();
                }

                @Override
                protected Dimension getThumbSize() {
                    return new Dimension(20, 20);
                }
            });
            return slider;
        }

        private void updateVolumeLabel(int value) {
            volumeValueLabel.setText(value + "%");
        }

        private void showOverlay() {
            int currentVolume = SoundManager.getMasterVolume();
            volumeSlider.setValue(currentVolume);
            updateVolumeLabel(currentVolume);
            setVisible(true);
            requestFocusInWindow();
            repaint();
        }

        private void hideOverlay() {
            setVisible(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            if (!isVisible()) {
                return;
            }

            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setColor(new Color(0, 0, 0, 185));
            g2.fillRect(0, 0, getWidth(), getHeight());

            if (optionsImage == null) {
                closeBounds.setBounds(0, 0, 0, 0);
                volumeSlider.setVisible(false);
                volumeValueLabel.setVisible(false);
                g2.setColor(Color.WHITE);
                g2.setFont(getHeadingFont(24f));
                g2.drawString("Options image not found.", Math.max(30, getWidth() / 2 - 135), getHeight() / 2);
                g2.dispose();
                return;
            }

            double scale = Math.min(
                    Math.min((getWidth() * MAX_WIDTH_RATIO) / optionsImage.getWidth(),
                            (getHeight() * MAX_HEIGHT_RATIO) / optionsImage.getHeight()),
                    1d);
            int drawWidth = Math.max(1, (int) Math.round(optionsImage.getWidth() * scale));
            int drawHeight = Math.max(1, (int) Math.round(optionsImage.getHeight() * scale));
            int drawX = (getWidth() - drawWidth) / 2;
            int drawY = (getHeight() - drawHeight) / 2;

            g2.drawImage(optionsImage, drawX, drawY, drawWidth, drawHeight, null);

            double scaleX = drawWidth / (double) OPTIONS_IMAGE_BASE_WIDTH;
            double scaleY = drawHeight / (double) OPTIONS_IMAGE_BASE_HEIGHT;
            int closeX = drawX + (int) Math.round(OPTIONS_CLOSE_BOX.x * scaleX);
            int closeY = drawY + (int) Math.round(OPTIONS_CLOSE_BOX.y * scaleY);
            int closeWidth = Math.max(20, (int) Math.round(OPTIONS_CLOSE_BOX.width * scaleX));
            int closeHeight = Math.max(20, (int) Math.round(OPTIONS_CLOSE_BOX.height * scaleY));
            closeBounds.setBounds(closeX, closeY, closeWidth, closeHeight);

            int sliderX = drawX + (int) Math.round(OPTIONS_SLIDER_BOX.x * scaleX);
            int sliderY = drawY + (int) Math.round(OPTIONS_SLIDER_BOX.y * scaleY);
            int sliderWidth = Math.max(140, (int) Math.round(OPTIONS_SLIDER_BOX.width * scaleX));
            int sliderHeight = Math.max(24, (int) Math.round(OPTIONS_SLIDER_BOX.height * scaleY));
            volumeSlider.setBounds(sliderX, sliderY, sliderWidth, sliderHeight);
            volumeSlider.setVisible(true);

            int valueX = drawX + (int) Math.round(OPTIONS_VALUE_BOX.x * scaleX);
            int valueY = drawY + (int) Math.round(OPTIONS_VALUE_BOX.y * scaleY);
            int valueWidth = Math.max(56, (int) Math.round(OPTIONS_VALUE_BOX.width * scaleX));
            int valueHeight = Math.max(28, (int) Math.round(OPTIONS_VALUE_BOX.height * scaleY));
            volumeValueLabel.setBounds(valueX, valueY, valueWidth, valueHeight);
            volumeValueLabel.setVisible(true);

            g2.dispose();
        }
    }

    BufferedImage loadCharacterPortrait(String title) {
        String normalizedTitle = title == null ? "" : title.trim().toLowerCase().replace(' ', '-');
        String[] candidates = {
                HERO_IMAGE_DIRECTORY + normalizedTitle + ".png",
                HERO_IMAGE_DIRECTORY + normalizedTitle + ".jpg",
                HERO_IMAGE_DIRECTORY + normalizedTitle + ".jpeg",
                "src/com/ror/models/" + HERO_IMAGE_DIRECTORY + normalizedTitle + ".png",
                "src/com/ror/models/" + HERO_IMAGE_DIRECTORY + normalizedTitle + ".jpg",
                "src/com/ror/models/" + HERO_IMAGE_DIRECTORY + normalizedTitle + ".jpeg"
        };

        for (String candidate : candidates) {
            BufferedImage image = loadImageAsset(candidate);
            if (image != null) return image;
        }
        return null;
    }

    Icon createScaledPortraitIcon(BufferedImage portrait, int targetWidth, int targetHeight) {
        BufferedImage scaled = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = scaled.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        double scale = Math.max(
                (double) targetWidth / portrait.getWidth(),
                (double) targetHeight / portrait.getHeight());
        int drawWidth = (int) Math.round(portrait.getWidth() * scale);
        int drawHeight = (int) Math.round(portrait.getHeight() * scale);
        int drawX = (targetWidth - drawWidth) / 2;
        int drawY = (targetHeight - drawHeight) / 2;

        g2.drawImage(portrait, drawX, drawY, drawWidth, drawHeight, null);
        g2.dispose();
        return new ImageIcon(scaled);
    }

    void setGameFonts(Font heading, Font body, Container rootPanel) {
        if (heading != null) {
            headingFont = heading;
            headingFontSize = heading.getSize2D();
        }
        if (body != null) {
            bodyFont = body;
            bodyFontSize = body.getSize2D();
        }
        applyFontsToAllComponents(rootPanel);
    }

    void setGameFontSizes(float headingSize, float bodySize, Container rootPanel) {
        headingFontSize = headingSize;
        bodyFontSize = bodySize;
        if (headingFont != null) headingFont = headingFont.deriveFont(Font.BOLD, headingSize);
        if (bodyFont != null) bodyFont = bodyFont.deriveFont(Font.PLAIN, bodySize);
        applyGlobalUIFont();
        applyFontsToAllComponents(rootPanel);
    }

    void updateEnemySprite(Entity enemy, BattlePanel battlePanel) {
        BufferedImage sprite = loadEnemySprite(enemy);
        if (sprite == null) {
            BufferedImage placeholder = loadImageAsset(ENEMY_IMAGE_DIRECTORY + "placeholder.png");
            if (placeholder != null) {
                battlePanel.getBattleEnemySpriteLabel().setText("");
                battlePanel.getBattleEnemySpriteLabel().setIcon(createScaledSpriteIcon(placeholder, 96, 96));
                refreshBattleSpriteLabel(battlePanel.getBattleEnemySpriteLabel());
            } else {
                battlePanel.getBattleEnemySpriteLabel().setIcon(null);
                battlePanel.getBattleEnemySpriteLabel().setText("");
                refreshBattleSpriteLabel(battlePanel.getBattleEnemySpriteLabel());
            }
            return;
        }
        battlePanel.getBattleEnemySpriteLabel().setText("");
        battlePanel.getBattleEnemySpriteLabel().setIcon(createScaledSpriteIcon(sprite, getEnemySpriteSize(enemy), getEnemySpriteSize(enemy)));
        refreshBattleSpriteLabel(battlePanel.getBattleEnemySpriteLabel());
    }

    void updateHeroSprite(Hero hero, BattlePanel battlePanel) {
        BufferedImage sprite = loadHeroBattleSprite(hero);
        if (sprite == null) {
            battlePanel.getBattleHeroSpriteLabel().setIcon(null);
            battlePanel.getBattleHeroSpriteLabel().setText("");
            refreshBattleSpriteLabel(battlePanel.getBattleHeroSpriteLabel());
            return;
        }

        if (hero instanceof Gunner) {
            sprite = trimTransparentBounds(sprite);
        }

        battlePanel.getBattleHeroSpriteLabel().setText("");
        battlePanel.getBattleHeroSpriteLabel().setIcon(createScaledSpriteIcon(sprite, getHeroSpriteSize(hero), getHeroSpriteSize(hero)));
        refreshBattleSpriteLabel(battlePanel.getBattleHeroSpriteLabel());
    }

    boolean updateHeroSkill1Frame(Hero hero, BattlePanel battlePanel, int frameIndex) {
        String sheetName = hero instanceof Mage
                ? "Mleux-Skill1.png"
                : hero instanceof Gunner
                ? "Fehld-Skill1.png"
                : "Neo-Skill1.png";
        return updateHeroActionFrame(hero, battlePanel, sheetName, frameIndex);
    }

    boolean updateHeroSkill2Frame(Hero hero, BattlePanel battlePanel, int frameIndex) {
        String sheetName = hero instanceof Mage
                ? "Mleux-Skill2.png"
                : hero instanceof Gunner
                ? "Fehld-Skill2.png"
                : "Neo-Skill2.png";
        return updateHeroActionFrame(hero, battlePanel, sheetName, frameIndex);
    }

    boolean updateHeroAttackFrame(Hero hero, BattlePanel battlePanel, int frameIndex) {
        String sheetName = hero instanceof Mage
                ? "Mleux-Attack.png"
                : hero instanceof Gunner
                ? "Fehld-Attack.png"
                : "Neo-Attack.png";
        return updateHeroActionFrame(hero, battlePanel, sheetName, frameIndex);
    }

    boolean updateHeroUltimateFrame(Hero hero, BattlePanel battlePanel, int frameIndex) {
        String sheetName = hero instanceof Mage
                ? "Mleux-Ultimate.png"
                : hero instanceof Gunner
                ? "Fehld-Ultimate.png"
                : "Neo-Ultimate.png";
        return updateHeroActionFrame(hero, battlePanel, sheetName, frameIndex);
    }

    private boolean updateHeroActionFrame(Hero hero, BattlePanel battlePanel, String sheetName, int frameIndex) {
        BufferedImage sprite = loadHeroActionFrame(hero, sheetName, frameIndex);
        if (sprite == null) {
            updateHeroSprite(hero, battlePanel);
            return false;
        }

        battlePanel.getBattleHeroSpriteLabel().setText("");
        battlePanel.getBattleHeroSpriteLabel().setIcon(createScaledSpriteIcon(sprite, getHeroSkillSpriteSize(hero), getHeroSkillSpriteSize(hero)));
        refreshBattleSpriteLabel(battlePanel.getBattleHeroSpriteLabel());
        return true;
    }

    boolean updateEnemyAttackFrame(Entity enemy, BattlePanel battlePanel, int frameIndex) {
        BufferedImage sprite = loadEnemyAttackFrame(enemy, frameIndex);
        if (sprite == null) {
            return false;
        }

        battlePanel.getBattleEnemySpriteLabel().setText("");
        battlePanel.getBattleEnemySpriteLabel().setIcon(createScaledSpriteIcon(sprite, getEnemySpriteSize(enemy), getEnemySpriteSize(enemy)));
        refreshBattleSpriteLabel(battlePanel.getBattleEnemySpriteLabel());
        return true;
    }

    boolean updateEnemyRebirthFrame(Entity enemy, BattlePanel battlePanel, int frameIndex) {
        BufferedImage sprite = loadEnemyRebirthFrame(enemy, frameIndex);
        if (sprite == null) {
            return false;
        }

        battlePanel.getBattleEnemySpriteLabel().setText("");
        battlePanel.getBattleEnemySpriteLabel().setIcon(createScaledSpriteIcon(sprite, getEnemyRebirthSpriteSize(enemy), getEnemyRebirthSpriteSize(enemy)));
        refreshBattleSpriteLabel(battlePanel.getBattleEnemySpriteLabel());
        return true;
    }

    private void refreshBattleSpriteLabel(JLabel label) {
        label.setSize(label.getPreferredSize());
        label.revalidate();
        label.repaint();
        Container parent = label.getParent();
        if (parent != null) {
            parent.revalidate();
            parent.repaint();
        }
    }

    private int getEnemySpriteSize(Entity enemy) {
        if (enemy instanceof Goblin) return 132;
        if (enemy instanceof Slime) return 132;
        if (enemy instanceof MudLurker) return 145;
        if (enemy instanceof SwampRat) return 140;
        if (enemy instanceof VeilSerpent) return 140;
        if (enemy instanceof ShadowAbyss) return 150;
        if (enemy instanceof VoidBeast) return 150;
        if (enemy instanceof FadingWarden) return 150;
        if (enemy instanceof HollowKing) return 150;
        if (enemy instanceof Elderthorn) return 150;
        if (enemy instanceof Azrael) return 185;
        if (enemy instanceof Kim) return 185;
        if (enemy instanceof Morgrath) return 155;
        return 96;
    }

    private int getEnemyRebirthSpriteSize(Entity enemy) {
        if (enemy instanceof Azrael) return 230;
        return getEnemySpriteSize(enemy);
    }

    private int getHeroSpriteSize(Hero hero) {
        if (hero instanceof Swordsman) return 140;
        if (hero instanceof Mage) return 300;
        if (hero instanceof Gunner) return 220;
        return 96;
    }

    private int getHeroSkillSpriteSize(Hero hero) {
        if (hero instanceof Swordsman) return 190;
        if (hero instanceof Mage) return 300;
        if (hero instanceof Gunner) return 220;
        return 128;
    }

    private JButton createStyledButton(String text, ButtonSkin skin, Color fallbackColor,
            Color textColor, Font font, int height) {
        PixelButton button = new PixelButton(text, skin, fallbackColor);
        button.setForeground(textColor);
        button.setFont(font);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        button.setMinimumSize(new Dimension(160, height));
        button.setPreferredSize(new Dimension(220, height));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        return button;
    }

    private JButton createLandingPlaceholderButton(String text) {
        return createLandingPlaceholderButton(text, 160, 48);
    }

    private JButton createLandingPlaceholderButton(String text, int width, int height) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g2 = (Graphics2D) graphics.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Color fill = new Color(242, 247, 250, 220);
                Color border = new Color(31, 157, 231);
                Color textColor = new Color(24, 156, 229);

                if (getModel().isPressed()) {
                    fill = new Color(220, 237, 246, 235);
                } else if (getModel().isRollover()) {
                    fill = new Color(248, 251, 253, 235);
                }

                g2.setColor(fill);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(border);
                g2.setStroke(new BasicStroke(3f));
                g2.drawRect(1, 1, getWidth() - 3, getHeight() - 3);

                Font font = getHeadingFont(14f);
                g2.setFont(font);
                FontMetrics metrics = g2.getFontMetrics(font);
                int textX = (getWidth() - metrics.stringWidth(getText())) / 2;
                int textY = ((getHeight() - metrics.getHeight()) / 2) + metrics.getAscent();
                g2.setColor(textColor);
                g2.drawString(getText(), textX, textY);
                g2.dispose();
            }

            @Override
            protected void paintBorder(Graphics graphics) {
            }
        };

        button.setPreferredSize(new Dimension(width, height));
        button.setMinimumSize(new Dimension(width, height));
        button.setMaximumSize(new Dimension(width, height));
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setOpaque(false);
        button.setRolloverEnabled(true);
        return button;
    }

    private JButton createLandingImageButton(BufferedImage image, int width, String fallbackText) {
        int safeWidth = Math.max(1, width);
        int safeHeight = Math.max(1, (int) Math.round(safeWidth * (image.getHeight() / (double) image.getWidth())));
        return createLandingImageButton(image, safeWidth, safeHeight, fallbackText);
    }

    private JButton createLandingImageButton(BufferedImage image, int width, int height, String fallbackText) {
        int safeWidth = Math.max(1, width);
        int safeHeight = Math.max(1, height);

        JButton button = new JButton(fallbackText) {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g2 = (Graphics2D) graphics.create();
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                int pressOffset = getModel().isPressed() ? 2 : 0;
                int drawX = 0;
                int drawY = pressOffset;
                int drawWidth = Math.max(1, getWidth());
                int drawHeight = Math.max(1, getHeight() - pressOffset);

                if (getModel().isRollover() && !getModel().isPressed()) {
                    paintImageGlow(g2, image, drawX, drawY, drawWidth, drawHeight,
                            new Color(120, 255, 225, 150));
                    drawY = Math.max(0, drawY - 1);
                }
                g2.drawImage(image, drawX, drawY, drawWidth, drawHeight, null);
                g2.dispose();
            }

            @Override
            protected void paintBorder(Graphics graphics) {
            }
        };

        button.setPreferredSize(new Dimension(safeWidth, safeHeight));
        button.setMinimumSize(new Dimension(safeWidth, safeHeight));
        button.setMaximumSize(new Dimension(safeWidth, safeHeight));
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setOpaque(false);
        button.setRolloverEnabled(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private void paintImageGlow(Graphics2D g2, BufferedImage image, int x, int y, int width, int height, Color color) {
        BufferedImage mask = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D maskGraphics = mask.createGraphics();
        maskGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        maskGraphics.drawImage(image, 0, 0, width, height, null);
        maskGraphics.setComposite(AlphaComposite.SrcAtop);
        maskGraphics.setColor(color);
        maskGraphics.fillRect(0, 0, width, height);
        maskGraphics.dispose();

        g2.drawImage(mask, x - 1, y - 1, width + 2, height + 2, null);
        g2.drawImage(mask, x, y, width, height, null);
    }

    private BufferedImage loadImageAsset(String path) {
        String normalizedPath = path.replace('\\', '/');
        String[] candidates = normalizedPath.startsWith("src/com/ror/models/")
                ? new String[] {
                        normalizedPath,
                        normalizedPath.substring("src/com/ror/models/".length())
                }
                : new String[] {
                        normalizedPath,
                        "src/com/ror/models/" + normalizedPath
                };

        for (String candidate : candidates) {
            String resourcePath = candidate.startsWith("/") ? candidate : "/com/ror/models/" + candidate;
            try (InputStream stream = getClass().getResourceAsStream(resourcePath)) {
                if (stream != null) {
                    BufferedImage image = ImageIO.read(stream);
                    if (image != null) {
                        return image;
                    }
                }
            } catch (Exception ignored) {
            }

            BufferedImage image = loadImageFile(candidate);
            if (image != null) {
                return image;
            }
        }

        return null;
    }

    private BufferedImage loadImageFile(String path) {
        String normalizedPath = path.replace('\\', '/');
        String[] roots = {
                "",
                "MystvaleAcademy/"
        };

        for (String root : roots) {
            Path candidate = Path.of(root + normalizedPath);
            if (!Files.exists(candidate)) {
                continue;
            }

            try {
                BufferedImage image = ImageIO.read(candidate.toFile());
                if (image != null) {
                    return image;
                }
            } catch (Exception ignored) {
            }
        }

        return null;
    }

    private BufferedImage[] loadLandingBackgroundFrames() {
        java.util.List<BufferedImage> frames = new java.util.ArrayList<>();

        String[][] frameCandidates = {
                {
                        "src/com/ror/models/assets/images/mainscreen/main-screen-background-1.png",
                },
                {
                        "src/com/ror/models/assets/images/mainscreen/main-screen-background-2.png",
                },
                {
                        "src/com/ror/models/assets/images/mainscreen/main-screen-background-3.png",
                },
                {
                        "src/com/ror/models/assets/images/mainscreen/main-screen-background-4.png",
                },
                {
                        "src/com/ror/models/assets/images/mainscreen/main-screen-background-5.png",
                },
                {
                        "src/com/ror/models/assets/images/mainscreen/main-screen-background-6.png",
                },
                {
                        "src/com/ror/models/assets/images/mainscreen/main-screen-background-7.png",
                },
                {
                        "src/com/ror/models/assets/images/mainscreen/main-screen-background-8.png",
                },
                {
                        "src/com/ror/models/assets/images/mainscreen/main-screen-background-9.png",
                },
                {
                        "src/com/ror/models/assets/images/mainscreen/main-screen-background-10.png",
                },
                {
                        "src/com/ror/models/assets/images/mainscreen/main-screen-background-11.png",
                },
                {
                        "src/com/ror/models/assets/images/mainscreen/main-screen-background-12.png",
                }
        };

        for (String[] candidateGroup : frameCandidates) {
            BufferedImage frame = loadFirstAvailableImage(candidateGroup);
            if (frame != null) {
                frames.add(frame);
            }
        }

        return frames.toArray(new BufferedImage[0]);
    }

    private BufferedImage getCurrentLandingBackground() {
        if (landingBackgroundFrames.length == 0) {
            return null;
        }
        int safeIndex = Math.floorMod(landingBackgroundIndex, landingBackgroundFrames.length);
        return landingBackgroundFrames[safeIndex];
    }

    private BufferedImage loadFirstAvailableImage(String... candidates) {
        for (String candidate : candidates) {
            if (candidate.startsWith("/")) {
                try (InputStream stream = getClass().getResourceAsStream(candidate)) {
                    if (stream != null) {
                        BufferedImage image = ImageIO.read(stream);
                        if (image != null) {
                            return image;
                        }
                    }
                } catch (Exception ignored) {
                }
            } else {
                BufferedImage image = loadImageAsset(candidate);
                if (image != null) {
                    return image;
                }
            }
        }

        return null;
    }

    private BufferedImage loadLandingButtonImage(String fileName) {
        return loadFirstAvailableImage(
                "/com/ror/models/assets/images/ui/" + fileName,
                "src/com/ror/models/assets/images/ui/" + fileName,
                "assets/images/ui/" + fileName);
    }

    BufferedImage loadUIImage(String fileName) {
        return loadFirstAvailableImage(
                "/com/ror/models/assets/images/ui/" + fileName,
                "src/com/ror/models/assets/images/ui/" + fileName,
                "assets/images/ui/" + fileName);
    }

    BufferedImage loadMainScreenImage(String fileName) {
        return loadFirstAvailableImage(
                "/com/ror/models/assets/images/mainscreen/" + fileName,
                "src/com/ror/models/assets/images/mainscreen/" + fileName,
                "assets/images/mainscreen/" + fileName);
    }

    BufferedImage loadStorySceneImage(String fileName) {
        return loadFirstAvailableImage(
                "/com/ror/models/assets/images/ui/" + fileName,
                "src/com/ror/models/assets/images/ui/" + fileName,
                "assets/images/ui/" + fileName,
                "C:/Users/Shayndel Mizy Amaga/Downloads/ChatGPT Image May 15, 2026, 02_30_29 AM.png");
    }

    BufferedImage loadStoryNarrationBackgroundImage(String fileName) {
        return loadFirstAvailableImage(
                "/com/ror/models/assets/images/ui/" + fileName,
                "src/com/ror/models/assets/images/ui/" + fileName,
                "assets/images/ui/" + fileName,
                "C:/Users/Shayndel Mizy Amaga/Downloads/narration-screen.png");
    }

    BufferedImage[] loadStoryHeroWalkFrames(Hero hero) {
        if (hero instanceof Swordsman) {
            BufferedImage sheet = loadHeroImage("neo/walking/neo-walk.png");
            if (sheet == null) {
                BufferedImage walk1 = loadHeroImage("neo/Neo-walk1.png");
                BufferedImage walk2 = loadHeroImage("neo/Neo-walk2.png");
                BufferedImage idle = loadHeroImage("neo/Neo-idle.png");
                return normalizeStoryFrames(
                        new BufferedImage[] { walk1, walk2, idle, walk2, walk1 },
                        STORY_WALK_X_OFFSETS_NEO,
                        STORY_WALK_Y_OFFSETS_NEO);
            }

            return loadStoryFramesFromSheet(sheet, STORY_WALK_X_OFFSETS_NEO, STORY_WALK_Y_OFFSETS_NEO);
        }

        if (hero instanceof Mage) {
            BufferedImage sheet = loadHeroImage("mleux/walking/mleux-walk.png");
            if (sheet != null) {
                return loadStoryFramesFromSheet(sheet, STORY_WALK_X_OFFSETS_MLEUX, STORY_WALK_Y_OFFSETS_MLEUX);
            }
        }

        if (hero instanceof Gunner) {
            BufferedImage sheet = loadHeroImage("fehld/walking/fehld-walk (2).png");
            if (sheet == null) {
                sheet = loadHeroImage("fehld/walking/fehld-walk.png");
            }
            if (sheet != null) {
                int frameCount = sheet.getWidth() == 1774 ? 4 : 5;
                return loadStoryFramesFromSheet(
                        sheet,
                        frameCount,
                        STORY_WALK_X_OFFSETS_FEHLD,
                        STORY_WALK_Y_OFFSETS_FEHLD);
            }
        }

        return new BufferedImage[0];
    }

    private BufferedImage[] loadStoryFramesFromSheet(BufferedImage sheet, int[] xOffsets, int[] yOffsets) {
        return loadStoryFramesFromSheet(sheet, 5, xOffsets, yOffsets);
    }

    private BufferedImage[] loadStoryFramesFromSheet(BufferedImage sheet, int frameCount, int[] xOffsets, int[] yOffsets) {
        BufferedImage[] frames = new BufferedImage[frameCount];
        for (int frameIndex = 0; frameIndex < frameCount; frameIndex++) {
            int startX = (int) Math.round(sheet.getWidth() * (frameIndex / (double) frameCount));
            int endX = (int) Math.round(sheet.getWidth() * ((frameIndex + 1) / (double) frameCount));
            int frameWidth = Math.max(1, endX - startX);
            frames[frameIndex] = sheet.getSubimage(startX, 0, frameWidth, sheet.getHeight());
        }
        return normalizeStoryFrames(frames, xOffsets, yOffsets);
    }

    private BufferedImage[] normalizeStoryFrames(BufferedImage[] frames, int[] xOffsets, int[] yOffsets) {
        BufferedImage[] normalized = new BufferedImage[frames.length];
        int maxWidth = 1;
        int maxHeight = 1;

        for (int i = 0; i < frames.length; i++) {
            normalized[i] = trimTransparentBounds(frames[i]);
            if (normalized[i] != null) {
                maxWidth = Math.max(maxWidth, normalized[i].getWidth());
                maxHeight = Math.max(maxHeight, normalized[i].getHeight());
            }
        }

        for (int i = 0; i < normalized.length; i++) {
            BufferedImage frame = normalized[i];
            if (frame == null) continue;

            BufferedImage canvas = new BufferedImage(maxWidth + 32, maxHeight + 16, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = canvas.createGraphics();
            g2.setComposite(AlphaComposite.SrcOver);
            int manualXOffset = i < xOffsets.length ? xOffsets[i] : 0;
            int manualYOffset = i < yOffsets.length ? yOffsets[i] : 0;
            int drawX = ((canvas.getWidth() - frame.getWidth()) / 2) + manualXOffset;
            int drawY = (canvas.getHeight() - frame.getHeight()) + manualYOffset;
            g2.drawImage(frame, drawX, drawY, null);
            g2.dispose();
            normalized[i] = canvas;
        }

        return normalized;
    }

    private BufferedImage[] normalizeActionFrames(BufferedImage[] frames) {
        BufferedImage[] normalized = new BufferedImage[frames.length];
        int maxWidth = 1;
        int maxHeight = 1;

        for (int i = 0; i < frames.length; i++) {
            normalized[i] = trimTransparentBounds(frames[i]);
            if (normalized[i] != null) {
                maxWidth = Math.max(maxWidth, normalized[i].getWidth());
                maxHeight = Math.max(maxHeight, normalized[i].getHeight());
            }
        }

        for (int i = 0; i < normalized.length; i++) {
            BufferedImage frame = normalized[i];
            if (frame == null) continue;

            BufferedImage canvas = new BufferedImage(maxWidth + 32, maxHeight + 16, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = canvas.createGraphics();
            g2.setComposite(AlphaComposite.SrcOver);
            int drawX = (canvas.getWidth() - frame.getWidth()) / 2;
            int drawY = canvas.getHeight() - frame.getHeight();
            g2.drawImage(frame, drawX, drawY, null);
            g2.dispose();
            normalized[i] = canvas;
        }

        return normalized;
    }

    private BufferedImage trimTransparentBounds(BufferedImage image) {
        if (image == null) return null;

        int minX = image.getWidth();
        int minY = image.getHeight();
        int maxX = -1;
        int maxY = -1;

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int alpha = (image.getRGB(x, y) >>> 24) & 0xFF;
                if (alpha <= 10) continue;
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
            }
        }

        if (maxX < minX || maxY < minY) {
            return image;
        }

        return image.getSubimage(minX, minY, (maxX - minX) + 1, (maxY - minY) + 1);
    }

    BufferedImage loadItemImage(String fileName) {
        return loadFirstAvailableImage(
                "/com/ror/models/assets/images/items/" + fileName,
                "src/com/ror/models/assets/images/items/" + fileName,
                "assets/images/items/" + fileName);
    }

    BufferedImage prepareShopButtonImage(String fileName) {
        return prepareUiButtonImage(fileName);
    }

    private BufferedImage prepareUiButtonImage(String fileName) {
        BufferedImage raw = loadFirstAvailableImage(
                "/com/ror/models/assets/images/ui/" + fileName,
                "src/com/ror/models/assets/images/ui/" + fileName,
                "assets/images/ui/" + fileName);
        if (raw == null) return null;

        BufferedImage cleaned = removeCornerBackground(raw);
        BufferedImage trimmed = cropUiButtonBounds(cleaned);
        return trimmed;
    }

    private BufferedImage prepareUiButtonSprite(String fileName, Rectangle sourceBounds) {
        BufferedImage sheet = loadFirstAvailableImage(
                "/com/ror/models/assets/images/ui/" + fileName,
                "src/com/ror/models/assets/images/ui/" + fileName,
                "assets/images/ui/" + fileName);
        if (sheet == null || sourceBounds == null) return null;

        Rectangle safeBounds = sourceBounds.intersection(new Rectangle(0, 0, sheet.getWidth(), sheet.getHeight()));
        if (safeBounds.width <= 0 || safeBounds.height <= 0) return null;

        BufferedImage cropped = new BufferedImage(safeBounds.width, safeBounds.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = cropped.createGraphics();
        g2.drawImage(sheet, 0, 0, safeBounds.width, safeBounds.height,
                safeBounds.x, safeBounds.y, safeBounds.x + safeBounds.width, safeBounds.y + safeBounds.height, null);
        g2.dispose();

        BufferedImage cleaned = removeCornerBackground(cropped);
        return cropUiButtonBounds(cleaned);
    }

    private BufferedImage firstAvailableButton(BufferedImage primary, BufferedImage fallback) {
        return primary != null ? primary : fallback;
    }

    private BufferedImage normalizeLandingSecondaryButton(BufferedImage source) {
        if (source == null) return null;
        BufferedImage normalized = new BufferedImage(
                LANDING_SECONDARY_BUTTON_WIDTH, LANDING_SECONDARY_BUTTON_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = normalized.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        double scale = Math.min(
                LANDING_SECONDARY_BUTTON_WIDTH / (double) Math.max(1, source.getWidth()),
                LANDING_SECONDARY_BUTTON_HEIGHT / (double) Math.max(1, source.getHeight()));
        int drawWidth = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int drawHeight = Math.max(1, (int) Math.round(source.getHeight() * scale));
        int drawX = (LANDING_SECONDARY_BUTTON_WIDTH - drawWidth) / 2;
        int drawY = (LANDING_SECONDARY_BUTTON_HEIGHT - drawHeight) / 2;

        g2.drawImage(source, drawX, drawY, drawWidth, drawHeight, null);
        g2.dispose();
        return normalized;
    }

    private BufferedImage cropUiButtonBounds(BufferedImage source) {
        int minX = source.getWidth();
        int minY = source.getHeight();
        int maxX = -1;
        int maxY = -1;

        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int argb = source.getRGB(x, y);
                if (isUiBackgroundColor(argb)) continue;
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
            }
        }

        if (maxX < minX || maxY < minY) return source;

        int padding = 6;
        int cropX = Math.max(0, minX - padding);
        int cropY = Math.max(0, minY - padding);
        int cropWidth = Math.min(source.getWidth() - cropX, (maxX - minX + 1) + (padding * 2));
        int cropHeight = Math.min(source.getHeight() - cropY, (maxY - minY + 1) + (padding * 2));

        BufferedImage cropped = new BufferedImage(cropWidth, cropHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = cropped.createGraphics();
        g2.drawImage(source, 0, 0, cropWidth, cropHeight,
                cropX, cropY, cropX + cropWidth, cropY + cropHeight, null);
        g2.dispose();
        return cropped;
    }

    private BufferedImage removeCornerBackground(BufferedImage source) {
        BufferedImage cleaned = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = cleaned.createGraphics();
        g2.drawImage(source, 0, 0, null);
        g2.dispose();

        boolean[][] visited = new boolean[cleaned.getHeight()][cleaned.getWidth()];
        floodClearBackground(cleaned, visited, 0, 0);
        floodClearBackground(cleaned, visited, cleaned.getWidth() - 1, 0);
        floodClearBackground(cleaned, visited, 0, cleaned.getHeight() - 1);
        floodClearBackground(cleaned, visited, cleaned.getWidth() - 1, cleaned.getHeight() - 1);
        return cleaned;
    }

    private void floodClearBackground(BufferedImage image, boolean[][] visited, int startX, int startY) {
        if (startX < 0 || startY < 0 || startX >= image.getWidth() || startY >= image.getHeight()) return;
        if (!isUiBackgroundColor(image.getRGB(startX, startY))) return;

        Deque<Point> queue = new ArrayDeque<>();
        queue.add(new Point(startX, startY));

        while (!queue.isEmpty()) {
            Point point = queue.removeFirst();
            int x = point.x;
            int y = point.y;

            if (x < 0 || y < 0 || x >= image.getWidth() || y >= image.getHeight()) continue;
            if (visited[y][x]) continue;
            visited[y][x] = true;

            if (!isUiBackgroundColor(image.getRGB(x, y))) continue;

            image.setRGB(x, y, 0x00000000);
            queue.addLast(new Point(x + 1, y));
            queue.addLast(new Point(x - 1, y));
            queue.addLast(new Point(x, y + 1));
            queue.addLast(new Point(x, y - 1));
        }
    }

    private boolean isUiBackgroundColor(int argb) {
        int alpha = (argb >>> 24) & 0xFF;
        int red = (argb >>> 16) & 0xFF;
        int green = (argb >>> 8) & 0xFF;
        int blue = argb & 0xFF;

        if (alpha < 12) return true;

        boolean chromaGreen = green >= 150 && red <= 120 && blue <= 120 && (green - red) >= 40;
        boolean screenshotWhite = red >= 245 && green >= 245 && blue >= 245;
        return chromaGreen || screenshotWhite;
    }

    private Icon createScaledSpriteIcon(BufferedImage sprite, int targetWidth, int targetHeight) {
        BufferedImage scaled = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = scaled.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        int drawWidth = sprite.getWidth();
        int drawHeight = sprite.getHeight();
        double scale = Math.min(
                (double) targetWidth / Math.max(1, drawWidth),
                (double) targetHeight / Math.max(1, drawHeight));
        int scaledWidth = Math.max(1, (int) Math.round(drawWidth * scale));
        int scaledHeight = Math.max(1, (int) Math.round(drawHeight * scale));
        int x = (targetWidth - scaledWidth) / 2;
        int y = (targetHeight - scaledHeight) / 2;

        g2.drawImage(sprite, x, y, scaledWidth, scaledHeight, null);
        g2.dispose();
        return new ImageIcon(scaled);
    }

    private void loadFonts() {
        try (InputStream cinzelStream = getClass()
                .getResourceAsStream("/com/ror/models/assets/fonts/Cinzel-VariableFont_wght.ttf");
             InputStream garamondStream = getClass()
                .getResourceAsStream("/com/ror/models/assets/fonts/EBGaramond-VariableFont_wght.ttf")) {

            if (cinzelStream != null) {
                Font loaded = Font.createFont(Font.TRUETYPE_FONT, cinzelStream).deriveFont(Font.BOLD, headingFontSize);
                headingFont = loaded;
                GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(loaded);
            }

            if (garamondStream != null) {
                Font loaded = Font.createFont(Font.TRUETYPE_FONT, garamondStream).deriveFont(Font.PLAIN, bodyFontSize);
                bodyFont = loaded;
                GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(loaded);
            }

            applyGlobalUIFont();
        } catch (Exception exception) {
            headingFont = new Font("Serif", Font.BOLD, Math.round(headingFontSize));
            bodyFont = new Font("SansSerif", Font.PLAIN, Math.round(bodyFontSize));
            applyGlobalUIFont();
        }
    }

    private void applyGlobalUIFont() {
        FontUIResource uiFont = new FontUIResource(bodyFont.deriveFont(bodyFontSize));
        UIManager.put("Label.font", uiFont);
        UIManager.put("Button.font", uiFont);
        UIManager.put("TextField.font", uiFont);
        UIManager.put("TextArea.font", uiFont);
        UIManager.put("TextPane.font", uiFont);
        UIManager.put("ComboBox.font", uiFont);
        UIManager.put("List.font", uiFont);
        UIManager.put("Table.font", uiFont);
        UIManager.put("ProgressBar.selectionForeground", Color.WHITE);
        UIManager.put("ProgressBar.selectionBackground", Color.WHITE);
    }

    private void applyFontsToAllComponents(Container container) {
        for (Component comp : container.getComponents()) {
            if (comp instanceof JLabel label) {
                Object role = label.getClientProperty("fontRole");
                label.setFont("heading".equals(role) ? getHeadingFont(headingFontSize) : getBodyFont(bodyFontSize));
                String text = label.getText();
                if (text != null && text.startsWith("<html>")) {
                    label.setText(styleHtml(text.substring(6, text.length() - 7)));
                }
            } else if (comp instanceof JButton button) {
                button.setFont(getBodyFont(button.getFont().getSize2D()));
            } else if (comp instanceof JTextArea textArea) {
                textArea.setFont(getBodyFont(bodyFontSize));
            } else if (comp instanceof JTextField textField) {
                textField.setFont(getBodyFont(bodyFontSize));
            }
            if (comp instanceof Container child) {
                applyFontsToAllComponents(child);
            }
        }
    }

    private BufferedImage loadEnemySprite(Entity enemy) {
        if (enemy == null) return null;
        if (enemy instanceof Goblin) return loadEnemyImage("goblin.png");
        if (enemy instanceof Slime) return loadEnemyImage("slime.png");
        if (enemy instanceof MudLurker) return loadEnemyImage("mudlurker.png");
        if (enemy instanceof SwampRat) return loadEnemyImage("swamprat.png");
        if (enemy instanceof VeilSerpent) return loadEnemyImage("swamprat.png");
        if (enemy instanceof ShadowAbyss) return loadEnemyImage("shadowabyss.png");
        if (enemy instanceof VoidBeast) return loadEnemyImage("voidbeast.png");
        if (enemy instanceof FadingWarden) return loadEnemyImage("fadingwarden.png");
        if (enemy instanceof HollowKing) return loadEnemyImage("hollowking.png");
        if (enemy instanceof Elderthorn) return loadEnemyImage("elderthorn.png");
        if (enemy instanceof Azrael) return loadEnemyImage("azrael.png");
        if (enemy instanceof Kim) return loadEnemyImage("kim.png");
        if (enemy instanceof Morgrath) return loadEnemyImage("morgrath.png");
        return null;
    }

    private BufferedImage loadEnemyAttackFrame(Entity enemy, int frameIndex) {
        if (enemy instanceof Goblin || enemy instanceof Slime) {
            String[] frames = { "goblin-frame2.png", "goblin-frame3.png", "goblin-frame4.png" };
            if (frameIndex >= 0 && frameIndex < frames.length) {
                return loadEnemyImage(frames[frameIndex]);
            }
        }
        if (enemy instanceof Elderthorn) {
            String[] frames = { "elderthorn-frame2.png", "elderthorn-frame3.png", "elderthorn-frame4.png" };
            if (frameIndex >= 0 && frameIndex < frames.length) {
                return loadEnemyImage(frames[frameIndex]);
            }
        }
        if (enemy instanceof MudLurker) {
            String[] frames = { "mudlurker-frame2.png", "mudlurker-frame3.png", "mudlurker-frame4.png" };
            if (frameIndex >= 0 && frameIndex < frames.length) {
                return loadEnemyImage(frames[frameIndex]);
            }
        }
        if (enemy instanceof SwampRat || enemy instanceof VeilSerpent) {
            String[] frames = { "swamprat-frame2.png", "swamprat-frame3.png", "swamprat-frame4.png" };
            if (frameIndex >= 0 && frameIndex < frames.length) {
                return loadEnemyImage(frames[frameIndex]);
            }
        }
        if (enemy instanceof ShadowAbyss) {
            String[] frames = { "shadowabyss-frame2.png", "shadowabyss-frame3.png", "shadowabyss-frame4.png" };
            if (frameIndex >= 0 && frameIndex < frames.length) {
                return loadEnemyImage(frames[frameIndex]);
            }
        }
        if (enemy instanceof VoidBeast) {
            String[] frames = { "voidbeast-frame2.png", "voidbeast-frame3.png", "voidbeast-frame4.png" };
            if (frameIndex >= 0 && frameIndex < frames.length) {
                return loadEnemyImage(frames[frameIndex]);
            }
        }
        if (enemy instanceof FadingWarden) {
            String[] frames = { "fadingwarden-frame2.png", "fadingwarden-frame3.png", "fadingwarden-frame4.png" };
            if (frameIndex >= 0 && frameIndex < frames.length) {
                return loadEnemyImage(frames[frameIndex]);
            }
        }
        if (enemy instanceof HollowKing) {
            String[] frames = { "hollowking-frame2.png", "hollowking-frame3.png", "hollowking-frame4.png" };
            if (frameIndex >= 0 && frameIndex < frames.length) {
                return loadEnemyImage(frames[frameIndex]);
            }
        }
        if (enemy instanceof Azrael) {
            String[] frames = { "azrael-frame2.png", "azrael-frame3.png", "azrael-frame4.png" };
            if (frameIndex >= 0 && frameIndex < frames.length) {
                return loadEnemyImage(frames[frameIndex]);
            }
        }
        if (enemy instanceof Kim) {
            String[] frames = { "kim-frame2.png", "kim-frame3.png", "kim-frame4.png" };
            if (frameIndex >= 0 && frameIndex < frames.length) {
                return loadEnemyImage(frames[frameIndex]);
            }
        }
        if (enemy instanceof Morgrath) {
            String[] frames = { "morgrath-frame2.png", "morgrath-frame3.png", "morgrath-frame4.png" };
            if (frameIndex >= 0 && frameIndex < frames.length) {
                return loadEnemyImage(frames[frameIndex]);
            }
        }
        return null;
    }

    private BufferedImage loadEnemyRebirthFrame(Entity enemy, int frameIndex) {
        if (enemy instanceof Azrael) {
            String[] frames = {
                    "azrael-rebirth-frame1.png",
                    "azrael-rebirth-frame2.png",
                    "azrael-rebirth-frame3.png",
                    "azrael-rebirth-frame4.png",
                    "azrael-rebirth-frame5.png"
            };
            if (frameIndex >= 0 && frameIndex < frames.length) {
                return loadEnemyImage(frames[frameIndex]);
            }
        }
        return null;
    }

    private BufferedImage loadEnemyImage(String fileName) {
        BufferedImage image = loadFirstAvailableImage(
                "/com/ror/models/assets/images/enemies/" + fileName,
                "src/com/ror/models/assets/images/enemies/" + fileName,
                ENEMY_IMAGE_DIRECTORY + fileName);
        return image == null ? null : removeCornerBackground(image);
    }

    private BufferedImage loadHeroBattleSprite(Hero hero) {
        if (hero instanceof Swordsman) {
            BufferedImage idle = loadHeroImage("neo/Neo-idle.png");
            if (idle != null) {
                return idle;
            }

            BufferedImage walkSheet = loadHeroImage("neo/walking/neo-walk.png");
            if (walkSheet != null) {
                int frameWidth = Math.max(1, walkSheet.getWidth() / 5);
                return walkSheet.getSubimage(0, 0, frameWidth, walkSheet.getHeight());
            }
        }
        if (hero instanceof Mage) {
            return loadHeroImage("mleux/idle.png");
        }
        if (hero instanceof Gunner) {
            BufferedImage walkSheet = loadHeroImage("fehld/walking/fehld-walk (2).png");
            if (walkSheet == null) {
                walkSheet = loadHeroImage("fehld/walking/fehld-walk.png");
            }
            if (walkSheet != null) {
                int frameCount = walkSheet.getWidth() == 1774 ? 4 : 5;
                int frameWidth = Math.max(1, walkSheet.getWidth() / frameCount);
                return walkSheet.getSubimage(0, 0, frameWidth, walkSheet.getHeight());
            }
        }
        return null;
    }

    private BufferedImage loadHeroActionFrame(Hero hero, String sheetName, int frameIndex) {
        if (hero instanceof Swordsman) {
            BufferedImage sheet = loadHeroImage("neo/skills/" + sheetName);
            if (sheet == null) {
                return null;
            }

            int frameCount = 3;
            if (frameIndex < 0 || frameIndex >= frameCount) {
                return null;
            }

            int frameWidth = sheet.getWidth() / frameCount;
            return sheet.getSubimage(frameIndex * frameWidth, 0, frameWidth, sheet.getHeight());
        }
        if (hero instanceof Mage && sheetName.startsWith("Mleux-")) {
            if ("Mleux-Skill2.png".equals(sheetName)) {
                BufferedImage[] frames = loadMageSkill2Frames();
                if (frameIndex < 0 || frameIndex >= frames.length) {
                    return null;
                }
                return frames[frameIndex];
            }

            String mageSheetPath = "Mleux-Skill1.png".equals(sheetName)
                    ? "mleux/skills/skill1/skill1main.png"
                    : "Mleux-Ultimate.png".equals(sheetName)
                    ? "mleux/skills/ulti/ulti.png"
                    : "Mleux-Attack.png".equals(sheetName)
                    ? "mleux/skills/basic attack/basic-attack.png"
                    : "mleux/skills/" + sheetName;
            BufferedImage sheet = loadHeroImage(mageSheetPath);
            if (sheet == null) {
                return null;
            }

            int frameCount = 3;
            if (frameIndex < 0 || frameIndex >= frameCount) {
                return null;
            }

            int frameWidth = sheet.getWidth() / frameCount;
            return sheet.getSubimage(frameIndex * frameWidth, 0, frameWidth, sheet.getHeight());
        }
        if (hero instanceof Gunner && sheetName.startsWith("Fehld-")) {
            String gunnerSheetPath = "Fehld-Skill1.png".equals(sheetName)
                    ? "fehld/skills/skill1/skill1.png"
                    : "Fehld-Skill2.png".equals(sheetName)
                    ? "fehld/skills/skill2/skill2.png"
                    : "Fehld-Attack.png".equals(sheetName)
                    ? "fehld/skills/basic attack/basic-attack.png"
                    : "Fehld-Ultimate.png".equals(sheetName)
                    ? "fehld/skills/ulti/ulti.png"
                    : null;
            if (gunnerSheetPath == null) {
                return null;
            }

            BufferedImage sheet = loadHeroImage(gunnerSheetPath);
            if (sheet == null) {
                return null;
            }

            BufferedImage[] frames = loadNormalizedFramesFromSheet(sheet, 3);
            if (frameIndex < 0 || frameIndex >= frames.length) {
                return null;
            }
            return frames[frameIndex];
        }
        return null;
    }

    private BufferedImage[] loadMageSkill2Frames() {
        BufferedImage sheet = loadHeroImage("mleux/skills/skill2/skill2.png");
        if (sheet == null) {
            return new BufferedImage[0];
        }

        return loadNormalizedFramesFromSheet(sheet, 3);
    }

    private BufferedImage[] loadNormalizedFramesFromSheet(BufferedImage sheet, int frameCount) {
        BufferedImage[] frames = new BufferedImage[frameCount];
        for (int frameIndex = 0; frameIndex < frameCount; frameIndex++) {
            int startX = (int) Math.round(sheet.getWidth() * (frameIndex / (double) frameCount));
            int endX = (int) Math.round(sheet.getWidth() * ((frameIndex + 1) / (double) frameCount));
            int frameWidth = Math.max(1, endX - startX);
            frames[frameIndex] = sheet.getSubimage(startX, 0, frameWidth, sheet.getHeight());
        }
        return normalizeActionFrames(frames);
    }

    private BufferedImage loadHeroImage(String path) {
        return loadFirstAvailableImage(
                "/com/ror/models/assets/images/heroes/" + path,
                "src/com/ror/models/assets/images/heroes/" + path,
                HERO_IMAGE_DIRECTORY + path);
    }

    private ButtonSkin loadButtonSkin(String baseName) {
        BufferedImage normal = loadImageAsset(UI_IMAGE_DIRECTORY + baseName + ".png");
        BufferedImage hover = loadImageAsset(UI_IMAGE_DIRECTORY + baseName + "-hover.png");
        BufferedImage pressed = loadImageAsset(UI_IMAGE_DIRECTORY + baseName + "-pressed.png");
        BufferedImage disabled = loadImageAsset(UI_IMAGE_DIRECTORY + baseName + "-disabled.png");
        return new ButtonSkin(normal, hover, pressed, disabled);
    }

    private static final class ButtonSkin {
        private final BufferedImage normal;
        private final BufferedImage hover;
        private final BufferedImage pressed;
        private final BufferedImage disabled;

        private ButtonSkin(BufferedImage normal, BufferedImage hover, BufferedImage pressed, BufferedImage disabled) {
            this.normal = normal;
            this.hover = hover;
            this.pressed = pressed;
            this.disabled = disabled;
        }

        private BufferedImage resolve(ButtonModel model, boolean rollover) {
            if (!model.isEnabled()) return disabled != null ? disabled : normal;
            if (model.isPressed()) return pressed != null ? pressed : hover != null ? hover : normal;
            if (rollover && hover != null) return hover;
            return normal;
        }

        private boolean hasAnyImage() {
            return normal != null || hover != null || pressed != null || disabled != null;
        }
    }

    private static final class PixelButton extends JButton {
        private final ButtonSkin skin;
        private final Color fallbackColor;
        private boolean rollover;

        private PixelButton(String text, ButtonSkin skin, Color fallbackColor) {
            super(text);
            this.skin = skin;
            this.fallbackColor = fallbackColor;

            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));
            setHorizontalTextPosition(JButton.CENTER);
            setVerticalTextPosition(JButton.CENTER);
            setRolloverEnabled(true);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent event) {
                    rollover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent event) {
                    rollover = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            if (skin != null && skin.hasAnyImage()) {
                BufferedImage image = skin.resolve(getModel(), rollover);
                if (image != null) {
                    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                    g2.drawImage(image, 0, 0, getWidth(), getHeight(), null);
                }
            } else {
                g2.setColor(resolveFallbackColor());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            }
            g2.dispose();
            super.paintComponent(graphics);
        }

        private Color resolveFallbackColor() {
            if (!isEnabled()) return fallbackColor.darker();
            if (getModel().isPressed()) return fallbackColor.darker();
            if (rollover) return fallbackColor.brighter();
            return fallbackColor;
        }
    }
}
