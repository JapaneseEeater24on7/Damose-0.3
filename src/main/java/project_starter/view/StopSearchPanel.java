package project_starter.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import project_starter.datas.Stops;

/**
 * Pannello laterale per la ricerca di fermate e linee.
 */
public class StopSearchPanel extends JPanel {

    private final JTextField searchField;
    private final DefaultListModel<Stops> stopListModel;
    private final JList<Stops> stopList;
    private final JRadioButton stopsMode;
    private final JRadioButton linesMode;

    public StopSearchPanel() {
        super(new BorderLayout());
        setPreferredSize(new Dimension(280, 700));
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Color.GRAY));
        setBackground(new Color(40, 40, 40));
        setVisible(false);

        // Top panel: search field + mode toggle
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(new Color(40, 40, 40));

        searchField = new JTextField();
        topPanel.add(searchField, BorderLayout.CENTER);

        stopsMode = new JRadioButton("Fermate", true);
        linesMode = new JRadioButton("Linee");
        stopsMode.setBackground(new Color(40, 40, 40));
        stopsMode.setForeground(Color.WHITE);
        linesMode.setBackground(new Color(40, 40, 40));
        linesMode.setForeground(Color.WHITE);

        ButtonGroup group = new ButtonGroup();
        group.add(stopsMode);
        group.add(linesMode);

        JPanel modePanel = new JPanel();
        modePanel.setBackground(new Color(40, 40, 40));
        modePanel.add(stopsMode);
        modePanel.add(linesMode);

        topPanel.add(modePanel, BorderLayout.SOUTH);
        add(topPanel, BorderLayout.NORTH);

        // Stop/Line list
        stopListModel = new DefaultListModel<>();
        stopList = new JList<>(stopListModel);
        stopList.setBackground(new Color(60, 60, 60));
        stopList.setForeground(Color.WHITE);
        add(new JScrollPane(stopList), BorderLayout.CENTER);
    }

    public JTextField getSearchField() { return searchField; }
    public JList<Stops> getStopList() { return stopList; }

    public void clearStopList() { stopListModel.clear(); }
    public void addStopToList(Stops stop) { stopListModel.addElement(stop); }

    public boolean isStopsMode() { return stopsMode.isSelected(); }
    public boolean isLinesMode() { return linesMode.isSelected(); }
}
