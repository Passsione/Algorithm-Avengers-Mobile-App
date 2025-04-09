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

    private String id;
    private String tutorId;
    private String tuteeId;
    private String subject;
    private String location;
    private Date startTime;
    private Date endTime;
    private Status status;
    private String tutorReviewId;
    private String tuteeReviewId;

    public Session() {
        status = Status.PENDING;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTutorId() {
        return tutorId;
    }

    public void setTutorId(String tutorId) {
        this.tutorId = tutorId;
    }

    public String getTuteeId() {
        return tuteeId;
    }

    public void setTuteeId(String tuteeId) {
        this.tuteeId = tuteeId;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }


}
