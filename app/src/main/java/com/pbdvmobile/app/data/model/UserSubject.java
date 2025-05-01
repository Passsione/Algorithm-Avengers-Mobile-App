package com.pbdvmobile.app.data.model;

import com.pbdvmobile.app.data.DataManager;

public class UserSubject {
    private int userId;
    private int subjectId;
    private double mark;
    private boolean tutoring;

    // Constructor
    public UserSubject() {
    }

    public UserSubject(int userId, int subjectId, double mark) {
        this.userId = userId;
        this.subjectId = subjectId;
        this.mark = mark;
        this.tutoring = false;
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
    public boolean getTutoring() {
        return tutoring;
    }


    public void setTutoring(boolean tutoring) {
        this.tutoring = tutoring;
    }
}
