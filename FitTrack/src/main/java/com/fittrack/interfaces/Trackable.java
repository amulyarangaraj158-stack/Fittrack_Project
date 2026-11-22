package com.fittrack.interfaces;

/**
 * Trackable interface defines the contract for any fitness activity
 * that can be tracked, timed, and summarized in FitTrack.
 *
 * Demonstrates OOP Abstraction via Java Interfaces.
 */
public interface Trackable {

    String trackActivity();

    int getDurationMinutes();

    double getCaloriesBurned();

    String getActivityDate();
}
