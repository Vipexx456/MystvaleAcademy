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

final class EnduranceTrainingDialog extends JDialog {
    private boolean success;

    private EnduranceTrainingDialog(Window owner, Font headingFont, Font bodyFont) {
        super(owner, "Endurance Training", ModalityType.APPLICATION_MODAL);
        setUndecorated(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setBackground(new Color(0, 0, 0, 0));

        EnduranceTrainingPanel panel = new EnduranceTrainingPanel(
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
        EnduranceTrainingDialog dialog = new EnduranceTrainingDialog(owner, headingFont, bodyFont);
        dialog.setVisible(true);
        return dialog.success;
    }

    private interface ResultHandler {
        void complete(boolean success);
    }

    private static final class EnduranceTrainingPanel extends JPanel {
        private static final Color PANEL_TOP = new Color(27, 35, 49, 246);
        private static final Color PANEL_BOTTOM = new Color(12, 18, 28, 246);
        private static final Color PANEL_BORDER = new Color(126, 144, 166, 185);
        private static final Color TITLE = new Color(240, 244, 249);
        private static final Color BODY = new Color(202, 214, 226);
        private static final Color ACCENT = new Color(153, 233, 197);
        private static final Color ACCENT_SOFT = new Color(153, 233, 197, 84);
        private static final Color FAILURE = new Color(214, 104, 104);
        private static final Color TRACK = new Color(9, 16, 23, 225);
        private static final Color TRACK_FRAME = new Color(156, 170, 201, 140);
        private static final Color SAFE_ZONE = new Color(153, 233, 197, 90);
        private static final Color SAFE_ZONE_BORDER = new Color(198, 255, 231, 190);
        private static final Color CORE = new Color(246, 251, 255);
        private static final Color CORE_GLOW = new Color(153, 233, 197, 160);
        private static final int PARTICLE_COUNT = 22;
        private static final int TARGET_SLIPS = 3;
        private static final float SUCCESS_SECONDS = 7.2f;

        private final Font headingFont;
        private final Font bodyFont;
        private final ResultHandler resultHandler;
        private final Random random = new Random();
        private final Timer animationTimer;
        private final Particle[] particles = new Particle[PARTICLE_COUNT];

        private float coreY = 0.5f;
        private float coreVelocity;
        private float safeZoneCenter = 0.5f;
        private float safeZoneHeight = 0.24f;
        private float holdSeconds;
        private float pulseClock;
        private float bannerAlpha;
        private float flashStrength;
        private float outcomeAlpha;
        private long lastUpdateNanos;
        private long outcomeShownAt;
        private int slips;
        private boolean holdingUp;
        private boolean finished;
        private boolean result;
        private String statusText = "Hold SPACE or mouse button to steady yourself. Stay inside the safe band.";
        private Color statusColor = BODY;

        private EnduranceTrainingPanel(Font headingFont, Font bodyFont, ResultHandler resultHandler) {
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
                        0.2f + random.nextFloat() * 0.8f, 0.8f + random.nextFloat() * 1.5f);
            }
        }

        private void bindControls() {
            InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
            ActionMap actionMap = getActionMap();
            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, false), "endurance-down");
            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, true), "endurance-up");
            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "endurance-cancel");

            actionMap.put("endurance-down", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    holdingUp = true;
                }
            });
            actionMap.put("endurance-up", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    holdingUp = false;
                }
            });
            actionMap.put("endurance-cancel", new AbstractAction() {
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
            pulseClock += delta * 2.05f;
            flashStrength = Math.max(0f, flashStrength - delta * 2.2f);
            updateParticles(delta);

            if (!finished) {
                updateSafeZone(delta);
                updateCore(delta);
                updateState();
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
                particle.x += Math.sin((pulseClock + particle.phase) * 1.1f) * delta * 0.013f;
                if (particle.y > 1.05f) {
                    particle.y = -0.04f;
                    particle.x = random.nextFloat();
                }
            }
        }

        private void updateSafeZone(float delta) {
            safeZoneCenter = 0.5f + (float) Math.sin(pulseClock * 0.55f) * 0.16f;
            safeZoneHeight = 0.22f + 0.02f * (float) Math.sin(pulseClock * 0.9f + 0.8f);
            coreVelocity += (holdingUp ? -0.95f : 0.82f) * delta;
            coreVelocity *= 0.96f;
        }

        private void updateCore(float delta) {
            coreY += coreVelocity * delta;
            coreY = Math.max(0f, Math.min(1f, coreY));
            if (coreY <= 0f || coreY >= 1f) {
                coreVelocity *= -0.2f;
            }
        }

        private void updateState() {
            float zoneTop = safeZoneCenter - safeZoneHeight * 0.5f;
            float zoneBottom = safeZoneCenter + safeZoneHeight * 0.5f;

            if (coreY >= zoneTop && coreY <= zoneBottom) {
                holdSeconds += 0.016f;
                statusText = "Steady. Breathe and hold the line.";
                statusColor = ACCENT;
                if (holdSeconds >= SUCCESS_SECONDS) {
                    finish(true, "Endurance training complete!");
                }
                return;
            }

            holdSeconds = Math.max(0f, holdSeconds - 0.028f);
            if (coreY <= 0.05f || coreY >= 0.95f) {
                slips++;
                flashStrength = 1f;
                coreY = 0.5f;
                coreVelocity = 0f;
                statusText = coreY <= 0.5f
                        ? "You overcorrected. Re-center your breath."
                        : "Your stance broke. Settle back in.";
                statusColor = FAILURE;
                if (slips >= TARGET_SLIPS) {
                    finish(false, "Endurance training failed.");
                }
            }
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
            g2.setColor(new Color(153, 233, 197, 70));
            g2.drawRoundRect(x + 10, y + 10, width - 20, height - 20, 24, 24);
        }

        private void paintParticles(Graphics2D g2, int x, int y, int width, int height) {
            g2.setClip(x + 12, y + 12, width - 24, height - 24);
            for (Particle particle : particles) {
                int px = x + Math.round(particle.x * width);
                int py = y + Math.round(particle.y * height);
                int size = Math.max(2, Math.round(particle.size));
                g2.setColor(new Color(153, 233, 197, 42 + (int) (particle.size * 35)));
                g2.fillOval(px, py, size, size);
            }
            g2.setClip(null);
        }

        private void paintHeader(Graphics2D g2, int x, int y, int width) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, bannerAlpha));
            g2.setFont(headingFont.deriveFont(Font.BOLD, 32f));
            drawCentered(g2, "Endurance Trial", x + width / 2, y + 62, TITLE);
            g2.setFont(bodyFont.deriveFont(Font.PLAIN, 16f));
            drawCentered(g2, "Hold SPACE or mouse button to resist the drift and stay centered.",
                    x + width / 2, y + 98, BODY);
            g2.setComposite(AlphaComposite.SrcOver);
        }

        private void paintProgress(Graphics2D g2, int x, int y, int width) {
            g2.setFont(bodyFont.deriveFont(Font.BOLD, 14f));
            g2.setColor(BODY);
            g2.drawString("Focus", x + 194, y + 146);
            g2.drawString("Slips", x + width - 290, y + 146);

            int progressX = x + 238;
            int progressY = y + 130;
            int progressWidth = 170;
            g2.setColor(new Color(255, 255, 255, 22));
            g2.fillRoundRect(progressX, progressY, progressWidth, 16, 12, 12);
            g2.setColor(ACCENT_SOFT);
            g2.fillRoundRect(progressX, progressY,
                    Math.max(6, Math.round(progressWidth * (holdSeconds / SUCCESS_SECONDS))), 16, 12, 12);
            g2.setColor(new Color(156, 170, 201, 110));
            g2.drawRoundRect(progressX, progressY, progressWidth, 16, 12, 12);

            int slipX = x + width - 248;
            for (int i = 0; i < TARGET_SLIPS; i++) {
                int orbX = slipX + i * 34;
                boolean marked = i < slips;
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

            g2.setColor(TRACK);
            g2.fillRoundRect(chamberX, chamberY, chamberWidth, chamberHeight, 28, 28);
            g2.setColor(TRACK_FRAME);
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(chamberX, chamberY, chamberWidth, chamberHeight, 28, 28);

            int pad = 18;
            int innerX = chamberX + pad;
            int innerY = chamberY + pad;
            int innerWidth = chamberWidth - pad * 2;
            int innerHeight = chamberHeight - pad * 2;

            int safeHeightPx = Math.round(innerHeight * safeZoneHeight);
            int safeY = innerY + Math.round(innerHeight * safeZoneCenter) - safeHeightPx / 2;
            float glow = 0.55f + 0.45f * (float) Math.sin(pulseClock * 2.1f);
            g2.setColor(new Color(153, 233, 197, 42 + Math.round(30 * glow)));
            g2.fillRoundRect(innerX - 8, safeY - 6, innerWidth + 16, safeHeightPx + 12, 22, 22);
            g2.setColor(SAFE_ZONE);
            g2.fillRoundRect(innerX, safeY, innerWidth, safeHeightPx, 18, 18);
            g2.setColor(SAFE_ZONE_BORDER);
            g2.drawRoundRect(innerX, safeY, innerWidth, safeHeightPx, 18, 18);

            int coreYpx = innerY + Math.round(innerHeight * coreY);
            g2.setColor(new Color(153, 233, 197, 48 + Math.round(80 * flashStrength)));
            g2.fillRoundRect(innerX - 10, coreYpx - 18, innerWidth + 20, 36, 20, 20);
            g2.setColor(CORE_GLOW);
            g2.fillRoundRect(innerX - 4, coreYpx - 12, innerWidth + 8, 24, 16, 16);
            g2.setColor(CORE);
            g2.fillRoundRect(innerX, coreYpx - 8, innerWidth, 16, 14, 14);

            g2.setColor(new Color(255, 255, 255, 24));
            for (int i = 1; i < 5; i++) {
                int lineY = innerY + (innerHeight * i / 5);
                g2.drawLine(innerX + 8, lineY, innerX + innerWidth - 8, lineY);
            }
        }

        private void paintFooter(Graphics2D g2, int x, int y, int width, int height) {
            g2.setFont(bodyFont.deriveFont(Font.BOLD, 17f));
            drawCentered(g2, statusText, x + width / 2, y + height - 108, statusColor);
            g2.setFont(bodyFont.deriveFont(Font.PLAIN, 14f));
            drawCentered(g2, "Endure the drift without slipping out of control.", x + width / 2, y + height - 68, BODY);
        }

        private void paintOutcome(Graphics2D g2, int x, int y, int width, int height) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(0.86f, outcomeAlpha * 0.86f)));
            g2.setColor(new Color(5, 8, 15, 210));
            g2.fillRoundRect(x + 40, y + 116, width - 80, height - 164, 26, 26);
            String message = result ? "Endurance Proven" : "Trial Failed";
            Color tone = result ? ACCENT : FAILURE;
            g2.setFont(headingFont.deriveFont(Font.BOLD, 34f));
            drawCentered(g2, message, x + width / 2, y + height / 2 - 10, tone);
            g2.setFont(bodyFont.deriveFont(Font.PLAIN, 16f));
            drawCentered(g2, result ? "Your breathing held through the strain." : "Your balance gave way under pressure.",
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
                holdingUp = true;
            } else if (event.getID() == java.awt.event.MouseEvent.MOUSE_RELEASED) {
                holdingUp = false;
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
