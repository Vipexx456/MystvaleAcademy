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

final class ManaRefinementDialog extends JDialog {
    private boolean success;

    private ManaRefinementDialog(Window owner, Font headingFont, Font bodyFont) {
        super(owner, "Mana Refinement", ModalityType.APPLICATION_MODAL);
        setUndecorated(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setBackground(new Color(0, 0, 0, 0));

        ManaRefinementPanel panel = new ManaRefinementPanel(
                headingFont != null ? headingFont : new Font("Serif", Font.BOLD, 30),
                bodyFont != null ? bodyFont : new Font("SansSerif", Font.PLAIN, 16),
                result -> {
                    success = result;
                    dispose();
                });
        setContentPane(panel);
        pack();
        setMinimumSize(new Dimension(920, 560));
        setSize(new Dimension(920, 560));
        setLocationRelativeTo(owner);
    }

    static boolean showDialog(Window owner, Font headingFont, Font bodyFont) {
        ManaRefinementDialog dialog = new ManaRefinementDialog(owner, headingFont, bodyFont);
        dialog.setVisible(true);
        return dialog.success;
    }

    private interface ResultHandler {
        void complete(boolean success);
    }

    private static final class ManaRefinementPanel extends JPanel {
        private static final Color PANEL_TOP = new Color(20, 28, 58, 246);
        private static final Color PANEL_BOTTOM = new Color(9, 13, 28, 246);
        private static final Color PANEL_BORDER = new Color(114, 146, 214, 185);
        private static final Color TITLE = new Color(235, 243, 255);
        private static final Color BODY = new Color(192, 208, 240);
        private static final Color ACCENT = new Color(105, 223, 255);
        private static final Color ACCENT_SOFT = new Color(105, 223, 255, 90);
        private static final Color FAILURE = new Color(214, 104, 104);
        private static final Color BAR_TRACK = new Color(9, 14, 28, 225);
        private static final Color BAR_FILL = new Color(88, 207, 255, 200);
        private static final Color BAR_FRAME = new Color(156, 170, 201, 150);
        private static final Color SAFE_ZONE = new Color(121, 241, 215, 84);
        private static final Color SAFE_ZONE_BORDER = new Color(162, 252, 231, 190);
        private static final int TARGET_BREAKS = 3;
        private static final int PARTICLE_COUNT = 24;
        private static final float SUCCESS_SECONDS = 6.5f;

        private final Font headingFont;
        private final Font bodyFont;
        private final ResultHandler resultHandler;
        private final Random random = new Random();
        private final Timer animationTimer;
        private final Particle[] particles = new Particle[PARTICLE_COUNT];

        private float manaLevel = 0.45f;
        private float safeZoneCenter = 0.52f;
        private float safeZoneHeight = 0.22f;
        private float heldSeconds;
        private float pulseClock;
        private float bannerAlpha;
        private float flashStrength;
        private float outcomeAlpha;
        private long lastUpdateNanos;
        private long outcomeShownAt;
        private int breaks;
        private boolean finished;
        private boolean result;
        private String statusText = "Tap SPACE or click to feed the rune. Keep the mana inside the safe zone.";
        private Color statusColor = BODY;

        private ManaRefinementPanel(Font headingFont, Font bodyFont, ResultHandler resultHandler) {
            this.headingFont = headingFont;
            this.bodyFont = bodyFont;
            this.resultHandler = resultHandler;
            setOpaque(false);
            setLayout(new BorderLayout());
            setPreferredSize(new Dimension(920, 560));
            initializeParticles();
            bindControls();
            lastUpdateNanos = System.nanoTime();
            animationTimer = new Timer(16, this::tick);
            animationTimer.start();
        }

        private void initializeParticles() {
            for (int i = 0; i < particles.length; i++) {
                particles[i] = new Particle(random.nextFloat(), random.nextFloat(),
                        0.2f + random.nextFloat() * 0.8f, 0.8f + random.nextFloat() * 1.6f);
            }
        }

        private void bindControls() {
            InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
            ActionMap actionMap = getActionMap();
            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "mana-feed");
            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "mana-feed");
            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "mana-cancel");

            actionMap.put("mana-feed", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    feedMana();
                }
            });
            actionMap.put("mana-cancel", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    finish(false, "Training cancelled.");
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

            bannerAlpha = Math.min(1f, bannerAlpha + delta * 2.5f);
            pulseClock += delta * 2.2f;
            flashStrength = Math.max(0f, flashStrength - delta * 2.3f);
            updateParticles(delta);

            if (!finished) {
                updateMana(delta);
                updateSafeZone(delta);
                checkState();
            } else {
                outcomeAlpha = Math.min(1f, outcomeAlpha + delta * 2.6f);
                if (System.currentTimeMillis() - outcomeShownAt >= 1050L) {
                    animationTimer.stop();
                    resultHandler.complete(result);
                    return;
                }
            }

            repaint();
        }

        private void updateParticles(float delta) {
            for (Particle particle : particles) {
                particle.y += particle.speed * delta * 0.04f;
                particle.x += Math.sin((pulseClock + particle.phase) * 1.2f) * delta * 0.014f;
                if (particle.y > 1.05f) {
                    particle.y = -0.04f;
                    particle.x = random.nextFloat();
                }
            }
        }

        private void updateMana(float delta) {
            float drain = 0.18f + 0.04f * (float) Math.sin(pulseClock * 0.9f);
            manaLevel -= drain * delta;
            manaLevel = clamp01(manaLevel);
        }

        private void updateSafeZone(float delta) {
            safeZoneCenter = 0.5f + (float) Math.sin(pulseClock * 0.55f) * 0.1f;
            safeZoneHeight = 0.2f + 0.03f * (float) Math.sin(pulseClock * 0.8f + 0.6f);
        }

        private void checkState() {
            float low = safeZoneCenter - safeZoneHeight * 0.5f;
            float high = safeZoneCenter + safeZoneHeight * 0.5f;
            if (manaLevel >= low && manaLevel <= high) {
                heldSeconds += 0.016f;
                statusText = "Steady. Keep the flow balanced.";
                statusColor = ACCENT;
                if (heldSeconds >= SUCCESS_SECONDS) {
                    finish(true, "Mana refinement complete!");
                }
            } else {
                heldSeconds = Math.max(0f, heldSeconds - 0.03f);
            }

            if (manaLevel <= 0.02f || manaLevel >= 0.98f) {
                breaks++;
                flashStrength = 1f;
                manaLevel = 0.48f;
                statusText = manaLevel <= 0.02f
                        ? "The rune starved. Feed it more carefully."
                        : "The rune overloaded. Ease the flow.";
                statusColor = FAILURE;
                if (breaks >= TARGET_BREAKS) {
                    finish(false, "Mana refinement failed.");
                }
            }
        }

        private void feedMana() {
            if (finished) return;
            manaLevel = Math.min(1f, manaLevel + 0.12f);
            flashStrength = Math.min(1f, flashStrength + 0.35f);
        }

        private void finish(boolean success, String message) {
            finished = true;
            result = success;
            statusText = message;
            statusColor = success ? ACCENT : FAILURE;
            outcomeShownAt = System.currentTimeMillis();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            int panelWidth = Math.min(760, getWidth() - 80);
            int panelHeight = Math.min(420, getHeight() - 80);
            int panelX = (getWidth() - panelWidth) / 2;
            int panelY = (getHeight() - panelHeight) / 2;

            paintFrame(g2, panelX, panelY, panelWidth, panelHeight);
            paintParticles(g2, panelX, panelY, panelWidth, panelHeight);
            paintHeader(g2, panelX, panelY, panelWidth);
            paintProgress(g2, panelX, panelY, panelWidth);
            paintChamber(g2, panelX, panelY, panelWidth, panelHeight);
            paintFooter(g2, panelX, panelY, panelWidth, panelHeight);
            if (finished) {
                paintOutcome(g2, panelX, panelY, panelWidth, panelHeight);
            }

            g2.dispose();
        }

        private void paintFrame(Graphics2D g2, int x, int y, int width, int height) {
            RoundRectangle2D.Float frame = new RoundRectangle2D.Float(x, y, width, height, 30, 30);
            GradientPaint paint = new GradientPaint(x, y, PANEL_TOP, x, y + height, PANEL_BOTTOM);
            g2.setPaint(paint);
            g2.fill(frame);
            g2.setColor(new Color(255, 255, 255, 16));
            g2.fillRoundRect(x + 12, y + 12, width - 24, 66, 24, 24);
            g2.setStroke(new BasicStroke(2.1f));
            g2.setColor(PANEL_BORDER);
            g2.draw(frame);
            g2.setStroke(new BasicStroke(1f));
            g2.setColor(new Color(105, 223, 255, 74));
            g2.drawRoundRect(x + 10, y + 10, width - 20, height - 20, 24, 24);
        }

        private void paintParticles(Graphics2D g2, int x, int y, int width, int height) {
            g2.setClip(x + 12, y + 12, width - 24, height - 24);
            for (Particle particle : particles) {
                int px = x + Math.round(particle.x * width);
                int py = y + Math.round(particle.y * height);
                int size = Math.max(2, Math.round(particle.size));
                g2.setColor(new Color(105, 223, 255, 40 + (int) (particle.size * 40)));
                g2.fillOval(px, py, size, size);
            }
            g2.setClip(null);
        }

        private void paintHeader(Graphics2D g2, int x, int y, int width) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, bannerAlpha));
            g2.setFont(headingFont.deriveFont(Font.BOLD, 32f));
            drawCentered(g2, "Mana Refinement", x + width / 2, y + 62, TITLE);
            g2.setFont(bodyFont.deriveFont(Font.PLAIN, 16f));
            drawCentered(g2, "Tap SPACE or click to keep the mana flow inside the safe zone.",
                    x + width / 2, y + 98, BODY);
            g2.setComposite(AlphaComposite.SrcOver);
        }

        private void paintProgress(Graphics2D g2, int x, int y, int width) {
            g2.setFont(bodyFont.deriveFont(Font.BOLD, 14f));
            g2.setColor(BODY);
            g2.drawString("Stability", x + 166, y + 146);
            g2.drawString("Breaks", x + width - 292, y + 146);

            int stabilityBarX = x + 238;
            int stabilityBarY = y + 130;
            int stabilityBarWidth = 170;
            g2.setColor(new Color(255, 255, 255, 22));
            g2.fillRoundRect(stabilityBarX, stabilityBarY, stabilityBarWidth, 16, 12, 12);
            g2.setColor(ACCENT_SOFT);
            g2.fillRoundRect(stabilityBarX, stabilityBarY,
                    Math.max(6, Math.round(stabilityBarWidth * (heldSeconds / SUCCESS_SECONDS))), 16, 12, 12);
            g2.setColor(new Color(156, 170, 201, 110));
            g2.drawRoundRect(stabilityBarX, stabilityBarY, stabilityBarWidth, 16, 12, 12);

            int breakX = x + width - 248;
            for (int i = 0; i < TARGET_BREAKS; i++) {
                int orbX = breakX + i * 34;
                boolean marked = i < breaks;
                g2.setColor(marked ? new Color(214, 104, 104, 80) : new Color(255, 255, 255, 18));
                g2.fillOval(orbX, y + 128, 18, 18);
                g2.setColor(marked ? FAILURE : new Color(156, 170, 201, 90));
                g2.drawOval(orbX, y + 128, 18, 18);
            }
        }

        private void paintChamber(Graphics2D g2, int x, int y, int width, int height) {
            int chamberX = x + width / 2 - 160;
            int chamberY = y + 176;
            int chamberWidth = 320;
            int chamberHeight = 140;

            g2.setColor(new Color(8, 14, 31, 220));
            g2.fillRoundRect(chamberX, chamberY, chamberWidth, chamberHeight, 28, 28);
            g2.setColor(BAR_FRAME);
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(chamberX, chamberY, chamberWidth, chamberHeight, 28, 28);

            int pad = 18;
            int innerX = chamberX + pad;
            int innerY = chamberY + pad;
            int innerWidth = chamberWidth - pad * 2;
            int innerHeight = chamberHeight - pad * 2;

            int safeHeightPx = Math.round(innerHeight * safeZoneHeight);
            int safeY = innerY + Math.round(innerHeight * (1f - safeZoneCenter)) - safeHeightPx / 2;
            float glow = 0.55f + 0.45f * (float) Math.sin(pulseClock * 2.1f);
            g2.setColor(new Color(121, 241, 215, 40 + Math.round(32 * glow)));
            g2.fillRoundRect(innerX - 8, safeY - 6, innerWidth + 16, safeHeightPx + 12, 22, 22);
            g2.setColor(SAFE_ZONE);
            g2.fillRoundRect(innerX, safeY, innerWidth, safeHeightPx, 18, 18);
            g2.setColor(SAFE_ZONE_BORDER);
            g2.drawRoundRect(innerX, safeY, innerWidth, safeHeightPx, 18, 18);

            int manaHeightPx = Math.max(8, Math.round(innerHeight * manaLevel));
            int manaY = innerY + innerHeight - manaHeightPx;
            g2.setColor(new Color(105, 223, 255, 50 + Math.round(70 * flashStrength)));
            g2.fillRoundRect(innerX - 6, manaY - 6, innerWidth + 12, manaHeightPx + 12, 18, 18);
            g2.setColor(BAR_FILL);
            g2.fillRoundRect(innerX, manaY, innerWidth, manaHeightPx, 16, 16);

            g2.setColor(new Color(255, 255, 255, 26));
            for (int i = 1; i < 5; i++) {
                int lineY = innerY + (innerHeight * i / 5);
                g2.drawLine(innerX + 8, lineY, innerX + innerWidth - 8, lineY);
            }
        }

        private void paintFooter(Graphics2D g2, int x, int y, int width, int height) {
            g2.setFont(bodyFont.deriveFont(Font.BOLD, 17f));
            drawCentered(g2, statusText, x + width / 2, y + height - 108, statusColor);
            g2.setFont(bodyFont.deriveFont(Font.PLAIN, 14f));
            drawCentered(g2, "Build the flow, but do not starve or overload the rune.", x + width / 2, y + height - 68, BODY);
        }

        private void paintOutcome(Graphics2D g2, int x, int y, int width, int height) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(0.86f, outcomeAlpha * 0.86f)));
            g2.setColor(new Color(5, 8, 15, 210));
            g2.fillRoundRect(x + 40, y + 116, width - 80, height - 164, 26, 26);
            String message = result ? "Mana Stabilized" : "Refinement Failed";
            Color tone = result ? ACCENT : FAILURE;
            g2.setFont(headingFont.deriveFont(Font.BOLD, 34f));
            drawCentered(g2, message, x + width / 2, y + height / 2 - 10, tone);
            g2.setFont(bodyFont.deriveFont(Font.PLAIN, 16f));
            drawCentered(g2, result ? "The rune hums in perfect balance." : "The flow slipped before it settled.",
                    x + width / 2, y + height / 2 + 26, BODY);
            g2.setComposite(AlphaComposite.SrcOver);
        }

        private void drawCentered(Graphics2D g2, String text, int centerX, int baselineY, Color color) {
            int width = g2.getFontMetrics().stringWidth(text);
            g2.setColor(new Color(0, 0, 0, 120));
            g2.drawString(text, centerX - width / 2 + 2, baselineY + 2);
            g2.setColor(color);
            g2.drawString(text, centerX - width / 2, baselineY);
        }

        private float clamp01(float value) {
            return Math.max(0f, Math.min(1f, value));
        }

        @Override
        protected void processMouseEvent(java.awt.event.MouseEvent event) {
            super.processMouseEvent(event);
            if (event.getID() == java.awt.event.MouseEvent.MOUSE_PRESSED) {
                feedMana();
            }
        }

        {
            enableEvents(java.awt.AWTEvent.MOUSE_EVENT_MASK);
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
