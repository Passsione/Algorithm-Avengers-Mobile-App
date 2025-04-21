package com.pbdvmobile.app.data.model;

public class UserSubject {
    private int userId;
    private int subjectId;
    private double mark;

    // Constructor
    public UserSubject() {
    }

    public UserSubject(int userId, int subjectId, double mark) {
        this.userId = userId;
        this.subjectId = subjectId;
        this.mark = mark;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(int subjectId) {
        this.subjectId = subjectId;
    }

    public double getMark() {
        return mark;
    }

    public void setMark(double mark) {
        this.mark = mark;
    }
}
