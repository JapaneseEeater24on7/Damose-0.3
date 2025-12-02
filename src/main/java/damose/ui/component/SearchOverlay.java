package damose.ui.component;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import damose.config.AppConstants;
import damose.data.model.Stop;

/**
 * Spotlight-style search overlay.
 */
public class SearchOverlay extends JPanel {

    private final JTextField searchField;
    private final DefaultListModel<Stop> listModel;
    private final JList<Stop> resultList;
    private final JPanel contentPanel;
    private final JLabel stopsModeBtn;
    private final JLabel linesModeBtn;

    private boolean stopsMode = true;
    private List<Stop> allStops = new ArrayList<>();
    private List<Stop> allLines = new ArrayList<>();
    private Consumer<Stop> onSelect;

    public SearchOverlay() {
        setLayout(null);
        setOpaque(false);

        contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(AppConstants.BG_MEDIUM);
        contentPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(AppConstants.BORDER_COLOR, 1),
            new EmptyBorder(16, 16, 16, 16)
        ));

        JPanel modePanel = new JPanel();
        modePanel.setOpaque(false);
        modePanel.setBorder(new EmptyBorder(0, 0, 12, 0));

        stopsModeBtn = createModeButton("Fermate", true);
        linesModeBtn = createModeButton("Linee", false);

        stopsModeBtn.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (!stopsMode) {
                    stopsMode = true;
                    updateModeButtons();
                    filterResults();
                }
            }
        });
        linesModeBtn.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (stopsMode) {
                    stopsMode = false;
                    updateModeButtons();
                    filterResults();
                }
            }
        });

        modePanel.add(stopsModeBtn);
        modePanel.add(Box.createHorizontalStrut(8));
        modePanel.add(linesModeBtn);

        searchField = new JTextField();
        searchField.setBackground(AppConstants.BG_FIELD);
        searchField.setForeground(AppConstants.TEXT_PRIMARY);
        searchField.setCaretColor(AppConstants.TEXT_PRIMARY);
        searchField.setFont(AppConstants.FONT_BODY);
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(AppConstants.BORDER_COLOR),
            new EmptyBorder(12, 14, 12, 14)
        ));
        searchField.setPreferredSize(new Dimension(468, 48));

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filterResults(); }
            public void removeUpdate(DocumentEvent e) { filterResults(); }
            public void changedUpdate(DocumentEvent e) { filterResults(); }
        });

        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int code = e.getKeyCode();
                if (code == KeyEvent.VK_ESCAPE) {
                    closeOverlay();
                } else if (code == KeyEvent.VK_ENTER) {
                    e.consume();
                    selectCurrentAndClose();
                } else if (code == KeyEvent.VK_DOWN) {
                    e.consume();
                    moveSelection(1);
                } else if (code == KeyEvent.VK_UP) {
                    e.consume();
                    moveSelection(-1);
                } else if (code == KeyEvent.VK_TAB) {
                    e.consume();
                    stopsMode = !stopsMode;
                    updateModeButtons();
                    filterResults();
                }
            }
        });

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.add(modePanel, BorderLayout.NORTH);
        topPanel.add(searchField, BorderLayout.CENTER);
        contentPanel.add(topPanel, BorderLayout.NORTH);

        listModel = new DefaultListModel<>();
        resultList = new JList<>(listModel);
        resultList.setBackground(AppConstants.LIST_BG);
        resultList.setForeground(AppConstants.TEXT_PRIMARY);
        resultList.setSelectionBackground(AppConstants.ACCENT);
        resultList.setSelectionForeground(Color.WHITE);
        resultList.setFont(AppConstants.FONT_BODY);
        resultList.setFixedCellHeight(48);
        resultList.setCellRenderer(new StopCellRenderer());

        resultList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    e.consume();
                    selectCurrentAndClose();
                }
            }
        });

        resultList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    selectCurrentAndClose();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(resultList);
        scrollPane.setBorder(BorderFactory.createLineBorder(AppConstants.BORDER_COLOR));
        scrollPane.setPreferredSize(new Dimension(468, 280));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        JPanel listPanel = new JPanel(new BorderLayout());
        listPanel.setOpaque(false);
        listPanel.setBorder(new EmptyBorder(12, 0, 0, 0));
        listPanel.add(scrollPane, BorderLayout.CENTER);
        contentPanel.add(listPanel, BorderLayout.CENTER);

        JLabel hint = new JLabel("Tab = cambia modo | Enter = seleziona | Esc = chiudi");
        hint.setFont(AppConstants.FONT_HINT);
        hint.setForeground(AppConstants.TEXT_SECONDARY);
        hint.setBorder(new EmptyBorder(10, 0, 0, 0));
        contentPanel.add(hint, BorderLayout.SOUTH);

        add(contentPanel);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!contentPanel.getBounds().contains(e.getPoint())) {
                    closeOverlay();
                }
            }
        });
    }

    private JLabel createModeButton(String text, boolean selected) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        label.setOpaque(true);
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        label.setBorder(new EmptyBorder(8, 20, 8, 20));
        if (selected) {
            label.setBackground(AppConstants.ACCENT);
            label.setForeground(Color.WHITE);
        } else {
            label.setBackground(AppConstants.BG_FIELD);
            label.setForeground(AppConstants.TEXT_SECONDARY);
        }
        return label;
    }

    private void updateModeButtons() {
        if (stopsMode) {
            stopsModeBtn.setBackground(AppConstants.ACCENT);
            stopsModeBtn.setForeground(Color.WHITE);
            linesModeBtn.setBackground(AppConstants.BG_FIELD);
            linesModeBtn.setForeground(AppConstants.TEXT_SECONDARY);
        } else {
            stopsModeBtn.setBackground(AppConstants.BG_FIELD);
            stopsModeBtn.setForeground(AppConstants.TEXT_SECONDARY);
            linesModeBtn.setBackground(AppConstants.ACCENT);
            linesModeBtn.setForeground(Color.WHITE);
        }
    }

    private void filterResults() {
        String query = searchField.getText().toLowerCase().trim();
        listModel.clear();

        List<Stop> source = stopsMode ? allStops : allLines;
        int count = 0;

        for (Stop s : source) {
            if (count >= 50) break;
            String name = s.getStopName().toLowerCase();
            String id = s.getStopId().toLowerCase();
            if (query.isEmpty() || name.contains(query) || id.contains(query)) {
                listModel.addElement(s);
                count++;
            }
        }

        if (!listModel.isEmpty()) {
            resultList.setSelectedIndex(0);
        }
    }

    private void moveSelection(int delta) {
        int idx = resultList.getSelectedIndex();
        int newIdx = idx + delta;
        if (newIdx >= 0 && newIdx < listModel.size()) {
            resultList.setSelectedIndex(newIdx);
            resultList.ensureIndexIsVisible(newIdx);
        }
    }

    private void selectCurrentAndClose() {
        Stop selected = resultList.getSelectedValue();
        if (selected != null) {
            Consumer<Stop> callback = onSelect;
            closeOverlay();
            if (callback != null) {
                SwingUtilities.invokeLater(() -> callback.accept(selected));
            }
        }
    }

    private void closeOverlay() {
        setVisible(false);
    }

    public void setData(List<Stop> stops, List<Stop> lines) {
        this.allStops = stops != null ? new ArrayList<>(stops) : new ArrayList<>();
        this.allLines = lines != null ? new ArrayList<>(lines) : new ArrayList<>();
    }

    public void setOnSelect(Consumer<Stop> callback) {
        this.onSelect = callback;
    }

    public void showSearch() {
        searchField.setText("");
        stopsMode = true;
        updateModeButtons();
        filterResults();
        setVisible(true);

        int panelW = 500;
        int panelH = 420;
        int x = (getWidth() - panelW) / 2;
        int y = (getHeight() - panelH) / 3;
        contentPanel.setBounds(x, y, panelW, panelH);

        SwingUtilities.invokeLater(() -> searchField.requestFocusInWindow());
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        if (contentPanel != null && isVisible()) {
            int panelW = 500;
            int panelH = 420;
            int px = (width - panelW) / 2;
            int py = (height - panelH) / 3;
            contentPanel.setBounds(px, py, panelW, panelH);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(new Color(0, 0, 0, 120));
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.dispose();
    }

    @Override
    public boolean isOpaque() {
        return false;
    }

    private class StopCellRenderer extends JPanel implements ListCellRenderer<Stop> {
        private final JLabel nameLabel;
        private final JLabel idLabel;

        public StopCellRenderer() {
            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(8, 14, 8, 14));

            JPanel textPanel = new JPanel();
            textPanel.setOpaque(false);
            textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));

            nameLabel = new JLabel();
            nameLabel.setFont(AppConstants.FONT_BODY);

            idLabel = new JLabel();
            idLabel.setFont(AppConstants.FONT_HINT);

            textPanel.add(nameLabel);
            textPanel.add(Box.createVerticalStrut(2));
            textPanel.add(idLabel);

            add(textPanel, BorderLayout.CENTER);
        }

        @Override
        public java.awt.Component getListCellRendererComponent(JList<? extends Stop> list,
                Stop value, int index, boolean isSelected, boolean cellHasFocus) {

            String name = value.getStopName();
            if (name.length() > 50) name = name.substring(0, 50) + "...";
            nameLabel.setText(name);
            idLabel.setText(value.isFakeLine() ? "Linea bus" : "Stop ID: " + value.getStopId());

            if (isSelected) {
                setBackground(AppConstants.ACCENT);
                nameLabel.setForeground(Color.WHITE);
                idLabel.setForeground(new Color(220, 220, 220));
            } else {
                setBackground(AppConstants.LIST_BG);
                nameLabel.setForeground(AppConstants.TEXT_PRIMARY);
                idLabel.setForeground(AppConstants.TEXT_SECONDARY);
            }

            return this;
        }
    }
}

