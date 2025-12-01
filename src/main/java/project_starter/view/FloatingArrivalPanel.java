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
 * Pannello flottante stile fumetto, con sfondo scuro,
 * scroll per la lista, pulsante di chiusura con X disegnata,
 * e animazione di fade-in all'apertura.
 *
 * Modifica: niente pallino nero fisso; per ogni riga viene mostrato
 * un pallino colorato (rosso ritardo, verde anticipo/in orario, bianco statico).
 */
public class FloatingArrivalPanel extends JPanel {

    private JLabel title;
    private JPanel arrivalsList;
    private JButton closeButton;
    private JPanel content;
    private JScrollPane scrollPane;

    private int maxRows = 8;        // massimo di righe visibili senza scroll
    private Runnable onClose;

    // Fade-in
    private float alpha = 1.0f;     // opacità corrente (0..1)
    private Timer fadeTimer;

    public FloatingArrivalPanel() {
        setLayout(null);
        setOpaque(false);

        // Contenitore principale
        content = new JPanel(new BorderLayout());
        content.setBackground(new Color(40, 40, 40));
        content.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.BLACK, 2),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        content.setOpaque(true);

        // Header: titolo + pulsante chiusura
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        title = new JLabel("Arrivi");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("SansSerif", Font.BOLD, 14));
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 6));

        closeButton = new JButton();
        closeButton.setFocusPainted(false);
        closeButton.setOpaque(true);
        closeButton.setContentAreaFilled(true);
        closeButton.setBackground(new Color(70, 70, 70));
        closeButton.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        closeButton.setPreferredSize(new Dimension(28, 28));
        closeButton.setIcon(new XIcon(12, Color.WHITE)); // X bianca disegnata
        closeButton.addActionListener(e -> {
            setVisible(false);
            if (onClose != null) onClose.run();
            stopFade(); // assicura che l'animazione sia ferma
            alpha = 1f; // reset
        });

        header.add(title, BorderLayout.WEST);
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

    // Callback esterna per chiusura
    public void setOnClose(Runnable r) { this.onClose = r; }

    // Cambia massimo righe visibili (senza scroll)
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
            g2.fillOval(x, y, size, size);
            g2.dispose();
        }
        @Override public int getIconWidth() { return size; }
        @Override public int getIconHeight() { return size; }
    }

    /** Icon che disegna una X (bianca) */
    private static class XIcon implements Icon {
        private final int size;
        private final Color color;
        XIcon(int size, Color color) { this.size = size; this.color = color; }
        @Override public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(color);
            int pad = 1;
            int x1 = x + pad;
            int y1 = y + pad;
            int x2 = x + size - pad;
            int y2 = y + size - pad;
            g2.drawLine(x1, y1, x2, y2);
            g2.drawLine(x1, y2, x2, y1);
            g2.dispose();
        }
        @Override public int getIconWidth() { return size; }
        @Override public int getIconHeight() { return size; }
    }

    // Aggiornamento contenuto e dimensioni
    public void update(String stopName, List<String> arrivi) {
        String safeName = stopName == null ? "" : stopName;
        title.setText("<html><div style='width:180px;'>Arrivi a " + safeName + "</div></html>");

        arrivalsList.removeAll();

        for (String a : arrivi) {
            // decide colore del pallino in base al testo
            Color dotColor = Color.WHITE; // default statico
            String lower = a.toLowerCase();
            if (lower.contains("ritardo")) {
                dotColor = Color.RED;
            } else if (lower.contains("anticipo") || lower.contains("in orario")) {
                dotColor = Color.GREEN;
            } else if (lower.contains("statico")) {
                dotColor = Color.WHITE;
            }

            JLabel label = new JLabel(a);
            label.setForeground(Color.WHITE);
            label.setFont(new Font("SansSerif", Font.PLAIN, 13));
            label.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
            label.setIcon(new DotIcon(8, dotColor));
            label.setIconTextGap(8);
            arrivalsList.add(label);
        }

        arrivalsList.revalidate();
        arrivalsList.repaint();

        int rows = Math.min(Math.max(arrivi.size(), 1), maxRows);
        int rowHeight = 24;
        int headerHeight = 40;
        int contentHeight = headerHeight + rows * rowHeight;
        int width = 280;

        content.setBounds(0, 0, width, contentHeight);
        scrollPane.setPreferredSize(new Dimension(width - 20, contentHeight - headerHeight));
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

    // Applica alpha a tutto il pannello (inclusi figli) per un fade uniforme
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

        // Freccia sotto al pannello (triangolo), bordo nero + riempimento colore content
        int cx = cb.x + cb.width / 2;
        int arrowW = 16;
        int arrowH = 8;

        Polygon triangle = new Polygon();
        triangle.addPoint(cx - arrowW / 2, cb.y + cb.height);
        triangle.addPoint(cx + arrowW / 2, cb.y + cb.height);
        triangle.addPoint(cx, cb.y + cb.height + arrowH);

        g2.setColor(Color.BLACK);
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

    // utile per posizionare il pannello: dimensione totale (content + freccia)
    public Dimension getPreferredPanelSize() {
        Rectangle cb = content.getBounds();
        if (cb.width == 0) return new Dimension(280, 140);
        return new Dimension(cb.width, cb.height + 12);
    }
}
