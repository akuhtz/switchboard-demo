package org.bidib.switchboard.component.view;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import org.bidib.switchboard.component.model.Block;
import org.bidib.switchboard.component.model.BlockModel;
import org.bidib.switchboard.component.service.RouterService;

import com.jgoodies.forms.builder.FormBuilder;

/**
 * Dialog for editing the predecessor/successor links of a block.
 * Shows a tabbed pane with a table on each tab listing the linked blocks,
 * plus Add/Remove buttons.
 */
public class BlockLinkDialog {

    private final BlockModel blockModel;
    private final RouterService routerService;
    private final ResourceBundle messages;

    public BlockLinkDialog(BlockModel blockModel, RouterService routerService, ResourceBundle messages) {
        this.blockModel = blockModel;
        this.routerService = routerService;
        this.messages = messages;
    }

    public void show(Component parent, Block block) {
        LinkTableModel predecessorModel = new LinkTableModel(block, true);
        LinkTableModel successorModel = new LinkTableModel(block, false);

        JPanel predecessorPanel = buildLinkPanel(parent, block, predecessorModel, true);
        JPanel successorPanel = buildLinkPanel(parent, block, successorModel, false);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab(messages.getString("blockLink.predecessors"), predecessorPanel);
        tabbedPane.addTab(messages.getString("blockLink.successors"), successorPanel);
        tabbedPane.setPreferredSize(new Dimension(400, 250));

        JOptionPane.showMessageDialog(parent, tabbedPane,
            messages.getString("blockLink.title") + " - " + block.getName(),
            JOptionPane.PLAIN_MESSAGE);
    }

    private JPanel buildLinkPanel(Component parent, Block block, LinkTableModel tableModel, boolean isPredecessor) {
        JTable table = new JTable(tableModel);
        table.setFillsViewportHeight(true);
        JScrollPane scrollPane = new JScrollPane(table);

        JButton addButton = new JButton(messages.getString("blockLink.add"));
        addButton.addActionListener(e -> {
            List<Block> candidates = findLinkCandidates(block, isPredecessor);
            if (candidates.isEmpty()) {
                JOptionPane.showMessageDialog(parent,
                    messages.getString("blockLink.noCandidates"),
                    messages.getString("blockLink.title"),
                    JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            Block selected = showCandidateSelection(parent, candidates);
            if (selected != null) {
                if (isPredecessor) {
                    blockModel.linkBlocks(selected.getId(), block.getId());
                } else {
                    blockModel.linkBlocks(block.getId(), selected.getId());
                }
                tableModel.refresh();
            }
        });

        JButton removeButton = new JButton(messages.getString("blockLink.remove"));
        removeButton.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                return;
            }
            String linkedId = tableModel.getBlockIdAt(row);
            if (isPredecessor) {
                blockModel.unlinkBlocks(linkedId, block.getId());
            } else {
                blockModel.unlinkBlocks(block.getId(), linkedId);
            }
            tableModel.refresh();
        });

        JPanel buttonPanel = FormBuilder.create()
            .columns("pref, 4dlu, pref")
            .rows("pref")
            .add(addButton).xy(1, 1)
            .add(removeButton).xy(3, 1)
            .build();

        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        return panel;
    }

    private List<Block> findLinkCandidates(Block block, boolean isPredecessor) {
        List<Block> candidates = new ArrayList<>();
        List<String> alreadyLinked = isPredecessor ? block.getPredecessorIds() : block.getSuccessorIds();

        for (Block other : blockModel.getBlocks().values()) {
            if (other.getId().equals(block.getId())) {
                continue;
            }
            if (alreadyLinked.contains(other.getId())) {
                continue;
            }
            if (routerService.areBlocksAdjacent(block, other)) {
                candidates.add(other);
            }
        }
        return candidates;
    }

    private Block showCandidateSelection(Component parent, List<Block> candidates) {
        String[] names = candidates.stream()
            .map(b -> b.getName() + " (" + b.getId() + ")")
            .toArray(String[]::new);

        String selected = (String) JOptionPane.showInputDialog(parent,
            messages.getString("blockLink.selectBlock"),
            messages.getString("blockLink.title"),
            JOptionPane.PLAIN_MESSAGE, null, names, names[0]);

        if (selected == null) {
            return null;
        }
        int idx = java.util.Arrays.asList(names).indexOf(selected);
        return idx >= 0 ? candidates.get(idx) : null;
    }

    // --- Table model ---

    private class LinkTableModel extends AbstractTableModel {
        private final Block block;
        private final boolean isPredecessor;
        private List<String> linkedIds;

        LinkTableModel(Block block, boolean isPredecessor) {
            this.block = block;
            this.isPredecessor = isPredecessor;
            refresh();
        }

        void refresh() {
            this.linkedIds = new ArrayList<>(isPredecessor ? block.getPredecessorIds() : block.getSuccessorIds());
            fireTableDataChanged();
        }

        String getBlockIdAt(int row) {
            return linkedIds.get(row);
        }

        @Override
        public int getRowCount() {
            return linkedIds.size();
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public String getColumnName(int column) {
            return column == 0 ? messages.getString("blockLink.colId") : messages.getString("blockLink.colName");
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            String id = linkedIds.get(rowIndex);
            Block b = blockModel.getBlock(id);
            if (columnIndex == 0) {
                return id;
            }
            return b != null ? b.getName() : "?";
        }
    }
}
