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

final class DurabilityTrainingDialog extends JDialog {
    private boolean success;

    private DurabilityTrainingDialog(Window owner, Font headingFont, Font bodyFont) {
        super(owner, "Durability Training", ModalityType.APPLICATION_MODAL);
        setUndecorated(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setBackground(new Color(0, 0, 0, 0));

        DurabilityTrainingPanel panel = new DurabilityTrainingPanel(
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
        DurabilityTrainingDialog dialog = new DurabilityTrainingDialog(owner, headingFont, bodyFont);
        dialog.setVisible(true);
        return dialog.success;
    }

    private interface ResultHandler {
        void complete(boolean success);
    }

    private static final class DurabilityTrainingPanel extends JPanel {
        private static final Color PANEL_TOP = new Color(25, 33, 51, 246);
        private static final Color PANEL_BOTTOM = new Color(10, 15, 27, 246);
        private static final Color PANEL_BORDER = new Color(124, 145, 184, 186);
        private static final Color TITLE = new Color(236, 243, 255);
        private static final Color BODY = new Color(196, 208, 232);
        private static final Color ACCENT = new Color(132, 224, 255);
        private static final Color ACCENT_SOFT = new Color(132, 224, 255, 82);
        private static final Color FAILURE = new Color(214, 104, 104);
        private static final Color TRACK = new Color(8, 14, 25, 225);
        private static final Color TRACK_BORDER = new Color(156, 170, 201, 138);
        private static final Color SHIELD = new Color(132, 224, 255, 110);
        private static final Color SHIELD_GLOW = new Color(132, 224, 255, 165);
        private static final Color SHIELD_CORE = new Color(240, 247, 255);
        private static final Color IMPACT_WINDOW = new Color(132, 224, 255, 94);
        private static final Color IMPACT_WINDOW_BORDER = new Color(201, 243, 255, 195);
        private static final int PARTICLE_COUNT = 20;
        private static final int REQUIRED_IMPACTS = 5;
        private static final int MAX_CRACKS = 3;

        private final Font headingFont;
        private final Font bodyFont;
        private final ResultHandler resultHandler;
        private final Random random = new Random();
        private final Timer animationTimer;
        private final Particle[] particles = new Particle[PARTICLE_COUNT];

        private float bannerAlpha;
        private float pulseClock;
        private float flashStrength;
        private float outcomeAlpha;
        private float shieldIntegrity = 1f;
        private float strain = 0.18f;
        private float progress = 0.02f;
        private float impactWindowCenter = 0.84f;
        private float impactWindowWidth = 0.12f;
        private float barrierPulse;
        private long lastUpdateNanos;
        private long outcomeShownAt;
        private int defendedImpacts;
        private int cracks;
        private boolean bracing;
        private boolean finished;
        private boolean result;
        private String statusText = "Hold SPACE or mouse only inside the impact window so the barrier does not crack.";
        private Color statusColor = BODY;

        private DurabilityTrainingPanel(Font headingFont, Font bodyFont, ResultHandler resultHandler) {
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
                        0.2f + random.nextFloat() * 0.7f, 0.7f + random.nextFloat() * 1.4f);
            }
        }

        private void bindControls() {
            InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
            ActionMap actionMap = getActionMap();

            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, false), "durability-down");
            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, true), "durability-up");
            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false), "durability-down");
            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, true), "durability-up");
            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "durability-cancel");

            actionMap.put("durability-down", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    bracing = true;
                }
            });
            actionMap.put("durability-up", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    bracing = false;
                }
            });
            actionMap.put("durability-cancel", new AbstractAction() {
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
            pulseClock += delta * 2.1f;
            flashStrength = Math.max(0f, flashStrength - delta * 2f);
            barrierPulse = Math.max(0f, barrierPulse - delta * 2.4f);
            updateParticles(delta);

            if (!finished) {
                updateWindow(delta);
                updateStrain(delta);
                updateImpactCycle(delta);
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
                particle.x += Math.sin((pulseClock + particle.phase) * 1.2f) * delta * 0.013f;
                if (particle.y > 1.05f) {
                    particle.y = -0.04f;
                    particle.x = random.nextFloat();
                }
            }
        }

        private void updateWindow(float delta) {
            impactWindowCenter = 0.84f + 0.03f * (float) Math.sin(pulseClock * 0.72f);
            impactWindowWidth = 0.11f + 0.015f * (float) Math.sin(pulseClock * 1.1f + 0.8f);
        }

        private void updateStrain(float delta) {
            if (bracing) {
                strain = Math.min(1f, strain + delta * 0.52f);
            } else {
                strain = Math.max(0f, strain - delta * 0.28f);
            }
        }

        private void updateImpactCycle(float delta) {
            progress += delta * (0.34f + defendedImpacts * 0.016f);
            float impactPoint = Math.min(0.96f, impactWindowCenter + impactWindowWidth * 0.35f);
            if (progress < impactPoint) {
                if (bracing && !isInsideWindow(progress) && strain > 0.72f) {
                    shieldIntegrity = Math.max(0f, shieldIntegrity - delta * 0.12f);
                    statusText = "Too early. Save your brace for the true impact.";
                    statusColor = BODY;
                } else if (!bracing) {
                    statusText = "Release and recover, then catch the impact window.";
                    statusColor = BODY;
                } else {
                    statusText = "Hold the brace. Let the impact meet the barrier.";
                    statusColor = ACCENT;
                }
                return;
            }

            resolveImpact();
        }

        private boolean isInsideWindow(float value) {
            float half = impactWindowWidth * 0.5f;
            return value >= impactWindowCenter - half && value <= impactWindowCenter + half;
        }

        private void resolveImpact() {
            float impactPoint = Math.min(0.96f, impactWindowCenter + impactWindowWidth * 0.35f);
            boolean timedBrace = bracing && isInsideWindow(impactPoint) && strain <= 0.88f;
            flashStrength = 1f;
            barrierPulse = 1f;

            if (timedBrace) {
                defendedImpacts++;
                shieldIntegrity = Math.min(1f, shieldIntegrity + 0.1f);
                strain = Math.min(0.58f, strain * 0.68f);
                statusText = defendedImpacts >= REQUIRED_IMPACTS
                        ? "The barrier endured every impact."
                        : "Clean brace. Your shield is holding.";
                statusColor = ACCENT;
                if (defendedImpacts >= REQUIRED_IMPACTS) {
                    finish(true, "Durability training complete!");
                    return;
                }
            } else {
                cracks++;
                shieldIntegrity = Math.max(0f, shieldIntegrity - 0.3f);
                strain = Math.min(1f, strain + 0.14f);
                statusText = "The barrier cracked. Brace later and keep it from breaking.";
                statusColor = FAILURE;
                if (cracks >= MAX_CRACKS || shieldIntegrity <= 0.05f) {
                    finish(false, "Durability training failed.");
                    return;
                }
            }

            progress = 0f;
        }

        private void finish(boolean success, String message) {
            if (finished) {
                return;
            }
            finished = true;
            result = success;
            bracing = false;
            statusText = message;
            statusColor = success ? ACCENT : FAILURE;
            outcomeShownAt = System.currentTimeMillis();
        }

        @Override
        protected void processMouseEvent(java.awt.event.MouseEvent event) {
            super.processMouseEvent(event);
            if (finished) {
                return;
            }
            if (event.getID() == java.awt.event.MouseEvent.MOUSE_PRESSED) {
                bracing = true;
            } else if (event.getID() == java.awt.event.MouseEvent.MOUSE_RELEASED) {
                bracing = false;
            }
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
            paintArena(g2, panelX, panelY, panelWidth, panelHeight);
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
            g2.setColor(new Color(132, 224, 255, 72));
            g2.drawRoundRect(x + 10, y + 10, width - 20, height - 20, 24, 24);
        }

        private void paintParticles(Graphics2D g2, int x, int y, int width, int height) {
            g2.setClip(x + 12, y + 12, width - 24, height - 24);
            for (Particle particle : particles) {
                int px = x + Math.round(particle.x * width);
                int py = y + Math.round(particle.y * height);
                int size = Math.max(2, Math.round(particle.size));
                g2.setColor(new Color(132, 224, 255, 36 + (int) (particle.size * 34)));
                g2.fillOval(px, py, size, size);
            }
            g2.setClip(null);
        }

        private void paintHeader(Graphics2D g2, int x, int y, int width) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, bannerAlpha));
            g2.setFont(headingFont.deriveFont(Font.BOLD, 32f));
            drawCentered(g2, "Durability Trial", x + width / 2, y + 62, TITLE);
            g2.setFont(bodyFont.deriveFont(Font.PLAIN, 16f));
            drawCentered(g2, "Brace only as the impact meets the ward. Endure the barrage without letting it break.",
                    x + width / 2, y + 98, BODY);
            g2.setComposite(AlphaComposite.SrcOver);
        }

        private void paintProgress(Graphics2D g2, int x, int y, int width) {
            g2.setFont(bodyFont.deriveFont(Font.BOLD, 14f));
            g2.setColor(BODY);
            g2.drawString("Integrity", x + 180, y + 146);
            g2.drawString("Cracks", x + width - 294, y + 146);

            int integrityX = x + 246;
            int integrityY = y + 130;
            int integrityWidth = 170;
            g2.setColor(new Color(255, 255, 255, 22));
            g2.fillRoundRect(integrityX, integrityY, integrityWidth, 16, 12, 12);
            g2.setColor(ACCENT_SOFT);
            g2.fillRoundRect(integrityX, integrityY,
                    Math.max(6, Math.round(integrityWidth * shieldIntegrity)), 16, 12, 12);
            g2.setColor(new Color(156, 170, 201, 110));
            g2.drawRoundRect(integrityX, integrityY, integrityWidth, 16, 12, 12);

            int crackX = x + width - 248;
            for (int i = 0; i < MAX_CRACKS; i++) {
                int orbX = crackX + i * 34;
                boolean marked = i < cracks;
                g2.setColor(marked ? new Color(214, 104, 104, 80) : new Color(255, 255, 255, 18));
                g2.fillOval(orbX, y + 128, 18, 18);
                g2.setColor(marked ? FAILURE : new Color(156, 170, 201, 90));
                g2.drawOval(orbX, y + 128, 18, 18);
            }
        }

        private void paintArena(Graphics2D g2, int x, int y, int width, int height) {
            int arenaX = x + 88;
            int arenaY = y + 176;
            int arenaWidth = width - 176;
            int arenaHeight = 148;

            g2.setColor(TRACK);
            g2.fillRoundRect(arenaX, arenaY, arenaWidth, arenaHeight, 28, 28);
            g2.setColor(TRACK_BORDER);
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(arenaX, arenaY, arenaWidth, arenaHeight, 28, 28);

            int trackX = arenaX + 42;
            int trackY = arenaY + arenaHeight / 2 - 18;
            int trackWidth = arenaWidth - 84;
            int trackHeight = 36;

            g2.setColor(new Color(255, 255, 255, 14));
            g2.fillRoundRect(trackX, trackY, trackWidth, trackHeight, 20, 20);

            int windowWidthPx = Math.round(trackWidth * impactWindowWidth);
            int windowCenterPx = trackX + Math.round(trackWidth * impactWindowCenter);
            int windowX = windowCenterPx - windowWidthPx / 2;
            g2.setColor(IMPACT_WINDOW);
            g2.fillRoundRect(windowX, trackY - 8, windowWidthPx, trackHeight + 16, 20, 20);
            g2.setColor(IMPACT_WINDOW_BORDER);
            g2.drawRoundRect(windowX, trackY - 8, windowWidthPx, trackHeight + 16, 20, 20);

            int pulseX = trackX + Math.round(trackWidth * Math.min(progress, 1f));
            g2.setColor(new Color(132, 224, 255, 80 + Math.round(75 * barrierPulse)));
            g2.fillRoundRect(pulseX - 12, trackY - 14, 24, trackHeight + 28, 20, 20);
            g2.setColor(SHIELD_CORE);
            g2.fillRoundRect(pulseX - 5, trackY - 8, 10, trackHeight + 16, 12, 12);

            int shieldCenterX = arenaX + arenaWidth / 2;
            int shieldY = arenaY + 38;
            int shieldWidth = 140;
            int shieldHeight = 62;
            float braceGlow = bracing ? 1f : 0.35f;
            g2.setColor(new Color(132, 224, 255, 36 + Math.round(56 * braceGlow)));
            g2.fillRoundRect(shieldCenterX - shieldWidth / 2 - 12, shieldY - 10,
                    shieldWidth + 24, shieldHeight + 20, 28, 28);
            g2.setColor(new Color(132, 224, 255, 70 + Math.round(60 * shieldIntegrity)));
            g2.fillRoundRect(shieldCenterX - shieldWidth / 2, shieldY, shieldWidth, shieldHeight, 24, 24);
            g2.setColor(bracing ? SHIELD_CORE : SHIELD_GLOW);
            g2.setStroke(new BasicStroke(bracing ? 3f : 2f));
            g2.drawRoundRect(shieldCenterX - shieldWidth / 2, shieldY, shieldWidth, shieldHeight, 24, 24);

            if (flashStrength > 0.01f) {
                Color flash = statusColor == FAILURE
                        ? new Color(255, 173, 173, Math.min(170, Math.round(150 * flashStrength)))
                        : new Color(255, 255, 255, Math.min(180, Math.round(160 * flashStrength)));
                g2.setColor(flash);
                g2.fillRoundRect(shieldCenterX - shieldWidth / 2 - 6, shieldY - 4,
                        shieldWidth + 12, shieldHeight + 8, 26, 26);
            }

            int strainBarWidth = 190;
            int strainBarX = arenaX + arenaWidth / 2 - strainBarWidth / 2;
            int strainBarY = arenaY + arenaHeight - 44;
            g2.setColor(new Color(255, 255, 255, 18));
            g2.fillRoundRect(strainBarX, strainBarY, strainBarWidth, 10, 10, 10);
            g2.setColor(new Color(132, 224, 255, 92));
            g2.fillRoundRect(strainBarX, strainBarY,
                    Math.max(8, Math.round(strainBarWidth * strain)), 10, 10, 10);
            g2.setColor(new Color(156, 170, 201, 96));
            g2.drawRoundRect(strainBarX, strainBarY, strainBarWidth, 10, 10, 10);
        }

        private void paintFooter(Graphics2D g2, int x, int y, int width, int height) {
            g2.setFont(bodyFont.deriveFont(Font.BOLD, 15f));
            drawCentered(g2, bracing ? "BRACE" : "RECOVER", x + width / 2, y + height - 74, bracing ? ACCENT : BODY);

            g2.setFont(bodyFont.deriveFont(Font.BOLD, 17f));
            drawCentered(g2, statusText, x + width / 2, y + height - 48, statusColor);
            g2.setFont(bodyFont.deriveFont(Font.PLAIN, 14f));
            drawCentered(g2, "Five defended impacts will prove the barrier cannot be broken.", x + width / 2, y + height - 22, BODY);
        }

        private void paintOutcome(Graphics2D g2, int x, int y, int width, int height) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(0.86f, outcomeAlpha * 0.86f)));
            g2.setColor(new Color(5, 8, 15, 210));
            g2.fillRoundRect(x + 40, y + 116, width - 80, height - 164, 26, 26);
            String message = result ? "Durability Proven" : "Trial Failed";
            Color tone = result ? ACCENT : FAILURE;
            g2.setFont(headingFont.deriveFont(Font.BOLD, 34f));
            drawCentered(g2, message, x + width / 2, y + height / 2 - 10, tone);
            g2.setFont(bodyFont.deriveFont(Font.PLAIN, 16f));
            drawCentered(g2, result ? "Your ward held through the full barrage." : "The barrier finally fractured under pressure.",
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
