package com.pbdvmobile.app.data.model;

public class Prize {
    private int prizeId; // Id set to AUTO INCREMENT
    private String prizeName;

    // Constructor
    public Prize() {
    }
    public Prize(String name) {
        this.prizeName = name;
    }

    // Getters and Setters
    public int getPrizeId() {
        return prizeId;
    }

    public void setPrizeId(int prizeId) {
        this.prizeId = prizeId;
    }

    public String getPrizeName() {
        return prizeName;
    }

    public void setPrizeName(String prizeName) {
        this.prizeName = prizeName;
    }
}