package damose.ui.dialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

import damose.config.AppConstants;

/**
 * Elegant loading frame with animated progress.
 * Uses JFrame to appear in taskbar for a seamless app experience.
 */
public class LoadingDialog extends JFrame {

    private final JLabel statusLabel;
    private final JLabel detailLabel;
    private final ProgressBarPanel progressPanel;
    private final StepIndicator[] steps;

    private Timer countdownTimer;
    private int secondsRemaining;
    private boolean dataReceived = false;
    private Runnable onComplete;

    public LoadingDialog(JFrame parent) {
        super("Damose - Caricamento");
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));
        setSize(480, 400);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        
        // Set app icon
        try {
            ImageIcon icon = new ImageIcon(getClass().getResource("/sprites/icon.png"));
            List<Image> icons = new ArrayList<>();
            icons.add(icon.getImage());
            setIconImages(icons);
        } catch (Exception e) {
            System.out.println("Could not load app icon: " + e.getMessage());
        }

        JPanel mainPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppConstants.BG_DARK);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
                g2.setColor(AppConstants.BORDER_COLOR);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 24, 24);
                g2.dispose();
            }
        };
        mainPanel.setOpaque(false);
        mainPanel.setBorder(new EmptyBorder(36, 44, 36, 44));

        JPanel headerPanel = new JPanel();
        headerPanel.setOpaque(false);
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));

        // Icon + Title row (centered)
        JPanel titleRow = new JPanel();
        titleRow.setOpaque(false);
        titleRow.setLayout(new BoxLayout(titleRow, BoxLayout.X_AXIS));
        titleRow.setAlignmentX(CENTER_ALIGNMENT);
        
        try {
            ImageIcon rawIcon = new ImageIcon(getClass().getResource("/sprites/icon.png"));
            Image scaled = rawIcon.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
            JLabel iconLabel = new JLabel(new ImageIcon(scaled));
            titleRow.add(iconLabel);
            titleRow.add(Box.createHorizontalStrut(12));
        } catch (Exception e) {
            // Icon not found
        }

        JLabel titleLabel = new JLabel("DAMOSE");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 36));
        titleLabel.setForeground(AppConstants.TEXT_PRIMARY);
        titleRow.add(titleLabel);
        
        headerPanel.add(titleRow);

        JLabel subtitleLabel = new JLabel("Rome Bus Tracker");
        subtitleLabel.setFont(AppConstants.FONT_SUBTITLE);
        subtitleLabel.setForeground(AppConstants.TEXT_MUTED);
        subtitleLabel.setAlignmentX(CENTER_ALIGNMENT);
        headerPanel.add(Box.createVerticalStrut(4));
        headerPanel.add(subtitleLabel);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel();
        centerPanel.setOpaque(false);
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBorder(new EmptyBorder(32, 0, 20, 0));

        steps = new StepIndicator[4];
        steps[0] = new StepIndicator("Inizializzazione");
        steps[1] = new StepIndicator("Caricamento dati GTFS");
        steps[2] = new StepIndicator("Connessione Real-Time");
        steps[3] = new StepIndicator("Avvio applicazione");

        for (int i = 0; i < steps.length; i++) {
            centerPanel.add(steps[i]);
            if (i < steps.length - 1) {
                centerPanel.add(Box.createVerticalStrut(14));
            }
        }

        centerPanel.add(Box.createVerticalStrut(28));

        progressPanel = new ProgressBarPanel();
        progressPanel.setAlignmentX(CENTER_ALIGNMENT);
        centerPanel.add(progressPanel);

        centerPanel.add(Box.createVerticalStrut(20));

        statusLabel = new JLabel("Preparazione...", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        statusLabel.setForeground(AppConstants.TEXT_PRIMARY);
        statusLabel.setAlignmentX(CENTER_ALIGNMENT);
        centerPanel.add(statusLabel);

        detailLabel = new JLabel(" ", SwingConstants.CENTER);
        detailLabel.setFont(AppConstants.FONT_SUBTITLE);
        detailLabel.setForeground(AppConstants.TEXT_MUTED);
        detailLabel.setAlignmentX(CENTER_ALIGNMENT);
        centerPanel.add(Box.createVerticalStrut(6));
        centerPanel.add(detailLabel);

        mainPanel.add(centerPanel, BorderLayout.CENTER);

        setContentPane(mainPanel);
        setShape(new RoundRectangle2D.Double(0, 0, 480, 400, 24, 24));
    }

    private class StepIndicator extends JPanel {
        private final JLabel iconLabel;
        private final JLabel textLabel;
        private State state = State.PENDING;

        enum State { PENDING, ACTIVE, DONE, WARNING }

        StepIndicator(String text) {
            setOpaque(false);
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            setAlignmentX(CENTER_ALIGNMENT);

            iconLabel = new JLabel("o");
            iconLabel.setFont(new Font("Segoe UI", Font.PLAIN, 18));
            iconLabel.setForeground(AppConstants.TEXT_MUTED);
            iconLabel.setPreferredSize(new Dimension(28, 28));

            textLabel = new JLabel(text);
            textLabel.setFont(new Font("Segoe UI", Font.PLAIN, 15));
            textLabel.setForeground(AppConstants.TEXT_SECONDARY);
            textLabel.setPreferredSize(new Dimension(220, 28));

            add(iconLabel);
            add(Box.createHorizontalStrut(10));
            add(textLabel);
        }

        void setState(State newState) {
            this.state = newState;
            SwingUtilities.invokeLater(() -> {
                switch (state) {
                    case PENDING:
                        iconLabel.setText("o");
                        iconLabel.setForeground(AppConstants.TEXT_MUTED);
                        textLabel.setForeground(AppConstants.TEXT_SECONDARY);
                        textLabel.setFont(new Font("Segoe UI", Font.PLAIN, 15));
                        break;
                    case ACTIVE:
                        iconLabel.setText(">");
                        iconLabel.setForeground(AppConstants.ACCENT);
                        textLabel.setForeground(AppConstants.TEXT_PRIMARY);
                        textLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
                        break;
                    case DONE:
                        iconLabel.setText("v");
                        iconLabel.setForeground(AppConstants.SUCCESS_COLOR);
                        textLabel.setForeground(AppConstants.SUCCESS_COLOR);
                        textLabel.setFont(new Font("Segoe UI", Font.PLAIN, 15));
                        break;
                    case WARNING:
                        iconLabel.setText("!");
                        iconLabel.setForeground(AppConstants.WARNING_COLOR);
                        textLabel.setForeground(AppConstants.WARNING_COLOR);
                        textLabel.setFont(new Font("Segoe UI", Font.PLAIN, 15));
                        break;
                }
            });
        }

        void setText(String text) {
            SwingUtilities.invokeLater(() -> textLabel.setText(text));
        }
    }

    private class ProgressBarPanel extends JPanel {
        private int progress = 0;

        ProgressBarPanel() {
            setOpaque(false);
            setPreferredSize(new Dimension(392, 14));
            setMaximumSize(new Dimension(392, 14));
        }

        void setProgress(int value) {
            this.progress = Math.max(0, Math.min(100, value));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int arc = h;

            g2.setColor(AppConstants.PROGRESS_BG);
            g2.fillRoundRect(0, 0, w, h, arc, arc);

            if (progress > 0) {
                int fillWidth = (int) (w * progress / 100.0);
                if (fillWidth > arc) {
                    g2.setColor(AppConstants.ACCENT);
                    g2.fillRoundRect(0, 0, fillWidth, h, arc, arc);
                }
            }

            g2.dispose();
        }
    }

    public void setProgress(int percent, String status) {
        SwingUtilities.invokeLater(() -> {
            progressPanel.setProgress(percent);
            statusLabel.setText(status);
        });
    }

    public void setDetail(String detail) {
        SwingUtilities.invokeLater(() -> detailLabel.setText(detail != null ? detail : " "));
    }

    public void stepInitStart() {
        steps[0].setState(StepIndicator.State.ACTIVE);
        setProgress(5, "Inizializzazione sistema...");
    }

    public void stepInitDone() {
        steps[0].setState(StepIndicator.State.DONE);
        setProgress(15, "Sistema pronto");
    }

    public void stepStaticStart() {
        steps[1].setState(StepIndicator.State.ACTIVE);
        setProgress(20, "Caricamento dati GTFS...");
    }

    public void stepStaticProgress(String item) {
        setDetail("Lettura " + item);
    }

    public void stepStaticDone(int stopsCount, int tripsCount) {
        steps[1].setState(StepIndicator.State.DONE);
        setProgress(50, "Dati GTFS caricati");
        setDetail(String.format("%,d fermate  |  %,d viaggi", stopsCount, tripsCount));
    }

    public void stepRTStart(int timeoutSeconds) {
        steps[2].setState(StepIndicator.State.ACTIVE);
        setProgress(55, "Connessione feed real-time...");
        this.secondsRemaining = timeoutSeconds;

        countdownTimer = new Timer(1000, e -> {
            secondsRemaining--;
            setDetail("Attesa risposta... " + secondsRemaining + "s");

            if (secondsRemaining <= 0 && !dataReceived) {
                countdownTimer.stop();
                stepRTTimeout();
            }
        });
        setDetail("Attesa risposta... " + secondsRemaining + "s");
        countdownTimer.start();
    }

    public void stepRTDone() {
        dataReceived = true;
        if (countdownTimer != null) countdownTimer.stop();
        steps[2].setState(StepIndicator.State.DONE);
        setProgress(80, "Dati real-time ricevuti");
        setDetail(null);
    }

    public void stepRTTimeout() {
        if (countdownTimer != null) countdownTimer.stop();
        steps[2].setState(StepIndicator.State.WARNING);
        steps[2].setText("Real-Time non disponibile");
        setProgress(75, "Modalita' offline");
        setDetail("Verranno usati solo dati statici");
    }

    public void stepAppStart() {
        steps[3].setState(StepIndicator.State.ACTIVE);
        setProgress(90, "Avvio interfaccia...");
        setDetail(null);
    }

    public void stepAppDone() {
        steps[3].setState(StepIndicator.State.DONE);
        setProgress(100, "Pronto!");
        setDetail(null);
    }

    public void setOnComplete(Runnable callback) {
        this.onComplete = callback;
    }

    public void completeAndClose() {
        stepAppDone();
        Timer closeTimer = new Timer(700, e -> {
            ((Timer) e.getSource()).stop();
            dispose();
            if (onComplete != null) {
                SwingUtilities.invokeLater(onComplete);
            }
        });
        closeTimer.setRepeats(false);
        closeTimer.start();
    }

    public boolean isDataReceived() {
        return dataReceived;
    }

    public void closeNow() {
        if (countdownTimer != null) {
            countdownTimer.stop();
        }
        dispose();
    }
}
