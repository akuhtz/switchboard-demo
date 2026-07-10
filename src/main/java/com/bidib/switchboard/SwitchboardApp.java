package com.bidib.switchboard;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import com.bidib.switchboard.model.RailwayModel;
import com.bidib.switchboard.model.SignalTile;
import com.bidib.switchboard.model.Tile;
import com.bidib.switchboard.model.TurnoutTile;
import com.bidib.switchboard.view.SwitchboardPanel;

/**
 * Main application class that wires the model and view together and launches the tile-based switchboard window.
 */
public class SwitchboardApp {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SwitchboardApp::createAndShowGui);
    }

    private static void createAndShowGui() {
        // --- Model ---
        RailwayModel model = new RailwayModel();
        model.addTurnout("W1");          // 2-way turnout
        model.addTurnout("W2");          // 2-way turnout
        model.addTurnout("W3", 3);       // 3-way turnout
        model.addSignal("S1");           // 2-aspect signal
        model.addSignal("S2", 3);        // 3-aspect signal

        // --- View (60x30 grid, 32x32 px tiles) ---
        SwitchboardPanel panel = new SwitchboardPanel(model);

        // 2-way turnout tiles (straight / diverted_left)
        panel.setTile(new TurnoutTile(2, 3, "W1", "/icons/turnout_straight.svg", "/icons/turnout_diverted_left.svg"));
        panel.setTile(new TurnoutTile(3, 3, "W2", "/icons/turnout_straight.svg", "/icons/turnout_diverted_left.svg"));

        // 3-way turnout tile (straight / diverted_left / diverted_right)
        panel.setTile(new TurnoutTile(4, 3, "W3", "/icons/turnout_straight.svg", "/icons/turnout_diverted_left.svg", "/icons/turnout_diverted_right.svg"));

        // Signal tiles (2-aspect: red/green, click to cycle)
        panel.setTile(new SignalTile(10, 3, "S1", "/icons/signal_red.svg", "/icons/signal_green.svg"));
        panel.setTile(new SignalTile(11, 3, "S2", "/icons/signal_red.svg", "/icons/signal_yellow.svg", "/icons/signal_green.svg"));

        // Fill some empty tiles for demonstration
        for (int col = 0; col < 5; col++) {
            panel.setTile(new Tile(col, 0, null, "/icons/empty.svg"));
        }

        // --- Frame with scroll support (board is 1920x960) ---
        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(32);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(32);

        JFrame frame = new JFrame("Model Railway Switchboard");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(scrollPane);
        frame.setSize(1024, 768);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
