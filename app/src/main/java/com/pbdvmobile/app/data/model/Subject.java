package com.pbdvmobile.app.data.model;

public class Subject {
    private int subjectId; // Id set to AUTO INCREMENT
    private String subjectName;

    // Constructor
    public Subject() {
    }

    public Subject(String subjectName) { // Id set to AUTO INCREMENT
        this.subjectName = subjectName;
    }

    // Getters and Setters
    public int getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(int subjectId) {
        this.subjectId = subjectId;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }
}