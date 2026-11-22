package com.fittrack;

import com.fittrack.database.DatabaseConnection;
import com.fittrack.ui.LoginFrame;
import com.fittrack.util.SampleDataSeeder;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Main entry point for the FitTrack – Personal Fitness Tracker desktop application.
 */
public class Main {

    public static void main(String[] args) {
        // Apply system or cross-platform look and feel cleanly
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        // Initialize SQLite database, tables, and first-run sample data
        try {
            DatabaseConnection.initializeDatabase();
            SampleDataSeeder.seedIfEmpty();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    null,
                    "Failed to initialize SQLite database:\n" + ex.getMessage(),
                    "Startup Error",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        // Launch Swing UI on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            LoginFrame loginFrame = new LoginFrame();
            loginFrame.setVisible(true);
        });
    }
}
