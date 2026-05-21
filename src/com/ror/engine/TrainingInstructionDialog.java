package com.ror.engine;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.Random;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.Timer;

final class TrainingInstructionDialog extends JDialog {
    private TrainingInstructionDialog(Window owner, Font headingFont, Font bodyFont, Theme theme, String title,
            String subtitle, String[] lines) {
        super(owner, title, ModalityType.APPLICATION_MODAL);
        setUndecorated(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setBackground(new Color(0, 0, 0, 0));

        InstructionPanel panel = new InstructionPanel(
                headingFont != null ? headingFont : new Font("Serif", Font.BOLD, 30),
                bodyFont != null ? bodyFont : new Font("SansSerif", Font.PLAIN, 16),
                theme, title, subtitle, lines, this::dispose);
        setContentPane(panel);
        pack();
        setMinimumSize(new Dimension(920, 560));
        setSize(new Dimension(920, 560));
        setLocationRelativeTo(owner);
    }

    static void showDialog(Window owner, Font headingFont, Font bodyFont, String taskName, String[] lines) {
        Theme theme = Theme.forTask(taskName);
        String title = taskName + " Trial";
        String subtitle = theme.subtitle;
        TrainingInstructionDialog dialog = new TrainingInstructionDialog(owner, headingFont, bodyFont, theme, title,
                subtitle, lines);
        dialog.setVisible(true);
    }

    private interface CloseHandler {
        void close();
    }

    private enum Theme {
        ENDURANCE(
                new Color(27, 35, 49, 246),
                new Color(12, 18, 28, 246),
                new Color(126, 144, 166, 185),
                new Color(153, 233, 197),
                new Color(153, 233, 197, 70),
                "Hold steady and endure the strain."),
        STRENGTH(
                new Color(28, 33, 52, 248),
                new Color(14, 18, 31, 248),
                new Color(109, 120, 156, 190),
                new Color(118, 236, 210),
                new Color(118, 236, 210, 70),
                "Strike with timing and control."),
        DURABILITY(
                new Color(25, 33, 51, 246),
                new Color(10, 15, 27, 246),
                new Color(124, 145, 184, 186),
                new Color(132, 224, 255),
                new Color(132, 224, 255, 72),
                "Brace only when the impact truly lands."),
        MANA(
                new Color(20, 28, 58, 246),
                new Color(9, 13, 28, 246),
                new Color(114, 146, 214, 185),
                new Color(105, 223, 255),
                new Color(105, 223, 255, 72),
                "Keep the flow stable without overloading.");

        private final Color panelTop;
        private final Color panelBottom;
        private final Color panelBorder;
        private final Color accent;
        private final Color accentLine;
        private final String subtitle;

        Theme(Color panelTop, Color panelBottom, Color panelBorder, Color accent, Color accentLine, String subtitle) {
            this.panelTop = panelTop;
            this.panelBottom = panelBottom;
            this.panelBorder = panelBorder;
            this.accent = accent;
            this.accentLine = accentLine;
            this.subtitle = subtitle;
        }

        private static Theme forTask(String taskName) {
            return switch (taskName) {
                case "Endurance" -> ENDURANCE;
                case "Strength" -> STRENGTH;
                case "Durability" -> DURABILITY;
                case "Mana Refinement" -> MANA;
                default -> STRENGTH;
            };
        }
    }

    private static final class InstructionPanel extends JPanel {
        private static final Color TITLE = new Color(236, 243, 255);
        private static final Color BODY = new Color(194, 208, 236);
        private static final Color BODY_SOFT = new Color(255, 255, 255);
        private static final Color BUTTON_BG = new Color(34, 39, 58);
        private static final Color BUTTON_BG_HOVER = new Color(49, 56, 80);
        private static final Color PARTICLE = new Color(255, 255, 255, 34);
        private static final int PARTICLE_COUNT = 20;

        private final Font headingFont;
        private final Font bodyFont;
        private final Theme theme;
        private final String title;
        private final String subtitle;
        private final String[] lines;
        private final CloseHandler closeHandler;
        private final Timer animationTimer;
        private final Particle[] particles = new Particle[PARTICLE_COUNT];
        private final Random random = new Random();

        private float bannerAlpha;
        private float pulseClock;
        private float buttonPulse;
        private float textRevealClock;
        private long lastUpdateNanos;
        private int currentStep;
        private int visibleChars;
        private boolean hoverPrimary;

        private InstructionPanel(Font headingFont, Font bodyFont, Theme theme, String title, String subtitle,
                String[] lines, CloseHandler closeHandler) {
            this.headingFont = headingFont;
            this.bodyFont = bodyFont;
            this.theme = theme;
            this.title = title;
            this.subtitle = subtitle;
            this.lines = lines;
            this.closeHandler = closeHandler;
            setOpaque(false);
            setLayout(new BorderLayout());
            setPreferredSize(new Dimension(920, 560));
            initializeParticles();
            bindControls();
            resetStepReveal();
            lastUpdateNanos = System.nanoTime();
            animationTimer = new Timer(16, this::tick);
            animationTimer.start();
        }

        private void initializeParticles() {
            for (int i = 0; i < particles.length; i++) {
                particles[i] = new Particle(random.nextFloat(), random.nextFloat(),
                        0.25f + random.nextFloat() * 0.75f, 0.8f + random.nextFloat() * 1.3f);
            }
        }

        private void bindControls() {
            InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
            ActionMap actionMap = getActionMap();
            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "next");
            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "next");
            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
            actionMap.put("next", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    advanceStep();
                }
            });
            actionMap.put("close", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    animationTimer.stop();
                    closeHandler.close();
                }
            });
        }

        @Override
        public void addNotify() {
            super.addNotify();
            requestFocusInWindow();
        }

        private void tick(ActionEvent event) {
            long now = System.nanoTime();
            float delta = Math.min(0.033f, (now - lastUpdateNanos) / 1_000_000_000f);
            lastUpdateNanos = now;
            bannerAlpha = Math.min(1f, bannerAlpha + delta * 2.6f);
            pulseClock += delta * 1.9f;
            buttonPulse += delta * 3f;
            textRevealClock += delta * 42f;
            int targetChars = Math.min(lines[currentStep].length(), Math.max(0, (int) textRevealClock));
            if (targetChars != visibleChars) {
                visibleChars = targetChars;
            }
            for (Particle particle : particles) {
                particle.y += particle.speed * delta * 0.04f;
                particle.x += Math.sin((pulseClock + particle.phase) * 1.15f) * delta * 0.012f;
                if (particle.y > 1.05f) {
                    particle.y = -0.04f;
                    particle.x = random.nextFloat();
                }
            }
            repaint();
        }

        @Override
        protected void processMouseEvent(java.awt.event.MouseEvent event) {
            super.processMouseEvent(event);
            if (event.getID() == java.awt.event.MouseEvent.MOUSE_CLICKED) {
                if (isInsidePrimaryButton(event.getX(), event.getY())) {
                    advanceStep();
                }
            }
        }

        @Override
        protected void processMouseMotionEvent(java.awt.event.MouseEvent event) {
            super.processMouseMotionEvent(event);
            hoverPrimary = isInsidePrimaryButton(event.getX(), event.getY());
            repaint();
        }

        private void advanceStep() {
            if (currentStep >= lines.length - 1) {
                animationTimer.stop();
                closeHandler.close();
                return;
            }
            currentStep++;
            resetStepReveal();
            repaint();
        }

        private void resetStepReveal() {
            textRevealClock = 0f;
            visibleChars = 0;
        }

        private boolean isInsidePrimaryButton(int mouseX, int mouseY) {
            int panelWidth = Math.min(760, getWidth() - 80);
            int panelHeight = 194;
            int panelX = (getWidth() - panelWidth) / 2;
            int panelY = (getHeight() - panelHeight) / 2;
            int buttonWidth = 128;
            int buttonHeight = 42;
            int buttonX = panelX + panelWidth / 2 - buttonWidth / 2;
            int buttonY = panelY + panelHeight - 54;
            return mouseX >= buttonX && mouseX <= buttonX + buttonWidth
                    && mouseY >= buttonY && mouseY <= buttonY + buttonHeight;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            int panelWidth = Math.min(760, getWidth() - 80);
            int panelHeight = 194;
            int panelX = (getWidth() - panelWidth) / 2;
            int panelY = (getHeight() - panelHeight) / 2;

            paintParticles(g2, panelX, panelY, panelWidth, panelHeight);
            paintInstructionCard(g2, panelX, panelY, panelWidth, panelHeight);
            paintButtons(g2, panelX, panelY, panelWidth, panelHeight);
            g2.dispose();
        }

        private void paintParticles(Graphics2D g2, int x, int y, int width, int height) {
            g2.setClip(x + 12, y + 12, width - 24, height - 24);
            for (Particle particle : particles) {
                int px = x + Math.round(particle.x * width);
                int py = y + Math.round(particle.y * height);
                int size = Math.max(2, Math.round(particle.size));
                g2.setColor(new Color(theme.accent.getRed(), theme.accent.getGreen(), theme.accent.getBlue(),
                        26 + (int) (particle.size * 28)));
                g2.fillOval(px, py, size, size);
            }
            g2.setClip(null);
        }

        private void paintInstructionCard(Graphics2D g2, int x, int y, int width, int height) {
            int cardX = x;
            int cardY = y;
            int cardWidth = width;
            int cardHeight = height;

            g2.setColor(new Color(9, 14, 28, 220));
            g2.fillRoundRect(cardX, cardY, cardWidth, cardHeight, 28, 28);
            g2.setColor(new Color(156, 170, 201, 120));
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(cardX, cardY, cardWidth, cardHeight, 28, 28);

            g2.setColor(new Color(theme.accent.getRed(), theme.accent.getGreen(), theme.accent.getBlue(), 42));
            g2.fillRoundRect(cardX + 18, cardY + 18, cardWidth - 36, 42, 20, 20);

            g2.setFont(bodyFont.deriveFont(Font.BOLD, 15f));
            drawCentered(g2, "How It Works", x + width / 2, cardY + 44, theme.accent);

            g2.setFont(bodyFont.deriveFont(Font.PLAIN, 16f));
            String visibleText = lines[currentStep].substring(0, Math.min(visibleChars, lines[currentStep].length()));
            drawWrappedInstruction(g2, visibleText, cardX + 58, cardY + 96, cardWidth - 116, 24);

            g2.setFont(bodyFont.deriveFont(Font.BOLD, 14f));
            drawCentered(g2, "Step " + (currentStep + 1) + " of " + lines.length, x + width / 2,
                    cardY + cardHeight - 80, BODY_SOFT);
        }

        private void drawWrappedInstruction(Graphics2D g2, String text, int x, int y, int width, int lineHeight) {
            String[] words = text.split(" ");
            StringBuilder line = new StringBuilder();
            int drawY = y;
            for (String word : words) {
                String candidate = line.isEmpty() ? word : line + " " + word;
                if (g2.getFontMetrics().stringWidth(candidate) > width && !line.isEmpty()) {
                    drawCentered(g2, line.toString(), x + width / 2, drawY, BODY);
                    line.setLength(0);
                    line.append(word);
                    drawY += lineHeight;
                } else {
                    line.setLength(0);
                    line.append(candidate);
                }
            }
            if (!line.isEmpty()) {
                drawCentered(g2, line.toString(), x + width / 2, drawY, BODY);
            }
        }

        private void paintButtons(Graphics2D g2, int x, int y, int width, int height) {
            paintPrimaryButton(g2, x, y, width, height);
        }

        private void paintPrimaryButton(Graphics2D g2, int x, int y, int width, int height) {
            int buttonWidth = 128;
            int buttonHeight = 42;
            int buttonX = x + width / 2 - buttonWidth / 2;
            int buttonY = y + height - 54;
            float glow = 0.45f + 0.55f * (float) Math.sin(buttonPulse);

            g2.setColor(new Color(theme.accent.getRed(), theme.accent.getGreen(), theme.accent.getBlue(),
                    26 + Math.round(36 * glow)));
            g2.fillRoundRect(buttonX - 8, buttonY - 6, buttonWidth + 16, buttonHeight + 12, 22, 22);
            g2.setColor(hoverPrimary ? BUTTON_BG_HOVER : BUTTON_BG);
            g2.fillRoundRect(buttonX, buttonY, buttonWidth, buttonHeight, 18, 18);
            g2.setColor(new Color(theme.accent.getRed(), theme.accent.getGreen(), theme.accent.getBlue(),
                    hoverPrimary ? 196 : 150));
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(buttonX, buttonY, buttonWidth, buttonHeight, 18, 18);

            g2.setFont(bodyFont.deriveFont(Font.BOLD, 15f));
            String label = currentStep >= lines.length - 1 ? "Begin" : "Next";
            drawCentered(g2, label, buttonX + buttonWidth / 2, buttonY + 27, TITLE);
        }

        private void drawCentered(Graphics2D g2, String text, int centerX, int baselineY, Color color) {
            int width = g2.getFontMetrics().stringWidth(text);
            g2.setColor(new Color(0, 0, 0, 120));
            g2.drawString(text, centerX - width / 2 + 2, baselineY + 2);
            g2.setColor(color);
            g2.drawString(text, centerX - width / 2, baselineY);
        }

        {
            enableEvents(java.awt.AWTEvent.MOUSE_EVENT_MASK | java.awt.AWTEvent.MOUSE_MOTION_EVENT_MASK);
        }

        private static final class Particle {
            private float x;
            private float y;
            private final float size;
            private final float speed;
            private final float phase;

            private Particle(float x, float y, float size, float speed) {
                this.x = x;
                this.y = y;
                this.size = size;
                this.speed = speed;
                this.phase = x * 6.28318f;
            }
        }
    }
}
