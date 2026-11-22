package com.fittrack.interfaces;

/**
 * Contract for unlockable fitness milestones and achievements.
 * Demonstrates OOP Abstraction via Java Interfaces.
 */
public interface Awardable {

    String getBadgeCode();

    String getTitle();

    String getDescription();

    String getUnlockedAt();
}
