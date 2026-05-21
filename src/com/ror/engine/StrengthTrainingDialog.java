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
import java.awt.Toolkit;
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

final class StrengthTrainingDialog extends JDialog {
    private boolean success;

    private StrengthTrainingDialog(Window owner, Font headingFont, Font bodyFont) {
        super(owner, "Strength Training", ModalityType.APPLICATION_MODAL);
        setUndecorated(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setBackground(new Color(0, 0, 0, 0));

        StrengthTrainingPanel panel = new StrengthTrainingPanel(
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
        StrengthTrainingDialog dialog = new StrengthTrainingDialog(owner, headingFont, bodyFont);
        dialog.setVisible(true);
        return dialog.success;
    }

    private interface ResultHandler {
        void complete(boolean success);
    }

    private static final class StrengthTrainingPanel extends JPanel {
        private static final Color PANEL_TOP = new Color(28, 33, 52, 248);
        private static final Color PANEL_BOTTOM = new Color(14, 18, 31, 248);
        private static final Color PANEL_BORDER = new Color(109, 120, 156, 190);
        private static final Color TITLE = new Color(232, 241, 255);
        private static final Color BODY = new Color(195, 209, 234);
        private static final Color ACCENT = new Color(118, 236, 210);
        private static final Color ACCENT_SOFT = new Color(118, 236, 210, 78);
        private static final Color FAILURE = new Color(214, 104, 104);
        private static final Color BAR_TRACK = new Color(10, 14, 24, 220);
        private static final Color BAR_FRAME = new Color(156, 170, 201, 150);
        private static final Color ZONE = new Color(118, 236, 210, 120);
        private static final Color ZONE_OUTLINE = new Color(166, 249, 228, 210);
        private static final Color MARKER_CORE = new Color(241, 248, 255);
        private static final Color MARKER_GLOW = new Color(118, 236, 210, 170);
        private static final int TARGET_SUCCESSES = 3;
        private static final int TARGET_MISSES = 3;
        private static final int PARTICLE_COUNT = 18;

        private final Font headingFont;
        private final Font bodyFont;
        private final ResultHandler resultHandler;
        private final Random random = new Random();
        private final Timer animationTimer;
        private final Particle[] particles = new Particle[PARTICLE_COUNT];

        private float markerPosition = 0.08f;
        private float markerVelocity = 0.0125f;
        private float targetCenter = 0.55f;
        private float targetWidth = 0.15f;
        private float pulse = 0f;
        private float flashStrength = 0f;
        private float shakeStrength = 0f;
        private float bannerAlpha = 0f;
        private float outcomeAlpha = 0f;
        private long lastUpdateNanos;
        private long roundFreezeUntil;
        private long outcomeShownAt;
        private int successes;
        private int misses;
        private boolean finished;
        private boolean result;
        private String statusText = "Strike when the ember aligns with the glowing rune.";
        private Color statusColor = BODY;

        private StrengthTrainingPanel(Font headingFont, Font bodyFont, ResultHandler resultHandler) {
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
                        0.2f + random.nextFloat() * 0.6f, 1.2f + random.nextFloat() * 2.0f);
            }
        }

        private void bindControls() {
            InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
            ActionMap actionMap = getActionMap();

            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "strength-hit");
            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "strength-hit");
            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "strength-cancel");

            actionMap.put("strength-hit", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    attemptStrike();
                }
            });
            actionMap.put("strength-cancel", new AbstractAction() {
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
            pulse += delta * 2.8f;
            flashStrength = Math.max(0f, flashStrength - delta * 1.9f);
            shakeStrength = Math.max(0f, shakeStrength - delta * 2.4f);
            updateParticles(delta);

            if (!finished && System.currentTimeMillis() >= roundFreezeUntil) {
                markerPosition += markerVelocity;
                if (markerPosition >= 1f) {
                    markerPosition = 1f;
                    markerVelocity = -Math.abs(markerVelocity);
                    Toolkit.getDefaultToolkit().sync();
                } else if (markerPosition <= 0f) {
                    markerPosition = 0f;
                    markerVelocity = Math.abs(markerVelocity);
                }

                targetCenter = 0.5f + (float) Math.sin(pulse * 0.85f) * 0.17f;
            }

            if (finished) {
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
                particle.y += particle.speed * delta * 0.06f;
                particle.x += Math.sin((pulse + particle.phase) * 1.4f) * delta * 0.02f;
                if (particle.y > 1.05f) {
                    particle.y = -0.04f;
                    particle.x = random.nextFloat();
                }
            }
        }

        private void attemptStrike() {
            if (finished || System.currentTimeMillis() < roundFreezeUntil) {
                return;
            }

            float zoneStart = targetCenter - targetWidth * 0.5f;
            float zoneEnd = targetCenter + targetWidth * 0.5f;
            boolean hit = markerPosition >= zoneStart && markerPosition <= zoneEnd;

            roundFreezeUntil = System.currentTimeMillis() + 190L;
            flashStrength = 1f;
            shakeStrength = hit ? 0.45f : 0.7f;

            if (hit) {
                successes++;
                markerVelocity = markerVelocity < 0f
                        ? -(Math.abs(markerVelocity) + 0.0011f)
                        : Math.abs(markerVelocity) + 0.0011f;
                statusText = successes >= TARGET_SUCCESSES
                        ? "The forge answers your strength."
                        : "Clean hit. Keep your rhythm.";
                statusColor = ACCENT;
                if (successes >= TARGET_SUCCESSES) {
                    finish(true, "Strength training complete!");
                    return;
                }
            } else {
                misses++;
                statusText = misses >= TARGET_MISSES
                        ? "Your form breaks under pressure."
                        : "Too early. Re-center and strike again.";
                statusColor = FAILURE;
                if (misses >= TARGET_MISSES) {
                    finish(false, "Strength training failed.");
                    return;
                }
            }
        }

        private void finish(boolean success, String message) {
            finished = true;
            result = success;
            statusText = message;
            statusColor = success ? ACCENT : FAILURE;
            outcomeShownAt = System.currentTimeMillis();
            roundFreezeUntil = Long.MAX_VALUE;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            float shakeX = (float) Math.sin(pulse * 18f) * 10f * shakeStrength;
            float shakeY = (float) Math.cos(pulse * 13f) * 6f * shakeStrength;
            g2.translate(shakeX, shakeY);

            int panelWidth = Math.min(760, getWidth() - 80);
            int panelHeight = Math.min(420, getHeight() - 80);
            int panelX = (getWidth() - panelWidth) / 2;
            int panelY = (getHeight() - panelHeight) / 2;

            paintFrame(g2, panelX, panelY, panelWidth, panelHeight);
            paintParticles(g2, panelX, panelY, panelWidth, panelHeight);
            paintHeader(g2, panelX, panelY, panelWidth);
            paintProgress(g2, panelX, panelY, panelWidth);
            paintBar(g2, panelX, panelY, panelWidth);
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
            g2.setColor(new Color(118, 236, 210, 70));
            g2.drawRoundRect(x + 10, y + 10, width - 20, height - 20, 24, 24);
        }

        private void paintParticles(Graphics2D g2, int x, int y, int width, int height) {
            g2.setClip(x + 12, y + 12, width - 24, height - 24);
            for (Particle particle : particles) {
                int px = x + Math.round(particle.x * width);
                int py = y + Math.round(particle.y * height);
                int size = Math.max(2, Math.round(particle.size));
                g2.setColor(new Color(118, 236, 210, 50 + (int) (particle.size * 45)));
                g2.fillOval(px, py, size, size);
            }
            g2.setClip(null);
        }

        private void paintHeader(Graphics2D g2, int x, int y, int width) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, bannerAlpha));
            g2.setFont(headingFont.deriveFont(Font.BOLD, 32f));
            drawCentered(g2, "Strength Trial", x + width / 2, y + 62, TITLE);

            g2.setFont(bodyFont.deriveFont(Font.PLAIN, 16f));
            drawCentered(g2, "Press SPACE or click when the ember meets the rune.",
                    x + width / 2, y + 98, BODY);
            g2.setComposite(AlphaComposite.SrcOver);
        }

        private void paintProgress(Graphics2D g2, int x, int y, int width) {
            int startX = x + 250;
            int topY = y + 128;
            for (int i = 0; i < TARGET_SUCCESSES; i++) {
                int orbX = startX + i * 54;
                boolean filled = i < successes;
                g2.setColor(filled ? ACCENT_SOFT : new Color(255, 255, 255, 20));
                g2.fillOval(orbX, topY, 26, 26);
                g2.setColor(filled ? ACCENT : new Color(156, 170, 201, 110));
                g2.setStroke(new BasicStroke(2f));
                g2.drawOval(orbX, topY, 26, 26);
            }

            int missX = x + width - 250;
            for (int i = 0; i < TARGET_MISSES; i++) {
                int orbX = missX + i * 34;
                boolean marked = i < misses;
                g2.setColor(marked ? new Color(214, 104, 104, 80) : new Color(255, 255, 255, 18));
                g2.fillOval(orbX, topY + 3, 18, 18);
                g2.setColor(marked ? FAILURE : new Color(156, 170, 201, 90));
                g2.drawOval(orbX, topY + 3, 18, 18);
            }

            g2.setFont(bodyFont.deriveFont(Font.BOLD, 14f));
            g2.setColor(BODY);
            g2.drawString("Hits", x + 190, topY + 18);
            g2.drawString("Misses", x + width - 306, topY + 18);
        }

        private void paintBar(Graphics2D g2, int x, int y, int width) {
            int barX = x + 82;
            int barY = y + 196;
            int barWidth = width - 164;
            int barHeight = 88;

            g2.setColor(BAR_TRACK);
            g2.fillRoundRect(barX, barY, barWidth, barHeight, 26, 26);
            g2.setColor(BAR_FRAME);
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(barX, barY, barWidth, barHeight, 26, 26);

            int zoneWidthPx = Math.round(barWidth * targetWidth);
            int zoneCenterPx = barX + Math.round(barWidth * targetCenter);
            int zoneX = zoneCenterPx - zoneWidthPx / 2;
            int zoneGlowHeight = barHeight - 26;
            int zoneGlowY = barY + 13;
            float pulseStrength = 0.5f + 0.5f * (float) Math.sin(pulse * 3f);

            g2.setColor(new Color(118, 236, 210, 48 + Math.round(40 * pulseStrength)));
            g2.fillRoundRect(zoneX - 8, zoneGlowY - 8, zoneWidthPx + 16, zoneGlowHeight + 16, 22, 22);
            g2.setColor(ZONE);
            g2.fillRoundRect(zoneX, zoneGlowY, zoneWidthPx, zoneGlowHeight, 18, 18);
            g2.setColor(ZONE_OUTLINE);
            g2.drawRoundRect(zoneX, zoneGlowY, zoneWidthPx, zoneGlowHeight, 18, 18);

            int markerX = barX + Math.round(barWidth * markerPosition);
            int markerHeight = barHeight + 32;
            int markerY = barY - 16;
            int markerWidth = 14;

            g2.setColor(new Color(118, 236, 210, 80 + Math.round(90 * flashStrength)));
            g2.fillRoundRect(markerX - 10, markerY + 6, markerWidth + 20, markerHeight - 12, 20, 20);
            g2.setColor(MARKER_GLOW);
            g2.fillRoundRect(markerX - 4, markerY, markerWidth + 8, markerHeight, 18, 18);
            g2.setColor(MARKER_CORE);
            g2.fillRoundRect(markerX, markerY + 4, markerWidth, markerHeight - 8, 12, 12);

            if (flashStrength > 0.01f) {
                g2.setColor(new Color(255, 255, 255, Math.min(220, Math.round(180 * flashStrength))));
                g2.setStroke(new BasicStroke(3f));
                g2.drawRoundRect(markerX - 18, barY - 10, 50, barHeight + 20, 22, 22);
            }
        }

        private void paintFooter(Graphics2D g2, int x, int y, int width, int height) {
            g2.setFont(bodyFont.deriveFont(Font.BOLD, 17f));
            drawCentered(g2, statusText, x + width / 2, y + height - 108, statusColor);

            g2.setFont(bodyFont.deriveFont(Font.PLAIN, 14f));
            drawCentered(g2, "Three clean hits to complete the trial.", x + width / 2, y + height - 68, BODY);
        }

        private void paintOutcome(Graphics2D g2, int x, int y, int width, int height) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(0.86f, outcomeAlpha * 0.86f)));
            g2.setColor(new Color(5, 8, 15, 210));
            g2.fillRoundRect(x + 40, y + 116, width - 80, height - 164, 26, 26);

            String message = result ? "Strength Mastered" : "Trial Failed";
            Color tone = result ? ACCENT : FAILURE;
            g2.setFont(headingFont.deriveFont(Font.BOLD, 34f));
            drawCentered(g2, message, x + width / 2, y + height / 2 - 10, tone);
            g2.setFont(bodyFont.deriveFont(Font.PLAIN, 16f));
            drawCentered(g2, result ? "The forge recognizes your resolve." : "Catch the rune more cleanly next time.",
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

        @Override
        protected void processMouseEvent(java.awt.event.MouseEvent event) {
            super.processMouseEvent(event);
            if (event.getID() == java.awt.event.MouseEvent.MOUSE_PRESSED) {
                attemptStrike();
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
