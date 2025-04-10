package com.pbdvmobile.app.data.model;


import java.util.Date;

public class Session {
    public enum Status {
        PENDING,
        CONFIRMED,
        CANCELLED,
        DECLINED,
        COMPLETED
    }

    private int id; // Id set to AUTO INCREMENT
    private int tutorId;
    private int tuteeId;
    private int subjectId;
    private String location; // lat and long JSON
    private Date startTime;
    private Date endTime;
    private Status status;
    private String tutorReview;
    private String tuteeReview;
    private Double tutorRating;
    private Double tuteeRating;
    // Constructor
    public Session() {
    }
    public Session(int tutorId, int tuteeId, int subId) {
        this.tutorId = tutorId;
        this.tuteeId = tuteeId;
        this.subjectId = subId;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getTutorId() {
        return tutorId;
    }

    public void setTutorId(int tutorId) {
        this.tutorId = tutorId;
    }

    public int getTuteeId() {
        return tuteeId;
    }

    public void setTuteeId(int tuteeId) {
        this.tuteeId = tuteeId;
    }

    public int getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(int subjectId) {
        this.subjectId = subjectId;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getTutorReview() {
        return tutorReview;
    }

    public void setTutorReview(String tutorReview) {
        this.tutorReview = tutorReview;
    }

    public String getTuteeReview() {
        return tuteeReview;
    }

    public void setTuteeReview(String tuteeReview) {
        this.tuteeReview = tuteeReview;
    }
    public Double getTutorRating() {
        return tutorRating;
    }

    public void setTutorRating(Double tutorRating) {
        this.tutorRating = tutorRating;
    }

    public Double getTuteeRating() {
        return tuteeRating;
    }

    public void setTuteeRating(Double tuteeRating) {
        this.tuteeRating = tuteeRating;
    }
}