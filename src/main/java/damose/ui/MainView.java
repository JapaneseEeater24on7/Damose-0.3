package damose.ui;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.UIManager;

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.viewer.GeoPosition;

import damose.config.AppConstants;
import damose.data.model.Stop;
import damose.database.SessionManager;
import damose.ui.component.FloatingArrivalPanel;
import damose.ui.component.SearchOverlay;
import damose.ui.map.GeoUtils;
import damose.ui.map.MapFactory;

/**
 * Main application view - Midnight Dark style.
 */
public class MainView {

    private JFrame frame;
    private JXMapViewer mapViewer;
    private JButton searchButton;
    private SearchOverlay searchOverlay;
    private JPanel overlayPanel;
    private FloatingArrivalPanel floatingPanel;
    private GeoPosition floatingAnchorGeo;
    private List<Stop> allStopsCache = new ArrayList<>();
    private List<Stop> allLinesCache = new ArrayList<>();

    private Point dragOffset;

    private final PropertyChangeListener mapListener = evt -> {
        String name = evt.getPropertyName();
        if ("zoom".equals(name) || "center".equals(name) || "tileFactory".equals(name)) {
            updateFloatingPanelPosition();
        }
    };

    public void showSearchOverlay() {
        if (searchOverlay != null) searchOverlay.showSearch();
    }

    public void setSearchData(List<Stop> stops, List<Stop> lines) {
        this.allLinesCache = lines != null ? lines : new ArrayList<>();
        if (searchOverlay != null) {
            searchOverlay.setData(stops, lines);
        }
    }

    public void setOnSearchSelect(java.util.function.Consumer<Stop> callback) {
        if (searchOverlay != null) {
            searchOverlay.setOnSelect(callback);
        }
    }

    public JButton getSearchButton() {
        return searchButton;
    }

    public JXMapViewer getMapViewer() {
        return mapViewer;
    }

    public void setFloatingPanelMaxRows(int maxRows) {
        if (floatingPanel != null) {
            floatingPanel.setPreferredRowsMax(maxRows);
        }
    }

    public void init() {
        try {
            UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatDarkLaf());
        } catch (Exception ignored) {
        }

        UIManager.put("Button.arc", 20);
        UIManager.put("TextField.arc", 15);

        frame = new JFrame("Damose");
        frame.setUndecorated(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1100, 750);
        frame.setLocationRelativeTo(null);
        frame.setBackground(AppConstants.BG_DARK);
        
        // Set app icon
        try {
            ImageIcon icon = new ImageIcon(getClass().getResource("/sprites/icon.png"));
            List<Image> icons = new ArrayList<>();
            icons.add(icon.getImage());
            frame.setIconImages(icons);
        } catch (Exception e) {
            System.out.println("Could not load app icon: " + e.getMessage());
        }

        JPanel mainContainer = new JPanel(new BorderLayout());
        mainContainer.setBackground(AppConstants.BG_DARK);

        JPanel titleBar = createTitleBar();
        mainContainer.add(titleBar, BorderLayout.NORTH);

        JPanel contentArea = new JPanel(new BorderLayout());
        contentArea.setBackground(AppConstants.BG_DARK);

        mapViewer = MapFactory.createMapViewer();

        JLayeredPane layeredPane = new JLayeredPane();
        contentArea.add(layeredPane, BorderLayout.CENTER);

        mapViewer.setBounds(0, 0, 1100, 700);
        layeredPane.add(mapViewer, JLayeredPane.DEFAULT_LAYER);

        overlayPanel = new JPanel(null);
        overlayPanel.setOpaque(false);
        overlayPanel.setBounds(0, 0, 1100, 700);
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
                if (searchOverlay != null) {
                    searchOverlay.setBounds(0, 0, w, h);
                }
                updateFloatingPanelPosition();
            }
        });

        floatingPanel = new FloatingArrivalPanel();
        floatingPanel.setVisible(false);
        floatingPanel.setOnClose(() -> floatingAnchorGeo = null);
        overlayPanel.add(floatingPanel);

        searchOverlay = new SearchOverlay();
        searchOverlay.setVisible(false);
        searchOverlay.setBounds(0, 0, 1100, 700);
        layeredPane.add(searchOverlay, JLayeredPane.POPUP_LAYER);

        mainContainer.add(contentArea, BorderLayout.CENTER);
        frame.setContentPane(mainContainer);

        mapViewer.addPropertyChangeListener(mapListener);
        setFloatingPanelMaxRows(10);

        frame.setVisible(true);
    }

    private JPanel createTitleBar() {
        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setBackground(AppConstants.BG_DARK);
        titleBar.setPreferredSize(new Dimension(1100, 38));
        titleBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, AppConstants.BORDER_COLOR));

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        leftPanel.setOpaque(false);
        leftPanel.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));

        // Add app icon
        try {
            ImageIcon rawIcon = new ImageIcon(getClass().getResource("/sprites/icon.png"));
            Image scaled = rawIcon.getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH);
            JLabel iconLabel = new JLabel(new ImageIcon(scaled));
            leftPanel.add(iconLabel);
        } catch (Exception e) {
            // Icon not found
        }

        JLabel titleLabel = new JLabel("Damose");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleLabel.setForeground(AppConstants.TEXT_PRIMARY);
        leftPanel.add(titleLabel);

        if (SessionManager.isLoggedIn()) {
            JLabel userLabel = new JLabel("  |  " + SessionManager.getCurrentUser().getUsername());
            userLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            userLabel.setForeground(AppConstants.TEXT_SECONDARY);
            leftPanel.add(userLabel);
        }

        titleBar.add(leftPanel, BorderLayout.WEST);

        titleBar.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                dragOffset = e.getPoint();
            }
        });
        titleBar.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                Point loc = frame.getLocation();
                frame.setLocation(loc.x + e.getX() - dragOffset.x, loc.y + e.getY() - dragOffset.y);
            }
        });

        JPanel controlPanel = new JPanel();
        controlPanel.setOpaque(false);
        controlPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 4));

        JButton minBtn = createWindowButton("-");
        minBtn.addActionListener(e -> frame.setState(JFrame.ICONIFIED));
        controlPanel.add(minBtn);

        JButton maxBtn = createWindowButton("o");
        maxBtn.addActionListener(e -> {
            if (frame.getExtendedState() == JFrame.MAXIMIZED_BOTH) {
                frame.setExtendedState(JFrame.NORMAL);
            } else {
                frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            }
        });
        controlPanel.add(maxBtn);

        JButton closeBtn = createWindowButton("x");
        closeBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { closeBtn.setForeground(AppConstants.ERROR_COLOR); }
            public void mouseExited(MouseEvent e) { closeBtn.setForeground(AppConstants.TEXT_SECONDARY); }
        });
        closeBtn.addActionListener(e -> System.exit(0));
        controlPanel.add(closeBtn);

        titleBar.add(controlPanel, BorderLayout.EAST);
        return titleBar;
    }

    private JButton createWindowButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btn.setForeground(AppConstants.TEXT_SECONDARY);
        btn.setPreferredSize(new Dimension(46, 32));
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                if (!text.equals("x")) btn.setForeground(AppConstants.TEXT_PRIMARY);
            }
            public void mouseExited(MouseEvent e) {
                if (!text.equals("x")) btn.setForeground(AppConstants.TEXT_SECONDARY);
            }
        });
        return btn;
    }

    public void addWaypointClickListener() {
        mapViewer.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (allStopsCache.isEmpty()) return;

                int x = e.getX();
                int y = e.getY();

                GeoPosition clickedPos = mapViewer.convertPointToGeoPosition(e.getPoint());
                Stop nearest = findNearestStop(clickedPos);

                if (nearest != null && GeoUtils.isClickCloseToStop(mapViewer, nearest, x, y)) {
                    if (stopClickListener != null) {
                        stopClickListener.onStopClicked(nearest);
                    }
                }
            }
        });
    }

    private Stop findNearestStop(GeoPosition pos) {
        double minDist = Double.MAX_VALUE;
        Stop nearest = null;

        for (Stop s : allStopsCache) {
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

    public interface StopClickListener {
        void onStopClicked(Stop stop);
    }

    private StopClickListener stopClickListener;

    public void setStopClickListener(StopClickListener listener) {
        this.stopClickListener = listener;
    }

    public void setAllStops(List<Stop> stops) {
        this.allStopsCache = stops;
    }

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

