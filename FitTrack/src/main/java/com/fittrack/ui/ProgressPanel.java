package com.fittrack.ui;

import com.fittrack.model.User;
import com.fittrack.model.Workout;
import com.fittrack.service.WorkoutService;
import com.fittrack.util.UITheme;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Progress Reports screen displaying JFreeChart analytics, weekly totals,
 * and complete workout history with CSV export.
 */
public class ProgressPanel extends JPanel {

    private User currentUser;
    private final WorkoutService workoutService;

    private JLabel totalSessionsLabel;
    private JLabel totalDurationLabel;
    private JLabel totalCaloriesLabel;
    private JLabel weeklyStepsLabel;

    private JPanel chartsContainer;
    private JTable historyTable;
    private DefaultTableModel historyModel;

    public ProgressPanel(User user) {
        this.currentUser = user;
        this.workoutService = new WorkoutService();
        initComponents();
        refreshReports();
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 16));
        setBackground(UITheme.MAIN_BG);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel titleBox = new JPanel(new BorderLayout(0, 4));
        titleBox.setOpaque(false);
        titleBox.add(UITheme.createPageTitle("Progress Reports & Analytics"), BorderLayout.NORTH);
        JLabel sub = new JLabel("Daily & weekly workout summaries powered by SQLite and JFreeChart");
        sub.setFont(UITheme.FONT_BODY);
        sub.setForeground(UITheme.TEXT_SECONDARY);
        titleBox.add(sub, BorderLayout.SOUTH);

        JButton exportBtn = UITheme.createBlueButton("Export History CSV");
        exportBtn.addActionListener(e -> exportCsv());

        header.add(titleBox, BorderLayout.WEST);
        header.add(exportBtn, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // Summary KPI strip
        JPanel summaryStrip = new JPanel(new GridLayout(1, 4, 14, 0));
        summaryStrip.setOpaque(false);

        totalSessionsLabel = new JLabel("0");
        totalDurationLabel = new JLabel("0 min");
        totalCaloriesLabel = new JLabel("0 kcal");
        weeklyStepsLabel = new JLabel("0");

        summaryStrip.add(buildSummaryCard("Total Workouts (All Time)", totalSessionsLabel, UITheme.ACCENT_GREEN_DARK));
        summaryStrip.add(buildSummaryCard("Total Duration (All Time)", totalDurationLabel, UITheme.ACCENT_BLUE));
        summaryStrip.add(buildSummaryCard("Total Calories Burned", totalCaloriesLabel, UITheme.ACCENT_AMBER));
        summaryStrip.add(buildSummaryCard("Weekly Steps (7 Days)", weeklyStepsLabel, new Color(139, 92, 246)));

        // Charts Row (JFreeChart)
        chartsContainer = new JPanel(new GridLayout(1, 2, 16, 0));
        chartsContainer.setOpaque(false);
        chartsContainer.setPreferredSize(new Dimension(800, 270));

        // History Table
        UITheme.RoundedPanel tableCard = new UITheme.RoundedPanel(16);
        tableCard.setLayout(new BorderLayout(0, 8));
        tableCard.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
        tableCard.add(UITheme.createSectionTitle("Detailed Workout Report"), BorderLayout.NORTH);

        String[] cols = {"Date", "Workout Type", "Duration (min)", "Intensity", "Steps", "Distance (km)", "Calories (kcal)"};
        historyModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        historyTable = new JTable(historyModel);
        UITheme.styleTable(historyTable);
        JScrollPane scroll = new JScrollPane(historyTable);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.CARD_BORDER));
        scroll.setPreferredSize(new Dimension(800, 180));
        tableCard.add(scroll, BorderLayout.CENTER);

        JPanel mainStack = new JPanel(new BorderLayout(0, 16));
        mainStack.setOpaque(false);
        mainStack.add(summaryStrip, BorderLayout.NORTH);
        mainStack.add(chartsContainer, BorderLayout.CENTER);
        mainStack.add(tableCard, BorderLayout.SOUTH);

        add(mainStack, BorderLayout.CENTER);
    }

    private UITheme.RoundedPanel buildSummaryCard(String label, JLabel valueLabel, Color accent) {
        UITheme.RoundedPanel card = new UITheme.RoundedPanel(14);
        card.setLayout(new BorderLayout(0, 6));
        card.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        JLabel title = new JLabel(label);
        title.setFont(UITheme.FONT_SMALL);
        title.setForeground(UITheme.TEXT_SECONDARY);

        valueLabel.setFont(UITheme.FONT_KPI);
        valueLabel.setForeground(accent);

        card.add(title, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    public void refreshReports() {
        try {
            ArrayList<Workout> allWorkouts = workoutService.getAllWorkouts(currentUser.getUserId());
            int totalSessions = allWorkouts.size();
            int totalMinutes = 0;
            double totalCalories = 0.0;

            Map<String, Double> caloriesByType = new LinkedHashMap<>();
            for (Workout w : allWorkouts) {
                totalMinutes += w.getDurationMinutes();
                totalCalories += w.getCaloriesBurned();
                caloriesByType.merge(w.getWorkoutType(), w.getCaloriesBurned(), Double::sum);
            }

            int weeklySteps = workoutService.getWeeklyTotalSteps(currentUser.getUserId());

            totalSessionsLabel.setText(String.valueOf(totalSessions));
            totalDurationLabel.setText(String.format("%,d min", totalMinutes));
            totalCaloriesLabel.setText(String.format("%,.1f kcal", totalCalories));
            weeklyStepsLabel.setText(String.format("%,d", weeklySteps));

            // Rebuild JFreeChart panels with live SQLite data
            chartsContainer.removeAll();
            chartsContainer.add(createWeeklyBarChartPanel());
            chartsContainer.add(createWorkoutBreakdownPiePanel(caloriesByType));
            chartsContainer.revalidate();
            chartsContainer.repaint();

            // Populate History Table
            historyModel.setRowCount(0);
            for (Workout w : allWorkouts) {
                historyModel.addRow(new Object[]{
                        w.getWorkoutDate(),
                        w.getWorkoutType(),
                        w.getDurationMinutes(),
                        w.getIntensity(),
                        w.getSteps(),
                        String.format("%.2f", w.getDistanceKm()),
                        String.format("%.1f", w.getCaloriesBurned())
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Could not generate progress reports: " + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JPanel createWeeklyBarChartPanel() throws Exception {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        Map<String, Double> last7Days = workoutService.getLast7DaysCaloriesMap(currentUser.getUserId());
        for (Map.Entry<String, Double> entry : last7Days.entrySet()) {
            // Display short MM-DD label on X axis
            String shortDate = entry.getKey().length() >= 10 ? entry.getKey().substring(5) : entry.getKey();
            dataset.addValue(entry.getValue(), "Calories", shortDate);
        }

        JFreeChart barChart = ChartFactory.createBarChart(
                "Calories Burned (Last 7 Days)",
                "Date",
                "Calories (kcal)",
                dataset,
                PlotOrientation.VERTICAL,
                false,
                true,
                false
        );

        barChart.setBackgroundPaint(Color.WHITE);
        barChart.getTitle().setFont(UITheme.FONT_SUBTITLE);

        CategoryPlot plot = barChart.getCategoryPlot();
        plot.setBackgroundPaint(new Color(248, 250, 252));
        plot.setRangeGridlinePaint(new Color(226, 232, 240));
        plot.setOutlineVisible(false);

        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, UITheme.ACCENT_GREEN);
        renderer.setBarPainter(new StandardBarPainter());
        renderer.setMaximumBarWidth(0.12);

        UITheme.RoundedPanel wrapper = new UITheme.RoundedPanel(16);
        wrapper.setLayout(new BorderLayout());
        wrapper.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        ChartPanel chartPanel = new ChartPanel(barChart);
        chartPanel.setMouseWheelEnabled(false);
        wrapper.add(chartPanel, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel createWorkoutBreakdownPiePanel(Map<String, Double> caloriesByType) {
        DefaultPieDataset<String> dataset = new DefaultPieDataset<>();
        if (caloriesByType.isEmpty()) {
            dataset.setValue("No Workouts Yet", 1.0);
        } else {
            for (Map.Entry<String, Double> entry : caloriesByType.entrySet()) {
                dataset.setValue(entry.getKey(), entry.getValue());
            }
        }

        JFreeChart pieChart = ChartFactory.createPieChart(
                "Calories by Workout Type",
                dataset,
                true,
                true,
                false
        );
        pieChart.setBackgroundPaint(Color.WHITE);
        pieChart.getTitle().setFont(UITheme.FONT_SUBTITLE);

        @SuppressWarnings("unchecked")
        PiePlot<String> plot = (PiePlot<String>) pieChart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setOutlineVisible(false);
        plot.setShadowPaint(null);
        plot.setSectionPaint("Walking", UITheme.ACCENT_GREEN);
        plot.setSectionPaint("Running", UITheme.ACCENT_BLUE);
        plot.setSectionPaint("Cycling", UITheme.ACCENT_AMBER);
        plot.setSectionPaint("Yoga", new Color(139, 92, 246));
        plot.setSectionPaint("Strength Training", UITheme.ACCENT_RED);
        plot.setSectionPaint("No Workouts Yet", new Color(203, 213, 225));

        UITheme.RoundedPanel wrapper = new UITheme.RoundedPanel(16);
        wrapper.setLayout(new BorderLayout());
        wrapper.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        ChartPanel chartPanel = new ChartPanel(pieChart);
        chartPanel.setMouseWheelEnabled(false);
        wrapper.add(chartPanel, BorderLayout.CENTER);
        return wrapper;
    }

    private void exportCsv() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("fittrack_report_" + LocalDate.now() + ".csv"));
        int choice = chooser.showSaveDialog(this);
        if (choice != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();
        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            pw.println("Date,Workout Type,Duration (min),Intensity,Steps,Distance (km),Calories (kcal)");
            for (int r = 0; r < historyModel.getRowCount(); r++) {
                StringBuilder line = new StringBuilder();
                for (int c = 0; c < historyModel.getColumnCount(); c++) {
                    if (c > 0) {
                        line.append(",");
                    }
                    line.append(String.valueOf(historyModel.getValueAt(r, c)).replace(",", ";"));
                }
                pw.println(line);
            }
            JOptionPane.showMessageDialog(this, "Report exported to " + file.getName(),
                    "Export Complete", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Could not export CSV: " + ex.getMessage(),
                    "Export Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
