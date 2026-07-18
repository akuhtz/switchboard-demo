package com.bidib.switchboard.view;

import java.awt.Component;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.jgoodies.forms.builder.FormBuilder;

import com.bidib.switchboard.model.Element;
import com.bidib.switchboard.model.Occupancy;
import com.bidib.switchboard.model.RailwayModel;

public class AssignOccupancyDialog {

    private AssignOccupancyDialog() {
    }

    public static void show(Component parent, RailwayModel model, Element el) {
        JTextField nodeIdField = new JTextField("0", 10);
        JTextField portIdField = new JTextField("0", 10);

        FormBuilder builder = FormBuilder.create()
            .columns("right:pref, 3dlu, 60dlu:grow")
            .rows("pref, 3dlu, pref");
        builder.addLabel("Node ID:").xy(1, 1);
        builder.add(nodeIdField).xy(3, 1);
        builder.addLabel("Port ID:").xy(1, 3);
        builder.add(portIdField).xy(3, 3);
        JPanel panel = builder.getPanel();

        int result = JOptionPane.showConfirmDialog(parent, panel, "Assign Occupancy",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            try {
                long nodeId = Long.parseLong(nodeIdField.getText().trim());
                int portId = Integer.parseInt(portIdField.getText().trim());
                Occupancy occ = Occupancy.create(nodeId, portId);
                model.addOccupancy(occ);
                el.setOccupancy(occ);
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(parent, "Invalid number", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
