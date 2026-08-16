package com.example.expensetracker.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "recurring_transactions")
public class RecurringTransaction {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private double amount;
    private String description;
    private String category;
    private String type;
    private int dayOfMonth; // 1-31
    private String userId;

    public RecurringTransaction() {
    }

    public RecurringTransaction(double amount, String description, String category, String type, int dayOfMonth, String userId) {
        this.amount = amount;
        this.description = description;
        this.category = category;
        this.type = type;
        this.dayOfMonth = dayOfMonth;
        this.userId = userId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getDayOfMonth() {
        return dayOfMonth;
    }

    public void setDayOfMonth(int dayOfMonth) {
        this.dayOfMonth = dayOfMonth;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
