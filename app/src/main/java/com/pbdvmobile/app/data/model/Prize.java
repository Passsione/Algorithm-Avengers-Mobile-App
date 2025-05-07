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

    /**
     * This table defines what can be "bought" with credits, specifically AI features.
     * Fields should include:
     * prizeId (PK)
     * name (e.g., "AI Document Summarizer - 1 Day Access", "AI Quiz Generator - 5 Uses")
     * description
     * creditCost (int)
     * featureIdentifier (String, e.g., "AI_SUMMARIZER", "AI_QUIZ_GENERATOR_USE") – a unique key for the feature.
     * durationMinutes (long, e.g., 1440 for 1 day. Could be 0 if it's usage-based).
     * maxUses (int, e.g., 5 for "5 Uses". Could be 0 or -1 if duration-based).
     * */
}