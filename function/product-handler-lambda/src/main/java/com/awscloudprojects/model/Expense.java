package com.awscloudprojects.model;

import java.util.List;

/**
 *

 * Expense

 String userId (user_id) PK

 String id (id)  SK

 Long creationTime (creation_time)

 String category (category)

 Float cost (cost)

 String description (description)

 String status (status)

 List<String> tags (tags)

 ------------

 gsi-creationTimeUserId

 creation_time, user_id


 lsi-cost

 cost


 lsi-status

 status
 */


public class Expense {
    private String userId;
    private String id; // set initially
    private Long creationTime; // set initially
    private String category;
    private Float cost;
    private String description;
    private String status;
    private List<String> tags;

    public Expense() {
    }
    public Expense(String userId, String id, Long creationTime, String category, Float cost, String description, String status, List<String> tags) {
        this.userId = userId;
        this.id = id;
        this.creationTime = creationTime;
        this.category = category;
        this.cost = cost;
        this.description = description;
        this.status = status;
        this.tags = tags;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public Float getCost() {
        return cost;
    }

    public void setCost(Float cost) {
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

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}
