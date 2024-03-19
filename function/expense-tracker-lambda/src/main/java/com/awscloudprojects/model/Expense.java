package com.awscloudprojects.model;

public class Expense {
    private String userId;
    private Long creationTime;
    private String category;
    private Integer cost;
    private String description;
    private String status;

    public Expense() {
    }
    public Expense(String userId, Long creationTime, String category, Integer cost, String description, String status) {
        this.userId = userId;
        this.creationTime = creationTime;
        this.category = category;
        this.cost = cost;
        this.description = description;
        this.status = status;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Long getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(Long creationTime) {
        this.creationTime = creationTime;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getCost() {
        return cost;
    }

    public void setCost(Integer cost) {
        this.cost = cost;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
