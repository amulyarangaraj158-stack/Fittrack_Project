package com.fittrack.model;

/**
 * Encapsulated model for persistent in-app user notifications.
 */
public class NotificationItem {

    private int notificationId;
    private int userId;
    private String category;
    private String title;
    private String message;
    private boolean read;
    private String createdAt;

    public NotificationItem() {
    }

    public NotificationItem(int notificationId, int userId, String category, String title,
                            String message, boolean read, String createdAt) {
        this.notificationId = notificationId;
        this.userId = userId;
        this.category = category;
        this.title = title;
        this.message = message;
        this.read = read;
        this.createdAt = createdAt;
    }

    public int getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(int notificationId) {
        this.notificationId = notificationId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
