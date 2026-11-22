package com.fittrack.ui;

import com.fittrack.model.FitnessGoal;
import com.fittrack.model.User;
import com.fittrack.model.WaterIntake;
import com.fittrack.service.GoalService;
import com.fittrack.service.WaterService;
import com.fittrack.util.UITheme;
import com.fittrack.util.ValidationUtils;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.util.ArrayList;

/**
 * Water Tracker screen for logging daily hydration, viewing remaining target,
 * progress bar, and managing intake history.
 */
public class WaterPanel extends JPanel {

    private User currentUser;
    private final WaterService waterService;
    private final GoalService goalService;
    private final Runnable onDataChanged;

    private JLabel consumedValueLabel;
    private JLabel remainingValueLabel;
    private JLabel targetValueLabel;
    private JProgressBar hydrationBar;

    private JTextField customMlField;
    private JTextField dateField;
    private JTextField noteField;

    private JTable waterTable;
    private DefaultTableModel tableModel;

    public WaterPanel(User user, Runnable onDataChanged) {
        this.currentUser = user;
        this.waterService = new WaterService();
        this.goalService = new GoalService();
        this.onDataChanged = onDataChanged;
        initComponents();
        refreshWaterData();
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 18));
        setBackground(UITheme.MAIN_BG);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        // Header
        JPanel header = new JPanel(new BorderLayout(0, 4));
        header.setOpaque(false);
        header.add(UITheme.createPageTitle("Water Tracker"), BorderLayout.NORTH);
        JLabel sub = new JLabel("Log daily water consumption and stay on top of your hydration target");
        sub.setFont(UITheme.FONT_BODY);
        sub.setForeground(UITheme.TEXT_SECONDARY);
        header.add(sub, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        // Top Grid: Summary + Add Form
        JPanel topGrid = new JPanel(new GridLayout(1, 2, 18, 0));
        topGrid.setOpaque(false);

        // Left Card: Hydration Progress
        UITheme.RoundedPanel summaryCard = new UITheme.RoundedPanel(16);
        summaryCard.setLayout(new BoxLayout(summaryCard, BoxLayout.Y_AXIS));
        summaryCard.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JLabel sumTitle = UITheme.createSectionTitle("Today's Hydration Status");
        sumTitle.setAlignmentX(LEFT_ALIGNMENT);
        summaryCard.add(sumTitle);
        summaryCard.add(Box.createVerticalStrut(14));

        JPanel statsRow = new JPanel(new GridLayout(1, 3, 10, 0));
        statsRow.setOpaque(false);
        statsRow.setAlignmentX(LEFT_ALIGNMENT);
        statsRow.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 65));

        consumedValueLabel = new JLabel("0 ml");
        remainingValueLabel = new JLabel("0 ml");
        targetValueLabel = new JLabel("2500 ml");

        statsRow.add(createMetricBox("Consumed Today", consumedValueLabel, UITheme.ACCENT_BLUE));
        statsRow.add(createMetricBox("Remaining", remainingValueLabel, UITheme.ACCENT_AMBER));
        statsRow.add(createMetricBox("Daily Target", targetValueLabel, UITheme.ACCENT_GREEN_DARK));

        summaryCard.add(statsRow);
        summaryCard.add(Box.createVerticalStrut(16));

        hydrationBar = UITheme.createProgressBar(UITheme.ACCENT_BLUE);
        hydrationBar.setAlignmentX(LEFT_ALIGNMENT);
        hydrationBar.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 24));
        summaryCard.add(hydrationBar);
        summaryCard.add(Box.createVerticalStrut(16));

        JLabel quickTitle = UITheme.createFieldLabel("Quick Add (Today)");
        quickTitle.setAlignmentX(LEFT_ALIGNMENT);
        summaryCard.add(quickTitle);
        summaryCard.add(Box.createVerticalStrut(8));

        JPanel quickRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        quickRow.setOpaque(false);
        quickRow.setAlignmentX(LEFT_ALIGNMENT);
        int[] presets = {250, 500, 750, 1000};
        for (int ml : presets) {
            JButton btn = UITheme.createBlueButton("+" + ml + " ml");
            btn.addActionListener(e -> quickAddWater(ml));
            quickRow.add(btn);
        }
        summaryCard.add(quickRow);

        // Right Card: Custom Water Form
        UITheme.RoundedPanel formCard = new UITheme.RoundedPanel(16);
        formCard.setLayout(new GridBagLayout());
        formCard.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(4, 0, 4, 0);

        gbc.gridy = 0;
        formCard.add(UITheme.createSectionTitle("Log Custom Water Intake"), gbc);

        customMlField = UITheme.createTextField();
        customMlField.setText("300");
        dateField = UITheme.createTextField();
        dateField.setText(LocalDate.now().toString());
        noteField = UITheme.createTextField();

        gbc.gridy = 1;
        gbc.insets = new Insets(12, 0, 3, 0);
        formCard.add(UITheme.createFieldLabel("Amount (ml) *"), gbc);
        gbc.gridy = 2;
        gbc.insets = new Insets(2, 0, 8, 0);
        formCard.add(customMlField, gbc);

        gbc.gridy = 3;
        formCard.add(UITheme.createFieldLabel("Date (YYYY-MM-DD) *"), gbc);
        gbc.gridy = 4;
        formCard.add(dateField, gbc);

        gbc.gridy = 5;
        formCard.add(UITheme.createFieldLabel("Note (optional)"), gbc);
        gbc.gridy = 6;
        formCard.add(noteField, gbc);

        JButton addBtn = UITheme.createPrimaryButton("Add Water Entry");
        addBtn.addActionListener(e -> handleCustomAdd());
        gbc.gridy = 7;
        gbc.insets = new Insets(12, 0, 2, 0);
        formCard.add(addBtn, gbc);

        topGrid.add(summaryCard);
        topGrid.add(formCard);

        // Bottom Card: Intake Log Table
        UITheme.RoundedPanel tableCard = new UITheme.RoundedPanel(16);
        tableCard.setLayout(new BorderLayout(0, 10));
        tableCard.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JPanel tableTop = new JPanel(new BorderLayout());
        tableTop.setOpaque(false);
        tableTop.add(UITheme.createSectionTitle("Recent Water Intake Logs"), BorderLayout.WEST);

        JButton deleteBtn = UITheme.createDangerButton("Delete Selected Entry");
        deleteBtn.addActionListener(e -> handleDeleteSelected());
        tableTop.add(deleteBtn, BorderLayout.EAST);

        String[] cols = {"ID", "Date", "Time", "Amount (ml)", "Note"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        waterTable = new JTable(tableModel);
        UITheme.styleTable(waterTable);
        waterTable.getColumnModel().getColumn(0).setPreferredWidth(50);

        JScrollPane scrollPane = new JScrollPane(waterTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(UITheme.CARD_BORDER));

        tableCard.add(tableTop, BorderLayout.NORTH);
        tableCard.add(scrollPane, BorderLayout.CENTER);

        JPanel centerBody = new JPanel(new BorderLayout(0, 18));
        centerBody.setOpaque(false);
        centerBody.add(topGrid, BorderLayout.NORTH);
        centerBody.add(tableCard, BorderLayout.CENTER);

        add(centerBody, BorderLayout.CENTER);
    }

    private JPanel createMetricBox(String label, JLabel valueLabel, java.awt.Color accent) {
        JPanel box = new JPanel(new BorderLayout(0, 4));
        box.setOpaque(false);
        JLabel title = new JLabel(label);
        title.setFont(UITheme.FONT_SMALL);
        title.setForeground(UITheme.TEXT_SECONDARY);
        valueLabel.setFont(UITheme.FONT_SUBTITLE);
        valueLabel.setForeground(accent);
        box.add(title, BorderLayout.NORTH);
        box.add(valueLabel, BorderLayout.CENTER);
        return box;
    }

    public void refreshWaterData() {
        try {
            FitnessGoal goal = goalService.getOrCreateGoal(currentUser.getUserId());
            int targetMl = goal.getDailyWaterTargetMl();
            int consumedMl = waterService.getTodayTotalMl(currentUser.getUserId());
            int remainingMl = Math.max(0, targetMl - consumedMl);
            int pct = goalService.calculatePercentage(consumedMl, targetMl);

            consumedValueLabel.setText(String.format("%,d ml", consumedMl));
            remainingValueLabel.setText(remainingMl == 0 ? "Target Reached!" : String.format("%,d ml", remainingMl));
            targetValueLabel.setText(String.format("%,d ml", targetMl));

            hydrationBar.setValue(pct);
            hydrationBar.setString(String.format("%d%% (%,d / %,d ml)", pct, consumedMl, targetMl));

            ArrayList<WaterIntake> entries = waterService.getRecentEntries(currentUser.getUserId());
            tableModel.setRowCount(0);
            for (WaterIntake entry : entries) {
                tableModel.addRow(new Object[]{
                        entry.getWaterId(),
                        entry.getIntakeDate(),
                        entry.getIntakeTime(),
                        entry.getAmountMl(),
                        entry.getNote()
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Could not load water tracker data: " + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void quickAddWater(int amountMl) {
        try {
            waterService.logWater(currentUser.getUserId(), amountMl);
            refreshWaterData();
            onDataChanged.run();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Could not log water: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleCustomAdd() {
        try {
            int amount = ValidationUtils.parsePositiveInt(customMlField.getText(), "Water amount");
            String date = dateField.getText().trim();
            String note = noteField.getText().trim();
            if (note.isEmpty()) {
                note = "Custom water intake";
            }

            waterService.logWater(currentUser.getUserId(), amount, date, note);
            noteField.setText("");
            refreshWaterData();
            onDataChanged.run();
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Validation Error", JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Could not log water: " + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleDeleteSelected() {
        int row = waterTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a water entry to delete.",
                    "No Selection", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int waterId = (Integer) tableModel.getValueAt(row, 0);
        try {
            if (waterService.deleteWaterEntry(waterId, currentUser.getUserId())) {
                refreshWaterData();
                onDataChanged.run();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Could not delete entry: " + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
