package com.awscloudprojects.model;

public class Order {
    private String userId; // (S)
    private String orderId; // (S)
    private float price; // (N)
    private long createdAt; // (N)

    public Order() {
    }

    public Order(String userId, String orderId, float price, long createdAt) {
        this.userId = userId;
        this.orderId = orderId;
        this.price = price;
        this.createdAt = createdAt;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public float getPrice() {
        return price;
    }

    public void setPrice(float price) {
        this.price = price;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
