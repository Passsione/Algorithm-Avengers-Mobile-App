package com.pbdvmobile.app.data.model;

import java.io.Serializable; // Add this

public class Prize implements Serializable { // Implement Serializable
    private int prizeId;
    private String prizeName;
    private int costInCredits; // ADD THIS FIELD

    // Constructors, getters, setters
    public Prize() {}

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

    public int getCostInCredits() {
        return costInCredits;
    }
    public void setCostInCredits(int costInCredits) {
        this.costInCredits = costInCredits;
    }
}