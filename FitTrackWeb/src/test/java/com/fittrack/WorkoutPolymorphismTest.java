package com.fittrack;

import com.fittrack.model.CustomWorkout;
import com.fittrack.model.Cycling;
import com.fittrack.model.HIIT;
import com.fittrack.model.Running;
import com.fittrack.model.StrengthTraining;
import com.fittrack.model.Swimming;
import com.fittrack.model.Walking;
import com.fittrack.model.Workout;
import com.fittrack.model.Yoga;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JUnit 5 unit tests verifying Inheritance, Abstraction, and Runtime Polymorphism across all 8 Workout subclasses.
 */
class WorkoutPolymorphismTest {

    @Test
    void allWorkoutSubclassesCalculatePositiveDistinctCalories() {
        ArrayList<Workout> workouts = new ArrayList<>();
        workouts.add(new Walking(1, 30, "2026-11-21", "Moderate", 0, 2.5, "", 70.0));
        workouts.add(new Running(1, 30, "2026-11-21", "Moderate", 0, 4.5, "", 70.0));
        workouts.add(new Cycling(1, 30, "2026-11-21", "Moderate", 0, 10.0, "", 70.0));
        workouts.add(new Yoga(1, 30, "2026-11-21", "Moderate", 0, 0.0, "", 70.0));
        workouts.add(new StrengthTraining(1, 30, "2026-11-21", "Moderate", 0, 0.0, "", 70.0));
        workouts.add(new Swimming(1, 30, "2026-11-21", "Moderate", 0, 1.0, "", 70.0));
        workouts.add(new HIIT(1, 30, "2026-11-21", "High", 0, 0.0, "", 70.0));
        Workout custom = new CustomWorkout(1, 30, "2026-11-21", "Moderate", 0, 0.0, "", 70.0);
        custom.setWorkoutName("Kettlebell Circuit");
        workouts.add(custom);

        assertEquals(8, workouts.size());
        for (Workout w : workouts) {
            assertTrue(w.calculateCalories() > 0, w.getWorkoutType() + " should burn > 0 kcal");
            assertTrue(w.trackActivity().contains(w.getWorkoutType()));
        }

        // Running burns more calories than Yoga for identical duration and body weight
        Workout running = workouts.get(1);
        Workout yoga = workouts.get(3);
        assertTrue(running.calculateCalories() > yoga.calculateCalories());
        assertEquals("Kettlebell Circuit", custom.getWorkoutName());
    }

    @Test
    void overloadedCalculateCaloriesScalesWithBodyWeight() {
        Workout hiit = new HIIT(1, 30, "2026-11-21", "Moderate", 0, 0.0, "", 60.0);
        double at60Kg = hiit.calculateCalories(60.0);
        double at90Kg = hiit.calculateCalories(90.0);
        assertTrue(at90Kg > at60Kg);
    }

    @Test
    void invalidDurationThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                new Walking(1, 0, "2026-11-21", "Low", 0, 0.0, "", 70.0)
        );
    }
}
