package com.fittrack.interfaces;

/**
 * Defines the contract for trackable fitness activities.
 * Demonstrates Abstraction through Java Interfaces.
 */
public interface Trackable {

    String trackActivity();

    int getDurationMinutes();

    double getCaloriesBurned();

    String getActivityDate();
}
