package org.bidib.switchboard.component.view;

import java.awt.Component;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.bidib.switchboard.component.model.Element;
import org.bidib.switchboard.component.model.Occupancy;
import org.bidib.switchboard.component.model.RailwayModel;

import com.jgoodies.forms.builder.FormBuilder;

public class AssignOccupancyDialog {

    private static final Logger LOGGER = LoggerFactory.getLogger(AssignOccupancyDialog.class);

    public void show(Component parent, RailwayModel model, Element el) {
        Occupancy existing = el.getOccupancy();

        DefaultComboBoxModel<Occupancy> comboModel = new DefaultComboBoxModel<>();
        comboModel.addElement(null);
        for (Occupancy occ : model.getOccupancies().values()) {
            comboModel.addElement(occ);
        }
        final JComboBox<Occupancy> occupancyCombo = new JComboBox<>(comboModel);
        occupancyCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Occupancy occ) {
                    setText(occ.getId() + " (" + occ.getState() + ")");
                } else {
                    setText(" ");
                }
                return this;
            }
        });
        if (existing != null) {
            for (int i = 0; i < occupancyCombo.getItemCount(); i++) {
                Occupancy occ = occupancyCombo.getItemAt(i);
                if (occ != null && occ.getId().equals(existing.getId())) {
                    occupancyCombo.setSelectedIndex(i);
                    break;
                }
            }
        }

        FormBuilder builder = FormBuilder.create().columns("right:pref, 3dlu, 60dlu:grow").rows("pref");
        builder.addLabel("Occupancy:").xy(1, 1);
        builder.add(occupancyCombo).xy(3, 1);
        JPanel panel = builder.getPanel();

        int result = JOptionPane.showConfirmDialog(parent, panel, "Assign Occupancy", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            Occupancy selected = (Occupancy) occupancyCombo.getSelectedItem();
            el.setOccupancy(selected);
            LOGGER.info("Assigned occupancy {}", selected);
        }
    }
}
