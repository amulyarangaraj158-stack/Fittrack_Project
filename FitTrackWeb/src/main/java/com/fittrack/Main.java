package com.fittrack;

import com.fittrack.database.DatabaseConnection;
import com.fittrack.server.FitTrackHttpServer;
import com.fittrack.util.SampleDataSeeder;

/**
 * Main entry point for the FitTrack Java OOP Backend + HTML/CSS/JS Web Interface.
 */
public class Main {

    public static final int DEFAULT_PORT = 8080;

    public static void main(String[] args) {
        try {
            DatabaseConnection.initializeDatabase();
            SampleDataSeeder.seedIfEmpty();

            int port = DEFAULT_PORT;
            String envPort = System.getenv("FITTRACK_PORT");
            if (envPort != null && !envPort.isBlank()) {
                port = Integer.parseInt(envPort.trim());
            }

            FitTrackHttpServer server = new FitTrackHttpServer(port);
            server.start();
            System.out.println("FitTrack server running at http://localhost:" + port);
            System.out.println("Demo login -> alex@fittrack.com / FitTrack@123");
        } catch (Exception ex) {
            System.err.println("Failed to start FitTrack: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
