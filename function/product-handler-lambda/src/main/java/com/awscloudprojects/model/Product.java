package com.awscloudprojects.model;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Product {
    private String productId; // (S) partition key
    private String title; // (S)
    private String category; // (S)
    private float price; // (N)
    private Set<String> allowedCreditCards = new HashSet<>(); // (SS)
    private Set<Integer> existingSizes = new HashSet<>(); // (NS)
    private int stockAmount; // (N)
    private boolean active = true; // (BOOL)
    private Map<String, String> features; // (M)
    private long createdAt; // (N)

    public Product() {
    }

    public Product(String productId, String title, String category, float price, Set<String> allowedCreditCards, Set<Integer> existingSizes,
                   int stockAmount, boolean active, Map<String, String> features, long createdAt) {
        this.productId = productId;
        this.title = title;
        this.category = category;
        this.price = price;
        this.allowedCreditCards = allowedCreditCards;
        this.existingSizes = existingSizes;
        this.stockAmount = stockAmount;
        this.active = active;
        this.features = features;
        this.createdAt = createdAt;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public float getPrice() {
        return price;
    }

    public void setPrice(float price) {
        this.price = price;
    }

    public Set<String> getAllowedCreditCards() {
        return allowedCreditCards;
    }

    public void setAllowedCreditCards(Set<String> allowedCreditCards) {
        this.allowedCreditCards = allowedCreditCards;
    }

    public Set<Integer> getExistingSizes() {
        return existingSizes;
    }

    public void setExistingSizes(Set<Integer> existingSizes) {
        this.existingSizes = existingSizes;
    }

    public int getStockAmount() {
        return stockAmount;
    }

    public void setStockAmount(int stockAmount) {
        this.stockAmount = stockAmount;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Map<String, String> getFeatures() {
        return features;
    }

    public void setFeatures(Map<String, String> features) {
        this.features = features;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
