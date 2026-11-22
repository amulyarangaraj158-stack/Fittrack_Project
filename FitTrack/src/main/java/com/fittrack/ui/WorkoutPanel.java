package com.fittrack.ui;

import com.fittrack.model.User;
import com.fittrack.model.Workout;
import com.fittrack.service.WorkoutService;
import com.fittrack.util.UITheme;
import com.fittrack.util.ValidationUtils;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.util.ArrayList;

/**
 * Workout Management screen allowing users to add, edit, delete, filter,
 * and preview calorie burn for all 5 Workout subclasses.
 */
public class WorkoutPanel extends JPanel {

    private User currentUser;
    private final WorkoutService workoutService;
    private final Runnable onDataChanged;

    private JComboBox<String> typeCombo;
    private JTextField durationField;
    private JTextField dateField;
    private JComboBox<String> intensityCombo;
    private JTextField stepsField;
    private JTextField distanceField;
    private JTextField notesField;
    private JLabel caloriePreviewLabel;

    private JComboBox<String> filterCombo;
    private JTable workoutTable;
    private DefaultTableModel tableModel;

    private Integer selectedWorkoutId = null;
    private JButton addButton;
    private JButton updateButton;
    private JButton deleteButton;

    public WorkoutPanel(User user, Runnable onDataChanged) {
        this.currentUser = user;
        this.workoutService = new WorkoutService();
        this.onDataChanged = onDataChanged;
        initComponents();
        loadWorkouts();
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        updateCaloriePreview();
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 18));
        setBackground(UITheme.MAIN_BG);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        JLabel pageTitle = UITheme.createPageTitle("Workout Management");
        JLabel subtitle = new JLabel("Log Walking, Running, Cycling, Yoga, or Strength Training sessions");
        subtitle.setFont(UITheme.FONT_BODY);
        subtitle.setForeground(UITheme.TEXT_SECONDARY);
        JPanel titleBox = new JPanel(new BorderLayout(0, 4));
        titleBox.setOpaque(false);
        titleBox.add(pageTitle, BorderLayout.NORTH);
        titleBox.add(subtitle, BorderLayout.SOUTH);
        headerPanel.add(titleBox, BorderLayout.WEST);
        add(headerPanel, BorderLayout.NORTH);

        // Form Card
        UITheme.RoundedPanel formCard = new UITheme.RoundedPanel(16);
        formCard.setLayout(new GridBagLayout());
        formCard.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 8, 5, 8);
        gbc.weightx = 0.25;

        typeCombo = new JComboBox<>(WorkoutService.WORKOUT_TYPES);
        UITheme.styleComboBox(typeCombo);
        durationField = UITheme.createTextField();
        durationField.setText("30");
        dateField = UITheme.createTextField();
        dateField.setText(LocalDate.now().toString());
        intensityCombo = new JComboBox<>(WorkoutService.INTENSITY_LEVELS);
        intensityCombo.setSelectedItem("Moderate");
        UITheme.styleComboBox(intensityCombo);

        stepsField = UITheme.createTextField();
        stepsField.setText("0");
        distanceField = UITheme.createTextField();
        distanceField.setText("0.0");
        notesField = UITheme.createTextField();

        // Row 0 Labels
        gbc.gridy = 0;
        gbc.gridx = 0;
        formCard.add(UITheme.createFieldLabel("Workout Type *"), gbc);
        gbc.gridx = 1;
        formCard.add(UITheme.createFieldLabel("Duration (minutes) *"), gbc);
        gbc.gridx = 2;
        formCard.add(UITheme.createFieldLabel("Date (YYYY-MM-DD) *"), gbc);
        gbc.gridx = 3;
        formCard.add(UITheme.createFieldLabel("Intensity"), gbc);

        // Row 1 Fields
        gbc.gridy = 1;
        gbc.gridx = 0;
        formCard.add(typeCombo, gbc);
        gbc.gridx = 1;
        formCard.add(durationField, gbc);
        gbc.gridx = 2;
        formCard.add(dateField, gbc);
        gbc.gridx = 3;
        formCard.add(intensityCombo, gbc);

        // Row 2 Labels
        gbc.gridy = 2;
        gbc.gridx = 0;
        formCard.add(UITheme.createFieldLabel("Steps (optional)"), gbc);
        gbc.gridx = 1;
        formCard.add(UITheme.createFieldLabel("Distance (km)"), gbc);
        gbc.gridx = 2;
        gbc.gridwidth = 2;
        formCard.add(UITheme.createFieldLabel("Notes"), gbc);

        // Row 3 Fields
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        formCard.add(stepsField, gbc);
        gbc.gridx = 1;
        formCard.add(distanceField, gbc);
        gbc.gridx = 2;
        gbc.gridwidth = 2;
        formCard.add(notesField, gbc);

        // Row 4 Actions + Calorie Preview
        JPanel actionBar = new JPanel(new BorderLayout(12, 0));
        actionBar.setOpaque(false);

        JPanel previewBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        previewBox.setOpaque(false);
        caloriePreviewLabel = new JLabel("Estimated Calories: -- kcal");
        caloriePreviewLabel.setFont(UITheme.FONT_SUBTITLE);
        caloriePreviewLabel.setForeground(UITheme.ACCENT_GREEN_DARK);
        JButton previewBtn = UITheme.createSecondaryButton("Calculate Calories");
        previewBtn.addActionListener(e -> updateCaloriePreview());
        previewBox.add(previewBtn);
        previewBox.add(caloriePreviewLabel);

        JPanel btnGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnGroup.setOpaque(false);
        addButton = UITheme.createPrimaryButton("Add Workout");
        updateButton = UITheme.createBlueButton("Update Selected");
        deleteButton = UITheme.createDangerButton("Delete Selected");
        JButton clearButton = UITheme.createSecondaryButton("Clear");

        updateButton.setEnabled(false);
        deleteButton.setEnabled(false);

        addButton.addActionListener(e -> handleAddWorkout());
        updateButton.addActionListener(e -> handleUpdateWorkout());
        deleteButton.addActionListener(e -> handleDeleteWorkout());
        clearButton.addActionListener(e -> clearForm());

        btnGroup.add(clearButton);
        btnGroup.add(deleteButton);
        btnGroup.add(updateButton);
        btnGroup.add(addButton);

        actionBar.add(previewBox, BorderLayout.WEST);
        actionBar.add(btnGroup, BorderLayout.EAST);

        gbc.gridy = 4;
        gbc.gridx = 0;
        gbc.gridwidth = 4;
        gbc.insets = new Insets(12, 8, 2, 8);
        formCard.add(actionBar, gbc);

        // Table Card
        UITheme.RoundedPanel tableCard = new UITheme.RoundedPanel(16);
        tableCard.setLayout(new BorderLayout(0, 12));
        tableCard.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JPanel tableHeaderBar = new JPanel(new BorderLayout());
        tableHeaderBar.setOpaque(false);
        tableHeaderBar.add(UITheme.createSectionTitle("Workout History"), BorderLayout.WEST);

        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        filterBar.setOpaque(false);
        filterBar.add(UITheme.createFieldLabel("Filter Type:"));
        filterCombo = new JComboBox<>(new String[]{
                "All Types", "Walking", "Running", "Cycling", "Yoga", "Strength Training"
        });
        UITheme.styleComboBox(filterCombo);
        filterCombo.setPreferredSize(new Dimension(170, 32));
        filterCombo.addActionListener(e -> loadWorkouts());
        filterBar.add(filterCombo);
        tableHeaderBar.add(filterBar, BorderLayout.EAST);

        String[] columns = {"ID", "Date", "Type", "Duration (min)", "Intensity", "Steps", "Distance (km)", "Calories (kcal)", "Notes"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        workoutTable = new JTable(tableModel);
        UITheme.styleTable(workoutTable);
        workoutTable.getColumnModel().getColumn(0).setPreferredWidth(45);
        workoutTable.getColumnModel().getColumn(2).setPreferredWidth(130);
        workoutTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                populateFormFromSelectedRow();
            }
        });

        JScrollPane tableScroll = new JScrollPane(workoutTable);
        tableScroll.setBorder(BorderFactory.createLineBorder(UITheme.CARD_BORDER));

        tableCard.add(tableHeaderBar, BorderLayout.NORTH);
        tableCard.add(tableScroll, BorderLayout.CENTER);

        JPanel bodyContainer = new JPanel(new BorderLayout(0, 16));
        bodyContainer.setOpaque(false);
        bodyContainer.add(formCard, BorderLayout.NORTH);
        bodyContainer.add(tableCard, BorderLayout.CENTER);

        add(bodyContainer, BorderLayout.CENTER);
        updateCaloriePreview();
    }

    public void loadWorkouts() {
        try {
            String selectedFilter = filterCombo != null ? (String) filterCombo.getSelectedItem() : "All Types";
            ArrayList<Workout> workouts = workoutService.getWorkoutsByType(currentUser.getUserId(), selectedFilter);
            tableModel.setRowCount(0);
            for (Workout w : workouts) {
                tableModel.addRow(new Object[]{
                        w.getWorkoutId(),
                        w.getWorkoutDate(),
                        w.getWorkoutType(),
                        w.getDurationMinutes(),
                        w.getIntensity(),
                        w.getSteps(),
                        String.format("%.2f", w.getDistanceKm()),
                        String.format("%.1f", w.getCaloriesBurned()),
                        w.getNotes()
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to load workouts: " + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateCaloriePreview() {
        try {
            String type = (String) typeCombo.getSelectedItem();
            int duration = ValidationUtils.parsePositiveInt(durationField.getText(), "Duration");
            String intensity = (String) intensityCombo.getSelectedItem();
            int steps = ValidationUtils.parseNonNegativeInt(stepsField.getText(), "Steps");
            double distance = ValidationUtils.parseNonNegativeDouble(distanceField.getText(), "Distance");

            double calories = workoutService.previewCalories(
                    type, duration, intensity, steps, distance, currentUser.getWeightKg()
            );
            caloriePreviewLabel.setText(String.format("Estimated Calories: %.1f kcal", calories));
        } catch (IllegalArgumentException ex) {
            caloriePreviewLabel.setText("Estimated Calories: -- kcal");
        }
    }

    private void handleAddWorkout() {
        try {
            String type = (String) typeCombo.getSelectedItem();
            int duration = ValidationUtils.parsePositiveInt(durationField.getText(), "Duration");
            String date = dateField.getText().trim();
            String intensity = (String) intensityCombo.getSelectedItem();
            int steps = ValidationUtils.parseNonNegativeInt(stepsField.getText(), "Steps");
            double distance = ValidationUtils.parseNonNegativeDouble(distanceField.getText(), "Distance");
            String notes = notesField.getText().trim();

            Workout created = workoutService.addWorkout(
                    currentUser, type, duration, date, intensity, steps, distance, notes
            );

            JOptionPane.showMessageDialog(
                    this,
                    String.format("Saved %s! Estimated %.1f kcal burned.", created.getWorkoutType(), created.getCaloriesBurned()),
                    "Workout Added",
                    JOptionPane.INFORMATION_MESSAGE
            );
            clearForm();
            loadWorkouts();
            onDataChanged.run();
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Validation Error", JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Could not add workout: " + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleUpdateWorkout() {
        if (selectedWorkoutId == null) {
            return;
        }
        try {
            String type = (String) typeCombo.getSelectedItem();
            int duration = ValidationUtils.parsePositiveInt(durationField.getText(), "Duration");
            String date = dateField.getText().trim();
            String intensity = (String) intensityCombo.getSelectedItem();
            int steps = ValidationUtils.parseNonNegativeInt(stepsField.getText(), "Steps");
            double distance = ValidationUtils.parseNonNegativeDouble(distanceField.getText(), "Distance");
            String notes = notesField.getText().trim();

            boolean updated = workoutService.updateWorkout(
                    currentUser, selectedWorkoutId, type, duration, date, intensity, steps, distance, notes
            );
            if (updated) {
                JOptionPane.showMessageDialog(this, "Workout updated successfully.",
                        "Updated", JOptionPane.INFORMATION_MESSAGE);
                clearForm();
                loadWorkouts();
                onDataChanged.run();
            }
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Validation Error", JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Could not update workout: " + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleDeleteWorkout() {
        if (selectedWorkoutId == null) {
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Delete workout #" + selectedWorkoutId + "?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION
        );
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            if (workoutService.deleteWorkout(selectedWorkoutId, currentUser.getUserId())) {
                clearForm();
                loadWorkouts();
                onDataChanged.run();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Could not delete workout: " + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void populateFormFromSelectedRow() {
        int row = workoutTable.getSelectedRow();
        if (row < 0) {
            return;
        }
        selectedWorkoutId = (Integer) tableModel.getValueAt(row, 0);
        dateField.setText(String.valueOf(tableModel.getValueAt(row, 1)));
        typeCombo.setSelectedItem(String.valueOf(tableModel.getValueAt(row, 2)));
        durationField.setText(String.valueOf(tableModel.getValueAt(row, 3)));
        intensityCombo.setSelectedItem(String.valueOf(tableModel.getValueAt(row, 4)));
        stepsField.setText(String.valueOf(tableModel.getValueAt(row, 5)));
        distanceField.setText(String.valueOf(tableModel.getValueAt(row, 6)));
        notesField.setText(String.valueOf(tableModel.getValueAt(row, 8)));

        updateButton.setEnabled(true);
        deleteButton.setEnabled(true);
        updateCaloriePreview();
    }

    private void clearForm() {
        selectedWorkoutId = null;
        workoutTable.clearSelection();
        typeCombo.setSelectedIndex(0);
        durationField.setText("30");
        dateField.setText(LocalDate.now().toString());
        intensityCombo.setSelectedItem("Moderate");
        stepsField.setText("0");
        distanceField.setText("0.0");
        notesField.setText("");
        updateButton.setEnabled(false);
        deleteButton.setEnabled(false);
        updateCaloriePreview();
    }
}
