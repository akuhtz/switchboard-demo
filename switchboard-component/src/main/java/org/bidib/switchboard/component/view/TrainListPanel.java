package org.bidib.switchboard.component.view;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceAdapter;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.List;
import java.util.ResourceBundle;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

import org.bidib.switchboard.component.model.Train;
import org.bidib.switchboard.component.model.TrainListModel;

import com.vlsolutions.swing.docking.Dockable;
import com.vlsolutions.swing.docking.DockKey;

/**
 * Dockable panel displaying a list of trains.
 */
public class TrainListPanel extends JPanel implements Dockable, PropertyChangeListener {

    private static final long serialVersionUID = 1L;

    /** Data flavor for dragging a Train object. */
    public static final DataFlavor TRAIN_FLAVOR = new DataFlavor(Train.class, "Train");

    private final DockKey dockKey = new DockKey("trainList");

    private final TrainListModel trainListModel;

    private final DefaultListModel<Train> listModel = new DefaultListModel<>();

    private final JList<Train> trainList;

    public TrainListPanel(TrainListModel trainListModel, ResourceBundle messages) {
        this.trainListModel = trainListModel;
        trainListModel.addPropertyChangeListener(this);

        dockKey.setName(messages != null ? messages.getString("trainList.title") : "Trains");
        dockKey.setTooltip(messages != null ? messages.getString("trainList.tooltip") : "List of trains");

        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(200, 300));

        trainList = new JList<>(listModel);
        trainList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        trainList.setCellRenderer(new TrainCellRenderer());
        trainList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    onTrainDoubleClick();
                }
            }
        });

        // Enable drag-and-drop from the train list
        enableDrag();

        JScrollPane scrollPane = new JScrollPane(trainList);
        add(scrollPane, BorderLayout.CENTER);

        syncFromModel();
    }

    @Override
    public DockKey getDockKey() {
        return dockKey;
    }

    @Override
    public Component getComponent() {
        return this;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        SwingUtilities.invokeLater(this::syncFromModel);
    }

    public Train getSelectedTrain() {
        return trainList.getSelectedValue();
    }

    public void addTrain(Train train) {
        trainListModel.addTrain(train);
    }

    public void removeSelectedTrain() {
        Train selected = getSelectedTrain();
        if (selected != null) {
            trainListModel.removeTrain(selected);
        }
    }

    private void syncFromModel() {
        listModel.clear();
        for (Train train : trainListModel.getTrains()) {
            listModel.addElement(train);
        }
    }

    private void onTrainDoubleClick() {
        // Placeholder for future edit dialog
    }

    /** Enables drag-and-drop from the JList. */
    private void enableDrag() {
        DragSource dragSource = new DragSource();
        dragSource.createDefaultDragGestureRecognizer(trainList, DnDConstants.ACTION_COPY, new DragGestureListener() {
            @Override
            public void dragGestureRecognized(DragGestureEvent dge) {
                Train selected = trainList.getSelectedValue();
                if (selected == null) {
                    return;
                }
                dge.startDrag(null, new TrainTransferable(selected));
            }
        });
    }

    /** Custom renderer showing train image, address and name. */
    private static class TrainCellRenderer extends DefaultListCellRenderer {

        private static final int ICON_SIZE = 24;

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, "", index, isSelected, cellHasFocus);
            if (value instanceof Train train) {
                setIcon(train.getScaledIcon(ICON_SIZE));
                String label = train.getAddress() != null
                    ? train.getAddress() + " - " + train.getName()
                    : train.getName();
                setText(label);
            }
            return this;
        }
    }

    /** Transferable wrapper for a Train object. */
    public static class TrainTransferable implements Transferable {

        private final Train train;

        public TrainTransferable(Train train) {
            this.train = train;
        }

        public Train getTrain() {
            return train;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[] { TRAIN_FLAVOR };
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return TRAIN_FLAVOR.equals(flavor);
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            if (!isDataFlavorSupported(flavor)) {
                throw new UnsupportedFlavorException(flavor);
            }
            return train;
        }
    }
}
