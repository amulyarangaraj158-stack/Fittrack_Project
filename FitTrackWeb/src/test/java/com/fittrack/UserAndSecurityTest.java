package com.fittrack;

import com.fittrack.model.FitnessGoal;
import com.fittrack.model.PremiumUser;
import com.fittrack.model.RegularUser;
import com.fittrack.model.User;
import com.fittrack.model.WaterTracker;
import com.fittrack.util.PasswordUtils;
import com.fittrack.util.ValidationUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JUnit 5 tests for abstract User inheritance, tier permissions, BMI calculation,
 * PBKDF2 hashing, WaterTracker, FitnessGoal, and ValidationUtils.
 */
class UserAndSecurityTest {

    @Test
    void userPolymorphismTierPermissionsAndBmiCalculation() {
        User regular = new RegularUser("Alex", "alex@fittrack.com", "hash");
        User premium = new PremiumUser("Maya", "maya@fittrack.com", "hash");

        assertEquals("Regular Member", regular.getMembershipBadge());
        assertEquals("Premium Member", premium.getMembershipBadge());
        assertNotEquals(regular.getMembershipBenefits(), premium.getMembershipBenefits());

        assertFalse(regular.hasPremiumAnalyticsAccess());
        assertTrue(premium.hasPremiumAnalyticsAccess());
        assertTrue(premium.getMaxWorkoutPlansAllowed() > regular.getMaxWorkoutPlansAllowed());

        // Overloaded BMI calculation: 70 kg, 175 cm -> 22.9
        assertEquals(22.9, regular.calculateBMI(175.0, 70.0));
    }

    @Test
    void pbkdf2PasswordHashingAndVerification() {
        String hash = PasswordUtils.hashPassword("FitTrack@123");
        assertTrue(hash.startsWith("pbkdf2$"));
        assertFalse(PasswordUtils.needsRehash(hash));
        assertTrue(PasswordUtils.verifyPassword("FitTrack@123", hash));
        assertFalse(PasswordUtils.verifyPassword("WrongPass", hash));
    }

    @Test
    void goalAndWaterCalculations() {
        FitnessGoal goal = new FitnessGoal(1, 8000, 2500, 5, 500.0, 150, 20.0);
        assertEquals(50, goal.calculateCompletionPercentage(4000, goal.getDailyStepTarget()));
        assertEquals(100, goal.calculateCompletionPercentage(9500, goal.getDailyStepTarget()));
        assertEquals(150, goal.getActiveMinutesTarget());
        assertEquals(20.0, goal.getDistanceKmTarget());

        assertEquals(750, WaterTracker.calculateRemainingMl(1750, 2500));
        assertEquals(0, WaterTracker.calculateRemainingMl(3000, 2500));
        assertEquals(70, WaterTracker.calculateHydrationPercent(1750, 2500));
    }

    @Test
    void validationHelpersRejectMalformedInputs() {
        assertTrue(ValidationUtils.isValidEmail("student@university.edu"));
        assertFalse(ValidationUtils.isValidEmail("not-an-email"));
        assertTrue(ValidationUtils.isValidDate("2026-11-21"));
        assertFalse(ValidationUtils.isValidDate("21-11-2026"));
    }
}
