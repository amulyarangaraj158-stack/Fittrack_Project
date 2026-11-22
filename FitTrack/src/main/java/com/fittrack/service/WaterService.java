package com.fittrack.service;

import com.fittrack.dao.WaterDAO;
import com.fittrack.model.WaterIntake;
import com.fittrack.util.ValidationUtils;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

/**
 * Service layer for Water Intake tracking.
 * Demonstrates Method Overloading with logWater().
 */
public class WaterService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private final WaterDAO waterDAO;

    public WaterService() {
        this.waterDAO = new WaterDAO();
    }

    // Overloaded method 1: logs water for today with default note
    public WaterIntake logWater(int userId, int amountMl) throws SQLException {
        return logWater(userId, amountMl, LocalDate.now().toString(), "Hydration log");
    }

    // Overloaded method 2: logs water with explicit date and custom note
    public WaterIntake logWater(int userId, int amountMl, String intakeDate, String note) throws SQLException {
        if (!ValidationUtils.isValidDate(intakeDate)) {
            throw new IllegalArgumentException("Date must be in YYYY-MM-DD format.");
        }
        String nowTime = LocalTime.now().format(TIME_FORMAT);
        WaterIntake intake = new WaterIntake(userId, amountMl, intakeDate, nowTime, note);
        return waterDAO.addWaterIntake(intake);
    }

    public boolean deleteWaterEntry(int waterId, int userId) throws SQLException {
        return waterDAO.deleteWaterIntake(waterId, userId);
    }

    public int getTodayTotalMl(int userId) throws SQLException {
        return waterDAO.getTotalWaterByDate(userId, LocalDate.now().toString());
    }

    public ArrayList<WaterIntake> getTodayEntries(int userId) throws SQLException {
        return waterDAO.findByUserAndDate(userId, LocalDate.now().toString());
    }

    public ArrayList<WaterIntake> getRecentEntries(int userId) throws SQLException {
        return waterDAO.findRecentByUser(userId, 30);
    }
}
