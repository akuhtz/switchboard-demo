package org.bidib.switchboard.demoapp.view;

import java.awt.Component;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.bidib.switchboard.component.model.Element;
import org.bidib.switchboard.component.model.Occupancy;
import org.bidib.switchboard.component.model.RailwayModel;
import org.bidib.switchboard.component.view.AssignOccupancyDialogFactory;
import org.bidib.switchboard.component.view.InputValidationDocument;
import org.bidib.switchboard.component.view.StringToIntegerConverter;
import org.bidib.switchboard.component.view.StringToUnsignedLongConverter;
import org.bidib.switchboard.demoapp.config.DemoOccupancy;

import com.jgoodies.binding.PresentationModel;
import com.jgoodies.binding.adapter.Bindings;
import com.jgoodies.binding.beans.Model;
import com.jgoodies.binding.value.BufferedValueModel;
import com.jgoodies.binding.value.ConverterValueModel;
import com.jgoodies.binding.value.Trigger;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.FormBuilder;

public class DemoAssignOccupancyDialogFactory implements AssignOccupancyDialogFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(DemoAssignOccupancyDialogFactory.class);

    @Override
    public void showAssignOccupancyDialog(Component parent, RailwayModel model, Element el) {
        final OccupancyBean bean = new OccupancyBean();
        final Occupancy existing = el.getOccupancy();
        if (existing instanceof DemoOccupancy d) {
            bean.setNodeId(d.getNodeId());
            bean.setPortId(d.getPortId());
        }

        Trigger trigger = new Trigger();
        PresentationModel<OccupancyBean> pm = new PresentationModel<>(bean, trigger);

        BufferedValueModel nodeIdModel = pm.getBufferedModel("nodeId");
        BufferedValueModel portIdModel = pm.getBufferedModel("portId");

        final ValueModel nodeIdConverterModel = new ConverterValueModel(nodeIdModel, new StringToUnsignedLongConverter());
        final ValueModel portIdConverterModel = new ConverterValueModel(portIdModel, new StringToIntegerConverter());

        final JTextField nodeIdField = new JTextField();
        nodeIdField.setDocument(new InputValidationDocument(5, InputValidationDocument.NUMERIC));
        Bindings.bind(nodeIdField, nodeIdConverterModel, false);
        nodeIdField.setEnabled(false);

        final JTextField portIdField = new JTextField();
        portIdField.setDocument(new InputValidationDocument(5, InputValidationDocument.NUMERIC));
        Bindings.bind(portIdField, portIdConverterModel, false);
        portIdField.setEnabled(false);

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
                if (value instanceof DemoOccupancy d) {
                    setText(d.getNodeId() + ":" + d.getPortId());
                } else if (value instanceof Occupancy occ) {
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
        occupancyCombo.addActionListener(e -> {
            Occupancy selected = (Occupancy) occupancyCombo.getSelectedItem();
            if (selected instanceof DemoOccupancy d) {
                nodeIdModel.setValue(d.getNodeId());
                portIdModel.setValue(d.getPortId());
            } else {
                nodeIdModel.setValue(null);
                portIdModel.setValue(null);
            }
        });

        FormBuilder builder = FormBuilder.create().columns("right:pref, 3dlu, 60dlu:grow")
                .rows("pref, 3dlu, pref, 3dlu, pref");
        builder.addLabel("Occupancy:").xy(1, 1);
        builder.add(occupancyCombo).xy(3, 1);
        builder.addLabel("Node ID:").xy(1, 3);
        builder.add(nodeIdField).xy(3, 3);
        builder.addLabel("Port ID:").xy(1, 5);
        builder.add(portIdField).xy(3, 5);
        JPanel panel = builder.getPanel();

        int result = JOptionPane.showConfirmDialog(parent, panel, "Assign Occupancy",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            trigger.triggerCommit();
            Occupancy selected = (Occupancy) occupancyCombo.getSelectedItem();
            el.setOccupancy(selected);
            LOGGER.info("Assigned occupancy {}", selected);
        } else {
            trigger.triggerFlush();
        }
    }

    public static class OccupancyBean extends Model {
        private static final long serialVersionUID = 1L;

        private Long nodeId;

        private Integer portId;

        public Long getNodeId() {
            return nodeId;
        }

        public void setNodeId(Long nodeId) {
            Long oldValue = this.nodeId;
            this.nodeId = nodeId;
            firePropertyChange("nodeId", oldValue, this.nodeId);
        }

        public Integer getPortId() {
            return portId;
        }

        public void setPortId(Integer portId) {
            Integer oldValue = this.portId;
            this.portId = portId;
            firePropertyChange("portId", oldValue, this.portId);
        }
    }
}
