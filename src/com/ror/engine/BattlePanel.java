package com.ror.engine;

import com.ror.models.Hero;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class BattlePanel extends JPanel {
    private static final String COMBAT_IMAGE_DIRECTORY = "src/com/ror/models/assets/images/combat/";
    private static final String FOREST_REVERIE_BATTLE_BACKGROUND = "forest_reverie_battle_background_panel.png";
    private static final String REVERIES_EDGE_BATTLE_BACKGROUND = "reveries_edge_battle_background_panel.png";
    private static final String FORSAKEN_LANDS_BATTLE_BACKGROUND = "forsaken_lands_battle_background_panel.png";
    private static final String ENEMY_SUMMARY_PANEL = "enemy_summary_panel.png";

    // Battle-related fields moved from GameWindow
    private final JLabel battleTitleValue = new JLabel("Battle");
    private final JLabel battleRoundValue = new JLabel("Round 1");
    private final JLabel battleHeroNameValue = new JLabel("-");
    private final JLabel battleHeroLevelValue = new JLabel("Level -");
    private final JLabel battleEnemyNameValue = new JLabel("-");
    private final JLabel battleHeroSpriteLabel = new JLabel();
    private final JLabel battleEffectSpriteLabel = new JLabel("", SwingConstants.CENTER);
    private final JLabel battleEnemySpriteLabel = new JLabel(
            createBattleSpriteIcon("src/com/ror/models/assets/images/enemies/goblin.png", 72));
    private final JProgressBar battleHeroHpBar = new JProgressBar();
    private final JProgressBar battleHeroManaBar = new JProgressBar();
    private final JProgressBar battleEnemyHpBar = new JProgressBar();
    private final JProgressBar battleEnemyManaBar = new JProgressBar();
    private final JTextArea battleLogArea = new JTextArea();
    private JPanel battleActionCards;
    private JPanel battleButtonsPanel;
    private JPanel battleImagePanel;
    private JPanel battleSpriteStrip;
    private JButton battleBasicButton;
    private JButton battlePotionButton;
    private JButton battleRunButton;
    private JButton battleSkill1Button;
    private JButton battleSkill2Button;
    private JButton battleUltimateButton;
    private boolean battleButtonsVisible = true;
    private int heroSpriteOffsetX;
    private int enemySpriteOffsetX;
    private float battleEffectProgress = -1f;
    private int battleEffectVerticalOffset;
    private BufferedImage battleArenaBackgroundImage;

    // Colors
    private static final Color COLOR_BATTLE_PANEL = new Color(0x00041B);
    private static final Color COLOR_BATTLE_SCREEN_BG = new Color(0x00041B);
    private static final Color COLOR_BATTLE_SURFACE = new Color(0x00041B);
    private static final Color COLOR_BATTLE_SURFACE_HOVER = new Color(0x06144A);
    private static final Color COLOR_BATTLE_SURFACE_PRESSED = new Color(0x00020F);
    private static final Color COLOR_BATTLE_SURFACE_DISABLED = new Color(0x11162A);
    private static final Color COLOR_BATTLE_BG = new Color(0x00041B);
    private static final Color COLOR_BATTLE_PLACEHOLDER = new Color(0x00041B);
    private static final Color COLOR_BATTLE_BORDER = new Color(20, 49, 126);
    private static final Color COLOR_BATTLE_BUTTON_AREA = new Color(0, 2, 14);
    private static final Color COLOR_BATTLE_BUTTON_BORDER = new Color(20, 49, 126);
    private static final Color COLOR_BATTLE_TITLE = Color.WHITE;
    private static final Color COLOR_BATTLE_INFO_SURFACE = new Color(0x00041B);
    private static final Color COLOR_BATTLE_INFO_TEXT = Color.WHITE;
    private static final Color COLOR_TEXT_DARK = Color.WHITE;
    private static final Color COLOR_TEXT_MUTED = Color.WHITE;
    private static final Color COLOR_BORDER = Color.decode("#4D5B9E");
    private static final Color COLOR_HERO_HP = new Color(164, 54, 54);
    private static final Color COLOR_HERO_MANA = new Color(52, 92, 156);

    private final Font headingFont;
    private final Font bodyFont;
    private final BattleActionListener actionListener;

    public interface BattleActionListener {
        void onBattleAction(int action);

        void onShowAttackPanel();
    }

    public BattlePanel(Font headingFont, Font bodyFont, BattleActionListener actionListener) {
        this.headingFont = headingFont;
        this.bodyFont = bodyFont;
        this.actionListener = actionListener;
        initialize();
    }

    private void initialize() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(true);
        setBackground(COLOR_BATTLE_SCREEN_BG);
        // Keep vertical breathing room but let center arena touch horizontal edges.
        setBorder(BorderFactory.createEmptyBorder(16, 0, 16, 0));

        battleTitleValue.setForeground(COLOR_BATTLE_TITLE);
        battleTitleValue.setFont(getHeadingFont(34f));
        battleHeroLevelValue.setFont(getBodyFont(14f).deriveFont(Font.BOLD, 14f));
        battleHeroLevelValue.setForeground(COLOR_TEXT_MUTED);

        JPanel topRow = new JPanel(new BorderLayout(12, 0));
        topRow.setOpaque(false);
        topRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));

        JPanel enemySummary = createEnemySummaryPanel();
        enemySummary.setPreferredSize(new Dimension(760, 118));
        enemySummary.setMinimumSize(new Dimension(760, 118));
        enemySummary.setMaximumSize(new Dimension(760, 118));

        JPanel titleWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 22, 10));
        titleWrap.setOpaque(false);
        titleWrap.add(battleTitleValue);

        JPanel enemyWrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 20));
        enemyWrap.setOpaque(false);
        enemyWrap.add(enemySummary);

        topRow.add(titleWrap, BorderLayout.WEST);
        topRow.add(enemyWrap, BorderLayout.CENTER);

        add(topRow);
        add(Box.createVerticalStrut(18));
        add(createBattleArenaPanel());
        add(Box.createVerticalStrut(22));

        battleBasicButton = createBattleButton("Fight");
        battlePotionButton = createBattleButton("Inventory");
        battleRunButton = createBattleButton("Run");
        battleSkill1Button = createBattleButton("Skill 1");
        battleSkill2Button = createBattleButton("Skill 2");
        battleUltimateButton = createBattleButton("Ultimate");

        battleBasicButton.addActionListener(event -> actionListener.onShowAttackPanel());
        battlePotionButton.addActionListener(event -> actionListener.onBattleAction(4));
        battleRunButton.addActionListener(event -> actionListener.onBattleAction(5));
        battleSkill1Button.addActionListener(event -> actionListener.onBattleAction(1));
        battleSkill2Button.addActionListener(event -> actionListener.onBattleAction(2));
        battleUltimateButton.addActionListener(event -> actionListener.onBattleAction(3));

        battleActionCards = new JPanel(new CardLayout());
        battleActionCards.setOpaque(true);
        battleActionCards.setBackground(COLOR_BATTLE_SCREEN_BG);

        battleButtonsPanel = new JPanel(new GridLayout(1, 3, 18, 0));
        battleButtonsPanel.setOpaque(true);
        battleButtonsPanel.setBackground(COLOR_BATTLE_BUTTON_AREA);
        battleButtonsPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BATTLE_BUTTON_BORDER, 1),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)));
        battleButtonsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 108));
        battleButtonsPanel.add(battleBasicButton);
        battleButtonsPanel.add(battlePotionButton);
        battleButtonsPanel.add(battleRunButton);

        JPanel attackPanel = new JPanel();
        attackPanel.setLayout(new BoxLayout(attackPanel, BoxLayout.Y_AXIS));
        attackPanel.setOpaque(true);
        attackPanel.setBackground(COLOR_BATTLE_BUTTON_AREA);
        attackPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BATTLE_BUTTON_BORDER, 1),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)));
        JPanel attackRow = new JPanel(new GridLayout(1, 3, 22, 0));
        attackRow.setOpaque(false);
        JButton battleAttackButton = createBattleButton("Attack");
        battleAttackButton.addActionListener(e -> actionListener.onBattleAction(0));
        attackRow.add(battleAttackButton);
        attackRow.add(battleSkill1Button);
        attackRow.add(battleSkill2Button);
        attackPanel.add(attackRow);
        attackPanel.add(Box.createVerticalStrut(16));
        JPanel ultimateRow = new JPanel(new GridLayout(1, 1));
        ultimateRow.setOpaque(false);
        battleUltimateButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 78));
        ultimateRow.add(battleUltimateButton);
        attackPanel.add(ultimateRow);

        battleImagePanel = new JPanel(new BorderLayout());
        battleImagePanel.setOpaque(true);
        battleImagePanel.setBackground(COLOR_BATTLE_PLACEHOLDER);
        battleImagePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BATTLE_BORDER, 1),
                BorderFactory.createEmptyBorder(18, 18, 18, 18)));
        JLabel battleImageLabel = new JLabel("<html><center>Battle Image Placeholder</center></html>",
                SwingConstants.CENTER);
        battleImageLabel.setFont(getBodyFont(16f).deriveFont(Font.BOLD, 16f));
        battleImageLabel.setForeground(COLOR_TEXT_DARK);
        battleImagePanel.add(battleImageLabel, BorderLayout.CENTER);

        battleActionCards.add(battleButtonsPanel, "buttons");
        battleActionCards.add(attackPanel, "attack");
        battleActionCards.add(battleImagePanel, "image");

        JPanel heroPanel = createBattleHeroPanel();
        heroPanel.setPreferredSize(new Dimension(360, 200));

        JPanel bottomRow = new JPanel(new BorderLayout(18, 0));
        bottomRow.setOpaque(false);
        bottomRow.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 0));
        bottomRow.add(heroPanel, BorderLayout.WEST);
        bottomRow.add(battleActionCards, BorderLayout.CENTER);

        add(bottomRow);
        add(Box.createVerticalStrut(12));
    }

    private Font getHeadingFont(float size) {
        return headingFont.deriveFont(size);
    }

    private Font getBodyFont(float size) {
        return bodyFont.deriveFont(size);
    }

    private static Icon createBattleSpriteIcon(String path, int size) {
        try {
            BufferedImage sprite = ImageIO.read(new File(path));
            BufferedImage scaled = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics2D = scaled.createGraphics();
            graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

            int drawWidth = sprite.getWidth();
            int drawHeight = sprite.getHeight();
            double scale = Math.min((double) size / drawWidth, (double) size / drawHeight);
            int scaledWidth = Math.max(1, (int) Math.round(drawWidth * scale));
            int scaledHeight = Math.max(1, (int) Math.round(drawHeight * scale));
            int x = (size - scaledWidth) / 2;
            int y = (size - scaledHeight) / 2;

            graphics2D.drawImage(sprite, x, y, scaledWidth, scaledHeight, null);
            graphics2D.dispose();
            return new ImageIcon(scaled);
        } catch (IOException exception) {
            return null;
        }
    }

    private JButton createBattleButton(String text) {
        BufferedImage image = loadBattleButtonImage(text);
        if (image != null) {
            return createImageBattleButton(text, image, getBattleButtonSourceBounds(text, image));
        }

        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D graphics2D = (Graphics2D) graphics.create();
                graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                ButtonModel model = getModel();
                Color fill = getBackground();
                if (!model.isEnabled()) {
                    fill = COLOR_BATTLE_SURFACE_DISABLED;
                } else if (model.isPressed()) {
                    fill = COLOR_BATTLE_SURFACE_PRESSED;
                } else if (model.isRollover()) {
                    fill = COLOR_BATTLE_SURFACE_HOVER;
                }
                graphics2D.setColor(fill);
                graphics2D.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
                graphics2D.dispose();
                super.paintComponent(graphics);
            }

            @Override
            protected void paintBorder(Graphics graphics) {
                Graphics2D graphics2D = (Graphics2D) graphics.create();
                graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                graphics2D.setColor(COLOR_BATTLE_BORDER);
                graphics2D.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
                graphics2D.dispose();
            }
        };
        button.setFont(getBodyFont(16f).deriveFont(Font.BOLD, 16f));
        button.setForeground(COLOR_TEXT_DARK);
        button.setBackground(COLOR_BATTLE_SURFACE);
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        button.setFocusPainted(false);
        button.setRolloverEnabled(true);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        return button;
    }

    private JButton createImageBattleButton(String text, BufferedImage image, Rectangle sourceBounds) {
        JButton button = new JButton() {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D graphics2D = (Graphics2D) graphics.create();
                graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING,
                        RenderingHints.VALUE_RENDER_QUALITY);
                graphics2D.drawImage(image,
                        0, 0, getWidth(), getHeight(),
                        sourceBounds.x, sourceBounds.y,
                        sourceBounds.x + sourceBounds.width,
                        sourceBounds.y + sourceBounds.height,
                        null);

                ButtonModel model = getModel();
                if (!model.isEnabled()) {
                    graphics2D.setColor(new Color(5, 7, 15, 135));
                    graphics2D.fillRect(0, 0, getWidth(), getHeight());
                } else if (model.isPressed()) {
                    graphics2D.setColor(new Color(0, 0, 0, 65));
                    graphics2D.fillRect(0, 0, getWidth(), getHeight());
                } else if (model.isRollover()) {
                    graphics2D.setColor(new Color(40, 121, 255, 28));
                    graphics2D.fillRect(0, 0, getWidth(), getHeight());
                }
                paintButtonStatus(graphics2D, this);
                graphics2D.dispose();
            }
        };
        button.setText(text);
        button.setName(text);
        button.getAccessibleContext().setAccessibleName(text);
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setFocusPainted(false);
        button.setRolloverEnabled(true);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 78));
        button.setPreferredSize(new Dimension(image.getWidth(), 78));
        return button;
    }

    private void paintButtonStatus(Graphics2D graphics2D, JComponent component) {
        Object status = component.getClientProperty("battleStatusText");
        if (!(status instanceof String statusText) || statusText.isBlank()) {
            return;
        }

        Font statusFont = getBodyFont(14f).deriveFont(Font.BOLD, 14f);
        graphics2D.setFont(statusFont);
        FontMetrics metrics = graphics2D.getFontMetrics(statusFont);
        int horizontalPadding = 10;
        int badgeWidth = metrics.stringWidth(statusText) + (horizontalPadding * 2);
        int badgeHeight = metrics.getHeight() + 6;
        int badgeX = Math.max(8, component.getWidth() - badgeWidth - 18);
        int badgeY = Math.max(8, (component.getHeight() - badgeHeight) / 2);

        graphics2D.setColor(new Color(0, 4, 27, 210));
        graphics2D.fillRoundRect(badgeX, badgeY, badgeWidth, badgeHeight, 12, 12);
        graphics2D.setColor(new Color(86, 156, 255, 210));
        graphics2D.drawRoundRect(badgeX, badgeY, badgeWidth - 1, badgeHeight - 1, 12, 12);
        graphics2D.setColor(Color.WHITE);
        int textX = badgeX + horizontalPadding;
        int textY = badgeY + ((badgeHeight - metrics.getHeight()) / 2) + metrics.getAscent();
        graphics2D.drawString(statusText, textX, textY);
    }

    private Rectangle getBattleButtonSourceBounds(String text, BufferedImage image) {
        return switch (text) {
            case "Fight", "Inventory", "Run" -> new Rectangle(0, 0, image.getWidth(), image.getHeight());
            case "Attack", "Skill 1", "Skill 2", "Ultimate" -> new Rectangle(0, 0, image.getWidth(), image.getHeight());
            default -> new Rectangle(0, 0, image.getWidth(), image.getHeight());
        };
    }

    private BufferedImage loadBattleButtonImage(String text) {
        String fileName = switch (text) {
            case "Fight" -> "fight_button_panel.png";
            case "Inventory" -> "inventory_button_panel.png";
            case "Run" -> "run_button_panel.png";
            case "Attack" -> "combat_attack_button_arcane.png";
            case "Skill 1" -> "combat_skill1_button_arcane.png";
            case "Skill 2" -> "combat_skill2_button_arcane.png";
            case "Ultimate" -> "combat_ultimate_button_arcane.png";
            default -> null;
        };
        if (fileName == null) {
            return null;
        }

        return loadCombatImage(fileName);
    }

    private BufferedImage loadCombatImage(String fileName) {
        try {
            return ImageIO.read(new File(COMBAT_IMAGE_DIRECTORY + fileName));
        } catch (IOException exception) {
            return null;
        }
    }

    private JPanel createBattleSummaryCard(String label, JLabel name, JProgressBar hp, JProgressBar mana) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setOpaque(true);
        card.setBackground(COLOR_BATTLE_SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER, 1),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)));

        name.setForeground(COLOR_TEXT_DARK);
        name.setFont(getBodyFont(28f).deriveFont(Font.BOLD, 28f));

        if (label != null && !label.isBlank()) {
            JLabel title = new JLabel(label);
            title.setFont(getBodyFont(14f).deriveFont(Font.BOLD, 14f));
            title.setForeground(COLOR_TEXT_DARK);
            card.add(title);
            card.add(Box.createVerticalStrut(4));
        }
        card.add(name);
        card.add(Box.createVerticalStrut(18));

        hp.setStringPainted(true);
        hp.setForeground(COLOR_HERO_HP);
        hp.setBackground(COLOR_BATTLE_BG);
        mana.setStringPainted(true);
        mana.setForeground(COLOR_HERO_MANA);
        mana.setBackground(COLOR_BATTLE_BG);

        JLabel hpLabel = new JLabel("Health");
        hpLabel.setFont(getBodyFont(13f).deriveFont(Font.BOLD, 13f));
        hpLabel.setForeground(COLOR_TEXT_DARK);
        card.add(hpLabel);
        card.add(hp);
        card.add(Box.createVerticalStrut(8));
        JLabel manaLabel = new JLabel("Mana");
        manaLabel.setFont(getBodyFont(13f).deriveFont(Font.BOLD, 13f));
        manaLabel.setForeground(COLOR_TEXT_DARK);
        card.add(manaLabel);
        card.add(mana);
        return card;
    }

    private JPanel createBattleHeroPanel() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setOpaque(true);
        card.setBackground(COLOR_BATTLE_INFO_SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER, 1),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)));

        battleHeroNameValue.setForeground(COLOR_BATTLE_INFO_TEXT);
        battleHeroNameValue.setFont(getBodyFont(28f).deriveFont(Font.BOLD, 28f));
        battleHeroLevelValue.setForeground(COLOR_BATTLE_INFO_TEXT);
        battleHeroLevelValue.setFont(getBodyFont(16f).deriveFont(Font.PLAIN, 16f));

        battleHeroHpBar.setStringPainted(true);
        battleHeroHpBar.setForeground(COLOR_HERO_HP);
        battleHeroHpBar.setBackground(COLOR_BATTLE_BG);
        battleHeroHpBar.setFont(getBodyFont(14f));
        battleHeroManaBar.setStringPainted(true);
        battleHeroManaBar.setForeground(COLOR_HERO_MANA);
        battleHeroManaBar.setBackground(COLOR_BATTLE_BG);
        battleHeroManaBar.setFont(getBodyFont(14f));

        card.add(battleHeroNameValue);
        card.add(Box.createVerticalStrut(4));
        card.add(battleHeroLevelValue);
        card.add(Box.createVerticalStrut(18));
        JLabel hpLabel = new JLabel("Health");
        hpLabel.setFont(getBodyFont(13f).deriveFont(Font.BOLD, 13f));
        hpLabel.setForeground(COLOR_BATTLE_INFO_TEXT);
        card.add(hpLabel);
        card.add(battleHeroHpBar);
        card.add(Box.createVerticalStrut(8));
        JLabel manaLabel = new JLabel("Mana");
        manaLabel.setFont(getBodyFont(13f).deriveFont(Font.BOLD, 13f));
        manaLabel.setForeground(COLOR_BATTLE_INFO_TEXT);
        card.add(manaLabel);
        card.add(battleHeroManaBar);
        return card;
    }

    private JPanel createBattleArenaPanel() {
        JPanel arena = new JPanel(new BorderLayout());
        arena.setOpaque(true);
        arena.setBackground(COLOR_BATTLE_BUTTON_AREA);
        arena.setBorder(BorderFactory.createLineBorder(COLOR_BATTLE_BUTTON_BORDER, 1));
        arena.setPreferredSize(new Dimension(0, 300));

        battleSpriteStrip = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                if (battleArenaBackgroundImage == null) {
                    return;
                }

                Graphics2D graphics2D = (Graphics2D) graphics.create();
                graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                graphics2D.drawImage(battleArenaBackgroundImage, 0, 0, getWidth(), getHeight(), null);
                graphics2D.dispose();
            }

            @Override
            public void doLayout() {
                Dimension heroSize = battleHeroSpriteLabel.getPreferredSize();
                Dimension effectSize = battleEffectSpriteLabel.getPreferredSize();
                Dimension enemySize = battleEnemySpriteLabel.getPreferredSize();
                int groundY = Math.max(12, getHeight() - 44);
                int heroX = Math.max(34, Math.round((getWidth() * 0.44f) - (heroSize.width / 2f)) + heroSpriteOffsetX);
                int enemyX = Math.max(34, Math.round((getWidth() * 0.56f) - (enemySize.width / 2f)) + enemySpriteOffsetX);

                battleHeroSpriteLabel.setBounds(
                        heroX,
                        Math.max(8, groundY - heroSize.height),
                        heroSize.width,
                        heroSize.height);
                battleEnemySpriteLabel.setBounds(
                        enemyX,
                        Math.max(8, groundY - enemySize.height),
                        enemySize.width,
                        enemySize.height);
                if (battleEffectSpriteLabel.isVisible() && effectSize.width > 0 && effectSize.height > 0) {
                    int heroCenterX = heroX + Math.round(heroSize.width * 0.58f);
                    int enemyCenterX = enemyX + Math.round(enemySize.width * 0.18f);
                    int effectCenterX = Math.round(heroCenterX
                            + ((enemyCenterX - heroCenterX) * Math.max(0f, Math.min(1f, battleEffectProgress))));
                    int heroTopY = Math.max(8, groundY - heroSize.height);
                    int effectY = Math.max(8,
                            heroTopY + (heroSize.height / 2) - (effectSize.height / 2) - 20 + battleEffectVerticalOffset);
                    battleEffectSpriteLabel.setBounds(
                            effectCenterX - (effectSize.width / 2),
                            effectY,
                            effectSize.width,
                            effectSize.height);
                } else {
                    battleEffectSpriteLabel.setBounds(0, 0, 0, 0);
                }
            }
        };
        battleSpriteStrip.setOpaque(true);
        battleSpriteStrip.setBackground(COLOR_BATTLE_BUTTON_AREA);

        battleHeroSpriteLabel.setHorizontalAlignment(SwingConstants.LEFT);
        battleHeroSpriteLabel.setVerticalAlignment(SwingConstants.CENTER);
        battleHeroSpriteLabel.setSize(battleHeroSpriteLabel.getPreferredSize());
        battleEffectSpriteLabel.setHorizontalAlignment(SwingConstants.CENTER);
        battleEffectSpriteLabel.setVerticalAlignment(SwingConstants.CENTER);
        battleEffectSpriteLabel.setVisible(false);
        battleEnemySpriteLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        battleEnemySpriteLabel.setVerticalAlignment(SwingConstants.CENTER);
        battleEnemySpriteLabel.setSize(battleEnemySpriteLabel.getPreferredSize());

        battleSpriteStrip.add(battleHeroSpriteLabel);
        battleSpriteStrip.add(battleEnemySpriteLabel);
        battleSpriteStrip.add(battleEffectSpriteLabel);
        arena.add(battleSpriteStrip, BorderLayout.CENTER);

        return arena;
    }

    private JPanel createEnemySummaryPanel() {
        BufferedImage backgroundImage = loadCombatImage(ENEMY_SUMMARY_PANEL);
        JPanel card = new JPanel(new BorderLayout(20, 0)) {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D graphics2D = (Graphics2D) graphics.create();
                graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                if (backgroundImage != null) {
                    graphics2D.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), null);
                } else {
                    graphics2D.setColor(COLOR_BATTLE_INFO_SURFACE);
                    graphics2D.fillRect(0, 0, getWidth(), getHeight());
                }
                graphics2D.dispose();
                super.paintComponent(graphics);
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(18, 28, 18, 28));

        battleEnemyNameValue.setForeground(COLOR_BATTLE_INFO_TEXT);
        battleEnemyNameValue.setFont(getBodyFont(22f).deriveFont(Font.BOLD, 22f));
        battleEnemyNameValue.setHorizontalAlignment(SwingConstants.LEFT);

        battleEnemyHpBar.setForeground(COLOR_HERO_HP);
        battleEnemyManaBar.setForeground(COLOR_HERO_MANA);

        JPanel barsPanel = new JPanel();
        barsPanel.setLayout(new BoxLayout(barsPanel, BoxLayout.Y_AXIS));
        barsPanel.setOpaque(false);

        barsPanel.add(createEnemyBarRow("Health", battleEnemyHpBar));
        barsPanel.add(Box.createVerticalStrut(10));
        barsPanel.add(createEnemyBarRow("Mana", battleEnemyManaBar));

        card.add(battleEnemyNameValue, BorderLayout.WEST);
        card.add(barsPanel, BorderLayout.CENTER);
        return card;
    }

    private JPanel createEnemyBarRow(String labelText, JProgressBar bar) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);

        JLabel label = new JLabel(labelText);
        label.setFont(getBodyFont(14f).deriveFont(Font.PLAIN, 14f));
        label.setForeground(COLOR_BATTLE_INFO_TEXT);
        label.setPreferredSize(new Dimension(62, 20));

        bar.setStringPainted(true);
        bar.setFont(getBodyFont(13f).deriveFont(Font.BOLD, 13f));
        bar.setPreferredSize(new Dimension(360, 20));
        bar.setBackground(COLOR_BATTLE_BG);

        row.add(label, BorderLayout.WEST);
        row.add(bar, BorderLayout.CENTER);
        return row;
    }

    // Methods to control the battle UI
    public void showBattleImage() {
        if (battleActionCards == null)
            return;
        CardLayout layout = (CardLayout) battleActionCards.getLayout();
        layout.show(battleActionCards, "image");
        battleButtonsVisible = false;
    }

    public void showAttackPanel() {
        if (battleActionCards == null)
            return;
        CardLayout layout = (CardLayout) battleActionCards.getLayout();
        layout.show(battleActionCards, "attack");
        battleButtonsVisible = false;
    }

    public void restoreBattleButtons() {
        if (battleActionCards == null)
            return;
        CardLayout layout = (CardLayout) battleActionCards.getLayout();
        layout.show(battleActionCards, "buttons");
        battleButtonsVisible = true;
        setBattleButtonsEnabled(true);
    }

    public void tryRestoreBattleButtons() {
        if (!battleButtonsVisible) {
            restoreBattleButtons();
        }
    }

    public void setBattleButtonsEnabled(boolean enabled) {
        battleBasicButton.setEnabled(enabled);
        battlePotionButton.setEnabled(enabled);
        battleRunButton.setEnabled(enabled);
        battleSkill1Button.setEnabled(enabled);
        battleSkill2Button.setEnabled(enabled);
        battleUltimateButton.setEnabled(enabled);
    }

    public void updateHeroActionButtonCooldowns(Hero hero) {
        if (hero == null) {
            return;
        }

        updateCooldownButton(battleSkill1Button, "Skill 1", hero.getCooldown1());
        updateCooldownButton(battleSkill2Button, "Skill 2", hero.getCooldown2());
        updateCooldownButton(battleUltimateButton, "Ultimate", hero.getCooldownU());
    }

    private void updateCooldownButton(JButton button, String baseText, int cooldown) {
        int safeCooldown = Math.max(0, cooldown);
        String statusText = safeCooldown > 0 ? "CD " + safeCooldown : "";
        button.putClientProperty("battleStatusText", statusText);
        button.setText(safeCooldown > 0 ? baseText + " (" + statusText + ")" : baseText);
        button.setEnabled(safeCooldown == 0);
        button.repaint();
    }

    public void setHeroSpriteOffsetX(int offsetX) {
        heroSpriteOffsetX = offsetX;
        refreshBattleSpriteLayout();
    }

    public void setEnemySpriteOffsetX(int offsetX) {
        enemySpriteOffsetX = offsetX;
        refreshBattleSpriteLayout();
    }

    public void resetBattleSpriteOffsets() {
        heroSpriteOffsetX = 0;
        enemySpriteOffsetX = 0;
        refreshBattleSpriteLayout();
    }

    public void setBattleEffectIcon(Icon icon, float progress, int verticalOffset) {
        battleEffectProgress = progress;
        battleEffectVerticalOffset = verticalOffset;
        battleEffectSpriteLabel.setText("");
        battleEffectSpriteLabel.setIcon(icon);
        battleEffectSpriteLabel.setSize(battleEffectSpriteLabel.getPreferredSize());
        battleEffectSpriteLabel.setVisible(icon != null);
        refreshBattleSpriteLayout();
    }

    public void clearBattleEffect() {
        battleEffectProgress = -1f;
        battleEffectVerticalOffset = 0;
        battleEffectSpriteLabel.setIcon(null);
        battleEffectSpriteLabel.setText("");
        battleEffectSpriteLabel.setVisible(false);
        refreshBattleSpriteLayout();
    }

    private void refreshBattleSpriteLayout() {
        if (battleSpriteStrip != null) {
            battleSpriteStrip.revalidate();
            battleSpriteStrip.repaint();
        }
    }

    public void setBattleBackgroundForArea(String areaName) {
        battleArenaBackgroundImage = switch (areaName) {
            case "Forest of Reverie" -> loadCombatImage(FOREST_REVERIE_BATTLE_BACKGROUND);
            case "Reverie's Edge" -> loadCombatImage(REVERIES_EDGE_BATTLE_BACKGROUND);
            case "Forsaken Lands" -> loadCombatImage(FORSAKEN_LANDS_BATTLE_BACKGROUND);
            default -> null;
        };

        if (battleSpriteStrip != null) {
            battleSpriteStrip.repaint();
        }
    }

    // Getters for updating UI
    public JLabel getBattleTitleValue() {
        return battleTitleValue;
    }

    public JLabel getBattleRoundValue() {
        return battleRoundValue;
    }

    public JLabel getBattleHeroNameValue() {
        return battleHeroNameValue;
    }

    public JLabel getBattleHeroLevelValue() {
        return battleHeroLevelValue;
    }

    public JLabel getBattleEnemyNameValue() {
        return battleEnemyNameValue;
    }

    public JProgressBar getBattleHeroHpBar() {
        return battleHeroHpBar;
    }

    public JProgressBar getBattleHeroManaBar() {
        return battleHeroManaBar;
    }

    public JProgressBar getBattleEnemyHpBar() {
        return battleEnemyHpBar;
    }

    public JProgressBar getBattleEnemyManaBar() {
        return battleEnemyManaBar;
    }

    public JTextArea getBattleLogArea() {
        return battleLogArea;
    }

    public JLabel getBattleHeroSpriteLabel() {
        return battleHeroSpriteLabel;
    }

    public JLabel getBattleEnemySpriteLabel() {
        return battleEnemySpriteLabel;
    }
}
