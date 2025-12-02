package project_starter.view;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.Timer;

/**
 * Pannello flottante per mostrare gli arrivi alla fermata.
 * Design pulito con sfondo scuro e testo chiaro.
 */
public class FloatingArrivalPanel extends JPanel {

    private JLabel title;
    private JPanel arrivalsList;
    private JButton closeButton;
    private JPanel content;
    private JScrollPane scrollPane;

    private int maxRows = 8;
    private Runnable onClose;

    // Fade-in
    private float alpha = 1.0f;
    private Timer fadeTimer;
    
    // Dimensioni
    private static final int PANEL_WIDTH = 360;
    private static final int ROW_HEIGHT = 32;
    private static final int HEADER_HEIGHT = 50;
    private static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 16);
    private static final Font ARRIVAL_FONT = new Font("SansSerif", Font.PLAIN, 14);

    public FloatingArrivalPanel() {
        setLayout(null);
        setOpaque(false);

        // Contenitore principale
        content = new JPanel(new BorderLayout());
        content.setBackground(new Color(35, 35, 35));
        content.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(60, 60, 60), 2),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)
        ));
        content.setOpaque(true);

        // Header: titolo + pulsante chiusura
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        title = new JLabel("Arrivi");
        title.setForeground(Color.WHITE);
        title.setFont(TITLE_FONT);
        title.setMaximumSize(new Dimension(PANEL_WIDTH - 60, 30)); // Prevent unbounded expansion

        closeButton = new JButton();
        closeButton.setFocusPainted(false);
        closeButton.setOpaque(true);
        closeButton.setContentAreaFilled(true);
        closeButton.setBackground(new Color(60, 60, 60));
        closeButton.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        closeButton.setPreferredSize(new Dimension(32, 32));
        closeButton.setIcon(new XIcon(14, Color.WHITE));
        closeButton.addActionListener(e -> {
            setVisible(false);
            if (onClose != null) onClose.run();
            stopFade();
            alpha = 1f;
        });

        header.add(title, BorderLayout.CENTER);
        header.add(closeButton, BorderLayout.EAST);

        // Lista arrivi
        arrivalsList = new JPanel();
        arrivalsList.setLayout(new BoxLayout(arrivalsList, BoxLayout.Y_AXIS));
        arrivalsList.setOpaque(false);

        // Scroll
        scrollPane = new JScrollPane(arrivalsList);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        content.add(header, BorderLayout.NORTH);
        content.add(scrollPane, BorderLayout.CENTER);

        add(content);
        setVisible(false);
    }

    public void setOnClose(Runnable r) { this.onClose = r; }
    public void setPreferredRowsMax(int max) { this.maxRows = Math.max(1, max); }

    /** Icon circolare per bullet colorato */
    private static class DotIcon implements Icon {
        private final int size;
        private final Color color;
        DotIcon(int size, Color color) { this.size = size; this.color = color; }
        @Override public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.fillOval(x, y + 2, size, size);
            g2.dispose();
        }
        @Override public int getIconWidth() { return size; }
        @Override public int getIconHeight() { return size; }
    }

    /** Icon che disegna una X */
    private static class XIcon implements Icon {
        private final int size;
        private final Color color;
        XIcon(int size, Color color) { this.size = size; this.color = color; }
        @Override public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(color);
            int pad = 2;
            g2.drawLine(x + pad, y + pad, x + size - pad, y + size - pad);
            g2.drawLine(x + pad, y + size - pad, x + size - pad, y + pad);
            g2.dispose();
        }
        @Override public int getIconWidth() { return size; }
        @Override public int getIconHeight() { return size; }
    }

    // Aggiornamento contenuto e dimensioni
    public void update(String stopName, List<String> arrivi) {
        String safeName = stopName == null ? "" : stopName;
        // Limit title width to prevent pushing close button off-screen
        String displayName = safeName.length() > 25 ? safeName.substring(0, 25) + "..." : safeName;
        title.setText("Arrivi a " + displayName);

        arrivalsList.removeAll();

        for (String a : arrivi) {
            Color dotColor = Color.WHITE;
            String lower = a.toLowerCase();
            if (lower.contains("ritardo")) {
                dotColor = new Color(255, 80, 80); // Rosso chiaro
            } else if (lower.contains("anticipo") || lower.contains("in orario")) {
                dotColor = new Color(80, 200, 80); // Verde chiaro
            } else if (lower.contains("statico")) {
                dotColor = new Color(180, 180, 180); // Grigio chiaro
            }

            JLabel label = new JLabel(a);
            label.setForeground(Color.WHITE);
            label.setFont(ARRIVAL_FONT);
            label.setBorder(BorderFactory.createEmptyBorder(8, 4, 8, 4));
            label.setIcon(new DotIcon(10, dotColor));
            label.setIconTextGap(12);
            arrivalsList.add(label);
        }

        arrivalsList.revalidate();
        arrivalsList.repaint();

        int rows = Math.min(Math.max(arrivi.size(), 1), maxRows);
        int contentHeight = HEADER_HEIGHT + rows * ROW_HEIGHT + 10;

        content.setBounds(0, 0, PANEL_WIDTH, contentHeight);
        scrollPane.setPreferredSize(new Dimension(PANEL_WIDTH - 28, contentHeight - HEADER_HEIGHT));
        content.revalidate();

        revalidate();
        repaint();
    }

    // Animazione: fade-in del pannello
    public void fadeIn(int durationMs, int steps) {
        stopFade();
        alpha = 0f;
        setVisible(true);

        int delay = Math.max(10, durationMs / Math.max(1, steps));
        fadeTimer = new Timer(delay, null);
        fadeTimer.addActionListener(e -> {
            alpha += 1f / steps;
            if (alpha >= 1f) {
                alpha = 1f;
                stopFade();
            }
            repaint();
        });
        fadeTimer.start();
    }

    private void stopFade() {
        if (fadeTimer != null) {
            fadeTimer.stop();
            fadeTimer = null;
        }
    }

    @Override
    public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        super.paint(g2);
        g2.dispose();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (content == null) return;
        Rectangle cb = content.getBounds();
        if (cb.width == 0 || cb.height == 0) return;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Freccia sotto al pannello
        int cx = cb.x + cb.width / 2;
        int arrowW = 18;
        int arrowH = 10;

        Polygon triangle = new Polygon();
        triangle.addPoint(cx - arrowW / 2, cb.y + cb.height);
        triangle.addPoint(cx + arrowW / 2, cb.y + cb.height);
        triangle.addPoint(cx, cb.y + cb.height + arrowH);

        g2.setColor(new Color(60, 60, 60));
        g2.fill(triangle);

        Polygon inner = new Polygon();
        int innerInset = 2;
        inner.addPoint(cx - arrowW / 2 + innerInset, cb.y + cb.height);
        inner.addPoint(cx + arrowW / 2 - innerInset, cb.y + cb.height);
        inner.addPoint(cx, cb.y + cb.height + arrowH - innerInset);

        g2.setColor(content.getBackground());
        g2.fill(inner);

        g2.dispose();
    }

    public Dimension getPreferredPanelSize() {
        Rectangle cb = content.getBounds();
        if (cb.width == 0) return new Dimension(PANEL_WIDTH, 160);
        return new Dimension(cb.width, cb.height + 14);
    }
}
