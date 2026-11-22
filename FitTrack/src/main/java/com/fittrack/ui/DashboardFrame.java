package com.fittrack.ui;

import com.fittrack.model.FitnessGoal;
import com.fittrack.model.User;
import com.fittrack.model.Workout;
import com.fittrack.service.GoalService;
import com.fittrack.service.WaterService;
import com.fittrack.service.WorkoutService;
import com.fittrack.util.UITheme;
import com.fittrack.util.ValidationUtils;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Main FitTrack Desktop Window featuring a dark navy sidebar, CardLayout navigation,
 * and live database-driven Dashboard Overview.
 */
public class DashboardFrame extends JFrame {

    private static final String CARD_OVERVIEW = "OVERVIEW";
    private static final String CARD_WORKOUTS = "WORKOUTS";
    private static final String CARD_GOALS = "GOALS";
    private static final String CARD_WATER = "WATER";
    private static final String CARD_PROGRESS = "PROGRESS";
    private static final String CARD_PROFILE = "PROFILE";

    private User currentUser;
    private final WorkoutService workoutService;
    private final GoalService goalService;
    private final WaterService waterService;

    private CardLayout cardLayout;
    private JPanel contentPanel;

    private final Map<String, JButton> navButtons = new LinkedHashMap<>();
    private JLabel sidebarUserLabel;
    private JLabel sidebarTierLabel;

    // Overview UI references
    private JLabel greetingLabel;
    private JLabel stepsKpiLabel;
    private JLabel stepsSubLabel;
    private JLabel caloriesKpiLabel;
    private JLabel caloriesSubLabel;
    private JLabel durationKpiLabel;
    private JLabel durationSubLabel;
    private JLabel waterKpiLabel;
    private JLabel waterSubLabel;

    private JProgressBar dashStepsBar;
    private JProgressBar dashWaterBar;
    private JProgressBar dashWeeklyBar;
    private DefaultTableModel weeklySummaryModel;
    private DefaultTableModel recentWorkoutsModel;

    // Feature Panels
    private WorkoutPanel workoutPanel;
    private GoalsPanel goalsPanel;
    private WaterPanel waterPanel;
    private ProgressPanel progressPanel;
    private ProfilePanel profilePanel;

    public DashboardFrame(User user) {
        this.currentUser = user;
        this.workoutService = new WorkoutService();
        this.goalService = new GoalService();
        this.waterService = new WaterService();
        initComponents();
        refreshAllPanels();
        switchCard(CARD_OVERVIEW);
    }

    private void initComponents() {
        setTitle("FitTrack — Personal Fitness Tracker");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1240, 790);
        setMinimumSize(new Dimension(1080, 700));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        add(buildSidebar(), BorderLayout.WEST);

        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(UITheme.MAIN_BG);

        JPanel overviewPanel = buildOverviewPanel();
        workoutPanel = new WorkoutPanel(currentUser, this::refreshAllPanels);
        goalsPanel = new GoalsPanel(currentUser, this::refreshAllPanels);
        waterPanel = new WaterPanel(currentUser, this::refreshAllPanels);
        progressPanel = new ProgressPanel(currentUser);
        profilePanel = new ProfilePanel(currentUser, this::handleUserProfileUpdated);

        contentPanel.add(overviewPanel, CARD_OVERVIEW);
        contentPanel.add(workoutPanel, CARD_WORKOUTS);
        contentPanel.add(goalsPanel, CARD_GOALS);
        contentPanel.add(waterPanel, CARD_WATER);
        contentPanel.add(progressPanel, CARD_PROGRESS);
        contentPanel.add(profilePanel, CARD_PROFILE);

        add(contentPanel, BorderLayout.CENTER);
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(UITheme.SIDEBAR_BG);
        sidebar.setPreferredSize(new Dimension(245, 790));
        sidebar.setBorder(BorderFactory.createEmptyBorder(24, 16, 20, 16));

        // Top Brand
        JPanel brandBox = new JPanel();
        brandBox.setOpaque(false);
        brandBox.setLayout(new BoxLayout(brandBox, BoxLayout.Y_AXIS));

        JLabel logoLabel = new JLabel("FITTRACK");
        logoLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        logoLabel.setForeground(UITheme.ACCENT_GREEN);
        logoLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel tagLabel = new JLabel("Personal Fitness OOP");
        tagLabel.setFont(UITheme.FONT_SMALL);
        tagLabel.setForeground(UITheme.TEXT_MUTED_LIGHT);
        tagLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        brandBox.add(logoLabel);
        brandBox.add(Box.createVerticalStrut(3));
        brandBox.add(tagLabel);
        brandBox.add(Box.createVerticalStrut(26));

        // Nav Menu
        JPanel navList = new JPanel();
        navList.setOpaque(false);
        navList.setLayout(new BoxLayout(navList, BoxLayout.Y_AXIS));

        addNavButton(navList, "Dashboard", CARD_OVERVIEW);
        addNavButton(navList, "Workouts", CARD_WORKOUTS);
        addNavButton(navList, "Fitness Goals", CARD_GOALS);
        addNavButton(navList, "Water Tracker", CARD_WATER);
        addNavButton(navList, "Progress Reports", CARD_PROGRESS);
        addNavButton(navList, "Profile & BMI", CARD_PROFILE);

        JPanel topSection = new JPanel(new BorderLayout());
        topSection.setOpaque(false);
        topSection.add(brandBox, BorderLayout.NORTH);
        topSection.add(navList, BorderLayout.CENTER);

        // Bottom User Card + Logout
        JPanel bottomBox = new JPanel();
        bottomBox.setOpaque(false);
        bottomBox.setLayout(new BoxLayout(bottomBox, BoxLayout.Y_AXIS));

        UITheme.RoundedPanel userBox = new UITheme.RoundedPanel(12, UITheme.SIDEBAR_HOVER, new Color(51, 65, 85));
        userBox.setLayout(new BoxLayout(userBox, BoxLayout.Y_AXIS));
        userBox.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        userBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        userBox.setMaximumSize(new Dimension(213, 66));

        sidebarUserLabel = new JLabel(currentUser.getFullName());
        sidebarUserLabel.setFont(UITheme.FONT_BODY_BOLD);
        sidebarUserLabel.setForeground(Color.WHITE);

        sidebarTierLabel = new JLabel(currentUser.getMembershipBadge());
        sidebarTierLabel.setFont(UITheme.FONT_SMALL);
        sidebarTierLabel.setForeground(UITheme.ACCENT_GREEN);

        userBox.add(sidebarUserLabel);
        userBox.add(Box.createVerticalStrut(3));
        userBox.add(sidebarTierLabel);

        JButton logoutBtn = UITheme.createDangerButton("Logout");
        logoutBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        logoutBtn.setMaximumSize(new Dimension(213, 38));
        logoutBtn.addActionListener(e -> handleLogout());

        bottomBox.add(userBox);
        bottomBox.add(Box.createVerticalStrut(12));
        bottomBox.add(logoutBtn);

        sidebar.add(topSection, BorderLayout.NORTH);
        sidebar.add(bottomBox, BorderLayout.SOUTH);
        return sidebar;
    }

    private void addNavButton(JPanel container, String label, String cardKey) {
        JButton btn = new JButton(label);
        btn.setFont(new Font("SansSerif", Font.BOLD, 14));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(213, 42));
        btn.setPreferredSize(new Dimension(213, 42));
        btn.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        btn.addActionListener(e -> switchCard(cardKey));

        navButtons.put(cardKey, btn);
        container.add(btn);
        container.add(Box.createVerticalStrut(6));
    }

    private void switchCard(String cardKey) {
        cardLayout.show(contentPanel, cardKey);
        for (Map.Entry<String, JButton> entry : navButtons.entrySet()) {
            boolean active = entry.getKey().equals(cardKey);
            JButton b = entry.getValue();
            if (active) {
                b.setBackground(UITheme.ACCENT_GREEN);
                b.setForeground(Color.WHITE);
            } else {
                b.setBackground(UITheme.SIDEBAR_BG);
                b.setForeground(new Color(203, 213, 225));
            }
        }
    }

    private JPanel buildOverviewPanel() {
        JPanel overview = new JPanel(new BorderLayout(0, 18));
        overview.setBackground(UITheme.MAIN_BG);
        overview.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        // Top Greeting + Quick Step Log Button
        JPanel topHeader = new JPanel(new BorderLayout());
        topHeader.setOpaque(false);

        JPanel greetBox = new JPanel(new BorderLayout(0, 4));
        greetBox.setOpaque(false);
        greetingLabel = UITheme.createPageTitle("Welcome back, " + currentUser.getFullName());
        JLabel dateSub = new JLabel("Today's Snapshot — " + LocalDate.now());
        dateSub.setFont(UITheme.FONT_BODY);
        dateSub.setForeground(UITheme.TEXT_SECONDARY);
        greetBox.add(greetingLabel, BorderLayout.NORTH);
        greetBox.add(dateSub, BorderLayout.SOUTH);

        JPanel quickActions = new JPanel();
        quickActions.setOpaque(false);
        JButton logStepsBtn = UITheme.createPrimaryButton("+ Log Daily Steps");
        logStepsBtn.addActionListener(e -> showLogStepsDialog());
        JButton addWorkoutShortcut = UITheme.createBlueButton("+ Add Workout");
        addWorkoutShortcut.addActionListener(e -> switchCard(CARD_WORKOUTS));
        quickActions.add(logStepsBtn);
        quickActions.add(Box.createHorizontalStrut(8));
        quickActions.add(addWorkoutShortcut);

        topHeader.add(greetBox, BorderLayout.WEST);
        topHeader.add(quickActions, BorderLayout.EAST);
        overview.add(topHeader, BorderLayout.NORTH);

        // KPI Row (4 Cards)
        JPanel kpiRow = new JPanel(new GridLayout(1, 4, 16, 0));
        kpiRow.setOpaque(false);

        stepsKpiLabel = new JLabel("0");
        stepsSubLabel = new JLabel("Target: 8,000 steps");
        caloriesKpiLabel = new JLabel("0 kcal");
        caloriesSubLabel = new JLabel("Target: 500 kcal");
        durationKpiLabel = new JLabel("0 min");
        durationSubLabel = new JLabel("Active workout time today");
        waterKpiLabel = new JLabel("0 ml");
        waterSubLabel = new JLabel("Target: 2,500 ml");

        kpiRow.add(createKpiCard("Daily Steps (Today)", stepsKpiLabel, stepsSubLabel, UITheme.ACCENT_GREEN_DARK));
        kpiRow.add(createKpiCard("Calories Burned (Today)", caloriesKpiLabel, caloriesSubLabel, UITheme.ACCENT_AMBER));
        kpiRow.add(createKpiCard("Workout Duration (Today)", durationKpiLabel, durationSubLabel, UITheme.ACCENT_BLUE));
        kpiRow.add(createKpiCard("Water Intake (Today)", waterKpiLabel, waterSubLabel, new Color(14, 165, 233)));

        // Middle Row: Fitness Goal Progress + Weekly Activity Summary
        JPanel middleGrid = new JPanel(new GridLayout(1, 2, 18, 0));
        middleGrid.setOpaque(false);

        UITheme.RoundedPanel goalsCard = new UITheme.RoundedPanel(16);
        goalsCard.setLayout(new BoxLayout(goalsCard, BoxLayout.Y_AXIS));
        goalsCard.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        JLabel gTitle = UITheme.createSectionTitle("Fitness Goal Progress");
        gTitle.setAlignmentX(LEFT_ALIGNMENT);
        goalsCard.add(gTitle);
        goalsCard.add(Box.createVerticalStrut(14));

        dashStepsBar = UITheme.createProgressBar(UITheme.ACCENT_GREEN);
        dashWaterBar = UITheme.createProgressBar(UITheme.ACCENT_BLUE);
        dashWeeklyBar = UITheme.createProgressBar(UITheme.ACCENT_AMBER);

        goalsCard.add(wrapBar("Daily Step Goal", dashStepsBar));
        goalsCard.add(Box.createVerticalStrut(14));
        goalsCard.add(wrapBar("Daily Water Goal", dashWaterBar));
        goalsCard.add(Box.createVerticalStrut(14));
        goalsCard.add(wrapBar("Weekly Workout Sessions", dashWeeklyBar));

        // Weekly Activity Summary Table
        UITheme.RoundedPanel weeklyCard = new UITheme.RoundedPanel(16);
        weeklyCard.setLayout(new BorderLayout(0, 10));
        weeklyCard.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        weeklyCard.add(UITheme.createSectionTitle("Weekly Activity Summary (Last 7 Days)"), BorderLayout.NORTH);

        weeklySummaryModel = new DefaultTableModel(new String[]{"Date", "Duration (min)", "Calories (kcal)"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        JTable weeklyTable = new JTable(weeklySummaryModel);
        UITheme.styleTable(weeklyTable);
        JScrollPane weeklyScroll = new JScrollPane(weeklyTable);
        weeklyScroll.setBorder(BorderFactory.createLineBorder(UITheme.CARD_BORDER));
        weeklyCard.add(weeklyScroll, BorderLayout.CENTER);

        middleGrid.add(goalsCard);
        middleGrid.add(weeklyCard);

        // Bottom Row: Recent Workouts
        UITheme.RoundedPanel recentCard = new UITheme.RoundedPanel(16);
        recentCard.setLayout(new BorderLayout(0, 10));
        recentCard.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        recentCard.setPreferredSize(new Dimension(900, 210));
        recentCard.add(UITheme.createSectionTitle("Recent Workouts"), BorderLayout.NORTH);

        recentWorkoutsModel = new DefaultTableModel(
                new String[]{"Date", "Workout Type", "Duration (min)", "Intensity", "Calories (kcal)", "Notes"}, 0
        ) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        JTable recentTable = new JTable(recentWorkoutsModel);
        UITheme.styleTable(recentTable);
        JScrollPane recentScroll = new JScrollPane(recentTable);
        recentScroll.setBorder(BorderFactory.createLineBorder(UITheme.CARD_BORDER));
        recentCard.add(recentScroll, BorderLayout.CENTER);

        JPanel centerStack = new JPanel(new BorderLayout(0, 16));
        centerStack.setOpaque(false);
        centerStack.add(kpiRow, BorderLayout.NORTH);
        centerStack.add(middleGrid, BorderLayout.CENTER);
        centerStack.add(recentCard, BorderLayout.SOUTH);

        overview.add(centerStack, BorderLayout.CENTER);
        return overview;
    }

    private UITheme.RoundedPanel createKpiCard(String title, JLabel valueLbl, JLabel subLbl, Color accent) {
        UITheme.RoundedPanel card = new UITheme.RoundedPanel(16);
        card.setLayout(new BorderLayout(0, 6));
        card.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(UITheme.FONT_BODY_BOLD);
        titleLbl.setForeground(UITheme.TEXT_SECONDARY);

        valueLbl.setFont(UITheme.FONT_KPI);
        valueLbl.setForeground(accent);

        subLbl.setFont(UITheme.FONT_SMALL);
        subLbl.setForeground(UITheme.TEXT_SECONDARY);

        card.add(titleLbl, BorderLayout.NORTH);
        card.add(valueLbl, BorderLayout.CENTER);
        card.add(subLbl, BorderLayout.SOUTH);
        return card;
    }

    private JPanel wrapBar(String title, JProgressBar bar) {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setOpaque(false);
        p.setAlignmentX(LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 54));
        p.add(UITheme.createFieldLabel(title), BorderLayout.NORTH);
        p.add(bar, BorderLayout.CENTER);
        return p;
    }

    private void refreshOverview() {
        try {
            int userId = currentUser.getUserId();
            FitnessGoal goal = goalService.getOrCreateGoal(userId);

            int todaySteps = workoutService.getTodaySteps(userId);
            double todayCalories = workoutService.getTodayCalories(userId);
            int todayDuration = workoutService.getTodayDuration(userId);
            int todayWater = waterService.getTodayTotalMl(userId);
            int weeklySessions = workoutService.getWeeklyWorkoutCount(userId);

            greetingLabel.setText("Welcome back, " + currentUser.getFullName());
            sidebarUserLabel.setText(currentUser.getFullName());
            sidebarTierLabel.setText(currentUser.getMembershipBadge());

            stepsKpiLabel.setText(String.format("%,d", todaySteps));
            stepsSubLabel.setText(String.format("Target: %,d steps", goal.getDailyStepTarget()));

            caloriesKpiLabel.setText(String.format("%.1f kcal", todayCalories));
            caloriesSubLabel.setText(String.format("Target: %.0f kcal", goal.getDailyCalorieTarget()));

            durationKpiLabel.setText(todayDuration + " min");
            durationSubLabel.setText(weeklySessions + " workouts in last 7 days");

            waterKpiLabel.setText(String.format("%,d ml", todayWater));
            waterSubLabel.setText(String.format("Target: %,d ml", goal.getDailyWaterTargetMl()));

            int stepPct = goalService.calculatePercentage(todaySteps, goal.getDailyStepTarget());
            dashStepsBar.setValue(stepPct);
            dashStepsBar.setString(String.format("%d%% (%,d / %,d)", stepPct, todaySteps, goal.getDailyStepTarget()));

            int waterPct = goalService.calculatePercentage(todayWater, goal.getDailyWaterTargetMl());
            dashWaterBar.setValue(waterPct);
            dashWaterBar.setString(String.format("%d%% (%,d / %,d ml)", waterPct, todayWater, goal.getDailyWaterTargetMl()));

            int weeklyPct = goalService.calculatePercentage(weeklySessions, goal.getWeeklyWorkoutTarget());
            dashWeeklyBar.setValue(weeklyPct);
            dashWeeklyBar.setString(String.format("%d%% (%d / %d sessions)", weeklyPct, weeklySessions, goal.getWeeklyWorkoutTarget()));

            // Weekly summary table
            Map<String, Integer> durMap = workoutService.getLast7DaysDurationMap(userId);
            Map<String, Double> calMap = workoutService.getLast7DaysCaloriesMap(userId);
            weeklySummaryModel.setRowCount(0);
            for (String date : durMap.keySet()) {
                weeklySummaryModel.addRow(new Object[]{
                        date,
                        durMap.get(date),
                        String.format("%.1f", calMap.getOrDefault(date, 0.0))
                });
            }

            // Recent workouts (top 6)
            ArrayList<Workout> workouts = workoutService.getAllWorkouts(userId);
            recentWorkoutsModel.setRowCount(0);
            int limit = Math.min(6, workouts.size());
            for (int i = 0; i < limit; i++) {
                Workout w = workouts.get(i);
                recentWorkoutsModel.addRow(new Object[]{
                        w.getWorkoutDate(),
                        w.getWorkoutType(),
                        w.getDurationMinutes(),
                        w.getIntensity(),
                        String.format("%.1f", w.getCaloriesBurned()),
                        w.getNotes()
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error refreshing dashboard: " + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showLogStepsDialog() {
        String input = JOptionPane.showInputDialog(
                this,
                "Enter steps to add for today (" + LocalDate.now() + "):",
                "Log Daily Steps",
                JOptionPane.PLAIN_MESSAGE
        );
        if (input == null) {
            return;
        }
        try {
            int steps = ValidationUtils.parsePositiveInt(input, "Steps");
            workoutService.logDailySteps(currentUser.getUserId(), steps, LocalDate.now().toString());
            refreshAllPanels();
            JOptionPane.showMessageDialog(this, "Logged " + steps + " steps!",
                    "Steps Saved", JOptionPane.INFORMATION_MESSAGE);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Validation Error", JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Could not log steps: " + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleUserProfileUpdated(User updatedUser) {
        this.currentUser = updatedUser;
        workoutPanel.setCurrentUser(updatedUser);
        goalsPanel.setCurrentUser(updatedUser);
        waterPanel.setCurrentUser(updatedUser);
        progressPanel.setCurrentUser(updatedUser);
        refreshAllPanels();
    }

    public void refreshAllPanels() {
        refreshOverview();
        workoutPanel.loadWorkouts();
        goalsPanel.refreshGoals();
        waterPanel.refreshWaterData();
        progressPanel.refreshReports();
    }

    private void handleLogout() {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to log out?",
                "Confirm Logout",
                JOptionPane.YES_NO_OPTION
        );
        if (confirm == JOptionPane.YES_OPTION) {
            dispose();
            new LoginFrame().setVisible(true);
        }
    }
}
