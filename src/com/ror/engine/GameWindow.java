package com.ror.engine;

import com.ror.engine.narration.Narration;
import com.ror.models.*;
import com.ror.models.Boss.*;
import com.ror.models.Inventory.Inventory;
import com.ror.models.Mobs.*;
import com.ror.models.training.StatProgress;
import com.ror.utils.sounds.SoundManager;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.*;
import javax.swing.border.Border;

public class GameWindow implements BattlePanel.BattleActionListener {

    // Asset directories and screen card names
    private static final String ROOT_LANDING = "landing";
    private static final String ROOT_GAME = "game";
    private static final String SCREEN_HOME = "home";
    private static final String SCREEN_CHARACTER = "character";
    private static final String SCREEN_STORY = "story";
    private static final String SCREEN_MAIN = "main";
    private static final String SCREEN_ACADEMY = "academy";
    private static final String SCREEN_SHOP = "shop";
    private static final String SCREEN_INVENTORY = "inventory";
    private static final String SCREEN_PROFILE = "profile";
    private static final String SCREEN_BATTLE = "battle";

    private final DecimalFormat statFormat = new DecimalFormat("#,##0");
    private final Random random = new Random();
    private final StatProgress statProgress = new StatProgress();
    private final GameWindowGraphics graphics = new GameWindowGraphics();
    private final Icon smallHealthPotionIcon = loadItemIcon("small-health-potion.png");
    private final Icon mediumHealthPotionIcon = loadItemIcon("medium-health-potion.png");
    private final Icon largeHealthPotionIcon = loadItemIcon("large-health-potion.png");
    private final Icon smallManaPotionIcon = loadItemIcon("small-mana-potion.png");
    private final Icon mediumManaPotionIcon = loadItemIcon("medium-mana-potion.png");
    private final Icon largeManaPotionIcon = loadItemIcon("large-mana-potion.png");
    private final Icon shopkeeperPrincipalIcon = loadPixelUiIcon("shopkeeper-principal.png", 380, 380);
    private final BufferedImage adventureOverviewPanelImage =
            graphics.loadMainScreenImage("adventure-overview-panel.png");
    private final BufferedImage journeyStatusPanelImage =
            graphics.loadMainScreenImage("journey-status-panel.png");
    private final BufferedImage regionMapPanelImage =
            graphics.loadMainScreenImage("region-map-panel.png");
    private final BufferedImage academyMapPanelImage =
            graphics.loadMainScreenImage("academy-map-panel.png");
    private final BufferedImage travelDestinationsPanelImage =
            graphics.loadMainScreenImage("travel-destinations-panel.png");
    private final BufferedImage chooseCharacterBackgroundImage =
            graphics.loadMainScreenImage("choose-character-background.png");
    private final BufferedImage shopBackgroundImage = graphics.loadUIImage("shop-background.jpg");
    private final BufferedImage shopHeaderBackgroundImage = graphics.loadUIImage("shop-header-background.png");
    private final BufferedImage storySceneBackgroundImage = graphics.loadStorySceneImage("story-opening-background.png");
    private final BufferedImage storyNarrationBackgroundImage =
            graphics.loadStoryNarrationBackgroundImage("story-narration-background.png");
    private BufferedImage[] storyHeroWalkFrames = new BufferedImage[0];

    // Legacy console area components
    private final JTextArea outputArea = new JTextArea();
    private final JTextField inputField = new JTextField();
    private final JButton sendButton = new JButton("Enter");
    private final StringBuilder pendingLegacyOutput = new StringBuilder();
    private static final int LEGACY_OUTPUT_TAIL_LIMIT = 500;
    private static final Pattern BOXED_YES_NO_PROMPT = Pattern.compile(
            "(?s)\\n?┌[^\\n]*┐\\n(?:│[^\\n]*\\n)+?└[^\\n]*┘\\n?(?:-->\\|\\s*)?");
    private static final Pattern INLINE_YES_NO_PROMPT = Pattern.compile(
            "\\n?[^\\n]*\\(y/n\\):\\s*");

    // Layout
    private final CardLayout rootLayout = new CardLayout();
    private final JPanel rootPanel = new JPanel(rootLayout);
    private final CardLayout screenLayout = new CardLayout();
    private final JPanel screenPanel = new JPanel(screenLayout);

    // Theme colors
    private static final Color COLOR_BACKGROUND = new Color(227, 221, 212);
    private static final Color COLOR_HEADER_BACKGROUND = new Color(0x0A0343);
    private static final Color COLOR_PANEL = new Color(239, 235, 228);
    private static final Color COLOR_ACADEMY_PANEL = new Color(0x050923);
    private static final Color COLOR_ACADEMY_CARD = new Color(0x07143A);
    private static final Color COLOR_ACADEMY_BORDER = new Color(0x1F78D6);
    private static final Color COLOR_ACADEMY_TEXT = new Color(0xF2F7FF);
    private static final Color COLOR_ACADEMY_TEXT_MUTED = new Color(0x8EDBFF);
    private static final Color COLOR_BATTLE_PANEL = new Color(0x00041B);
    private static final Color COLOR_BATTLE_SURFACE = new Color(0x00041B);
    private static final Color COLOR_BATTLE_BG = new Color(71, 54, 44);
    private static final Color COLOR_BATTLE_PLACEHOLDER = new Color(125, 100, 88);
    private static final Color COLOR_BATTLE_BORDER = new Color(109, 88, 70);
    private static final Color COLOR_TEXT_DARK = new Color(46, 31, 20);
    private static final Color COLOR_TEXT_MUTED = new Color(106, 79, 60);
    private static final Color COLOR_BORDER = new Color(145, 114, 91);
    private static final Color COLOR_BUTTON_PRIMARY = new Color(0x0B1033);
    private static final Color COLOR_BUTTON_BORDER = new Color(0x2B356E);
    private static final Color COLOR_SHOP_OUTSIDE = new Color(30, 28, 40);
    private static final Color COLOR_SHOP_TEXT = new Color(246, 239, 221);
    private static final Color COLOR_SHOP_TEXT_MUTED = new Color(211, 190, 154);
    private static final Color COLOR_SHOP_BORDER = new Color(93, 87, 111);
    private static final Color COLOR_HERO_HP = new Color(164, 54, 54);
    private static final Color COLOR_HERO_MANA = new Color(52, 92, 156);
    private static final Color COLOR_STORY_BORDER = new Color(86, 108, 188, 150);
    private static final Color COLOR_CHARSEL_BACKGROUND = new Color(227, 221, 212);
    private static final Color COLOR_CHARSEL_PANEL = new Color(239, 235, 228);
    private static final Color COLOR_CHARSEL_TEXT_DARK = new Color(46, 31, 20);
    private static final Color COLOR_CHARSEL_TEXT_MUTED = new Color(106, 79, 60);

    // Header components
    private final JLabel titleLabel = new JLabel("Mystvale Academy");
    private final JLabel subtitleLabel = new JLabel("GUI RPG");
    private JPanel headerPanel;
    private JButton headerBackButton;
    private JButton headerSaveButton;
    private JButton headerExitButton;
    private JPanel gameShellPanel;
    private JPanel leftPane;
    private Border defaultLeftPaneBorder;
    private Border defaultHeaderBorder;

    // Hero dashboard components
    private final JLabel heroNameValue = new JLabel("-");
    private final JLabel heroClassValue = new JLabel("-");
    private final JLabel heroLevelValue = new JLabel("-");
    private final JLabel heroGoldValue = new JLabel("-");
    private final JProgressBar hpBar = new JProgressBar();
    private final JProgressBar manaBar = new JProgressBar();

    // Journey status labels
    private final JLabel journeyLocationLabel = createBody("Current location: No hero selected");
    private final JLabel journeyCompletionLabel = createBody("No hero chosen yet.");
    private final JLabel journeyObjectivesLabel = createBody("No objectives yet. Choose a hero to start.");

    // Inventory panel labels
    private final JLabel inventorySummary = new JLabel("No hero selected.");
    private final JLabel smallHealthCount = new JLabel("0");
    private final JLabel mediumHealthCount = new JLabel("0");
    private final JLabel largeHealthCount = new JLabel("0");
    private final JLabel smallManaCount = new JLabel("0");
    private final JLabel mediumManaCount = new JLabel("0");
    private final JLabel largeManaCount = new JLabel("0");

    // Shop labels
    private final JLabel shopGoldLabel = new JLabel("Gold: -");
    private final JLabel shopStatusLabel = new JLabel("Choose an item and quantity.");
    private static final int[] SHOP_PRICES = { 450, 1350, 2750, 450, 1350, 2750 };
    private static final String[] SHOP_ITEM_NAMES = {
            "Small Health Potion", "Medium Health Potion", "Large Health Potion",
            "Small Mana Potion", "Medium Mana Potion", "Large Mana Potion"
    };
    private static final String[] SHOP_ITEM_DETAILS = {
            "Restores 20% HP.",
            "Restores 45% HP.",
            "Restores 70% HP.",
            "Restores 20% Mana.",
            "Restores 45% Mana.",
            "Restores 70% Mana."
    };

    // Profile panel labels
    private final JLabel profileName = new JLabel("-");
    private final JLabel profileClass = new JLabel("-");
    private final JLabel profileWeapon = new JLabel("-");
    private final JLabel profileLevel = new JLabel("-");
    private final JLabel profileAttack = new JLabel("-");
    private final JLabel profileDefense = new JLabel("-");
    private final JLabel profileSpeed = new JLabel("-");
    private final JLabel profileSkill1 = new JLabel("-");
    private final JLabel profileSkill2 = new JLabel("-");
    private final JLabel profileUltimate = new JLabel("-");

    // Story screen components
    private final JTextArea storyTextArea = new JTextArea();
    private final JLabel storyTitleLabel = new JLabel("Story");
    private final JLabel storyProgressLabel = new JLabel("Scene 1 / 1");
    private final StoryScenePreviewPanel storyScenePreviewPanel = new StoryScenePreviewPanel();
    private Timer storyTypewriterTimer;
    private String currentStoryLine = "";
    private static final int STORY_TYPEWRITER_DELAY_MS = 14;

    // Area / quick-choice buttons (assigned during build)
    private JButton forestButton;
    private JButton swampButton;
    private JButton forsakenButton;
    private JButton storyNextButton;
    private JButton storySkipButton;
    private JButton storyStartButton;
    private JPanel quickChoicePanel;
    private JLabel quickChoicePromptLabel;
    private JButton yesChoiceButton;
    private JButton noChoiceButton;

    // Battle action synchronization
    private final Object battleActionLock = new Object();
    private volatile Integer pendingBattleAction = null;
    private final transitions transitionManager = new transitions();

    // State
    private Hero hero;
    private String[] currentStorySequence = new String[0];
    private int currentStoryIndex = 0;
    private boolean inventoryNarrationShown = false;
    private JFrame frame;
    private AdventurePanel overlay;
    private BattlePanel battlePanel;

    // Piped streams for legacy console I/O
    private final PipedInputStream gameInputStream;
    private final PipedOutputStream gameInputWriter;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public GameWindow() {
        try {
            gameInputStream = new PipedInputStream();
            gameInputWriter = new PipedOutputStream(gameInputStream);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to initialize GUI input stream.", exception);
        }
    }

    // -------------------------------------------------------------------------
    // Launch
    // -------------------------------------------------------------------------

    public void launchGame() {
        wireConsoleStreams();
        frame = buildFrame();
        setupBattleBackspaceBinding();
        frame.setVisible(true);
            System.out.println("DEBUG: Working Directory = " + System.getProperty("user.dir"));
        showLandingScreen();
        SoundManager.playMusic("src/com/ror/models/assets/sounds/titleScreen.wav");
    }

    // -------------------------------------------------------------------------
    // Frame + shell
    // -------------------------------------------------------------------------

    private JFrame buildFrame() {
        JFrame window = new JFrame("Mystvale Academy RPG");
        window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        window.setMinimumSize(new Dimension(1280, 800));
        window.setSize(1280, 800);
        window.setResizable(false);
        window.setContentPane(rootPanel);

        overlay = new AdventurePanel(window, getHeadingFont(30f), getBodyFont(16f));
        window.setGlassPane(overlay);

        battlePanel = new BattlePanel(getHeadingFont(30f), getBodyFont(16f), this);

        rootPanel.add(buildLandingScreen(), ROOT_LANDING);
        rootPanel.add(buildGameShell(), ROOT_GAME);
        window.setLocationRelativeTo(null);

        window.addWindowListener(new java.awt.event.WindowAdapter() {
        @Override
        public void windowClosing(java.awt.event.WindowEvent e) {
            System.out.println("Closing game... stopping audio.");
            SoundManager.shutdownSound();
            System.exit(0);
        }
    });

        return window;
    }

    private JPanel buildGameShell() {
        JPanel panel = new JPanel(new BorderLayout(18, 0)) {
            @Override
            protected void paintComponent(Graphics graphicsContext) {
                if (COLOR_SHOP_OUTSIDE.equals(getBackground()) && shopHeaderBackgroundImage != null) {
                    Graphics2D g2 = (Graphics2D) graphicsContext.create();
                    paintShopSkyBackground(g2, getWidth(), getHeight(), 92);
                    g2.dispose();
                    return;
                }

                Graphics2D g2 = (Graphics2D) graphicsContext.create();
                g2.setColor(COLOR_HEADER_BACKGROUND);
                g2.fillRect(0, 0, getWidth(), getHeight());

                g2.dispose();
            }
        };
        gameShellPanel = panel;
        panel.setBackground(COLOR_HEADER_BACKGROUND);

        headerPanel = buildHeader();
        leftPane = buildLeftPane();

        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(leftPane, BorderLayout.CENTER);
        return panel;
    }

    private void setupBattleBackspaceBinding() {
        if (frame == null) return;

        InputMap inputMap = frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = frame.getRootPane().getActionMap();
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "battle.restoreButtons");
        actionMap.put("battle.restoreButtons", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                battlePanel.tryRestoreBattleButtons();
            }
        });
    }

    // -------------------------------------------------------------------------
    // Header
    // -------------------------------------------------------------------------

    private JPanel buildHeader() {
        JPanel panel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics graphicsContext) {
                if (COLOR_SHOP_OUTSIDE.equals(getBackground())) {
                    return;
                }

                Graphics2D g2 = (Graphics2D) graphicsContext.create();
                g2.setColor(COLOR_HEADER_BACKGROUND);
                g2.fillRect(0, 0, getWidth(), getHeight());

                g2.dispose();
            }
        };
        panel.setPreferredSize(new Dimension(1280, 112));
        panel.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));
        defaultHeaderBorder = panel.getBorder();
        panel.setBackground(COLOR_HEADER_BACKGROUND);
        panel.setOpaque(true);

        titleLabel.setFont(getHeadingFont(30f));
        titleLabel.setForeground(Color.WHITE);

        subtitleLabel.setFont(getBodyFont(15f));
        subtitleLabel.setForeground(new Color(210, 224, 255));

        JPanel textBlock = new JPanel();
        textBlock.setLayout(new BoxLayout(textBlock, BoxLayout.Y_AXIS));
        textBlock.setOpaque(false);
        textBlock.add(titleLabel);
        textBlock.add(Box.createVerticalStrut(4));
        textBlock.add(subtitleLabel);

        headerBackButton = new JButton("Back to Academy");
        headerBackButton.setFont(getBodyFont(13f).deriveFont(Font.BOLD, 13f));
        headerBackButton.setForeground(COLOR_TEXT_DARK);
        headerBackButton.setFocusPainted(false);
        headerBackButton.setContentAreaFilled(false);
        headerBackButton.setOpaque(false);
        headerBackButton.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        headerBackButton.setPreferredSize(new Dimension(140, 34));
        headerBackButton.setVisible(false);
        headerBackButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                headerBackButton.setForeground(isShopHeaderActive() ? COLOR_SHOP_TEXT_MUTED : COLOR_TEXT_MUTED);
            }
            @Override
            public void mouseExited(MouseEvent event) {
                headerBackButton.setForeground(isShopHeaderActive() ? COLOR_SHOP_TEXT : COLOR_TEXT_DARK);
            }
        });
        headerBackButton.addActionListener(event -> {
            subtitleLabel.setText("Explore Mystvale Academy.");
            refreshHeroDashboard();
            showScreen(SCREEN_ACADEMY);
        });

        headerSaveButton = new JButton("Save");
        headerSaveButton.setFont(getBodyFont(13f).deriveFont(Font.BOLD, 13f));
        headerSaveButton.setForeground(Color.WHITE);
        headerSaveButton.setFocusPainted(false);
        headerSaveButton.setContentAreaFilled(false);
        headerSaveButton.setOpaque(false);
        headerSaveButton.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        headerSaveButton.setPreferredSize(new Dimension(90, 34));
        headerSaveButton.setVisible(false);
        headerSaveButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                headerSaveButton.setForeground(isShopHeaderActive() ? COLOR_SHOP_TEXT_MUTED : COLOR_TEXT_MUTED);
            }
            @Override
            public void mouseExited(MouseEvent event) {
                headerSaveButton.setForeground(isShopHeaderActive() ? COLOR_SHOP_TEXT : Color.WHITE);
            }
        });
        headerSaveButton.addActionListener(event -> saveCurrentGame());

        headerExitButton = new JButton("Exit");
        headerExitButton.setFont(getBodyFont(13f).deriveFont(Font.BOLD, 13f));
        headerExitButton.setForeground(Color.WHITE);
        headerExitButton.setFocusPainted(false);
        headerExitButton.setContentAreaFilled(false);
        headerExitButton.setOpaque(false);
        headerExitButton.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        headerExitButton.setPreferredSize(new Dimension(90, 34));
        headerExitButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                headerExitButton.setForeground(isShopHeaderActive() ? COLOR_SHOP_TEXT_MUTED : COLOR_TEXT_MUTED);
            }
            @Override
            public void mouseExited(MouseEvent event) {
                headerExitButton.setForeground(isShopHeaderActive() ? COLOR_SHOP_TEXT : Color.WHITE);
            }
        });
        headerExitButton.addActionListener(event -> {
            int choice = showConfirmSync("Exit", "Do you want to go back to the main menu?");
            if (choice == JOptionPane.YES_OPTION) {
                showLandingScreen();
            }
        });

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        actions.add(headerBackButton);
        actions.add(headerSaveButton);
        actions.add(headerExitButton);

        panel.add(textBlock, BorderLayout.WEST);
        panel.add(actions, BorderLayout.EAST);
        return panel;
    }

    // -------------------------------------------------------------------------
    // Left pane + screen panel
    // -------------------------------------------------------------------------

    private JPanel buildLeftPane() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(1220, 720));
        defaultLeftPaneBorder = BorderFactory.createEmptyBorder(0, 22, 22, 22);
        panel.setBorder(defaultLeftPaneBorder);
        panel.setBackground(COLOR_HEADER_BACKGROUND);
        panel.setOpaque(false);

        screenPanel.setOpaque(false);
        screenPanel.add(buildHomeScreen(), SCREEN_HOME);
        screenPanel.add(buildCharacterScreen(), SCREEN_CHARACTER);
        screenPanel.add(buildStoryScreen(), SCREEN_STORY);
        screenPanel.add(buildMainScreen(), SCREEN_MAIN);
        screenPanel.add(buildAcademyScreen(), SCREEN_ACADEMY);
        screenPanel.add(buildShopScreen(), SCREEN_SHOP);
        screenPanel.add(buildInventoryScreen(), SCREEN_INVENTORY);
        screenPanel.add(buildProfileScreen(), SCREEN_PROFILE);
        screenPanel.add(battlePanel, SCREEN_BATTLE);

        panel.add(screenPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel buildRightPane() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 22, 22));
        panel.setBackground(COLOR_BACKGROUND);

        outputArea.setEditable(false);
        outputArea.setLineWrap(true);
        outputArea.setWrapStyleWord(true);
        outputArea.setFont(getBodyFont(14f));
        outputArea.setBackground(new Color(20, 24, 31));
        outputArea.setForeground(new Color(234, 239, 245));
        outputArea.setMargin(new Insets(14, 14, 14, 14));

        JScrollPane scrollPane = new JScrollPane(outputArea);
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(61, 48, 39), 1),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)));

        JPanel inputPanel = new JPanel(new BorderLayout(8, 8));
        inputPanel.setBackground(COLOR_BACKGROUND);

        JLabel inputHelp = new JLabel("Game input");
        inputHelp.setForeground(COLOR_TEXT_MUTED);

        inputField.addActionListener(event -> submitLegacyInput());
        sendButton.addActionListener(event -> submitLegacyInput());

        JButton clearButton = createSecondaryButton("Clear Log");
        clearButton.addActionListener(event -> outputArea.setText(""));

        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.add(inputField, BorderLayout.CENTER);
        row.add(sendButton, BorderLayout.EAST);

        quickChoicePanel = new JPanel(new GridLayout(1, 2, 8, 0));
        quickChoicePanel.setOpaque(false);
        quickChoicePromptLabel = new JLabel("Choose:");
        quickChoicePromptLabel.setForeground(COLOR_TEXT_MUTED);
        quickChoicePromptLabel.setVisible(false);
        yesChoiceButton = createSecondaryButton("Yes");
        noChoiceButton = createSecondaryButton("No");
        yesChoiceButton.addActionListener(event -> submitLegacyShortcut("y"));
        noChoiceButton.addActionListener(event -> submitLegacyShortcut("n"));
        quickChoicePanel.add(yesChoiceButton);
        quickChoicePanel.add(noChoiceButton);
        quickChoicePanel.setVisible(false);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.add(inputHelp, BorderLayout.CENTER);
        footer.add(clearButton, BorderLayout.EAST);

        inputPanel.add(footer, BorderLayout.NORTH);
        inputPanel.add(row, BorderLayout.CENTER);

        JPanel quickChoiceWrapper = new JPanel(new BorderLayout(0, 6));
        quickChoiceWrapper.setOpaque(false);
        quickChoiceWrapper.add(quickChoicePromptLabel, BorderLayout.NORTH);
        quickChoiceWrapper.add(quickChoicePanel, BorderLayout.CENTER);
        inputPanel.add(quickChoiceWrapper, BorderLayout.SOUTH);

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(inputPanel, BorderLayout.SOUTH);
        return panel;
    }

    // -------------------------------------------------------------------------
    // Screen builders
    // -------------------------------------------------------------------------
    
    private JPanel buildHomeScreen() {
        JPanel panel = createCardPanel();

        JLabel heading = createHeading("Main Screen");
        JLabel body = createBody("Choose your hero and begin.");

        JButton chooseHeroButton = createPrimaryButton("Choose Hero");
        chooseHeroButton.addActionListener(event -> {
            subtitleLabel.setText("Choose a hero to begin.");
            showScreen(SCREEN_CHARACTER);
        });

        JButton titleScreenButton = createSecondaryButton("Return to Title");
        titleScreenButton.addActionListener(event -> showLandingScreen());

        panel.add(heading);
        panel.add(Box.createVerticalStrut(10));
        panel.add(body);
        panel.add(Box.createVerticalStrut(18));
        panel.add(chooseHeroButton);
        panel.add(Box.createVerticalStrut(10));
        panel.add(titleScreenButton);
        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private JPanel buildCharacterScreen() {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics graphicsContext) {
                Graphics2D g2 = (Graphics2D) graphicsContext.create();
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                if (chooseCharacterBackgroundImage != null) {
                    g2.drawImage(chooseCharacterBackgroundImage, 0, 0, getWidth(), getHeight(), null);
                } else {
                    g2.setColor(COLOR_CHARSEL_BACKGROUND);
                    g2.fillRect(0, 0, getWidth(), getHeight());
                }
                g2.dispose();
                super.paintComponent(graphicsContext);
            }
        };
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(34, 38, 34, 34));

        JLabel heading = new JLabel("CHOOSE YOUR HERO");
        heading.setFont(getHeadingFont(26f));
        heading.setForeground(Color.WHITE);
        heading.setAlignmentX(Component.CENTER_ALIGNMENT);
        heading.setHorizontalAlignment(SwingConstants.CENTER);

        /*JLabel introLabel = new JLabel("Every path is different. Pick the one that speaks to you.");
        introLabel.setFont(getHeadingFont(26f));
        introLabel.setForeground(COLOR_CHARSEL_TEXT_DARK);
        introLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        introLabel.setHorizontalAlignment(SwingConstants.CENTER);*/

        //panel.add(Box.createVerticalStrut(8));
        panel.add(heading);
        //panel.add(Box.createVerticalStrut(6));
        //panel.add(introLabel);
        panel.add(Box.createVerticalStrut(15));

        JPanel cardsRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 24, 0));
        cardsRow.setOpaque(false);
        cardsRow.setAlignmentX(Component.CENTER_ALIGNMENT);
        cardsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 520));

        cardsRow.add(createCharacterButton(
                "Swordsman", "Neo", "Frontline Fighter",
                () -> selectHero(new Swordsman())));
        cardsRow.add(createCharacterButton(
                "Gunner", "Fehld", "Ranged Attacker",
                () -> selectHero(new Gunner())));
        cardsRow.add(createCharacterButton(
                "Mage", "Mleux", "Spell Caster",
                () -> selectHero(new Mage())));

        panel.add(cardsRow);
        panel.add(Box.createVerticalStrut(25));

        JButton backButton = createCharacterSelectButton("← Back to Title");
        backButton.addActionListener(event -> showLandingScreen());
        backButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        backButton.setMaximumSize(new Dimension(220, 44));
        panel.add(backButton);
        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private JPanel buildMainScreen() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBackground(COLOR_HEADER_BACKGROUND);
        panel.setOpaque(true);

        JPanel topRow = new JPanel(new GridLayout(1, 2, 12, 0));
        topRow.setOpaque(false);

        JPanel adventPanel = createImageCardPanel(adventureOverviewPanelImage);
        adventPanel.setLayout(new BoxLayout(adventPanel, BoxLayout.Y_AXIS));
        adventPanel.add(createMainScreenHeading("Adventure Overview"));
        adventPanel.add(Box.createVerticalStrut(10));
        adventPanel.add(buildHeroSummaryCard());

        JPanel statusPanel = createImageCardPanel(journeyStatusPanelImage);
        statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.Y_AXIS));
        statusPanel.add(createMainScreenHeading("Journey Status"));
        statusPanel.add(Box.createVerticalStrut(10));
        journeyLocationLabel.setForeground(Color.WHITE);
        journeyCompletionLabel.setForeground(Color.WHITE);
        journeyObjectivesLabel.setForeground(Color.WHITE);
        statusPanel.add(journeyLocationLabel);
        statusPanel.add(Box.createVerticalStrut(8));
        statusPanel.add(journeyCompletionLabel);
        statusPanel.add(Box.createVerticalStrut(10));
        statusPanel.add(createMainScreenHeading("Objectives"));
        statusPanel.add(Box.createVerticalStrut(8));
        statusPanel.add(journeyObjectivesLabel);

        topRow.add(adventPanel);
        topRow.add(statusPanel);

        JPanel midRow = new JPanel(new BorderLayout(12, 12));
        midRow.setOpaque(false);

        JPanel mapCard = createAnimatedRegionMapPanel();
        mapCard.setLayout(new BorderLayout(10, 10));

        JPanel destinations = createImageCardPanel(travelDestinationsPanelImage);
        destinations.setLayout(new BoxLayout(destinations, BoxLayout.Y_AXIS));

        JLabel destinationsTitle = createMainScreenHeading("Travel Destinations");
        destinationsTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        destinations.add(destinationsTitle);
        destinations.add(Box.createVerticalStrut(22));

        JButton academyButton = createPrimaryButton("Go to Academy");
        academyButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        academyButton.setMaximumSize(new Dimension(200, 42));
        academyButton.addActionListener(event -> {
            if (requireHero()) {
                boolean firstVisit = !hero.hasVisitedAcademy();
                hero.setHasVisitedAcademy(true);
                subtitleLabel.setText("Explore Mystvale Academy.");
                showScreen(SCREEN_ACADEMY);
                if (firstVisit) {
                    playNarrationSequence("Academy Narration", Narration.buildAcademyNarration());
                }
            }
        });

        JButton forestButtonLocal = createPrimaryButton("Forest of Reverie");
        forestButtonLocal.setMaximumSize(new Dimension(200, 42));
        forestButtonLocal.setAlignmentX(Component.CENTER_ALIGNMENT);
        forestButtonLocal.addActionListener(event -> launchArea1());

        JButton edgeButton = createPrimaryButton("Reverie's Edge");
        edgeButton.setMaximumSize(new Dimension(200, 42));
        edgeButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        edgeButton.addActionListener(event -> launchArea2());

        JButton forsakenButtonLocal = createPrimaryButton("Forsaken Lands");
        forsakenButtonLocal.setMaximumSize(new Dimension(200, 42));
        forsakenButtonLocal.setAlignmentX(Component.CENTER_ALIGNMENT);
        forsakenButtonLocal.addActionListener(event -> launchArea3());

        destinations.add(academyButton);
        destinations.add(Box.createVerticalStrut(8));
        destinations.add(forestButtonLocal);
        destinations.add(Box.createVerticalStrut(8));
        destinations.add(edgeButton);
        destinations.add(Box.createVerticalStrut(8));
        destinations.add(forsakenButtonLocal);
        destinations.add(Box.createVerticalGlue());

        midRow.add(mapCard, BorderLayout.CENTER);
        midRow.add(destinations, BorderLayout.EAST);

        panel.add(topRow, BorderLayout.NORTH);
        panel.add(midRow, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildAcademyScreen() {
        JPanel panel = graphics.createCardPanel(COLOR_ACADEMY_BORDER, COLOR_ACADEMY_PANEL);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        panel.add(createAcademyHeading("Academy (Inside)"));
        panel.add(Box.createVerticalStrut(8));
        panel.add(createAcademyBody("Library, Training Ground, Shop, Principal's Office"));
        panel.add(Box.createVerticalStrut(18));

        JPanel academyMapCard = new JPanel(new BorderLayout(10, 10));
        academyMapCard.setOpaque(true);
        academyMapCard.setBackground(COLOR_ACADEMY_CARD);
        academyMapCard.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        academyMapCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        academyMapCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));

        JLabel academyMapTitle = createAcademyHeading("Academy Map");
        academyMapTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        academyMapCard.add(academyMapTitle, BorderLayout.NORTH);

        academyMapCard.add(createAcademyRegionMapPanel(), BorderLayout.CENTER);

        panel.add(academyMapCard);
        panel.add(Box.createVerticalStrut(18));

        JButton libraryButton = createPrimaryButton("Library");
        libraryButton.addActionListener(event -> openLibraryGui());

        JButton trainingButton = createPrimaryButton("Training Ground");
        trainingButton.addActionListener(event -> openTrainingGui());

        JButton officeButton = createPrimaryButton("Principal Office");
        officeButton.addActionListener(event -> openPrincipalOfficeGui());

        JButton shopButton = createPrimaryButton("Shop");
        shopButton.addActionListener(event -> openShopGui());

        JButton backButton = createSecondaryButton("Back to Main Menu");
        backButton.addActionListener(event -> {
            transitionManager.runTransition(frame,
                    () -> {
                        subtitleLabel.setText("Adventure overview.");
                        refreshHeroDashboard();
                        showScreen(SCREEN_MAIN);
                    },
                    null);
        });

        panel.add(libraryButton);
        panel.add(Box.createVerticalStrut(10));
        panel.add(trainingButton);
        panel.add(Box.createVerticalStrut(10));
        panel.add(officeButton);
        panel.add(Box.createVerticalStrut(10));
        panel.add(shopButton);
        panel.add(Box.createVerticalStrut(18));
        panel.add(backButton);
        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private JPanel buildStoryScreen() {
        JPanel panel = createImageCardPanel(storyNarrationBackgroundImage);
        panel.setLayout(new BorderLayout(22, 0));

        storyTitleLabel.setFont(getHeadingFont(28f));
        storyTitleLabel.setForeground(Color.WHITE);
        storyTitleLabel.setAlignmentX(JLabel.LEFT_ALIGNMENT);

        storyProgressLabel.setFont(getBodyFont(13f));
        storyProgressLabel.setForeground(new Color(225, 230, 255));
        storyProgressLabel.setAlignmentX(JLabel.LEFT_ALIGNMENT);

        storyTextArea.setEditable(false);
        storyTextArea.setLineWrap(true);
        storyTextArea.setWrapStyleWord(true);
        storyTextArea.setFont(getBodyFont(18f));
        storyTextArea.setBackground(new Color(7, 11, 38));
        storyTextArea.setForeground(Color.WHITE);
        storyTextArea.setMargin(new Insets(16, 16, 16, 16));
        storyTextArea.setCaretColor(Color.WHITE);
        storyTextArea.setOpaque(true);

        JScrollPane storyScrollPane = new JScrollPane(storyTextArea);
        storyScrollPane.setAlignmentX(JPanel.LEFT_ALIGNMENT);
        storyScrollPane.setPreferredSize(new Dimension(920, 420));
        storyScrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, 420));
        storyScrollPane.setOpaque(false);
        storyScrollPane.getViewport().setOpaque(true);
        storyScrollPane.getViewport().setBackground(new Color(7, 11, 38));
        storyScrollPane.setBorder(BorderFactory.createEmptyBorder());

        storyNextButton = createPrimaryButton("Next");
        storyNextButton.addActionListener(event -> advanceStoryBeat());

        storySkipButton = createSecondaryButton("Skip Story");
        storySkipButton.addActionListener(
                event -> finishStorySequence("Story skipped. You can replay it anytime from the main menu."));

        storyStartButton = createPrimaryButton("Start Adventure");
        storyStartButton.addActionListener(event -> finishStorySequence("Story complete. Your journey continues."));
        storyStartButton.setVisible(false);

        JButton backButton = createSecondaryButton("Back to Character Select");
        backButton.addActionListener(event -> {
            subtitleLabel.setText("Every path is different. Pick the one that speaks to you.");
            showScreen(SCREEN_CHARACTER);
        });

        JPanel leftColumn = new JPanel();
        leftColumn.setOpaque(false);
        leftColumn.setLayout(new BoxLayout(leftColumn, BoxLayout.Y_AXIS));
        leftColumn.setPreferredSize(new Dimension(700, 0));
        leftColumn.setMinimumSize(new Dimension(700, 0));

        storyScenePreviewPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftColumn.add(storyScenePreviewPanel);
        leftColumn.add(Box.createVerticalStrut(14));

        JPanel leftButtonRow = new JPanel(new GridLayout(1, 3, 10, 0));
        leftButtonRow.setOpaque(false);
        leftButtonRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftButtonRow.setPreferredSize(new Dimension(700, 42));
        leftButtonRow.setMaximumSize(new Dimension(700, 42));
        leftButtonRow.add(storyNextButton);
        leftButtonRow.add(storySkipButton);
        leftButtonRow.add(storyStartButton);
        leftColumn.add(leftButtonRow);
        leftColumn.add(Box.createVerticalGlue());

        JPanel rightColumn = new JPanel();
        rightColumn.setOpaque(true);
        rightColumn.setBackground(new Color(5, 10, 36));
        rightColumn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(5, 10, 36), 1),
                BorderFactory.createEmptyBorder(18, 18, 18, 18)));
        rightColumn.setLayout(new BoxLayout(rightColumn, BoxLayout.Y_AXIS));
        rightColumn.add(storyTitleLabel);
        rightColumn.add(Box.createVerticalStrut(6));
        rightColumn.add(storyProgressLabel);
        rightColumn.add(Box.createVerticalStrut(14));
        rightColumn.add(storyScrollPane);
        rightColumn.add(Box.createVerticalStrut(14));
        rightColumn.add(backButton);
        rightColumn.add(Box.createVerticalGlue());

        panel.add(leftColumn, BorderLayout.WEST);
        panel.add(rightColumn, BorderLayout.CENTER);
        return panel;
    }

    private final class StoryScenePreviewPanel extends JPanel {
        private static final float PAN_STEP = 0.0011f;
        private final Timer panTimer;
        private final Timer heroFrameTimer;
        private float panProgress = 0f;
        private int heroFrameIndex = 0;

        private StoryScenePreviewPanel() {
            setOpaque(false);
            setPreferredSize(new Dimension(700, 360));
            setMinimumSize(new Dimension(700, 300));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 360));
            setBorder(BorderFactory.createEmptyBorder());

            panTimer = new Timer(33, event -> {
                panProgress += PAN_STEP;
                if (panProgress >= 1f) {
                    panProgress = 1f;
                    ((Timer) event.getSource()).stop();
                }
                repaint();
            });

            heroFrameTimer = new Timer(140, event -> {
                if (storyHeroWalkFrames.length == 0) return;
                heroFrameIndex = (heroFrameIndex + 1) % storyHeroWalkFrames.length;
                repaint();
            });
        }

        private void startAnimation() {
            panProgress = 0f;
            heroFrameIndex = 0;
            if (panTimer.isRunning()) {
                panTimer.stop();
            }
            if (heroFrameTimer.isRunning()) {
                heroFrameTimer.stop();
            }
            panTimer.start();
            if (storyHeroWalkFrames.length > 0) {
                heroFrameTimer.start();
            }
            repaint();
        }

        private void stopAnimation() {
            panTimer.stop();
            heroFrameTimer.stop();
        }

        @Override
        public void removeNotify() {
            stopAnimation();
            super.removeNotify();
        }

        @Override
        protected void paintComponent(Graphics graphicsContext) {
            super.paintComponent(graphicsContext);
            Graphics2D g2 = (Graphics2D) graphicsContext.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            int width = getWidth();
            int height = getHeight();
            g2.setColor(new Color(7, 8, 32));
            g2.fillRect(0, 0, width, height);

            if (storySceneBackgroundImage != null) {
                double scale = height / (double) storySceneBackgroundImage.getHeight();
                int drawWidth = (int) Math.ceil(storySceneBackgroundImage.getWidth() * scale);
                int drawHeight = (int) Math.ceil(storySceneBackgroundImage.getHeight() * scale);
                int extraWidth = Math.max(0, drawWidth - width);
                int drawX = extraWidth > 0 ? -Math.round(extraWidth * panProgress) : 0;
                int drawY = 0;
                g2.drawImage(storySceneBackgroundImage, drawX, drawY, drawWidth, drawHeight, null);
            } else {
                g2.setColor(new Color(225, 225, 235, 170));
                g2.setFont(getBodyFont(18f));
                FontMetrics metrics = g2.getFontMetrics();
                String fallback = "Story scene background not found";
                int textX = (width - metrics.stringWidth(fallback)) / 2;
                int textY = (height + metrics.getAscent()) / 2;
                g2.drawString(fallback, Math.max(18, textX), textY);
            }

            GradientPaint vignette = new GradientPaint(
                    0, 0, new Color(5, 6, 24, 12),
                    0, height, new Color(5, 6, 24, 96));
            g2.setPaint(vignette);
            g2.fillRect(0, 0, width, height);

            BufferedImage heroFrame = getCurrentStoryHeroFrame();
            if (heroFrame != null) {
                double scale = Math.min(
                        (width * 0.28) / heroFrame.getWidth(),
                        (height * 0.48) / heroFrame.getHeight());
                scale = Math.max(scale, 0.18d);
                int heroWidth = Math.max(1, (int) Math.round(heroFrame.getWidth() * scale));
                int heroHeight = Math.max(1, (int) Math.round(heroFrame.getHeight() * scale));
                int heroAnchorX = width / 2;
                int heroX = heroAnchorX - (heroWidth / 2);
                int heroY = height - heroHeight - 18;

                RadialGradientPaint spotlight = new RadialGradientPaint(
                        new Point(heroAnchorX, heroY + (heroHeight / 2)),
                        Math.max(heroWidth, heroHeight),
                        new float[] { 0f, 0.45f, 1f },
                        new Color[] {
                                new Color(114, 190, 255, 82),
                                new Color(114, 190, 255, 34),
                                new Color(114, 190, 255, 0)
                        });
                g2.setPaint(spotlight);
                g2.fillOval(heroX - 28, heroY - 20, heroWidth + 56, heroHeight + 40);

                g2.setColor(new Color(0, 0, 0, 140));
                g2.fillOval(heroAnchorX - (heroWidth / 3), heroY + heroHeight - 8, Math.max(40, heroWidth * 2 / 3), 18);

                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                g2.drawImage(heroFrame, heroX, heroY, heroWidth, heroHeight, null);
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            }
            g2.dispose();
        }

        private BufferedImage getCurrentStoryHeroFrame() {
            if (storyHeroWalkFrames.length == 0) return null;
            return storyHeroWalkFrames[Math.floorMod(heroFrameIndex, storyHeroWalkFrames.length)];
        }
    }

    private JPanel buildShopScreen() {
        JPanel mainPanel = createShopBackgroundPanel();
        mainPanel.setLayout(new BorderLayout());

        JPanel header = new JPanel() {
            @Override
            protected void paintComponent(Graphics graphicsContext) {
                Graphics2D g2 = (Graphics2D) graphicsContext.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(25, 23, 33, 230));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(graphicsContext);
            }
        };
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBorder(BorderFactory.createEmptyBorder(14, 20, 12, 20));

        JLabel shopTitle = createHeading("Shop");
        shopTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        shopTitle.setForeground(COLOR_SHOP_TEXT);
        header.add(shopTitle);
        header.add(Box.createVerticalStrut(8));

        shopGoldLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        shopGoldLabel.setForeground(new Color(226, 201, 139));
        header.add(shopGoldLabel);
        mainPanel.add(header, BorderLayout.NORTH);

        JPanel contentWrapper = new JPanel(new GridLayout(1, 2, 28, 0));
        contentWrapper.setOpaque(false);
        contentWrapper.setBorder(BorderFactory.createEmptyBorder(16, 28, 24, 28));

        JPanel leftPanel = new JPanel(new GridBagLayout());
        leftPanel.setOpaque(false);
        leftPanel.add(createShopkeeperDisplay());

        JPanel rightPanel = new JPanel();
        rightPanel.setOpaque(false);
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));

        rightPanel.add(createShopRow("Small Health Potion", 450, 0, smallHealthPotionIcon));
        rightPanel.add(Box.createVerticalStrut(6));
        rightPanel.add(createShopRow("Medium Health Potion", 1350, 1, mediumHealthPotionIcon));
        rightPanel.add(Box.createVerticalStrut(6));
        rightPanel.add(createShopRow("Large Health Potion", 2750, 2, largeHealthPotionIcon));
        rightPanel.add(Box.createVerticalStrut(6));
        rightPanel.add(createShopRow("Small Mana Potion", 450, 3, smallManaPotionIcon));
        rightPanel.add(Box.createVerticalStrut(6));
        rightPanel.add(createShopRow("Medium Mana Potion", 1350, 4, mediumManaPotionIcon));
        rightPanel.add(Box.createVerticalStrut(6));
        rightPanel.add(createShopRow("Large Mana Potion", 2750, 5, largeManaPotionIcon));
        rightPanel.add(Box.createVerticalGlue());

        JScrollPane itemScrollPane = new JScrollPane(rightPanel);
        itemScrollPane.setBorder(BorderFactory.createEmptyBorder());
        itemScrollPane.setOpaque(false);
        itemScrollPane.setBackground(new Color(0, 0, 0, 0));
        itemScrollPane.getViewport().setOpaque(false);
        itemScrollPane.getViewport().setBackground(new Color(0, 0, 0, 0));
        itemScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        itemScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        itemScrollPane.getVerticalScrollBar().setBackground(COLOR_SHOP_OUTSIDE);
        itemScrollPane.getHorizontalScrollBar().setBackground(COLOR_SHOP_OUTSIDE);

        contentWrapper.add(leftPanel);
        contentWrapper.add(itemScrollPane);
        mainPanel.add(contentWrapper, BorderLayout.CENTER);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(COLOR_SHOP_OUTSIDE);
        panel.add(mainPanel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildInventoryScreen() {
        JPanel panel = createCardPanel();

        inventorySummary.setForeground(COLOR_TEXT_DARK);
        panel.add(createHeading("Inventory"));
        panel.add(Box.createVerticalStrut(8));
        panel.add(inventorySummary);
        panel.add(Box.createVerticalStrut(14));

        panel.add(createInventoryRow("Small Health Potion", smallHealthCount, () -> usePotion("SH")));
        panel.add(Box.createVerticalStrut(8));
        panel.add(createInventoryRow("Medium Health Potion", mediumHealthCount, () -> usePotion("MH")));
        panel.add(Box.createVerticalStrut(8));
        panel.add(createInventoryRow("Large Health Potion", largeHealthCount, () -> usePotion("LH")));
        panel.add(Box.createVerticalStrut(8));
        panel.add(createInventoryRow("Small Mana Potion", smallManaCount, () -> usePotion("SM")));
        panel.add(Box.createVerticalStrut(8));
        panel.add(createInventoryRow("Medium Mana Potion", mediumManaCount, () -> usePotion("MM")));
        panel.add(Box.createVerticalStrut(8));
        panel.add(createInventoryRow("Large Mana Potion", largeManaCount, () -> usePotion("LM")));
        panel.add(Box.createVerticalStrut(18));

        JButton backButton = createSecondaryButton("Back to Main Menu");
        backButton.addActionListener(event -> {
            transitionManager.runTransition(frame,
                    () -> {
                        subtitleLabel.setText("Adventure overview.");
                        refreshHeroDashboard();
                        showScreen(SCREEN_MAIN);
                    },
                    null);
        });

        panel.add(backButton);
        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private JPanel buildProfileScreen() {
        JPanel panel = createCardPanel();

        profileName.setForeground(COLOR_TEXT_DARK);
        profileClass.setForeground(COLOR_TEXT_DARK);
        profileWeapon.setForeground(COLOR_TEXT_DARK);
        profileLevel.setForeground(COLOR_TEXT_DARK);
        profileAttack.setForeground(COLOR_TEXT_DARK);
        profileDefense.setForeground(COLOR_TEXT_DARK);
        profileSpeed.setForeground(COLOR_TEXT_DARK);
        profileSkill1.setForeground(COLOR_TEXT_DARK);
        profileSkill2.setForeground(COLOR_TEXT_DARK);
        profileUltimate.setForeground(COLOR_TEXT_DARK);

        panel.add(createHeading("Hero Details"));
        panel.add(Box.createVerticalStrut(12));
        panel.add(createStatLine("Name", profileName));
        panel.add(createStatLine("Class", profileClass));
        panel.add(createStatLine("Weapon", profileWeapon));
        panel.add(createStatLine("Level", profileLevel));
        panel.add(createStatLine("Attack", profileAttack));
        panel.add(createStatLine("Defense", profileDefense));
        panel.add(createStatLine("Speed", profileSpeed));
        panel.add(createStatLine("Skill 1", profileSkill1));
        panel.add(createStatLine("Skill 2", profileSkill2));
        panel.add(createStatLine("Ultimate", profileUltimate));
        panel.add(Box.createVerticalStrut(18));

        JButton backButton = createSecondaryButton("Back to Main Menu");
        backButton.addActionListener(event -> {
            transitionManager.runTransition(frame,
                    () -> {
                        subtitleLabel.setText("Adventure overview.");
                        refreshHeroDashboard();
                        showScreen(SCREEN_MAIN);
                    },
                    null);
        });

        panel.add(backButton);
        panel.add(Box.createVerticalGlue());
        return panel;
    }

    // -------------------------------------------------------------------------
    // Hero summary card (adventure overview panel)
    // -------------------------------------------------------------------------

    private JPanel buildHeroSummaryCard() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        heroNameValue.setFont(getBodyFont(20f).deriveFont(Font.BOLD, 20f));
        heroNameValue.setForeground(Color.WHITE);
        heroClassValue.setFont(getBodyFont(14f));
        heroClassValue.setForeground(Color.WHITE);
        heroLevelValue.setFont(getBodyFont(14f));
        heroLevelValue.setForeground(Color.WHITE);
        heroGoldValue.setFont(getBodyFont(14f));
        heroGoldValue.setForeground(Color.WHITE);

        hpBar.setStringPainted(true);
        hpBar.setForeground(COLOR_HERO_HP);
        hpBar.setBackground(Color.decode("#14215A"));
        hpBar.setFont(getBodyFont(14f));

        manaBar.setStringPainted(true);
        manaBar.setForeground(COLOR_HERO_MANA);
        manaBar.setBackground(Color.decode("#14215A"));
        manaBar.setFont(getBodyFont(14f));

        JLabel healthText = new JLabel("Health");
        healthText.setFont(getBodyFont(14f).deriveFont(Font.BOLD, 14f));
        healthText.setForeground(Color.WHITE);

        JLabel manaText = new JLabel("Mana");
        manaText.setFont(getBodyFont(14f).deriveFont(Font.BOLD, 14f));
        manaText.setForeground(Color.WHITE);

        panel.add(heroNameValue);
        panel.add(Box.createVerticalStrut(4));
        panel.add(heroClassValue);
        panel.add(heroLevelValue);
        panel.add(heroGoldValue);
        panel.add(Box.createVerticalStrut(10));
        panel.add(healthText);
        panel.add(hpBar);
        panel.add(Box.createVerticalStrut(8));
        panel.add(manaText);
        panel.add(manaBar);
        return panel;
    }

    // -------------------------------------------------------------------------
    // Journey status helpers
    // -------------------------------------------------------------------------

    private String getCurrentLocationForHero() {
        if (hero == null) return "No hero selected";
        if (hero.hasUnlockedArea3()) return "Forsaken Lands";
        if (hero.hasUnlockedArea2()) return "Reverie's Edge";
        if (hero.hasUnlockedArea1()) return "Forest of Reverie";
        if (hero.hasVisitedAcademy()) return "Mystvale Academy";
        return "Academy";
    }

    private String getAdventureProgressText() {
        if (hero == null) return "No data available. Choose a hero.";

        int trainingCompleted = 0;
        trainingCompleted += hero.hasFinishedEndurance() ? 1 : 0;
        trainingCompleted += hero.hasFinishedStrength() ? 1 : 0;
        trainingCompleted += hero.hasFinishedDurability() ? 1 : 0;
        trainingCompleted += hero.hasFinishedManaRefinement() ? 1 : 0;

        StringBuilder sb = new StringBuilder("<html>");
        sb.append("Training: ").append(trainingCompleted).append(" / 4 completed<br>");
        sb.append("Unlocked: ").append(
                hero.hasUnlockedArea3() ? "Forsaken Lands" :
                hero.hasUnlockedArea2() ? "Reverie's Edge" :
                hero.hasUnlockedArea1() ? "Forest of Reverie" : "Mystvale Academy").append("<br>");
        sb.append("Boss progress: ")
                .append(hero.getHaveDefeatedArea1Boss() ? "Forest boss defeated" : "Forest boss alive")
                .append(", ")
                .append(hero.getHaveDefeatedArea2Boss() ? "Reverie's Edge boss defeated" : "Reverie's Edge boss alive")
                .append("<br>");
        sb.append("Gold: ").append(statFormat.format(hero.getGold())).append("<br>");
        sb.append("Level: ").append(hero.getLevel());
        sb.append("</html>");

        String content = sb.toString();
        if (content.startsWith("<html>")) content = content.substring(6, content.length() - 7);
        return styleHtml(content);
    }

    private void refreshJourneyStatus() {
        if (hero == null) {
            journeyLocationLabel.setText(styleRaw("Current location: No hero selected"));
            journeyCompletionLabel.setText(styleRaw("No hero chosen yet."));
            journeyObjectivesLabel.setText(styleRaw("No objectives yet. Choose a hero to start."));
            return;
        }

        journeyLocationLabel.setText(styleRaw("Current location: " + getCurrentLocationForHero()));
        journeyCompletionLabel.setText(getCompletionStatusForHero());
        journeyObjectivesLabel.setText(getAdventureObjectivesText());
    }

    private String getCompletionStatusForHero() {
        if (hero == null) return "No hero chosen yet.";

        int trainingCompleted = 0;
        trainingCompleted += hero.hasFinishedEndurance() ? 1 : 0;
        trainingCompleted += hero.hasFinishedStrength() ? 1 : 0;
        trainingCompleted += hero.hasFinishedDurability() ? 1 : 0;
        trainingCompleted += hero.hasFinishedManaRefinement() ? 1 : 0;

        String unlockedAreaName = hero.hasUnlockedArea3() ? "Forsaken Lands" :
                hero.hasUnlockedArea2() ? "Reverie's Edge" :
                hero.hasUnlockedArea1() ? "Forest of Reverie" :
                hero.hasVisitedAcademy() ? "Mystvale Academy" : "Academy";

        int bossesDefeated =
                (hero.getHaveDefeatedArea1Boss() ? 1 : 0) +
                (hero.getHaveDefeatedArea2Boss() ? 1 : 0) +
                (hero.getHaveDefeatedArea3Boss() ? 1 : 0);

        return styleHtml("Completion: " + trainingCompleted + " / 4 training completed<br>Unlocked: "
                + unlockedAreaName + "<br>Bosses defeated: " + bossesDefeated + " / 3");
    }

    private String getAdventureObjectivesText() {
        if (hero == null) return "No objectives yet. Choose a hero to start.";

        StringBuilder sb = new StringBuilder();
        sb.append("Training progress: ").append(
                hero.hasFinishedAllTraining() ? "All training tasks complete" :
                "Continue training through Academy and areas").append("<br>");

        sb.append("Area unlocks: ").append(
                hero.hasUnlockedArea3() ? "Forsaken Lands" :
                hero.hasUnlockedArea2() ? "Reverie's Edge" :
                hero.hasUnlockedArea1() ? "Forest of Reverie" : "Mystvale Academy").append("<br>");

        sb.append("Next boss: ");
        if (!hero.getHaveDefeatedArea1Boss()) sb.append("Forest boss (area 1)");
        else if (!hero.getHaveDefeatedArea2Boss()) sb.append("Reverie's Edge boss (area 2)");
        else if (!hero.getHaveDefeatedArea3Boss()) sb.append("Forsaken Lands boss (area 3)");
        else sb.append("All bosses defeated");

        return styleHtml(sb.toString());
    }

    // -------------------------------------------------------------------------
    // Row / line component helpers
    // -------------------------------------------------------------------------

    private JPanel createInventoryRow(String name, JLabel countLabel, Runnable action) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        row.setBackground(COLOR_PANEL);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER, 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));

        JLabel nameLabel = new JLabel(name);
        nameLabel.setForeground(COLOR_TEXT_DARK);
        countLabel.setForeground(COLOR_TEXT_DARK);
        countLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JButton useButton = createSecondaryButton("Use");
        useButton.setPreferredSize(new Dimension(84, 30));
        useButton.addActionListener(event -> action.run());

        JPanel right = new JPanel(new GridLayout(1, 2, 10, 0));
        right.setOpaque(false);
        right.add(countLabel);
        right.add(useButton);

        row.add(nameLabel, BorderLayout.CENTER);
        row.add(right, BorderLayout.EAST);
        return row;
    }

    private JPanel createShopRow(String name, int price, int itemIndex) {
        return createShopRow(name, price, itemIndex, null);
    }

    private JPanel createShopRow(String name, int price, int itemIndex, Icon icon) {
        JPanel row = new JPanel(new BorderLayout(16, 0)) {
            @Override
            protected void paintComponent(Graphics graphicsContext) {
                Graphics2D g2 = (Graphics2D) graphicsContext.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int arc = 8;
                g2.setColor(new Color(28, 27, 36, 220));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);

                g2.setColor(new Color(255, 255, 255, 18));
                g2.drawLine(10, 8, Math.max(10, getWidth() - 12), 8);
                g2.dispose();
                super.paintComponent(graphicsContext);
            }
        };
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(8, 24, 8, 48));
        row.setPreferredSize(new Dimension(520, 112));
        row.setMaximumSize(new Dimension(520, 112));
        row.setAlignmentX(Component.RIGHT_ALIGNMENT);

        JPanel itemPanel = new JPanel(new BorderLayout(12, 0));
        itemPanel.setOpaque(false);
        itemPanel.setPreferredSize(new Dimension(292, 96));

        JPanel nameAndIconPanel = new JPanel();
        nameAndIconPanel.setOpaque(false);
        nameAndIconPanel.setLayout(new BoxLayout(nameAndIconPanel, BoxLayout.Y_AXIS));
        nameAndIconPanel.setPreferredSize(new Dimension(190, 96));

        JLabel nameLabel = new JLabel(name);
        nameLabel.setForeground(new Color(246, 239, 221));
        nameLabel.setFont(getBodyFont(14f).deriveFont(Font.BOLD, 14f));
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        nameLabel.setHorizontalAlignment(SwingConstants.CENTER);
        nameAndIconPanel.add(nameLabel);
        nameAndIconPanel.add(Box.createVerticalGlue());

        JLabel iconLabel = new JLabel(icon == null ? null : scaleIcon(icon, 44, 44));
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
        nameAndIconPanel.add(iconLabel);
        nameAndIconPanel.add(Box.createVerticalGlue());
        itemPanel.add(nameAndIconPanel, BorderLayout.CENTER);

        JPanel pricePanel = new JPanel(new BorderLayout());
        pricePanel.setOpaque(false);
        pricePanel.setPreferredSize(new Dimension(118, 96));
        pricePanel.add(createShopPriceTag(price), BorderLayout.NORTH);
        itemPanel.add(pricePanel, BorderLayout.EAST);
        row.add(itemPanel, BorderLayout.CENTER);

        JPanel actions = new JPanel();
        actions.setLayout(new BoxLayout(actions, BoxLayout.Y_AXIS));
        actions.setOpaque(false);
        actions.setPreferredSize(new Dimension(160, 96));

        JButton buyOneButton = createShopBuyButton(1);
        buyOneButton.addActionListener(event -> buyShopItem(itemIndex, 1));
        JButton buyFiveButton = createShopBuyButton(5);
        buyFiveButton.addActionListener(event -> buyShopItem(itemIndex, 5));

        actions.add(buyOneButton);
        actions.add(Box.createVerticalGlue());
        actions.add(buyFiveButton);

        row.add(actions, BorderLayout.EAST);
        return row;
    }

    private JButton createShopBuyButton(int quantity) {
        String label = "Buy x" + quantity;
        BufferedImage buttonImage = graphics.prepareShopButtonImage(label + ".png");
        JButton button = buttonImage == null ? createSecondaryButton(label) : new JButton(label) {
            @Override
            protected void paintComponent(Graphics graphicsContext) {
                Graphics2D g2 = (Graphics2D) graphicsContext.create();
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                ButtonModel model = getModel();
                int yOffset = model.isArmed() && model.isPressed() ? 1 : 0;
                int drawWidth = getWidth();
                int drawHeight = Math.max(1, getHeight() - yOffset);

                if (model.isRollover() && !model.isPressed()) {
                    paintImageGlow(g2, buttonImage, 0, yOffset, drawWidth, drawHeight,
                            new Color(120, 255, 225, 135));
                    yOffset = Math.max(0, yOffset - 1);
                }
                g2.drawImage(buttonImage, 0, yOffset, drawWidth, drawHeight, null);

                g2.dispose();
            }
        };
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setOpaque(false);
        button.setRolloverEnabled(true);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setPreferredSize(new Dimension(160, 40));
        button.setMaximumSize(new Dimension(160, 40));
        button.setMinimumSize(new Dimension(160, 40));
        button.setToolTipText(null);
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

    private JPanel createShopkeeperDisplay() {
        final float[] floatPhase = { 0f };
        JPanel panel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintChildren(Graphics graphicsContext) {
                Graphics2D g2 = (Graphics2D) graphicsContext.create();
                int floatOffset = Math.round((float) Math.sin(floatPhase[0]) * 10f);
                g2.translate(0, floatOffset);
                super.paintChildren(g2);
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.setPreferredSize(new Dimension(520, 520));
        panel.setMaximumSize(new Dimension(520, 520));

        JLabel shopkeeperLabel = new JLabel(scaleIcon(shopkeeperPrincipalIcon, 500, 500));
        shopkeeperLabel.setHorizontalAlignment(SwingConstants.CENTER);
        shopkeeperLabel.setVerticalAlignment(SwingConstants.CENTER);
        panel.add(shopkeeperLabel, BorderLayout.CENTER);

        Timer floatTimer = new Timer(40, event -> {
            if (!panel.isShowing()) {
                return;
            }
            floatPhase[0] += 0.12f;
            panel.repaint();
        });
        floatTimer.start();
        panel.addHierarchyListener(event -> {
            if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) == 0) {
                return;
            }
            if (panel.isShowing()) {
                floatTimer.start();
            } else {
                floatTimer.stop();
            }
        });
        return panel;
    }

    private JPanel createShopPriceTag(int price) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        panel.setAlignmentX(Component.RIGHT_ALIGNMENT);

        JLabel priceLabel = new JLabel(statFormat.format(price));
        priceLabel.setForeground(new Color(226, 201, 139));
        priceLabel.setFont(getBodyFont(15f).deriveFont(Font.BOLD, 15f));

        panel.add(priceLabel);
        return panel;
    }

    private JPanel createShopBackgroundPanel() {
        return new JPanel() {
            @Override
            protected void paintComponent(Graphics graphicsContext) {
                super.paintComponent(graphicsContext);
                if (shopBackgroundImage == null) {
                    graphicsContext.setColor(COLOR_PANEL);
                    graphicsContext.fillRect(0, 0, getWidth(), getHeight());
                    return;
                }

                Graphics2D g2 = (Graphics2D) graphicsContext.create();
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                double scale = Math.max(
                        getWidth() / (double) shopBackgroundImage.getWidth(),
                        getHeight() / (double) shopBackgroundImage.getHeight());
                int drawWidth = (int) Math.ceil(shopBackgroundImage.getWidth() * scale);
                int drawHeight = (int) Math.ceil(shopBackgroundImage.getHeight() * scale);
                int drawX = (getWidth() - drawWidth) / 2;
                int drawY = (getHeight() - drawHeight) / 2;

                g2.drawImage(shopBackgroundImage, drawX, drawY, drawWidth, drawHeight, null);
                g2.setColor(new Color(10, 9, 24, 70));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
    }

    private void paintShopSkyBackground(Graphics2D g2, int width, int height, int overlayAlpha) {
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        if (shopHeaderBackgroundImage == null) {
            g2.setColor(COLOR_SHOP_OUTSIDE);
            g2.fillRect(0, 0, width, height);
            return;
        }

        double scale = Math.max(
                width / (double) shopHeaderBackgroundImage.getWidth(),
                height / (double) shopHeaderBackgroundImage.getHeight());
        int drawWidth = (int) Math.ceil(shopHeaderBackgroundImage.getWidth() * scale);
        int drawHeight = (int) Math.ceil(shopHeaderBackgroundImage.getHeight() * scale);
        int drawX = (width - drawWidth) / 2;
        int drawY = (height - drawHeight) / 2;

        g2.drawImage(shopHeaderBackgroundImage, drawX, drawY, drawWidth, drawHeight, null);
        g2.setColor(new Color(10, 9, 24, overlayAlpha));
        g2.fillRect(0, 0, width, height);
    }

    private Icon scaleIcon(Icon icon, int targetWidth, int targetHeight) {
        if (!(icon instanceof ImageIcon imageIcon)) return icon;
        Image scaled = imageIcon.getImage().getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }

    private Icon loadItemIcon(String fileName) {
        BufferedImage image = graphics.loadItemImage(fileName);
        return image == null ? null : new ImageIcon(image);
    }

    private Icon loadPixelUiIcon(String fileName, int targetWidth, int targetHeight) {
        BufferedImage image = graphics.loadUIImage(fileName);
        if (image == null) return null;

        BufferedImage scaled = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = scaled.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        double scale = Math.min(
                targetWidth / (double) Math.max(1, image.getWidth()),
                targetHeight / (double) Math.max(1, image.getHeight()));
        int drawWidth = Math.max(1, (int) Math.round(image.getWidth() * scale));
        int drawHeight = Math.max(1, (int) Math.round(image.getHeight() * scale));
        int drawX = (targetWidth - drawWidth) / 2;
        int drawY = (targetHeight - drawHeight) / 2;

        g2.drawImage(image, drawX, drawY, drawWidth, drawHeight, null);
        g2.dispose();
        return new ImageIcon(scaled);
    }

    private JPanel createStatLine(String label, JLabel value) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));

        JLabel name = new JLabel(label);
        name.setForeground(COLOR_TEXT_MUTED);

        value.setHorizontalAlignment(SwingConstants.RIGHT);
        value.setForeground(COLOR_TEXT_DARK);

        row.add(name, BorderLayout.WEST);
        row.add(value, BorderLayout.EAST);
        return row;
    }

    // -------------------------------------------------------------------------
    // Character card builder (updated layout)
    // -------------------------------------------------------------------------

    private JPanel createCharacterButton(String title, String role, Runnable action) {
        return createCharacterButton(title, title, role, action);
    }

    private JPanel createCharacterButton(String portraitKey, String title, String role, Runnable action) {
        final BufferedImage portraitImage = loadCharacterPortrait(portraitKey);
        final boolean[] hovered = { false };

        JPanel wrapper = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);

                Graphics2D g2 = (Graphics2D) graphics.create();
                if (portraitImage != null) {
                    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

                    double scale = Math.max(
                            getWidth() / (double) portraitImage.getWidth(),
                            getHeight() / (double) portraitImage.getHeight());
                    int drawWidth = (int) Math.ceil(portraitImage.getWidth() * scale);
                    int drawHeight = (int) Math.ceil(portraitImage.getHeight() * scale);
                    int drawX = (getWidth() - drawWidth) / 2;
                    int drawY = (getHeight() - drawHeight) / 2;

                    g2.drawImage(portraitImage, drawX, drawY, drawWidth, drawHeight, null);
                } else {
                    g2.setColor(new Color(200, 193, 183));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                }

                g2.setPaint(new GradientPaint(
                        0, 0, new Color(8, 8, 14, 18),
                        0, getHeight(), new Color(8, 8, 14, 55)));
                g2.fillRect(0, 0, getWidth(), getHeight());

                if (hovered[0]) {
                    g2.setPaint(new GradientPaint(
                            0, 0, new Color(20, 28, 48, 35),
                            0, getHeight(), new Color(20, 28, 48, 95)));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    g2.setColor(new Color(170, 225, 255, 70));
                    g2.fillRoundRect(6, 6, getWidth() - 12, getHeight() - 12, 18, 18);
                }
                g2.dispose();
            }
        };
        wrapper.setBackground(COLOR_CHARSEL_PANEL);
        wrapper.setBorder(BorderFactory.createEmptyBorder());
        wrapper.setPreferredSize(new Dimension(290, 480));
        wrapper.setMinimumSize(new Dimension(290, 480));
        wrapper.setMaximumSize(new Dimension(290, 480));
        wrapper.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                hovered[0] = true;
                wrapper.repaint();
            }

            @Override
            public void mouseExited(MouseEvent event) {
                hovered[0] = false;
                wrapper.repaint();
            }
        });

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(getHeadingFont(22f));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel roleLabel = new JLabel(role.toUpperCase());
        roleLabel.setFont(getBodyFont(11f).deriveFont(Font.BOLD, 11f));
        roleLabel.setForeground(new Color(228, 220, 203));
        roleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        roleLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JButton chooseButton = createCharacterSelectButton("Play as " + title);
        chooseButton.addActionListener(event -> action.run());
        chooseButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        chooseButton.setMaximumSize(new Dimension(180, 44));

        JPanel topOverlay = new JPanel();
        topOverlay.setOpaque(false);
        topOverlay.setLayout(new BoxLayout(topOverlay, BoxLayout.Y_AXIS));
        topOverlay.setBorder(BorderFactory.createEmptyBorder(30, 14, 0, 14));
        topOverlay.add(titleLabel);
        topOverlay.add(Box.createVerticalStrut(2));
        topOverlay.add(roleLabel);

        JPanel bottomOverlay = new JPanel();
        bottomOverlay.setOpaque(false);
        bottomOverlay.setLayout(new BoxLayout(bottomOverlay, BoxLayout.Y_AXIS));
        bottomOverlay.setBorder(BorderFactory.createEmptyBorder(0, 18, 25, 18));

        /*if (portraitImage == null) {
            JLabel placeholder = new JLabel("<html><div style='text-align:center;'>Add " + title + " portrait</div></html>");
            placeholder.setFont(getBodyFont(13f).deriveFont(Font.BOLD, 13f));
            placeholder.setForeground(new Color(238, 231, 221));
            placeholder.setAlignmentX(Component.CENTER_ALIGNMENT);
            placeholder.setHorizontalAlignment(SwingConstants.CENTER);
            bottomOverlay.add(placeholder);
            bottomOverlay.add(Box.createVerticalStrut(10));
        }*/

        bottomOverlay.add(chooseButton);

        wrapper.add(topOverlay, BorderLayout.NORTH);
        wrapper.add(bottomOverlay, BorderLayout.SOUTH);
        return wrapper;
    }

    // -------------------------------------------------------------------------
    // Card panel + font helpers
    // -------------------------------------------------------------------------

    private JPanel createCardPanel() {
        return graphics.createCardPanel(COLOR_BORDER, COLOR_PANEL);
    }

    private JPanel createImageCardPanel(BufferedImage backgroundImage) {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics graphicsContext) {
                Graphics2D g2 = (Graphics2D) graphicsContext.create();
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                if (backgroundImage != null) {
                    g2.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), null);
                } else {
                    g2.setColor(COLOR_PANEL);
                    g2.fillRect(0, 0, getWidth(), getHeight());
                }
                g2.dispose();
                super.paintComponent(graphicsContext);
            }
        };
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        return panel;
    }

    private JPanel createAnimatedRegionMapPanel() {
        final float[] animationPhase = { 0f };
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics graphicsContext) {
                Graphics2D g2 = (Graphics2D) graphicsContext.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                if (regionMapPanelImage != null) {
                    g2.drawImage(regionMapPanelImage, 0, 0, getWidth(), getHeight(), null);
                } else {
                    g2.setColor(COLOR_PANEL);
                    g2.fillRect(0, 0, getWidth(), getHeight());
                }

                Point forest = scaledPoint(0.155, 0.605);
                Point lowerRuins = scaledPoint(0.395, 0.72);
                Point academy = scaledPoint(0.525, 0.41);
                Point redSpire = scaledPoint(0.755, 0.505);
                Point violetKeep = scaledPoint(0.935, 0.335);

                float phase = animationPhase[0];
                drawAnimatedRoute(g2, forest, lowerRuins, phase * 0.9f);
                drawAnimatedRoute(g2, forest, academy, phase * 0.75f + 0.15f);
                drawAnimatedRoute(g2, lowerRuins, academy, phase * 0.7f + 0.30f);
                drawAnimatedRoute(g2, academy, redSpire, phase * 0.8f + 0.45f);
                drawAnimatedRoute(g2, lowerRuins, redSpire, phase * 0.95f + 0.6f);
                drawAnimatedRoute(g2, redSpire, violetKeep, phase * 0.85f + 0.2f);

                drawPulseNode(g2, forest, new Color(255, 202, 98), 0.0f, phase);
                drawPulseNode(g2, lowerRuins, new Color(255, 198, 92), 0.55f, phase);
                drawPulseNode(g2, academy, new Color(93, 214, 255), 1.1f, phase);
                drawPulseNode(g2, redSpire, new Color(255, 138, 69), 1.65f, phase);
                drawPulseNode(g2, violetKeep, new Color(180, 122, 255), 2.2f, phase);

                g2.dispose();
                super.paintComponent(graphicsContext);
            }

            private Point scaledPoint(double xRatio, double yRatio) {
                return new Point((int) Math.round(getWidth() * xRatio),
                        (int) Math.round(getHeight() * yRatio));
            }

            private void drawAnimatedRoute(Graphics2D g2, Point start, Point end, float phase) {
                Stroke previousStroke = g2.getStroke();
                float dashPhase = (phase - (float) Math.floor(phase)) * 24f;
                g2.setColor(new Color(255, 244, 191, 80));
                g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                        1f, new float[] { 7f, 10f }, dashPhase));
                g2.drawLine(start.x, start.y, end.x, end.y);

                double progress = phase - Math.floor(phase);
                int sparkX = (int) Math.round(start.x + ((end.x - start.x) * progress));
                int sparkY = (int) Math.round(start.y + ((end.y - start.y) * progress));
                RadialGradientPaint spark = new RadialGradientPaint(
                        new Point(sparkX, sparkY),
                        14f,
                        new float[] { 0f, 0.45f, 1f },
                        new Color[] {
                                new Color(255, 246, 210, 210),
                                new Color(255, 223, 132, 120),
                                new Color(255, 223, 132, 0)
                        });
                g2.setPaint(spark);
                g2.fillOval(sparkX - 14, sparkY - 14, 28, 28);
                g2.setStroke(previousStroke);
            }

            private void drawPulseNode(Graphics2D g2, Point center, Color glow, float offset, float phase) {
                float pulse = 0.5f + (0.5f * (float) Math.sin((phase * 2.8f) + offset));
                int glowRadius = 20 + Math.round(pulse * 8f);
                RadialGradientPaint glowPaint = new RadialGradientPaint(
                        center,
                        glowRadius,
                        new float[] { 0f, 0.4f, 1f },
                        new Color[] {
                                new Color(glow.getRed(), glow.getGreen(), glow.getBlue(), 180),
                                new Color(glow.getRed(), glow.getGreen(), glow.getBlue(), 72),
                                new Color(glow.getRed(), glow.getGreen(), glow.getBlue(), 0)
                        });
                g2.setPaint(glowPaint);
                g2.fillOval(center.x - glowRadius, center.y - glowRadius, glowRadius * 2, glowRadius * 2);
            }
        };
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        Timer shimmerTimer = new Timer(50, event -> {
            if (!panel.isShowing()) {
                return;
            }
            animationPhase[0] += 0.03f;
            panel.repaint();
        });
        shimmerTimer.start();
        panel.addHierarchyListener(event -> {
            if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) == 0) {
                return;
            }
            if (panel.isShowing()) {
                shimmerTimer.start();
            } else {
                shimmerTimer.stop();
            }
        });
        return panel;
    }

    private JPanel createAcademyRegionMapPanel() {
        final float[] animationPhase = { 0f };
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics graphicsContext) {
                Graphics2D g2 = (Graphics2D) graphicsContext.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                if (academyMapPanelImage != null) {
                    g2.drawImage(academyMapPanelImage, 0, 0, getWidth(), getHeight(), null);
                } else {
                    GradientPaint fallback = new GradientPaint(0, 0, new Color(7, 20, 50),
                            0, getHeight(), new Color(2, 8, 24));
                    g2.setPaint(fallback);
                    g2.fillRect(0, 0, getWidth(), getHeight());
                }

                g2.setPaint(new GradientPaint(0, 0, new Color(4, 10, 28, 24),
                        0, getHeight(), new Color(3, 8, 20, 110)));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);

                // Hide baked-in location text so only the custom overlays remain visible.
                int officeMaskX = (int) Math.round(getWidth() * 0.66);
                int officeMaskY = (int) Math.round(getHeight() * 0.13);
                int officeMaskWidth = (int) Math.round(getWidth() * 0.18);
                int officeMaskHeight = (int) Math.round(getHeight() * 0.07);
                GradientPaint officeMask = new GradientPaint(
                        officeMaskX, officeMaskY, new Color(10, 18, 46, 232),
                        officeMaskX, officeMaskY + officeMaskHeight, new Color(8, 14, 34, 214));
                g2.setPaint(officeMask);
                g2.fillRoundRect(officeMaskX, officeMaskY, officeMaskWidth, officeMaskHeight, 10, 10);

                Point academy = scaledPoint(0.48, 0.70);
                Point forest = scaledPoint(0.16, 0.42);
                Point edge = scaledPoint(0.62, 0.26);
                Point forsaken = scaledPoint(0.83, 0.56);

                float phase = animationPhase[0];
                drawMapRoute(g2, academy, forest, phase * 0.85f);
                drawMapRoute(g2, academy, edge, phase * 0.75f + 0.2f);
                drawMapRoute(g2, academy, forsaken, phase * 0.95f + 0.45f);

                drawMapNode(g2, academy.x, academy.y, new Color(88, 208, 255), new Color(31, 108, 255), 0.0f, phase);
                drawMapNode(g2, forest.x, forest.y, new Color(64, 223, 219), new Color(242, 196, 83), 0.65f, phase);
                drawMapNode(g2, edge.x, edge.y, new Color(150, 114, 255), new Color(92, 212, 255), 1.15f, phase);
                drawMapNode(g2, forsaken.x, forsaken.y, new Color(255, 116, 142), new Color(255, 188, 86), 1.75f, phase);

                drawMapLabel(g2, "Forest of Reverie", forest.x - 72, forest.y - 38);
                drawMapLabel(g2, "Reveries Edge", edge.x - 54, edge.y - 40);
                drawMapLabel(g2, "Forsaken Lands", forsaken.x - 58, forsaken.y - 40);

                g2.dispose();
                super.paintComponent(graphicsContext);
            }

            private Point scaledPoint(double xRatio, double yRatio) {
                return new Point((int) Math.round(getWidth() * xRatio),
                        (int) Math.round(getHeight() * yRatio));
            }

            private void drawMapRoute(Graphics2D g2, Point start, Point end, float phase) {
                Stroke previousStroke = g2.getStroke();
                g2.setColor(new Color(236, 222, 177, 168));
                float dashPhase = (phase - (float) Math.floor(phase)) * 22f;
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                        1f, new float[] { 7f, 9f }, dashPhase));
                g2.drawLine(start.x, start.y, end.x, end.y);

                double progress = phase - Math.floor(phase);
                int sparkX = (int) Math.round(start.x + ((end.x - start.x) * progress));
                int sparkY = (int) Math.round(start.y + ((end.y - start.y) * progress));
                RadialGradientPaint spark = new RadialGradientPaint(
                        new Point(sparkX, sparkY),
                        12f,
                        new float[] { 0f, 0.45f, 1f },
                        new Color[] {
                                new Color(255, 245, 210, 205),
                                new Color(255, 221, 128, 110),
                                new Color(255, 221, 128, 0)
                        });
                g2.setPaint(spark);
                g2.fillOval(sparkX - 12, sparkY - 12, 24, 24);
                g2.setStroke(previousStroke);
            }

            private void drawMapNode(Graphics2D g2, int centerX, int centerY, Color glowColor, Color coreColor,
                    float offset, float phase) {
                float pulse = 0.5f + (0.5f * (float) Math.sin((phase * 2.8f) + offset));
                int outerRadius = 18 + Math.round(pulse * 6f);
                int middleRadius = 13 + Math.round(pulse * 3f);
                g2.setColor(new Color(glowColor.getRed(), glowColor.getGreen(), glowColor.getBlue(), 55));
                g2.fillOval(centerX - outerRadius, centerY - outerRadius, outerRadius * 2, outerRadius * 2);
                g2.setColor(new Color(glowColor.getRed(), glowColor.getGreen(), glowColor.getBlue(), 90));
                g2.fillOval(centerX - middleRadius, centerY - middleRadius, middleRadius * 2, middleRadius * 2);
                g2.setColor(coreColor);
                g2.fillOval(centerX - 8, centerY - 8, 16, 16);
                g2.setColor(new Color(255, 249, 215, 245));
                g2.setStroke(new BasicStroke(2f));
                g2.drawOval(centerX - 10, centerY - 10, 20, 20);
            }

            private void drawMapLabel(Graphics2D g2, String text, int x, int y) {
                Font labelFont = getBodyFont(11f).deriveFont(Font.BOLD, 11f);
                FontMetrics metrics = g2.getFontMetrics(labelFont);
                int paddingX = 12;
                int labelWidth = metrics.stringWidth(text) + (paddingX * 2);
                int labelHeight = 24;

                g2.setColor(new Color(8, 22, 62, 214));
                g2.fillRoundRect(x, y, labelWidth, labelHeight, 12, 12);
                g2.setColor(new Color(52, 154, 255, 210));
                g2.setStroke(new BasicStroke(1.4f));
                g2.drawRoundRect(x, y, labelWidth, labelHeight, 12, 12);

                g2.setFont(labelFont);
                g2.setColor(new Color(240, 247, 255));
                g2.drawString(text, x + paddingX, y + 16);
            }
        };
        panel.setOpaque(false);
        panel.setPreferredSize(new Dimension(900, 180));
        panel.setBorder(BorderFactory.createLineBorder(COLOR_ACADEMY_BORDER, 2));

        Timer shimmerTimer = new Timer(50, event -> {
            if (!panel.isShowing()) {
                return;
            }
            animationPhase[0] += 0.03f;
            panel.repaint();
        });
        shimmerTimer.start();
        panel.addHierarchyListener(event -> {
            if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) == 0) {
                return;
            }
            if (panel.isShowing()) {
                shimmerTimer.start();
            } else {
                shimmerTimer.stop();
            }
        });
        return panel;
    }

    private JLabel createMainScreenHeading(String text) {
        JLabel label = graphics.createHeading(text, new Color(235, 250, 255));
        label.setFont(getHeadingFont(24f));
        return label;
    }

    private Font getHeadingFont(float size) {
        return graphics.getHeadingFont(size);
    }

    private Font getBodyFont(float size) {
        return graphics.getBodyFont(size);
    }

    private String styleHtml(String htmlBody) {
        return graphics.styleHtml(htmlBody);
    }

    private String styleRaw(String text) {
        return graphics.styleRaw(text);
    }

    private JLabel createHeading(String text) {
        return graphics.createHeading(text, COLOR_TEXT_DARK);
    }

    private JLabel createBody(String text) {
        return graphics.createBody(text, COLOR_TEXT_MUTED);
    }

    private JLabel createAcademyHeading(String text) {
        return graphics.createHeading(text, COLOR_ACADEMY_TEXT);
    }

    private JLabel createAcademyBody(String text) {
        return graphics.createBody(text, COLOR_ACADEMY_TEXT_MUTED);
    }

    // -------------------------------------------------------------------------
    // Button factories
    // -------------------------------------------------------------------------

    private JButton createPrimaryButton(String text) {
        return graphics.createPrimaryButton(text);
    }

    private JButton createSecondaryButton(String text) {
        return graphics.createSecondaryButton(text);
    }

    private JButton createBattleButton(String text) {
        return graphics.createBattleButton(text, COLOR_BATTLE_SURFACE);
    }

    private JButton createCharacterSelectButton(String text) {
        return graphics.createCharacterSelectButton(text);
    }

    // -------------------------------------------------------------------------
    // Landing screen
    // -------------------------------------------------------------------------

    private JPanel buildLandingScreen() {
        return graphics.buildLandingScreen(frame, this::openCharacterSelectFromLanding,
                this::loadSavedGame,
                () -> { if (frame != null) frame.dispose(); });
    }

    private Icon createScaledPortraitIcon(BufferedImage portrait, int targetWidth, int targetHeight) {
        return graphics.createScaledPortraitIcon(portrait, targetWidth, targetHeight);
    }

    private BufferedImage loadCharacterPortrait(String title) {
        return graphics.loadCharacterPortrait(title);
    }

    public void setGameFonts(Font heading, Font body) {
        graphics.setGameFonts(heading, body, rootPanel);
    }

    public void setGameFontSizes(float headingSize, float bodySize) {
        graphics.setGameFontSizes(headingSize, bodySize, rootPanel);
    }

    // -------------------------------------------------------------------------
    // Enemy sprite
    // -------------------------------------------------------------------------

    private void updateEnemySprite(Entity enemy) {
        graphics.updateEnemySprite(enemy, battlePanel);
    }

    private void updateHeroSprite() {
        graphics.updateHeroSprite(hero, battlePanel);
    }

    // -------------------------------------------------------------------------
    // Console stream wiring
    // -------------------------------------------------------------------------

    private void wireConsoleStreams() {
        System.setIn(gameInputStream);

        PrintStream guiOut = new PrintStream(
                new BufferedOutputStream(
                        new TextAreaOutputStream(outputArea, this::filterLegacyOutput, this::observeLegacyOutput), 1),
                true, StandardCharsets.UTF_8);

        //System.setOut(guiOut);
        //System.setErr(guiOut);
    }

    // -------------------------------------------------------------------------
    // Hero selection + dashboard refresh
    // -------------------------------------------------------------------------

    private void selectHero(Hero selectedHero) {
        hero = selectedHero;
        inventoryNarrationShown = false;
        hero.setHpCap(hero.getHp());
        hero.setManaCap(hero.getMana());
        refreshHeroDashboard();
        refreshInventoryPanel();
        refreshProfilePanel();
        appendLog("\nHero selected: " + hero.getName() + " the " + hero.getCharClass() + ".\n");
        beginStoryForHero(true);
    }

    private void saveCurrentGame() {
        if (!requireHero()) return;

        try {
            int slot = chooseSaveSlot();
            if (slot == -1) return;

            Load.SlotInfo existingSlot = Load.getSlotInfo(slot);
            if (existingSlot.isOccupied()) {
                int overwrite = showConfirmSync("Overwrite Save",
                        existingSlot.getSummary() + "\n\nOverwrite this save?");
                if (overwrite != JOptionPane.YES_OPTION) return;
            }

            Load.saveGame(hero, slot);
            Load.SlotInfo savedSlot = Load.getSlotInfo(slot);
            showInfoSync("Game Saved", savedSlot.getSummary());
        } catch (Exception exception) {
            showWarningSync("Save Failed", exception.getMessage());
        }
    }

    private void loadSavedGame() {
        try {
            int slot = chooseLoadSlot();
            if (slot == -1) return;

            Hero loadedHero = Load.loadGame(slot);
            if (loadedHero == null) {
                showInfoSync("Load Game", "That save slot is empty.");
                return;
            }

            hero = loadedHero;
            inventoryNarrationShown = hero.hasOpenedInventory();
            currentStorySequence = new String[0];
            currentStoryIndex = 0;
            refreshHeroDashboard();
            refreshInventoryPanel();
            refreshProfilePanel();
            subtitleLabel.setText("Adventure overview.");
            rootLayout.show(rootPanel, ROOT_GAME);
            showScreen(SCREEN_MAIN);
            appendLog("\nLoaded save: " + hero.getName() + " the " + hero.getCharClass() + ".\n");
            showInfoSync("Game Loaded", "Loaded " + hero.getName() + ".");
        } catch (Exception exception) {
            showWarningSync("Load Failed", exception.getMessage());
        }
    }

    private int chooseSaveSlot() {
        int choice = showSlotChooserSync("Save Game", "Choose a save slot.", true);
        return choice >= 0 && choice < Load.SLOT_COUNT ? choice + 1 : -1;
    }

    private int chooseLoadSlot() {
        if (!Load.hasSave()) {
            showInfoSync("Load Game", "No saved game found yet.");
            return -1;
        }

        int choice = showSlotChooserSync("Load Game", "Choose a save to load.", false);
        if (choice < 0 || choice >= Load.SLOT_COUNT) return -1;

        Load.SlotInfo selectedSlot = Load.getSlotInfo(choice + 1);
        if (!selectedSlot.isOccupied()) {
            showInfoSync("Load Game", "That save slot is empty.");
            return -1;
        }
        if (!selectedSlot.isReadable()) {
            showWarningSync("Load Game", "That save slot could not be read.");
            return -1;
        }
        return choice + 1;
    }

    private void refreshHeroDashboard() {
        if (hero == null) {
            heroNameValue.setText("-");
            heroClassValue.setText("-");
            heroLevelValue.setText("-");
            heroGoldValue.setText("-");
            hpBar.setMaximum(1); hpBar.setValue(0); hpBar.setString("0 / 0");
            manaBar.setMaximum(1); manaBar.setValue(0); manaBar.setString("0 / 0");
            refreshJourneyStatus();
            return;
        }

        heroNameValue.setText(hero.getName());
        heroClassValue.setText("Class: " + hero.getCharClass());
        heroLevelValue.setText("Level: " + hero.getLevel());
        heroGoldValue.setText("Gold: " + statFormat.format(hero.getGold()));

        hpBar.setMaximum(Math.max(1, hero.getHpCap()));
        hpBar.setValue(Math.max(0, hero.getHp()));
        hpBar.setString(statFormat.format(hero.getHp()) + " / " + statFormat.format(hero.getHpCap()));

        manaBar.setMaximum(Math.max(1, hero.getManaCap()));
        manaBar.setValue(Math.max(0, hero.getMana()));
        manaBar.setString(statFormat.format(hero.getMana()) + " / " + statFormat.format(hero.getManaCap()));

        refreshAreaButtons();
        refreshJourneyStatus();
    }

    private void refreshAreaButtons() {
        if (forestButton == null || swampButton == null || forsakenButton == null) return;

        boolean hasHero = hero != null;
        forestButton.setEnabled(hasHero && hero.hasUnlockedArea1());
        swampButton.setEnabled(hasHero && hero.hasUnlockedArea2());
        forsakenButton.setEnabled(hasHero && hero.hasUnlockedArea3());

        forestButton.setToolTipText(hasHero && !hero.hasUnlockedArea1() ? "Unlock through the Principal Office flow." : null);
        swampButton.setToolTipText(hasHero && !hero.hasUnlockedArea2() ? "Unlock through the Principal Office flow." : null);
        forsakenButton.setToolTipText(hasHero && !hero.hasUnlockedArea3() ? "Unlock through the Principal Office flow." : null);
    }

    private void refreshInventoryPanel() {
        if (hero == null) {
            inventorySummary.setText("No hero selected.");
            smallHealthCount.setText("0");
            mediumHealthCount.setText("0");
            largeHealthCount.setText("0");
            smallManaCount.setText("0");
            mediumManaCount.setText("0");
            largeManaCount.setText("0");
            shopGoldLabel.setText("Gold: -");
            shopStatusLabel.setText("Choose a hero first.");
            return;
        }

        Inventory inventory = hero.getInventory();
        inventorySummary.setText(
                "HP " + statFormat.format(hero.getHp()) + "/" + statFormat.format(hero.getHpCap()) +
                "  |  Mana " + statFormat.format(hero.getMana()) + "/" + statFormat.format(hero.getManaCap()));
        smallHealthCount.setText(String.valueOf(inventory.getSmallHealthPotion()));
        mediumHealthCount.setText(String.valueOf(inventory.getMediumHealthPotion()));
        largeHealthCount.setText(String.valueOf(inventory.getLargeHealthPotion()));
        smallManaCount.setText(String.valueOf(inventory.getSmallManaPotion()));
        mediumManaCount.setText(String.valueOf(inventory.getMediumManaPotion()));
        largeManaCount.setText(String.valueOf(inventory.getLargeManaPotion()));
        shopGoldLabel.setText("Gold: " + statFormat.format(hero.getGold()));
    }

    private void refreshProfilePanel() {
        if (hero == null) return;
        profileName.setText(hero.getName());
        profileClass.setText(hero.getCharClass());
        profileWeapon.setText(hero.getWeapon());
        profileLevel.setText(String.valueOf(hero.getLevel()));
        profileAttack.setText(statFormat.format(hero.getAttack()));
        profileDefense.setText(statFormat.format(hero.getDefense()));
        profileSpeed.setText(statFormat.format(hero.getSpeed()));
        profileSkill1.setText(hero.getSkill1());
        profileSkill2.setText(hero.getSkill2());
        profileUltimate.setText(hero.getUltimate());
    }

    // -------------------------------------------------------------------------
    // Story sequence
    // -------------------------------------------------------------------------

    private void beginStoryForHero(boolean firstTimeSelection) {
        if (hero == null) return;
        storyHeroWalkFrames = graphics.loadStoryHeroWalkFrames(hero);
        currentStorySequence = buildIntroStoryForHero(hero);
        currentStoryIndex = 0;
        storyTitleLabel.setText(firstTimeSelection ? "Opening Story" : "Storyline Recap");
        subtitleLabel.setText(firstTimeSelection ? "Story begins." : "Review your hero's storyline.");
        showCurrentStoryBeat();
        showScreen(SCREEN_STORY);
    }

    private void showCurrentStoryBeat() {
        if (currentStorySequence.length == 0) {
            finishStorySequence("No story scenes are available for this hero yet.");
            return;
        }
        int safeIndex = Math.max(0, Math.min(currentStoryIndex, currentStorySequence.length - 1));
        storyProgressLabel.setText("Scene " + (safeIndex + 1) + " / " + currentStorySequence.length);
        startStoryTypewriter(currentStorySequence[safeIndex]);

        boolean isLastScene = safeIndex >= currentStorySequence.length - 1;
        storyNextButton.setVisible(!isLastScene);
        storyStartButton.setVisible(isLastScene);
    }

    private void advanceStoryBeat() {
        if (storyTypewriterTimer != null && storyTypewriterTimer.isRunning()) {
            completeStoryTypewriter();
            return;
        }

        if (currentStoryIndex < currentStorySequence.length - 1) {
            currentStoryIndex++;
            showCurrentStoryBeat();
        } else {
            finishStorySequence("Story complete. Your journey continues.");
        }
    }

    private void finishStorySequence(String logMessage) {
        if (storyTypewriterTimer != null) {
            storyTypewriterTimer.stop();
            storyTypewriterTimer = null;
        }
        subtitleLabel.setText("Adventure overview.");
        refreshHeroDashboard();
        showScreen(SCREEN_MAIN);
        appendLog("\n" + logMessage + "\n");
    }

    private void startStoryTypewriter(String fullText) {
        if (storyTypewriterTimer != null) {
            storyTypewriterTimer.stop();
        }

        currentStoryLine = fullText == null ? "" : fullText;
        storyTextArea.setText("");
        storyTextArea.setCaretPosition(0);

        if (currentStoryLine.isEmpty()) {
            return;
        }

        final int[] index = { 0 };
        storyTypewriterTimer = new Timer(STORY_TYPEWRITER_DELAY_MS, event -> {
            if (index[0] >= currentStoryLine.length()) {
                ((Timer) event.getSource()).stop();
                return;
            }

            index[0]++;
            storyTextArea.setText(currentStoryLine.substring(0, index[0]));
            storyTextArea.setCaretPosition(storyTextArea.getDocument().getLength());
        });
        storyTypewriterTimer.start();
    }

    private void completeStoryTypewriter() {
        if (storyTypewriterTimer != null) {
            storyTypewriterTimer.stop();
        }
        storyTextArea.setText(currentStoryLine);
        storyTextArea.setCaretPosition(storyTextArea.getDocument().getLength());
    }

    private String[] buildIntroStoryForHero(Hero selectedHero) {
        String prologue = "Your days as a student have been a blur of half-hearted effort and quiet frustration. " +
                "One restless night, sleep gives way to something stranger. You wake beneath a pale moon, " +
                "facing a vast academy beyond the trees. A wandering spirit named Void Aetherlight " +
                "greets you and warns that Mystvale Academy is both sanctuary and trial. To survive, " +
                "you must grow stronger, uncover the truth behind Kim Morvain, and decide what kind of hero you will become.";

        String academy = "Mystvale Academy stands at the edge of danger. Within its halls lie the Library, the Training Ground, " +
                "the Principal's Office, and the paths leading toward the Forest of Reverie, Reverie's Edge, and the Forsaken Lands. " +
                "Every step forward reveals more than monsters. The academy hides wounds, secrets, and a fate tied to your chosen hero.";

        if (selectedHero instanceof Swordsman) {
            return new String[] {
                    prologue, academy,
                    "Neo Solmere carries the burden of a bloodline stained by betrayal. The Solmere family once guarded a forbidden power tied to Kim Morvain, " +
                    "and the curse left behind still follows their name. Neo enters Mystvale to become stronger, protect the people he loves, and uncover the truth hidden in his family's shadow.",
                    "For Neo, this journey is not just about defeating enemies. It is about confronting legacy, loyalty, and the darkness woven into his own blood. " +
                    "Every battle at Mystvale brings him closer to either breaking the curse or becoming part of it."
            };
        }

        if (selectedHero instanceof Gunner) {
            return new String[] {
                    prologue, academy,
                    "Fehld was stolen from home and forced into Project LUCENT, a cruel experiment designed by Kim Morvain. " +
                    "The trials gave Fehld terrifying precision and Aether-fueled power, but they also carved pain deep into body and memory.",
                    "Aria arrives in Mystvale carrying anger, survival instinct, and unfinished vengeance. Each step through the academy is a step toward the truth behind Project LUCENT " +
                    "and a reckoning with the man who tried to turn Aria into a weapon."
            };
        }

        return new String[] {
                prologue, academy,
                "Mleux was born with brilliance in cosmic magic, but her gift drew her into Kim Morvain's schemes. " +
                "She helped shape dangerous magic before discovering the horror it would unleash. By the time she tried to escape, a curse had already marked her.",
                "Selene now walks Mystvale with guilt, pride, and a fierce need to set things right. The road ahead is filled with spells, sacrifice, and buried truths. " +
                "To end Morvain's reign, she must face both the enemy before her and the regret still burning inside."
        };
    }

    // -------------------------------------------------------------------------
    // Academy GUI flows
    // -------------------------------------------------------------------------

    private void openLibraryGui() {
        if (!requireHero()) return;

        boolean firstVisit = !hero.hasVisitedLibrary();
        hero.setVisitedLibrary(true);
        if (firstVisit) playNarrationSequence("Library Narration", Narration.buildLibraryNarration());

        boolean hasQuest1 = !hero.isLibraryQuest1Done();
        boolean hasQuest2 = !hero.isLibraryQuest2Done();

        if (!hasQuest1 && !hasQuest2) {
            showInfoSync("Library", "All library quests are already completed.");
            return;
        }

        int questChoice;
        if (hasQuest1 && hasQuest2) {
            Object[] options = { "Lost Book Quest", "Riddle Quest", "Back" };
            questChoice = showOptionSync("Library", "Choose a library quest.", options, options[0]);
        } else if (hasQuest1) {
            questChoice = 0;
        } else {
            questChoice = 1;
        }

        if (questChoice == 0 && hasQuest1) {
            int accept = showConfirmSync("Library Quest", "Accept 'Find the Lost Book' quest?");
            if (accept == 0) {
                hero.setLibraryQuest1Done(true);
                grantRewards(800, 120, "Lost Book quest complete!");
            }
            return;
        }

        if (questChoice == 1 && hasQuest2) {
            int accept = showConfirmSync("Library Quest", "Accept 'Riddles of the Library' quest?");
            if (accept != 0) return;

            hero.setRiddle1Done(askRiddle(
                    "Riddle 1: I have pages, but I am not a tree. What am I?",
                    new Object[]{ "Book", "Sword", "Lantern" }, 0));
            hero.setRiddle2Done(askRiddle(
                    "Riddle 2: I am spent when used to light the way. What am I?",
                    new Object[]{ "Shadow", "Candle", "Stone" }, 1));
            hero.setRiddle3Done(askRiddle(
                    "Riddle 3: What grows stronger the more you train?",
                    new Object[]{ "Luck", "Discipline", "Silence" }, 1));

            if (hero.isAllRiddlesDone()) {
                hero.setLibraryQuest2Done(true);
                grantRewards(1200, 220, "Riddle quest complete!");
            } else {
                showInfoSync("Library", "Not all riddles were correct. Try the quest again.");
            }
        }
    }

    private void openTrainingGui() {
        if (!requireHero()) return;

        boolean firstVisit = !hero.hasVisitedGym();
        hero.setVisitedGym(true);
        if (firstVisit) playNarrationSequence("Training Ground", Narration.buildTrainingNarration());

        if (hero.hasFinishedAllTraining()) {
            showInfoSync("Training Ground", "Training is already complete.");
            return;
        }

        while (true) {
            Object[] options = {
                    hero.hasFinishedEndurance() ? "Endurance (Done)" : "Endurance",
                    hero.hasFinishedStrength() ? "Strength (Done)" : "Strength",
                    hero.hasFinishedDurability() ? "Durability (Done)" : "Durability",
                    hero.hasFinishedManaRefinement() ? "Mana Refinement (Done)" : "Mana Refinement",
                    "Quit Training"
            };

            int choice = showOptionSync("Training Ground",
                    "Complete all 4 trainings to unlock eligibility for Forest of Reverie.",
                    options, options[0]);

            if (choice == 4 || choice == -1) {
                if (hero.getNumberOfTrainingFinished() > 0 && hero.getNumberOfTrainingFinished() < 4) {
                    int confirm = showConfirmSync("Training Ground", "Quit now? Incomplete training progress will reset.");
                    if (confirm == 0) {
                        resetTrainingProgress();
                        showInfoSync("Training Ground", "Training progress reset.");
                        refreshHeroDashboard();
                        refreshProfilePanel();
                        return;
                    }
                    continue;
                }
                return;
            }

            if (choice == 0 && !hero.hasFinishedEndurance()) resolveTrainingTask("Endurance", () -> hero.setFinishedEndurance(true));
            else if (choice == 1 && !hero.hasFinishedStrength()) resolveTrainingTask("Strength", () -> hero.setFinishedStrength(true));
            else if (choice == 2 && !hero.hasFinishedDurability()) resolveTrainingTask("Durability", () -> hero.setFinishedDurability(true));
            else if (choice == 3 && !hero.hasFinishedManaRefinement()) resolveTrainingTask("Mana Refinement", () -> hero.setFinishedManaRefinement(true));

            if (hero.getNumberOfTrainingFinished() >= 4
                    && hero.hasFinishedEndurance() && hero.hasFinishedStrength()
                    && hero.hasFinishedDurability() && hero.hasFinishedManaRefinement()) {
                hero.setFinishedAllTraining(true);
                hero.unlockArea1(true);
                statProgress.endurance(hero);
                statProgress.strength(hero);
                statProgress.durability(hero);
                statProgress.mana(hero);
                grantRewards(2500, 500, "Training complete! You can now apply for Area 1 eligibility.");
                refreshHeroDashboard();
                refreshProfilePanel();
                return;
            }
        }
    }

    private void openShopGui() {
        if (!requireHero()) return;

        boolean firstVisit = !hero.hasVisitedShop();
        hero.setHasVisitedShop(true);
        if (firstVisit) {
            playNarrationSequence("Shop Narration", Narration.buildShopNarration());
            playNarrationSequence("Shopkeeper", Narration.buildShopConversationNarration());
        }
        subtitleLabel.setText("Browse the academy shop.");
        shopStatusLabel.setText("Choose an item, view details, or purchase a custom amount.");
        refreshInventoryPanel();
        refreshHeroDashboard();
        showScreen(SCREEN_SHOP);
    }

    private void openShopItemMenu(int itemChoice) {
        if (!requireHero()) return;

        String name = SHOP_ITEM_NAMES[itemChoice];
        int price = SHOP_PRICES[itemChoice];
        int owned = getPotionCount(hero.getInventory(), itemChoice);

        Object[] options = { "Purchase Item", "View Item Details", "Cancel" };
        int choice = showOptionSync(name,
                "Selected: " + name
                        + "\nPrice: " + statFormat.format(price) + " gold"
                        + "\nOwned: " + owned,
                options, options[0]);

        if (choice == 0) {
            buyShopItem(itemChoice);
        } else if (choice == 1) {
            showInfoSync(name, SHOP_ITEM_DETAILS[itemChoice]);
            shopStatusLabel.setText("Viewed details for " + name + ".");
        } else if (choice == 2) {
            shopStatusLabel.setText("Transaction cancelled.");
        }
    }

    private void buyShopItem(int itemChoice, int requestedQuantity) {
        if (!requireHero()) return;

        Inventory inventory = hero.getInventory();
        int currentCount = getPotionCount(inventory, itemChoice);
        int availableCapacity = inventory.getCapacity() - currentCount;
        String itemName = SHOP_ITEM_NAMES[itemChoice];
        int price = SHOP_PRICES[itemChoice];

        if (availableCapacity <= 0) {
            showShopNotice("Shop Keeper", itemName + " is already at max capacity.");
            return;
        }

        int quantity = Math.min(requestedQuantity, availableCapacity);
        int totalCost = price * quantity;

        if (hero.getGold() < totalCost) {
            showShopNotice("Shop Keeper", "Not enough gold for " + quantity + " " + itemName + ".");
            return;
        }

        hero.setGold(hero.getGold() - totalCost);
        setPotionCount(inventory, itemChoice, currentCount + quantity);
        refreshInventoryPanel();
        refreshHeroDashboard();

        String cappedNote = quantity < requestedQuantity
                ? " Max capacity reached, so only " + quantity + " bought." : "";
        shopStatusLabel.setText("Purchased " + quantity + " " + itemName
                + " for " + statFormat.format(totalCost) + " gold." + cappedNote);
    }

    private void buyShopItem(int itemChoice) {
        if (!requireHero()) return;

        Inventory inventory = hero.getInventory();
        int currentCount = getPotionCount(inventory, itemChoice);
        int availableCapacity = inventory.getCapacity() - currentCount;
        String itemName = SHOP_ITEM_NAMES[itemChoice];
        int price = SHOP_PRICES[itemChoice];

        if (availableCapacity <= 0) {
            showShopNotice("Shop Keeper", itemName + " is already at max capacity.");
            return;
        }

        if (hero.getGold() < price) {
            showShopNotice("Shop Keeper", "Not enough gold to purchase " + itemName + ".");
            return;
        }

        int maxAffordable = hero.getGold() / price;
        int maxQuantity = Math.min(availableCapacity, Math.min(inventory.getCapacity(), maxAffordable));
        int quantity = promptShopQuantity(itemName, maxQuantity);
        if (quantity <= 0) {
            shopStatusLabel.setText("Purchase cancelled.");
            return;
        }

        int totalCost = price * quantity;

        if (hero.getGold() < totalCost) {
            showShopNotice("Shop Keeper", "Not enough gold for " + quantity + " " + itemName + ".");
            return;
        }

        int confirm = showConfirmSync("Confirm Purchase",
                "Purchase " + quantity + " " + itemName + "(s)"
                        + "\nfor " + statFormat.format(totalCost) + " gold?");
        if (confirm != JOptionPane.YES_OPTION) {
            shopStatusLabel.setText("Purchase cancelled.");
            return;
        }

        hero.setGold(hero.getGold() - totalCost);
        setPotionCount(inventory, itemChoice, currentCount + quantity);
        refreshInventoryPanel();
        refreshHeroDashboard();

        shopStatusLabel.setText("Purchased " + quantity + " " + itemName
                + " for " + statFormat.format(totalCost) + " gold.");
    }

    private int promptShopQuantity(String itemName, int maxQuantity) {
        if (maxQuantity <= 0) return 0;

        final int[] quantity = { 0 };
        runOnEdtSync(() -> {
            JSpinner amountSpinner = new JSpinner(new SpinnerNumberModel(1, 1, maxQuantity, 1));
            amountSpinner.setFont(getBodyFont(15f));

            JPanel panel = new JPanel(new BorderLayout(0, 8));
            panel.add(new JLabel("Enter amount to purchase (max " + maxQuantity + "):"), BorderLayout.NORTH);
            panel.add(amountSpinner, BorderLayout.CENTER);

            int result = JOptionPane.showConfirmDialog(frame, panel, itemName,
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (result == JOptionPane.OK_OPTION) {
                quantity[0] = (Integer) amountSpinner.getValue();
            }
        });
        return quantity[0];
    }

    private void showShopNotice(String title, String message) {
        shopStatusLabel.setText("Choose an item, view details, or purchase a custom amount.");
        showInfoSync(title, message);
    }

    private void openPrincipalOfficeGui() {
        if (!requireHero()) return;

        boolean firstVisit = !hero.hasVisitedOffice();
        hero.setVisitedOffice(true);
        if (firstVisit) playNarrationSequence("Principal's Office", Narration.buildPrincipalOfficeNarration());

        if (!hero.hasUnlockedArea1() && hero.canEnterArea1() && hero.hasFinishedAllTraining()) {
            playNarrationSequence("Eligibility Granted", Narration.buildArea1EligibilityNarration());
            hero.setUnlockArea1(true);
            grantRewards(2500, 500, "Eligibility granted: Forest of Reverie unlocked.");
            refreshHeroDashboard();
            return;
        }

        if (!hero.hasUnlockedArea2() && hero.canEnterArea2() && hero.getHaveDefeatedArea1Boss()) {
            playNarrationSequence("Eligibility Granted", Narration.buildArea2EligibilityNarration());
            hero.setUnlockArea2(true);
            grantRewards(2500, 500, "Eligibility granted: Reverie's Edge unlocked.");
            refreshHeroDashboard();
            return;
        }

        if (!hero.hasUnlockedArea3() && hero.canEnterArea3() && hero.getHaveDefeatedArea2Boss()) {
            playNarrationSequence("Eligibility Granted", Narration.buildArea3EligibilityNarration());
            hero.setUnlockArea3(true);
            grantRewards(2500, 500, "Eligibility granted: Forsaken Lands unlocked.");
            refreshHeroDashboard();
            return;
        }

        showInfoSync("Principal's Office", "Not eligible yet.\nFinish training and defeat area bosses first.");
    }

    // -------------------------------------------------------------------------
    // Training helpers
    // -------------------------------------------------------------------------

    private void resolveTrainingTask(String taskName, Runnable markComplete) {
        int start = showConfirmSync("Training Ground", "Start " + taskName + " challenge?");
        if (start != 0) return;

        boolean success = random.nextInt(10) <= 7;
        if (success) {
            markComplete.run();
            hero.setNumberOfTrainingFinished(hero.getNumberOfTrainingFinished() + 1);
            showInfoSync("Training Ground", taskName + " complete!");
        } else {
            showInfoSync("Training Ground", taskName + " failed. Try again.");
        }
    }

    private void resetTrainingProgress() {
        hero.setFinishedEndurance(false);
        hero.setFinishedStrength(false);
        hero.setFinishedDurability(false);
        hero.setFinishedManaRefinement(false);
        hero.setFinishedAllTraining(false);
        hero.setNumberOfTrainingFinished(0);
        hero.unlockArea1(false);
    }

    // -------------------------------------------------------------------------
    // Potion helpers
    // -------------------------------------------------------------------------

    private int getPotionCount(Inventory inventory, int index) {
        return switch (index) {
            case 0 -> inventory.getSmallHealthPotion();
            case 1 -> inventory.getMediumHealthPotion();
            case 2 -> inventory.getLargeHealthPotion();
            case 3 -> inventory.getSmallManaPotion();
            case 4 -> inventory.getMediumManaPotion();
            case 5 -> inventory.getLargeManaPotion();
            default -> 0;
        };
    }

    private void setPotionCount(Inventory inventory, int index, int value) {
        switch (index) {
            case 0 -> inventory.setSmallHealthPotion(value);
            case 1 -> inventory.setMediumHealthPotion(value);
            case 2 -> inventory.setLargeHealthPotion(value);
            case 3 -> inventory.setSmallManaPotion(value);
            case 4 -> inventory.setMediumManaPotion(value);
            case 5 -> inventory.setLargeManaPotion(value);
        }
    }

    private void usePotion(String itemCode) {
        if (!requireHero()) return;
        Inventory inventory = hero.getInventory();

        switch (itemCode) {
            case "SH" -> { if (inventory.getSmallHealthPotion() > 0) inventory.useSmallHealthPotion(hero); }
            case "MH" -> { if (inventory.getMediumHealthPotion() > 0) inventory.useMediumHealthPotion(hero); }
            case "LH" -> { if (inventory.getLargeHealthPotion() > 0) inventory.useLargeHealthPotion(hero); }
            case "SM" -> { if (inventory.getSmallManaPotion() > 0) inventory.useSmallManaPotion(hero); }
            case "MM" -> { if (inventory.getMediumManaPotion() > 0) inventory.useMediumManaPotion(hero); }
            case "LM" -> { if (inventory.getLargeManaPotion() > 0) inventory.useLargeManaPotion(hero); }
        }

        refreshInventoryPanel();
        refreshHeroDashboard();
    }

    // -------------------------------------------------------------------------
    // Rewards + riddles
    // -------------------------------------------------------------------------

    private void grantRewards(int gold, int xp, String message) {
        hero.setGold(hero.getGold() + gold);
        hero.levelUp(xp);
        if (hero.getHp() > hero.getHpCap()) hero.setHpCap(hero.getHp());
        if (hero.getMana() > hero.getManaCap()) hero.setManaCap(hero.getMana());
        refreshHeroDashboard();
        refreshInventoryPanel();
        refreshProfilePanel();
        showInfoSync("Rewards", message + "\nRewards: +" + statFormat.format(gold) + " gold, +" + statFormat.format(xp) + " XP.");
    }

    private boolean askRiddle(String question, Object[] options, int correctIndex) {
        return showOptionSync("Library Riddle", question, options, options[0]) == correctIndex;
    }

    // -------------------------------------------------------------------------
    // Area launchers
    // -------------------------------------------------------------------------

    private void launchArea1() {
        if (!requireHero()) return;
        if (!hero.hasUnlockedArea1()) { showInfoSync("Forest of Reverie", "Forest of Reverie is still locked."); return; }
        startAreaAdventureAsync(this::startForestOfReverieAdventure);
        SoundManager.stopMusic();
        SoundManager.playMusic("src/com/ror/models/assets/sounds/forestOfReverie.wav");
    }

    private void launchArea2() {
        if (!requireHero()) return;
        if (!hero.hasUnlockedArea2()) { showInfoSync("Reverie's Edge", "Reverie's Edge is still locked."); return; }
        startAreaAdventureAsync(this::startReveriesEdgeAdventure);
        SoundManager.stopMusic();
        SoundManager.playMusic("src/com/ror/models/assets/sounds/reverieLands.wav");
    }

    private void launchArea3() {
        if (!requireHero()) return;
        if (!hero.hasUnlockedArea3()) { showInfoSync("Forsaken Lands", "Forsaken Lands is still locked."); return; }
        startAreaAdventureAsync(this::startForsakenLandsAdventure);
        SoundManager.stopMusic();
        SoundManager.playMusic("src/com/ror/models/assets/sounds/forsakenLands.wav");
    }

    private void startAreaAdventureAsync(Runnable adventureTask) {
        battlePanel.setBattleButtonsEnabled(false);

        Thread worker = new Thread(() -> {
            try {
                adventureTask.run();
            } catch (Exception exception) {
                SwingUtilities.invokeLater(() -> showWarningSync("Error", "Area flow error: " + exception.getMessage()));
            } finally {
                SwingUtilities.invokeLater(() -> {
                    refreshHeroDashboard();
                    refreshInventoryPanel();
                    refreshProfilePanel();
                    if (hero == null) {
                        subtitleLabel.setText("Choose a hero to begin.");
                        showScreen(SCREEN_CHARACTER);
                    } else {
                        subtitleLabel.setText("Adventure overview.");
                        showScreen(SCREEN_MAIN);
                    }
                    battlePanel.setBattleButtonsEnabled(false);
                });
            }
        }, "area-adventure");

        worker.start();
    }

    private void runTravelTransitionSync() {
        if (frame == null) return;

        CountDownLatch transitionFinished = new CountDownLatch(1);
        SwingUtilities.invokeLater(() -> transitionManager.runTransition(frame, null, transitionFinished::countDown));
        try {
            transitionFinished.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    // -------------------------------------------------------------------------
    // Area adventures
    // -------------------------------------------------------------------------

    private void startForestOfReverieAdventure() {
        if (!requireHero()) return;

        boolean firstVisit = !hero.hasVisitedArea1();
        hero.setHasVisitedArea1(true);
        if (firstVisit) playNarrationSequence("Forest of Reverie", Narration.buildArea1Narration());
        runTravelTransitionSync();

        Entity[] encounters = { new Goblin(), new Slime(), new MudLurker(), new Elderthorn() };
        int[] goldRewards = { 360, 420, 680, 1200 };
        int[] xpRewards = { 90, 110, 180, 320 };

        if (!runAreaAdventure("Forest of Reverie", encounters, goldRewards, xpRewards)) return;

        hero.setHaveDefeatedArea1Boss(true);
        hero.unlockArea2(true);
        showInfoSync("Area Cleared", "Forest of Reverie cleared.\nVisit Principal's Office to process Area 2 eligibility.");
    }

    private void startReveriesEdgeAdventure() {
        if (!requireHero()) return;

        boolean firstVisit = !hero.hasVisitedArea2();
        hero.setHasVisitedArea2(true);
        if (firstVisit) playNarrationSequence("Reverie's Edge", Narration.buildArea2Narration());
        runTravelTransitionSync();

        Entity[] encounters = { new SwampRat(), new VeilSerpent(), new FadingWarden(), new Morgrath() };
        int[] goldRewards = { 520, 620, 900, 1500 };
        int[] xpRewards = { 130, 170, 240, 420 };

        if (!runAreaAdventure("Reverie's Edge", encounters, goldRewards, xpRewards)) return;

        hero.setHaveDefeatedArea2Boss(true);
        hero.unlockArea3(true);
        showInfoSync("Area Cleared", "Reverie's Edge cleared.\nVisit Principal's Office to process Area 3 eligibility.");
    }

    private void startForsakenLandsAdventure() {
        if (!requireHero()) return;

        boolean firstVisit = !hero.hasVisitedArea3();
        hero.setHasVisitedArea3(true);
        if (firstVisit) playNarrationSequence("Forsaken Lands", Narration.buildArea3Narration());
        runTravelTransitionSync();

        Entity[] encounters = { new ShadowAbyss(), new VoidBeast(), new HollowKing(), new Azrael(), new Kim() };
        int[] goldRewards = { 760, 880, 1300, 2000, 4000 };
        int[] xpRewards = { 210, 260, 360, 650, 1500 };

        if (!runAreaAdventure("Forsaken Lands", encounters, goldRewards, xpRewards)) return;

        hero.setHaveDefeatedArea3Boss(true);

        Object[] options = { "Sacrifice and Return", "Refuse and Repeat Cycle" };
        int endingChoice = showOptionSync("Final Choice", "Kim Morvain has fallen.\nChoose your fate:", options, options[0]);

        if (endingChoice == 0) {
            playNarrationSequence("Final Ending", Narration.buildSacrificeEndingNarration());
            showInfoSync("Ending", "You sacrificed your character and returned to the real world.\nYou cleared the game.");
        } else {
            playNarrationSequence("Loop Ending", Narration.buildLoopEndingNarration());
            hero.resetAllProgress();
            hero = null;
            runOnEdtSync(() -> {
                refreshHeroDashboard();
                refreshInventoryPanel();
                refreshProfilePanel();
                subtitleLabel.setText("Choose a hero to begin.");
                showScreen(SCREEN_CHARACTER);
            });
            showInfoSync("Loop Ending", "The cycle repeats. Progress has been reset.");
        }
    }

    private boolean runAreaAdventure(String areaName, Entity[] encounters, int[] goldRewards, int[] xpRewards) {
        if (encounters.length > 0) {
            showAreaBattleContext(areaName, encounters[0], "Arrived in " + areaName + ".\nPrepare for battle.");
        }
        showInfoSync(areaName, "Entering " + areaName + "...\nPrepare for battle!");

        for (int i = 0; i < encounters.length; i++) {
            Entity enemy = encounters[i];
            if (enemy instanceof Kim) {
                SoundManager.stopMusic();
                SoundManager.playMusic("src/com/ror/models/assets/sounds/finalBoss.wav");
            }
            showAreaBattleContext(areaName, enemy, enemy.getName() + " blocks your path.");
            int proceed = showConfirmSync(areaName, enemy.getName() + "\nStart battle?");
            if (proceed != 0) { showInfoSync(areaName, "Adventure cancelled."); return false; }

            BattleOutcome outcome = runButtonBattle(enemy, areaName);
            if (outcome == BattleOutcome.RAN) { showInfoSync(areaName, "You retreated from battle."); return false; }
            if (outcome == BattleOutcome.LOST) {
                showWarningSync("Defeat", "You were defeated and brought back outside the academy.\nArea progress was not completed.");
                return false;
            }

            grantRewards(goldRewards[i], xpRewards[i], enemy.getName() + " defeated!");
        }

        return true;
    }

    private void showAreaBattleContext(String areaName, Entity enemy, String logText) {
        runOnEdtSync(() -> {
            enemy.setHpCap(enemy.getHp());
            enemy.setManaCap(enemy.getMana());
            battlePanel.restoreBattleButtons();
            battlePanel.getBattleTitleValue().setText(areaName);
            battlePanel.getBattleRoundValue().setText("Prepare");
            battlePanel.getBattleHeroNameValue().setText(hero.getName());
            battlePanel.getBattleEnemyNameValue().setText(enemy.getName());
            updateEnemySprite(enemy);
            updateBattleBars(enemy);
            battlePanel.getBattleLogArea().setText(logText);
            battlePanel.getBattleLogArea().setCaretPosition(0);
            battlePanel.setBattleButtonsEnabled(false);
            subtitleLabel.setText(areaName);
            showScreen(SCREEN_BATTLE);
        });
    }

    // -------------------------------------------------------------------------
    // Battle engine
    // -------------------------------------------------------------------------

    private BattleOutcome runButtonBattle(Entity enemy, String battleTitle) {
        if (!requireHero()) return BattleOutcome.LOST;

        int originalHp = hero.getHp();
        int originalMana = hero.getMana();
        int originalCooldown1 = hero.getCooldown1();
        int originalCooldown2 = hero.getCooldown2();
        int originalCooldownU = hero.getCooldownU();
        int originalStun = hero.getStunned();
        int originalPoison = hero.getPoison();

        enemy.setHpCap(enemy.getHp());
        enemy.setManaCap(enemy.getMana());
        int round = 1;
        String battleLog = "Battle started against " + enemy.getName() + ".";

        while (hero.getHp() > 0 && enemy.getHp() > 0) {
            refreshHeroDashboard();
            refreshInventoryPanel();
            refreshProfilePanel();

            if (hero.getStunned() > 0) {
                battleLog = appendBattleLog(battleLog, hero.getName() + " is stunned and loses this turn.");
            } else {
                int choice = chooseBattleAction(enemy, round, battleTitle, battleLog);
                if (choice == -1) choice = 5;

                if (choice == 4) { usePotionInBattle(); continue; }

                if (choice == 5) {
                    if (attemptRunAway(enemy)) {
                        restoreHeroBattleState(originalHp, originalMana, originalCooldown1,
                                originalCooldown2, originalCooldownU, originalStun, originalPoison);
                        return BattleOutcome.RAN;
                    }
                    battleLog = appendBattleLog(battleLog, "Retreat failed.");
                } else {
                    String result = performPlayerAction(choice, enemy);
                    if (result.startsWith("INVALID:")) {
                        showInfoSync("Battle", result.replace("INVALID:", "").trim());
                        continue;
                    }
                    battleLog = appendBattleLog(battleLog, result);
                }
            }

            applyPlayerAfterTurnEffects();
            reducePlayerCooldowns();

            if (willTriggerAzraelRebirth(enemy)) {
                playEnemyRebirthAnimation(enemy);
                battleLog = appendBattleLog(battleLog, performAzraelRebirth(enemy));
                applyEnemyAfterTurnEffects(enemy);
                reduceEnemyCooldowns(enemy);
                round++;
                continue;
            }

            if (enemy.getHp() <= 0) {
                enemy.setHp(0);
                battleLog = enemy.getName() + " was defeated.";
                showInfoSync(battleTitle, battleLog);
                restoreHeroBattleState(originalHp, originalMana, originalCooldown1,
                        originalCooldown2, originalCooldownU, originalStun, originalPoison);
                return BattleOutcome.WON;
            }

            if (enemy.getStunned() > 0) {
                battleLog = appendBattleLog(battleLog, enemy.getName() + " is stunned and cannot act.");
            } else if (enemy.getDisabled() > 0) {
                battleLog = appendBattleLog(battleLog, enemy.getName() + " is disabled and cannot act.");
            } else {
                if (willTriggerAzraelRebirth(enemy)) {
                    playEnemyRebirthAnimation(enemy);
                } else {
                    playEnemyAttackAnimation(enemy);
                }
                battleLog = appendBattleLog(battleLog, performEnemyAction(enemy));
            }

            applyEnemyAfterTurnEffects(enemy);
            reduceEnemyCooldowns(enemy);

            if (hero.getHp() <= 0) {
                hero.setHp(0);
                showWarningSync(battleTitle, battleLog);
                restoreHeroBattleState(originalHp, originalMana, originalCooldown1,
                        originalCooldown2, originalCooldownU, originalStun, originalPoison);
                return BattleOutcome.LOST;
            }

            round++;
        }

        restoreHeroBattleState(originalHp, originalMana, originalCooldown1,
                originalCooldown2, originalCooldownU, originalStun, originalPoison);
        return hero.getHp() > 0 ? BattleOutcome.WON : BattleOutcome.LOST;
    }

    private int chooseBattleAction(Entity enemy, int round, String battleTitle, String battleLog) {
        runOnEdtSync(() -> {
            battlePanel.restoreBattleButtons();
            battlePanel.setBattleBackgroundForArea(battleTitle);
            battlePanel.getBattleTitleValue().setText(battleTitle);
            battlePanel.getBattleRoundValue().setText("Round " + round);
            battlePanel.getBattleHeroNameValue().setText(hero.getName());
            battlePanel.getBattleEnemyNameValue().setText(enemy.getName());
            updateHeroSprite();
            updateEnemySprite(enemy);
            updateBattleBars(enemy);
            battlePanel.getBattleLogArea().setText(truncateBattleLog(battleLog));
            battlePanel.getBattleLogArea().setCaretPosition(
                    battlePanel.getBattleLogArea().getDocument().getLength());
            subtitleLabel.setText(battleTitle);
            showScreen(SCREEN_BATTLE);
            battlePanel.setBattleButtonsEnabled(true);
            battlePanel.updateHeroActionButtonCooldowns(hero);
        });
        return waitForBattleAction();
    }

    private void updateBattleBars(Entity enemy) {
        battlePanel.getBattleHeroHpBar().setMaximum(Math.max(1, hero.getHpCap()));
        battlePanel.getBattleHeroHpBar().setValue(Math.max(0, hero.getHp()));
        battlePanel.getBattleHeroHpBar().setString(
                statFormat.format(Math.max(0, hero.getHp())) + " / " + statFormat.format(hero.getHpCap()));
        battlePanel.getBattleHeroLevelValue().setText("Level " + hero.getLevel());

        battlePanel.getBattleHeroManaBar().setMaximum(Math.max(1, hero.getManaCap()));
        battlePanel.getBattleHeroManaBar().setValue(Math.max(0, hero.getMana()));
        battlePanel.getBattleHeroManaBar().setString(
                statFormat.format(Math.max(0, hero.getMana())) + " / " + statFormat.format(hero.getManaCap()));

        battlePanel.getBattleEnemyHpBar().setMaximum(Math.max(1, enemy.getHpCap()));
        battlePanel.getBattleEnemyHpBar().setValue(Math.max(0, enemy.getHp()));
        battlePanel.getBattleEnemyHpBar().setString(
                statFormat.format(Math.max(0, enemy.getHp())) + " / " + statFormat.format(enemy.getHpCap()));

        battlePanel.getBattleEnemyManaBar().setMaximum(Math.max(1, enemy.getManaCap()));
        battlePanel.getBattleEnemyManaBar().setValue(Math.max(0, enemy.getMana()));
        battlePanel.getBattleEnemyManaBar().setString(
                statFormat.format(Math.max(0, enemy.getMana())) + " / " + statFormat.format(enemy.getManaCap()));
    }

    private void submitBattleAction(int action) {
        synchronized (battleActionLock) {
            if (pendingBattleAction != null) return;
            pendingBattleAction = action;
            runOnEdtSync(() -> battlePanel.setBattleButtonsEnabled(false));
            battleActionLock.notifyAll();
        }
    }

    private int waitForBattleAction() {
        synchronized (battleActionLock) {
            pendingBattleAction = null;
            while (pendingBattleAction == null) {
                try {
                    battleActionLock.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return 5;
                }
            }
            int action = pendingBattleAction;
            pendingBattleAction = null;
            return action;
        }
    }

    // -------------------------------------------------------------------------
    // Player / enemy actions
    // -------------------------------------------------------------------------

    private String performPlayerAction(int choice, Entity enemy) {
        int enemyHpBefore = enemy.getHp();
        int heroManaBefore = hero.getMana();
        int enemyStunBefore = enemy.getStunned();

        switch (choice) {
            case 0 -> {
                playHeroAttackAnimation();
                hero.basicAttack(hero, enemy);
            }
            case 1 -> {
                if (hero.getMana() < hero.scaledCost(hero.getManaCostSkill1())) return "INVALID:Not enough mana for Skill 1.";
                if (hero.getCooldown1() > 0) return "INVALID:Skill 1 is on cooldown (" + hero.getCooldown1() + ").";
                playHeroSkill1Animation();
                hero.skill1(hero, enemy);
            }
            case 2 -> {
                if (hero.getMana() < hero.scaledCost(hero.getManaCostSkill2())) return "INVALID:Not enough mana for Skill 2.";
                if (hero.getCooldown2() > 0) return "INVALID:Skill 2 is on cooldown (" + hero.getCooldown2() + ").";
                playHeroSkill2Animation();
                hero.skill2(hero, enemy);
            }
            case 3 -> {
                if (hero.getMana() < hero.scaledCost(hero.getManaCostUltimate())) return "INVALID:Not enough mana for Ultimate.";
                if (hero.getCooldownU() > 0) return "INVALID:Ultimate is on cooldown (" + hero.getCooldownU() + ").";
                playHeroUltimateAnimation();
                hero.ultimate(hero, enemy);
            }
            default -> { return "INVALID:Invalid action."; }
        }

        int dealt = Math.max(0, enemyHpBefore - enemy.getHp());
        int manaSpent = Math.max(0, heroManaBefore - hero.getMana());
        boolean stunned = enemy.getStunned() > enemyStunBefore;

        String text = hero.getName() + " dealt " + statFormat.format(dealt) + " to " + enemy.getName() + ".";
        if (manaSpent > 0) text += " Mana -" + statFormat.format(manaSpent) + ".";
        if (stunned) text += " Enemy stunned.";
        return text;
    }

    private String performEnemyAction(Entity enemy) {
        int heroHpBefore = hero.getHp();
        int enemyManaBefore = enemy.getMana();
        int heroStunBefore = hero.getStunned();
        int heroPoisonBefore = hero.getPoison();

        int choice = selectEnemyAction(enemy);
        switch (choice) {
            case 1 -> enemy.basicAttack(enemy, hero);
            case 2 -> enemy.skill1(enemy, hero);
            case 3 -> enemy.skill2(enemy, hero);
            case 4 -> enemy.skill3(enemy, hero);
            case 5 -> enemy.ultimate(enemy, hero);
            default -> enemy.basicAttack(enemy, hero);
        }

        int damage = Math.max(0, heroHpBefore - hero.getHp());
        int manaSpent = Math.max(0, enemyManaBefore - enemy.getMana());
        boolean stunned = hero.getStunned() > heroStunBefore;
        boolean poisoned = hero.getPoison() > heroPoisonBefore;

        String text = enemy.getName() + " dealt " + statFormat.format(damage) + " to " + hero.getName() + ".";
        if (manaSpent > 0) text += " Enemy mana -" + statFormat.format(manaSpent) + ".";
        if (stunned) text += " You are stunned.";
        if (poisoned) text += " You are poisoned.";
        return text;
    }

    private String performAzraelRebirth(Entity enemy) {
        if (!(enemy instanceof Azrael azrael)) {
            return enemy.getName() + " returns from the brink.";
        }

        int hpBefore = azrael.getHp();
        int manaBefore = azrael.getMana();
        azrael.skill3(azrael, hero);
        int restored = Math.max(0, azrael.getHp() - Math.max(0, hpBefore));
        int manaSpent = Math.max(0, manaBefore - azrael.getMana());

        runOnEdtSync(() -> updateBattleBars(azrael));

        String text = azrael.getName() + " is reborn and restores " + statFormat.format(restored) + " HP.";
        if (manaSpent > 0) text += " Enemy mana -" + statFormat.format(manaSpent) + ".";
        text += " Azrael is exhausted.";
        return text;
    }

    private void playEnemyAttackAnimation(Entity enemy) {
        int[] offsets = { -70, -130, -70 };
        for (int frameIndex = 0; frameIndex < 3; frameIndex++) {
            final int currentFrame = frameIndex;
            final int currentOffset = offsets[frameIndex];
            final boolean[] frameShown = { false };
            runOnEdtSync(() -> {
                battlePanel.setEnemySpriteOffsetX(currentOffset);
                frameShown[0] = graphics.updateEnemyAttackFrame(enemy, battlePanel, currentFrame);
            });
            if (!frameShown[0]) {
                runOnEdtSync(() -> battlePanel.setEnemySpriteOffsetX(0));
                return;
            }
            sleepQuietly(120);
        }

        runOnEdtSync(() -> {
            battlePanel.setEnemySpriteOffsetX(0);
            updateEnemySprite(enemy);
        });
        sleepQuietly(80);
    }

    private void playEnemyRebirthAnimation(Entity enemy) {
        for (int frameIndex = 0; frameIndex < 5; frameIndex++) {
            final int currentFrame = frameIndex;
            final boolean[] frameShown = { false };
            runOnEdtSync(() -> {
                battlePanel.setEnemySpriteOffsetX(0);
                frameShown[0] = graphics.updateEnemyRebirthFrame(enemy, battlePanel, currentFrame);
            });
            if (!frameShown[0]) {
                runOnEdtSync(() -> battlePanel.setEnemySpriteOffsetX(0));
                return;
            }
            sleepQuietly(150);
        }

        runOnEdtSync(() -> {
            battlePanel.setEnemySpriteOffsetX(0);
            updateEnemySprite(enemy);
        });
        sleepQuietly(100);
    }

    private boolean willTriggerAzraelRebirth(Entity enemy) {
        return enemy instanceof Azrael azrael
                && !azrael.getRevive()
                && azrael.getHp() < azrael.getHpCap() * 0.3;
    }

    private void playHeroSkill1Animation() {
        playHeroSkillAnimation(1);
    }

    private void playHeroSkill2Animation() {
        playHeroSkillAnimation(2);
    }

    private void playHeroUltimateAnimation() {
        playHeroActionAnimation(3);
    }

    private void playHeroAttackAnimation() {
        playHeroActionAnimation(0);
    }

    private void playHeroSkillAnimation(int skillNumber) {
        playHeroActionAnimation(skillNumber);
    }

    private void playHeroActionAnimation(int actionNumber) {
        int[] offsets = (hero instanceof Mage || hero instanceof Gunner)
                ? new int[] { 0, 0, 0 }
                : new int[] { 70, 130, 70 };
        for (int frameIndex = 0; frameIndex < 3; frameIndex++) {
            final int currentFrame = frameIndex;
            final int currentOffset = offsets[frameIndex];
            final boolean[] frameShown = { false };
            runOnEdtSync(() -> {
                battlePanel.setHeroSpriteOffsetX(currentOffset);
                frameShown[0] = switch (actionNumber) {
                    case 0 -> graphics.updateHeroAttackFrame(hero, battlePanel, currentFrame);
                    case 1 -> graphics.updateHeroSkill1Frame(hero, battlePanel, currentFrame);
                    case 2 -> graphics.updateHeroSkill2Frame(hero, battlePanel, currentFrame);
                    case 3 -> graphics.updateHeroUltimateFrame(hero, battlePanel, currentFrame);
                    default -> false;
                };
            });
            if (!frameShown[0]) {
                runOnEdtSync(() -> battlePanel.setHeroSpriteOffsetX(0));
                return;
            }
            sleepQuietly(120);
        }

        runOnEdtSync(() -> {
            battlePanel.setHeroSpriteOffsetX(0);
            updateHeroSprite();
        });
        sleepQuietly(60);
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private int selectEnemyAction(Entity enemy) {
        int[] pool = new int[5];
        int count = 0;
        pool[count++] = 1;

        if (enemy.getMana() >= enemy.getManaCostSkill1() && enemy.getCooldown1() == 0) pool[count++] = 2;
        if (enemy.getMana() >= enemy.getManaCostSkill2() && enemy.getCooldown2() == 0) pool[count++] = 3;

        boolean hasSkill3 = enemy.getSkill3() != null && !enemy.getSkill3().isBlank()
                && !"Unknown".equalsIgnoreCase(enemy.getSkill3());
        if (hasSkill3 && enemy.getMana() >= enemy.getManaCostSkill3() && enemy.getCooldown3() == 0) pool[count++] = 4;

        boolean hasUltimate = enemy.getUltimate() != null && !enemy.getUltimate().isBlank()
                && !"Unknown".equalsIgnoreCase(enemy.getUltimate());
        if (hasUltimate && enemy.getMana() >= enemy.getManaCostUltimate() && enemy.getCooldownU() == 0) pool[count++] = 5;

        return pool[random.nextInt(count)];
    }

    // -------------------------------------------------------------------------
    // Turn effects + cooldowns
    // -------------------------------------------------------------------------

    private void applyPlayerAfterTurnEffects() {
        if (hero.getPoison() > 0) {
            int poisonDamage = Math.max(1, (int) Math.round(hero.getHpCap() * 0.05));
            hero.setHp(Math.max(0, hero.getHp() - poisonDamage));
            hero.setPoison(hero.getPoison() - 1);
        }
        if (hero.getStunned() >= 0) hero.setStun(hero.getStunned() - 1);
        if (hero.getStunned() < -1) hero.setStun(-1);
        if (hero.getPoison() < 0) hero.setPoison(0);
    }

    private void applyEnemyAfterTurnEffects(Entity enemy) {
        if (enemy.getStunned() >= 0) enemy.setStun(enemy.getStunned() - 1);
        if (enemy.getDisabled() >= 0) enemy.setDisabled(enemy.getDisabled() - 1);
        if (enemy.getStunned() < -1) enemy.setStun(-1);
        if (enemy.getDisabled() < -1) enemy.setDisabled(-1);
    }

    private void reducePlayerCooldowns() {
        if (hero.getCooldown1() > 0) hero.setCooldown1(hero.getCooldown1() - 1);
        if (hero.getCooldown2() > 0) hero.setCooldown2(hero.getCooldown2() - 1);
        if (hero.getCooldownU() > 0) hero.setCooldownU(hero.getCooldownU() - 1);
    }

    private void reduceEnemyCooldowns(Entity enemy) {
        if (enemy.getCooldown1() > 0) enemy.setCooldown1(enemy.getCooldown1() - 1);
        if (enemy.getCooldown2() > 0) enemy.setCooldown2(enemy.getCooldown2() - 1);
        if (enemy.getCooldown3() > 0) enemy.setCooldown3(enemy.getCooldown3() - 1);
        if (enemy.getCooldownU() > 0) enemy.setCooldownU(enemy.getCooldownU() - 1);
    }

    // -------------------------------------------------------------------------
    // Run + potion in battle
    // -------------------------------------------------------------------------

    private boolean attemptRunAway(Entity enemy) {
        double baseChance = 30.0;
        double speedFactor = (hero.getSpeed() - enemy.getSpeed()) * 0.5;
        double successChance = Math.max(10.0, Math.min(90.0, baseChance + speedFactor));
        return random.nextDouble() * 100.0 <= successChance;
    }

    private void usePotionInBattle() {
        Inventory inventory = hero.getInventory();
        String[] options = {
                "Small HP (" + inventory.getSmallHealthPotion() + ")",
                "Medium HP (" + inventory.getMediumHealthPotion() + ")",
                "Large HP (" + inventory.getLargeHealthPotion() + ")",
                "Small Mana (" + inventory.getSmallManaPotion() + ")",
                "Medium Mana (" + inventory.getMediumManaPotion() + ")",
                "Large Mana (" + inventory.getLargeManaPotion() + ")",
                "Back"
        };

        int choice = showOptionSync("Inventory", "Choose a potion.", options, options[0]);
        if (choice == 6 || choice == -1) return;

        int hpBefore = hero.getHp();
        int manaBefore = hero.getMana();

        switch (choice) {
            case 0 -> { if (inventory.getSmallHealthPotion() > 0) inventory.useSmallHealthPotion(hero); else { showInfoSync("Inventory", "No Small Health Potion left."); return; } }
            case 1 -> { if (inventory.getMediumHealthPotion() > 0) inventory.useMediumHealthPotion(hero); else { showInfoSync("Inventory", "No Medium Health Potion left."); return; } }
            case 2 -> { if (inventory.getLargeHealthPotion() > 0) inventory.useLargeHealthPotion(hero); else { showInfoSync("Inventory", "No Large Health Potion left."); return; } }
            case 3 -> { if (inventory.getSmallManaPotion() > 0) inventory.useSmallManaPotion(hero); else { showInfoSync("Inventory", "No Small Mana Potion left."); return; } }
            case 4 -> { if (inventory.getMediumManaPotion() > 0) inventory.useMediumManaPotion(hero); else { showInfoSync("Inventory", "No Medium Mana Potion left."); return; } }
            case 5 -> { if (inventory.getLargeManaPotion() > 0) inventory.useLargeManaPotion(hero); else { showInfoSync("Inventory", "No Large Mana Potion left."); return; } }
            default -> { return; }
        }

        int hpGain = Math.max(0, hero.getHp() - hpBefore);
        int manaGain = Math.max(0, hero.getMana() - manaBefore);
        showInfoSync("Inventory", "Potion used.\nHP +" + statFormat.format(hpGain) + " | Mana +" + statFormat.format(manaGain));
        refreshInventoryPanel();
        refreshHeroDashboard();
    }

    private void restoreHeroBattleState(int hp, int mana, int cd1, int cd2, int cdU, int stun, int poison) {
        hero.setHp(hp);
        hero.setMana(mana);
        hero.setCooldown1(cd1);
        hero.setCooldown2(cd2);
        hero.setCooldownU(cdU);
        hero.setStun(stun);
        hero.setPoison(poison);
        refreshHeroDashboard();
        refreshInventoryPanel();
        refreshProfilePanel();
    }

    // -------------------------------------------------------------------------
    // Battle log helpers + outcome enum
    // -------------------------------------------------------------------------

    private String appendBattleLog(String current, String line) {
        return (current == null || current.isBlank()) ? line : current + "\n" + line;
    }

    private String truncateBattleLog(String log) {
        if (log == null) return "";
        String[] lines = log.split("\\R");
        int start = Math.max(0, lines.length - 6);
        StringBuilder builder = new StringBuilder();
        for (int i = start; i < lines.length; i++) builder.append(lines[i]).append("\n");
        return builder.toString().trim();
    }

    private enum BattleOutcome { WON, LOST, RAN }

    // -------------------------------------------------------------------------
    // Legacy console helpers
    // -------------------------------------------------------------------------

    private void launchLegacyAction(String title, Runnable action) {
        if (!requireHero()) return;
        appendLog("\n[" + title + "] opened from GUI buttons.\n");

        Thread worker = new Thread(() -> {
            try { action.run(); } catch (Exception e) { e.printStackTrace(); }
            finally {
                SwingUtilities.invokeLater(() -> {
                    refreshHeroDashboard();
                    refreshInventoryPanel();
                    refreshProfilePanel();
                });
            }
        }, "legacy-" + title.toLowerCase().replace(' ', '-'));

        worker.start();
    }

    private boolean requireHero() {
        if (hero != null) return true;
        showInfoSync("Action Required", "Choose a character first.");
        return false;
    }

    private void submitLegacyInput() {
        String raw = inputField.getText();
        if (raw == null) return;
        submitLegacyInputValue(raw.strip());
    }

    private void submitLegacyShortcut(String input) { submitLegacyInputValue(input); }

    private void submitLegacyInputValue(String input) {
        try {
            gameInputWriter.write((input + System.lineSeparator()).getBytes(StandardCharsets.UTF_8));
            gameInputWriter.flush();
            appendLog("\n> " + input + "\n");
            inputField.setText("");
            setQuickChoiceVisible(false);
        } catch (Exception e) {
            appendLog("\n[Input error] " + e.getMessage() + "\n");
        }
    }

    private void observeLegacyOutput(String text) {
        if (text == null || text.isEmpty()) return;
        if (!containsYesNoPrompt(text) && quickChoicePanel != null && quickChoicePanel.isVisible()) {
            setQuickChoiceVisible(false);
        }
    }

    private String filterLegacyOutput(String text) {
        if (text == null || text.isEmpty()) return text;

        pendingLegacyOutput.append(text);
        StringBuilder visibleOutput = new StringBuilder();

        while (pendingLegacyOutput.length() > 0) {
            MatchResult promptMatch = findPromptMatch(pendingLegacyOutput.toString());

            if (promptMatch == null) {
                int safeLength = Math.max(0, pendingLegacyOutput.length() - LEGACY_OUTPUT_TAIL_LIMIT);
                if (safeLength == 0) break;
                visibleOutput.append(pendingLegacyOutput, 0, safeLength);
                pendingLegacyOutput.delete(0, safeLength);
                continue;
            }

            if (promptMatch.start > 0) {
                visibleOutput.append(pendingLegacyOutput, 0, promptMatch.start);
                pendingLegacyOutput.delete(0, promptMatch.start);
            }

            String promptBlock = pendingLegacyOutput.substring(0, promptMatch.end - promptMatch.start);
            String promptText = extractPromptText(promptBlock);

            if (promptText != null && !promptText.isBlank()) {
                quickChoicePromptLabel.setText(promptText);
                setQuickChoiceVisible(true);
            }

            pendingLegacyOutput.delete(0, promptBlock.length());
        }

        if (pendingLegacyOutput.length() > 0 && !looksLikePromptPrefix(pendingLegacyOutput.toString())) {
            visibleOutput.append(pendingLegacyOutput);
            pendingLegacyOutput.setLength(0);
        }

        return visibleOutput.toString();
    }

    private void setQuickChoiceVisible(boolean visible) {
        if (quickChoicePanel == null || quickChoicePromptLabel == null) return;
        if (quickChoicePanel.isVisible() == visible && quickChoicePromptLabel.isVisible() == visible) return;
        quickChoicePromptLabel.setVisible(visible);
        quickChoicePanel.setVisible(visible);
        quickChoicePanel.revalidate();
        quickChoicePanel.repaint();
    }

    private MatchResult findPromptMatch(String text) {
        Matcher boxedMatcher = BOXED_YES_NO_PROMPT.matcher(text);
        while (boxedMatcher.find()) {
            if (containsYesNoPrompt(boxedMatcher.group())) {
                return new MatchResult(boxedMatcher.start(), boxedMatcher.end());
            }
        }
        Matcher inlineMatcher = INLINE_YES_NO_PROMPT.matcher(text);
        if (inlineMatcher.find() && containsYesNoPrompt(inlineMatcher.group())) {
            return new MatchResult(inlineMatcher.start(), inlineMatcher.end());
        }
        return null;
    }

    private boolean containsYesNoPrompt(String text) { return text != null && text.contains("(y/n)"); }

    private boolean looksLikePromptPrefix(String text) {
        return text.contains("┌") || text.contains("(y/n)") || text.endsWith(": ") || text.endsWith("-->| ");
    }

    private String extractPromptText(String promptBlock) {
        for (String line : promptBlock.split("\\R")) {
            if (!line.contains("(y/n)")) continue;
            String cleaned = line.replace("│", "").replace("(y/n)", "")
                    .replace(":", "").replace("-->|", "").trim();
            if (!cleaned.isEmpty()) return cleaned;
        }
        return "Choose:";
    }

    private static final class MatchResult {
        private final int start;
        private final int end;
        private MatchResult(int start, int end) { this.start = start; this.end = end; }
    }

    private void appendLog(String text) {
        outputArea.append(text);
        outputArea.setCaretPosition(outputArea.getDocument().getLength());
    }

    // -------------------------------------------------------------------------
    // Narration sequences
    // ------------------------------------------------------------------------- 

    // -------------------------------------------------------------------------
    // Narration sequences
    // ------------------------------------------------------------------------- 

    private void playNarrationSequence(String title, String[] lines) {
        if (lines == null || lines.length == 0) return;

        List<String> visibleLines = new ArrayList<>();
        for (String line : lines) {
            if (line != null && !line.isBlank()) {
                visibleLines.add(line.trim());
            }
        }

        for (int i = 0; i < visibleLines.size(); i++) {
            boolean fadeIn = i == 0;
            boolean fadeOut = i == visibleLines.size() - 1;
            showNarrationSync(title, visibleLines.get(i), fadeIn, fadeOut);
        }
    }

    // -------------------------------------------------------------------------
    // Screen navigation
    // -------------------------------------------------------------------------

    private void showLandingScreen() {
        titleLabel.setText("Mystvale Academy");
        subtitleLabel.setText("GUI RPG");
        rootLayout.show(rootPanel, ROOT_LANDING);
    }

    private void openCharacterSelectFromLanding() {
        titleLabel.setText("Mystvale Academy");
        subtitleLabel.setText("Choose a hero to begin.");
        rootLayout.show(rootPanel, ROOT_GAME);
        showScreen(SCREEN_CHARACTER);
    }

    private void showScreen(String screenName) {
        if (SCREEN_MAIN.equals(screenName)) {
            playAdventureOverviewMusic();
        }
        screenLayout.show(screenPanel, screenName);

        boolean isBattleScreen = SCREEN_BATTLE.equals(screenName);
        boolean isShopScreen = SCREEN_SHOP.equals(screenName);
        boolean isStoryScreen = SCREEN_STORY.equals(screenName);
        Color normalScreenBackground = COLOR_HEADER_BACKGROUND;
        if (isStoryScreen) {
            storyScenePreviewPanel.startAnimation();
        } else {
            storyScenePreviewPanel.stopAnimation();
        }
        if (headerPanel != null) headerPanel.setVisible(!isBattleScreen);
        if (gameShellPanel != null) {
            gameShellPanel.setBackground(isShopScreen ? COLOR_SHOP_OUTSIDE : normalScreenBackground);
        }
        if (headerExitButton != null) {
            boolean hideExit = SCREEN_CHARACTER.equals(screenName)
                    || SCREEN_STORY.equals(screenName)
                    || SCREEN_SHOP.equals(screenName);
            headerExitButton.setVisible(!hideExit);
            headerExitButton.setForeground(isShopScreen ? COLOR_SHOP_TEXT : Color.WHITE);
            headerExitButton.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        }
        if (headerSaveButton != null) {
            headerSaveButton.setVisible(SCREEN_MAIN.equals(screenName));
            headerSaveButton.setForeground(isShopScreen ? COLOR_SHOP_TEXT : Color.WHITE);
            headerSaveButton.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        }
        if (headerBackButton != null) {
            headerBackButton.setVisible(isShopScreen);
            headerBackButton.setForeground(isShopScreen ? COLOR_SHOP_TEXT : Color.WHITE);
            headerBackButton.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        }
        if (headerPanel != null) {
            headerPanel.setBackground(isShopScreen ? COLOR_SHOP_OUTSIDE : COLOR_HEADER_BACKGROUND);
            headerPanel.setOpaque(!isShopScreen);
            headerPanel.setBorder(isShopScreen
                    ? BorderFactory.createEmptyBorder(18, 22, 14, 22)
                    : defaultHeaderBorder);
        }
        titleLabel.setForeground(isShopScreen ? COLOR_SHOP_TEXT : Color.WHITE);
        subtitleLabel.setForeground(isShopScreen ? Color.WHITE : new Color(210, 224, 255));
        if (leftPane != null) {
            leftPane.setBackground(isBattleScreen ? COLOR_BATTLE_PANEL : isShopScreen ? COLOR_SHOP_OUTSIDE : normalScreenBackground);
            leftPane.setOpaque(!isShopScreen);
            leftPane.setBorder(isBattleScreen ? BorderFactory.createEmptyBorder() : defaultLeftPaneBorder);
        }
        if (screenPanel != null) {
            screenPanel.setOpaque(isBattleScreen);
            screenPanel.setBackground(isBattleScreen ? COLOR_BATTLE_PANEL : isShopScreen ? COLOR_SHOP_OUTSIDE : normalScreenBackground);
        }
    }

    private boolean isShopHeaderActive() {
        return headerPanel != null && COLOR_SHOP_OUTSIDE.equals(headerPanel.getBackground());
    }

    private void playAdventureOverviewMusic() {
        SoundManager.stopMusic();
        SoundManager.playMusic("src/com/ror/models/assets/sounds/titleScreen.wav");
    }

    // -------------------------------------------------------------------------
    // Sync dialog helpers
    // -------------------------------------------------------------------------

    private void runOnEdtSync(Runnable task) {
        if (SwingUtilities.isEventDispatchThread()) { task.run(); return; }
        try { SwingUtilities.invokeAndWait(task); } catch (Exception ignored) {}
    }

    private void showInfoSync(String title, String message) {
        runOnEdtSync(() -> { if (overlay != null) overlay.showMessage(title, message); });
    }

    private void showNarrationSync(String title, String message) {
        runOnEdtSync(() -> { if (overlay != null) overlay.showMessage(title, message); });
    }

    private void showNarrationSync(String title, String message, boolean fadeIn, boolean fadeOut) {
        runOnEdtSync(() -> {
            if (overlay != null) {
                overlay.showMessageSequence(title, message, fadeIn, fadeOut);
            }
        });
    }

    private void showWarningSync(String title, String message) {
        runOnEdtSync(() -> { if (overlay != null) overlay.showMessage(title, message); });
    }

    private int showConfirmSync(String title, String message) {
        final int[] choice = { 1 };
        runOnEdtSync(() -> { if (overlay != null) choice[0] = overlay.showConfirm(title, message); });
        return choice[0];
    }

    private int showOptionSync(String title, String message, Object[] options, Object initial) {
        final int[] choice = { -1 };
        runOnEdtSync(() -> { if (overlay != null) choice[0] = overlay.showOptions(title, message, options, initial); });
        return choice[0];
    }

    private int showSlotChooserSync(String title, String prompt, boolean allowEmptySlots) {
        final int[] choice = { -1 };
        runOnEdtSync(() -> {
            if (overlay != null) {
                choice[0] = overlay.showSlotChooser(title, prompt, Load.getSlotInfos(), allowEmptySlots);
            }
        });
        return choice[0];
    }

    // -------------------------------------------------------------------------
    // BattlePanel listener
    // -------------------------------------------------------------------------

    @Override
    public void onBattleAction(int action) { submitBattleAction(action); }

    @Override
    public void onShowAttackPanel() { battlePanel.showAttackPanel(); }
}
