package com.fittrack.ui;

import com.fittrack.model.FitnessGoal;
import com.fittrack.model.User;
import com.fittrack.service.GoalService;
import com.fittrack.service.WaterService;
import com.fittrack.service.WorkoutService;
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
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;

/**
 * Fitness Goals screen for configuring targets and visualizing completion progress.
 */
public class GoalsPanel extends JPanel {

    private User currentUser;
    private final GoalService goalService;
    private final WorkoutService workoutService;
    private final WaterService waterService;
    private final Runnable onDataChanged;

    private JTextField stepTargetField;
    private JTextField waterTargetField;
    private JTextField weeklyWorkoutTargetField;
    private JTextField calorieTargetField;

    private JProgressBar stepsBar;
    private JLabel stepsStatusLabel;
    private JProgressBar waterBar;
    private JLabel waterStatusLabel;
    private JProgressBar weeklyWorkoutBar;
    private JLabel weeklyWorkoutStatusLabel;
    private JProgressBar caloriesBar;
    private JLabel caloriesStatusLabel;

    public GoalsPanel(User user, Runnable onDataChanged) {
        this.currentUser = user;
        this.goalService = new GoalService();
        this.workoutService = new WorkoutService();
        this.waterService = new WaterService();
        this.onDataChanged = onDataChanged;
        initComponents();
        refreshGoals();
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 20));
        setBackground(UITheme.MAIN_BG);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        JPanel header = new JPanel(new BorderLayout(0, 4));
        header.setOpaque(false);
        header.add(UITheme.createPageTitle("Fitness Goals"), BorderLayout.NORTH);
        JLabel sub = new JLabel("Set daily and weekly targets and monitor your real-time completion progress");
        sub.setFont(UITheme.FONT_BODY);
        sub.setForeground(UITheme.TEXT_SECONDARY);
        header.add(sub, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        JPanel contentGrid = new JPanel(new GridLayout(1, 2, 20, 0));
        contentGrid.setOpaque(false);

        // Left: Target Configuration Form
        UITheme.RoundedPanel formCard = new UITheme.RoundedPanel(16);
        formCard.setLayout(new GridBagLayout());
        formCard.setBorder(BorderFactory.createEmptyBorder(22, 24, 22, 24));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(6, 0, 6, 0);

        gbc.gridy = 0;
        formCard.add(UITheme.createSectionTitle("Configure Personal Targets"), gbc);

        stepTargetField = UITheme.createTextField();
        waterTargetField = UITheme.createTextField();
        weeklyWorkoutTargetField = UITheme.createTextField();
        calorieTargetField = UITheme.createTextField();

        gbc.gridy = 1;
        gbc.insets = new Insets(14, 0, 4, 0);
        formCard.add(UITheme.createFieldLabel("Daily Step Target (steps/day)"), gbc);
        gbc.gridy = 2;
        gbc.insets = new Insets(2, 0, 8, 0);
        formCard.add(stepTargetField, gbc);

        gbc.gridy = 3;
        formCard.add(UITheme.createFieldLabel("Daily Water Target (ml/day)"), gbc);
        gbc.gridy = 4;
        formCard.add(waterTargetField, gbc);

        gbc.gridy = 5;
        formCard.add(UITheme.createFieldLabel("Weekly Workout Target (sessions/week)"), gbc);
        gbc.gridy = 6;
        formCard.add(weeklyWorkoutTargetField, gbc);

        gbc.gridy = 7;
        formCard.add(UITheme.createFieldLabel("Daily Calorie Burn Target (kcal/day)"), gbc);
        gbc.gridy = 8;
        formCard.add(calorieTargetField, gbc);

        JButton saveBtn = UITheme.createPrimaryButton("Save Fitness Goals");
        saveBtn.addActionListener(e -> handleSaveGoals());
        gbc.gridy = 9;
        gbc.insets = new Insets(16, 0, 4, 0);
        formCard.add(saveBtn, gbc);

        // Right: Progress Overview
        UITheme.RoundedPanel progressCard = new UITheme.RoundedPanel(16);
        progressCard.setLayout(new BoxLayout(progressCard, BoxLayout.Y_AXIS));
        progressCard.setBorder(BorderFactory.createEmptyBorder(22, 24, 22, 24));

        JLabel progressHeading = UITheme.createSectionTitle("Goal Completion Progress");
        progressHeading.setAlignmentX(LEFT_ALIGNMENT);
        progressCard.add(progressHeading);
        progressCard.add(Box.createVerticalStrut(18));

        stepsBar = UITheme.createProgressBar(UITheme.ACCENT_GREEN);
        stepsStatusLabel = new JLabel();
        progressCard.add(buildGoalProgressBlock("Daily Steps", stepsStatusLabel, stepsBar));
        progressCard.add(Box.createVerticalStrut(18));

        waterBar = UITheme.createProgressBar(UITheme.ACCENT_BLUE);
        waterStatusLabel = new JLabel();
        progressCard.add(buildGoalProgressBlock("Daily Water Intake", waterStatusLabel, waterBar));
        progressCard.add(Box.createVerticalStrut(18));

        weeklyWorkoutBar = UITheme.createProgressBar(UITheme.ACCENT_AMBER);
        weeklyWorkoutStatusLabel = new JLabel();
        progressCard.add(buildGoalProgressBlock("Weekly Workouts (Last 7 Days)", weeklyWorkoutStatusLabel, weeklyWorkoutBar));
        progressCard.add(Box.createVerticalStrut(18));

        caloriesBar = UITheme.createProgressBar(new Color(139, 92, 246));
        caloriesStatusLabel = new JLabel();
        progressCard.add(buildGoalProgressBlock("Daily Calories Burned", caloriesStatusLabel, caloriesBar));
        progressCard.add(Box.createVerticalGlue());

        contentGrid.add(formCard);
        contentGrid.add(progressCard);
        add(contentGrid, BorderLayout.CENTER);
    }

    private JPanel buildGoalProgressBlock(String title, JLabel statusLabel, JProgressBar bar) {
        JPanel block = new JPanel(new BorderLayout(0, 6));
        block.setOpaque(false);
        block.setAlignmentX(LEFT_ALIGNMENT);
        block.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 64));

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);
        JLabel titleLbl = UITheme.createFieldLabel(title);
        statusLabel.setFont(UITheme.FONT_SMALL);
        statusLabel.setForeground(UITheme.TEXT_SECONDARY);
        topRow.add(titleLbl, BorderLayout.WEST);
        topRow.add(statusLabel, BorderLayout.EAST);

        block.add(topRow, BorderLayout.NORTH);
        block.add(bar, BorderLayout.CENTER);
        return block;
    }

    public void refreshGoals() {
        try {
            FitnessGoal goal = goalService.getOrCreateGoal(currentUser.getUserId());
            stepTargetField.setText(String.valueOf(goal.getDailyStepTarget()));
            waterTargetField.setText(String.valueOf(goal.getDailyWaterTargetMl()));
            weeklyWorkoutTargetField.setText(String.valueOf(goal.getWeeklyWorkoutTarget()));
            calorieTargetField.setText(String.format("%.0f", goal.getDailyCalorieTarget()));

            int todaySteps = workoutService.getTodaySteps(currentUser.getUserId());
            int todayWater = waterService.getTodayTotalMl(currentUser.getUserId());
            int weeklyWorkouts = workoutService.getWeeklyWorkoutCount(currentUser.getUserId());
            double todayCalories = workoutService.getTodayCalories(currentUser.getUserId());

            int stepPct = goalService.calculatePercentage(todaySteps, goal.getDailyStepTarget());
            stepsBar.setValue(stepPct);
            stepsBar.setString(stepPct + "%");
            stepsStatusLabel.setText(String.format("%,d / %,d steps", todaySteps, goal.getDailyStepTarget()));

            int waterPct = goalService.calculatePercentage(todayWater, goal.getDailyWaterTargetMl());
            waterBar.setValue(waterPct);
            waterBar.setString(waterPct + "%");
            waterStatusLabel.setText(String.format("%,d / %,d ml", todayWater, goal.getDailyWaterTargetMl()));

            int weeklyPct = goalService.calculatePercentage(weeklyWorkouts, goal.getWeeklyWorkoutTarget());
            weeklyWorkoutBar.setValue(weeklyPct);
            weeklyWorkoutBar.setString(weeklyPct + "%");
            weeklyWorkoutStatusLabel.setText(String.format("%d / %d sessions", weeklyWorkouts, goal.getWeeklyWorkoutTarget()));

            int calPct = goalService.calculatePercentage(todayCalories, goal.getDailyCalorieTarget());
            caloriesBar.setValue(calPct);
            caloriesBar.setString(calPct + "%");
            caloriesStatusLabel.setText(String.format("%.1f / %.0f kcal", todayCalories, goal.getDailyCalorieTarget()));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Could not load fitness goals: " + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleSaveGoals() {
        try {
            int stepTarget = ValidationUtils.parsePositiveInt(stepTargetField.getText(), "Daily step target");
            int waterTarget = ValidationUtils.parsePositiveInt(waterTargetField.getText(), "Daily water target");
            int weeklyTarget = ValidationUtils.parsePositiveInt(weeklyWorkoutTargetField.getText(), "Weekly workout target");
            double calTarget = ValidationUtils.parsePositiveDouble(calorieTargetField.getText(), "Daily calorie target");

            goalService.updateGoal(currentUser.getUserId(), stepTarget, waterTarget, weeklyTarget, calTarget);
            refreshGoals();
            onDataChanged.run();
            JOptionPane.showMessageDialog(this, "Fitness goals updated successfully.",
                    "Goals Saved", JOptionPane.INFORMATION_MESSAGE);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Validation Error", JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Could not save goals: " + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
