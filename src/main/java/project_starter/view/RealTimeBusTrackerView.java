package project_starter.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.viewer.GeoPosition;

import project_starter.datas.Stops;

/**
 * View principale dell'applicazione Rome Bus Tracker.
 */
public class RealTimeBusTrackerView {

    private JFrame frame;
    private JXMapViewer mapViewer;
    private JButton searchButton;
    private StopSearchPanel sidePanel;
    private JPanel overlayPanel;
    private FloatingArrivalPanel floatingPanel;
    private GeoPosition floatingAnchorGeo;
    private List<Stops> allStopsCache = new ArrayList<>();

    private final PropertyChangeListener mapListener = evt -> {
        String name = evt.getPropertyName();
        if ("zoom".equals(name) || "center".equals(name) || "tileFactory".equals(name)) {
            updateFloatingPanelPosition();
        }
    };

    // -------- ACCESSORI --------
    public JTextField getSearchField() { return sidePanel.getSearchField(); }
    public JList<Stops> getStopList() { return sidePanel.getStopList(); }
    public void clearStopList() { sidePanel.clearStopList(); }
    public void addStopToList(Stops stop) { sidePanel.addStopToList(stop); }

    public void toggleSidePanel() {
        if (sidePanel != null) sidePanel.setVisible(!sidePanel.isVisible());
    }

    public JButton getSearchButton() { return searchButton; }
    public JXMapViewer getMapViewer() { return mapViewer; }

    public boolean isStopsMode() { return sidePanel.isStopsMode(); }
    public boolean isLinesMode() { return sidePanel.isLinesMode(); }

    public void setFloatingPanelMaxRows(int maxRows) {
        if (floatingPanel != null) {
            floatingPanel.setPreferredRowsMax(maxRows);
        }
    }

    // -------- INIT --------
    public void init() {
        try {
            UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatDarkLaf());
        } catch (Exception ignored) {}

        UIManager.put("Button.arc", 20);
        UIManager.put("TextField.arc", 15);

        frame = new JFrame("Rome Bus Tracker");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.setSize(900, 700);
        frame.setLocationRelativeTo(null);

        mapViewer = MapFactory.createMapViewer();

        JLayeredPane layeredPane = new JLayeredPane();
        frame.add(layeredPane, BorderLayout.CENTER);

        mapViewer.setBounds(0, 0, 900, 700);
        layeredPane.add(mapViewer, JLayeredPane.DEFAULT_LAYER);

        overlayPanel = new JPanel(null);
        overlayPanel.setOpaque(false);
        overlayPanel.setBounds(0, 0, 900, 700);
        layeredPane.add(overlayPanel, JLayeredPane.PALETTE_LAYER);

        ImageIcon lensIcon = new ImageIcon(getClass().getResource("/sprites/lente.png"));
        Image scaled = lensIcon.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
        searchButton = new JButton(new ImageIcon(scaled));
        searchButton.setContentAreaFilled(false);
        searchButton.setBorderPainted(false);
        searchButton.setBounds(15, 15, 40, 40);
        overlayPanel.add(searchButton);

        layeredPane.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int w = layeredPane.getWidth();
                int h = layeredPane.getHeight();
                mapViewer.setBounds(0, 0, w, h);
                overlayPanel.setBounds(0, 0, w, h);
                updateFloatingPanelPosition();
            }
        });

        sidePanel = new StopSearchPanel();
        frame.add(sidePanel, BorderLayout.WEST);

        floatingPanel = new FloatingArrivalPanel();
        floatingPanel.setVisible(false);
        floatingPanel.setOnClose(() -> floatingAnchorGeo = null);
        overlayPanel.add(floatingPanel);

        mapViewer.addPropertyChangeListener(mapListener);
        setFloatingPanelMaxRows(10);

        frame.setVisible(true);
    }

    // -------- CLICK & DISTANZA --------
    public void addWaypointClickListener() {
        mapViewer.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (allStopsCache.isEmpty()) return;

                int x = e.getX();
                int y = e.getY();

                GeoPosition clickedPos = mapViewer.convertPointToGeoPosition(e.getPoint());
                Stops nearest = findNearestStop(clickedPos);

                if (nearest != null && GeoUtils.isClickCloseToStop(mapViewer, nearest, x, y)) {
                    if (stopClickListener != null) {
                        stopClickListener.onStopClicked(nearest);
                    }
                }
            }
        });
    }

    private Stops findNearestStop(GeoPosition pos) {
        double minDist = Double.MAX_VALUE;
        Stops nearest = null;

        for (Stops s : allStopsCache) {
            double d = GeoUtils.haversine(
                    pos.getLatitude(), pos.getLongitude(),
                    s.getStopLat(), s.getStopLon()
            );
            if (d < minDist) {
                minDist = d;
                nearest = s;
            }
        }
        return nearest;
    }

    // -------- STOP CLICK LISTENER --------
    public interface StopClickListener {
        void onStopClicked(Stops stop);
    }

    private StopClickListener stopClickListener;
    public void setStopClickListener(StopClickListener listener) {
        this.stopClickListener = listener;
    }

    public void setAllStops(List<Stops> stops) {
        this.allStopsCache = stops;
    }

    // -------- FLOATING PANEL --------
    public void showFloatingPanel(String stopName, List<String> arrivi, Point2D pos, GeoPosition anchorGeo) {
        floatingPanel.update(stopName, arrivi);
        this.floatingAnchorGeo = anchorGeo;

        Point2D p = pos;
        if (p == null && anchorGeo != null) {
            p = mapViewer.convertGeoPositionToPoint(anchorGeo);
        }

        if (p != null) {
            Dimension pref = floatingPanel.getPreferredPanelSize();
            int panelWidth = pref.width;
            int panelHeight = pref.height;
            int x = (int) p.getX() - panelWidth / 2;
            int y = (int) p.getY() - panelHeight - 8;

            int maxX = Math.max(10, mapViewer.getWidth() - panelWidth - 10);
            int maxY = Math.max(10, mapViewer.getHeight() - panelHeight - 10);
            x = Math.max(10, Math.min(x, maxX));
            y = Math.max(10, Math.min(y, maxY));

            floatingPanel.setBounds(x, y, panelWidth, panelHeight);
        }

        floatingPanel.revalidate();
        floatingPanel.repaint();
        floatingPanel.fadeIn(300, 15);
    }

    public void showFloatingPanel(String stopName, List<String> arrivi, Point2D pos) {
        showFloatingPanel(stopName, arrivi, pos, null);
    }

    public void hideFloatingPanel() {
        floatingPanel.setVisible(false);
        floatingAnchorGeo = null;
    }

    private void updateFloatingPanelPosition() {
        if (!floatingPanel.isVisible() || floatingAnchorGeo == null) return;

        Point2D p2d = mapViewer.convertGeoPositionToPoint(floatingAnchorGeo);
        if (p2d == null) return;

        Dimension pref = floatingPanel.getPreferredPanelSize();
        int panelWidth = pref.width;
        int panelHeight = pref.height;
        int x = (int) p2d.getX() - panelWidth / 2;
        int y = (int) p2d.getY() - panelHeight - 8;

        int maxX = Math.max(10, mapViewer.getWidth() - panelWidth - 10);
        int maxY = Math.max(10, mapViewer.getHeight() - panelHeight - 10);
        x = Math.max(10, Math.min(x, maxX));
        y = Math.max(10, Math.min(y, maxY));

        floatingPanel.setBounds(x, y, panelWidth, panelHeight);
        floatingPanel.revalidate();
        floatingPanel.repaint();
    }
}
