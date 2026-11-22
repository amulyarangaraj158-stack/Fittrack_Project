# FitTrack — Personal Fitness Tracker (Java 17 OOP + Swing + SQLite)

## Overview
FitTrack is a desktop fitness tracking application built with **Java 17**, **Java Swing**, **SQLite (JDBC)**, **JFreeChart**, and **Maven**.

## Requirements
- **JDK 17** or newer
- **Apache Maven 3.8+** (or IntelliJ IDEA / Eclipse with Maven integration)

## How to Run (Command Line with Maven)
```bash
cd FitTrack
mvn compile exec:java
```

## How to Build a Runnable JAR
```bash
cd FitTrack
mvn clean package
java -jar target/fittrack-desktop-1.0.0.jar
```

## How to Run in IntelliJ IDEA or Eclipse
1. Open / Import `FitTrack/pom.xml` as a **Maven Project**.
2. Wait for Maven to download `sqlite-jdbc` and `jfreechart`.
3. Run `src/main/java/com/fittrack/Main.java`.

## Demo Credentials (Auto-Seeded on First Launch)
- **Regular User:** `alex@fittrack.com` / `FitTrack@123`
- **Premium User:** `maya@fittrack.com` / `FitTrack@123`
- Or click **Create Account** to register a brand-new user.
