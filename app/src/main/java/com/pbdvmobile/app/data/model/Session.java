package com.pbdvmobile.app.data.model;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.Timestamp; // Use Firestore Timestamp for dates
import java.io.Serializable;
// import java.util.Date; // Replaced by com.google.firebase.Timestamp

public class Session implements Serializable {

    public enum Status {
        PENDING,
        CONFIRMED,
        CANCELLED,
        DECLINED,
        COMPLETED
    }

    @Exclude // The document ID will serve as the ID
    private String id;          // Firestore Document ID
    private String tutorUid;    // Firebase UID of the tutor
    private String tuteeUid;    // Firebase UID of the tutee
    private String subjectId;   // Firestore Document ID of the Subject
    private String subjectName; // Denormalized for easier display
    private String location;
    private Timestamp startTime; // Use Firestore Timestamp
    private Timestamp endTime;   // Use Firestore Timestamp
    private Status status;
    private String tutorReview;
    private String tuteeReview;
    private Double tutorRating; // Rating given by the tutee to the tutor
    private Double tuteeRating; // Rating given by the tutor to the tutee
    private Timestamp createdAt; // Optional: when the session request was created

    // Firestore requires a public no-argument constructor
    public Session() {
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTutorUid() { return tutorUid; }
    public void setTutorUid(String tutorUid) { this.tutorUid = tutorUid; }

    public String getTuteeUid() { return tuteeUid; }
    public void setTuteeUid(String tuteeUid) { this.tuteeUid = tuteeUid; }

    public String getSubjectId() { return subjectId; }
    public void setSubjectId(String subjectId) { this.subjectId = subjectId; }

    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public Timestamp getStartTime() { return startTime; }
    public void setStartTime(Timestamp startTime) { this.startTime = startTime; }

    public Timestamp getEndTime() { return endTime; }
    public void setEndTime(Timestamp endTime) { this.endTime = endTime; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public String getTutorReview() { return tutorReview; }
    public void setTutorReview(String tutorReview) { this.tutorReview = tutorReview; }

    public String getTuteeReview() { return tuteeReview; }
    public void setTuteeReview(String tuteeReview) { this.tuteeReview = tuteeReview; }

    public Double getTutorRating() { return tutorRating; }
    public void setTutorRating(Double tutorRating) { this.tutorRating = tutorRating; }

    public Double getTuteeRating() { return tuteeRating; }
    public void setTuteeRating(Double tuteeRating) { this.tuteeRating = tuteeRating; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    // Utility methods to convert between java.util.Date and com.google.firebase.Timestamp if needed
    // These are useful if your UI components or other logic still use java.util.Date
    @Exclude
    public java.util.Date getStartTimeAsDate() {
        return startTime != null ? startTime.toDate() : null;
    }

    @Exclude
    public void setStartTimeAsDate(java.util.Date date) {
        if (date != null) {
            this.startTime = new Timestamp(date);
        } else {
            this.startTime = null;
        }
    }

    @Exclude
    public java.util.Date getEndTimeAsDate() {
        return endTime != null ? endTime.toDate() : null;
    }

    @Exclude
    public void setEndTimeAsDate(java.util.Date date) {
        if (date != null) {
            this.endTime = new Timestamp(date);
        } else {
            this.endTime = null;
        }
    }
    @Exclude
    public java.util.Date getCreatedAtAsDate() {
        return createdAt != null ? createdAt.toDate() : null;
    }

    @Exclude
    public void setCreatedAtAsDate(java.util.Date date) {
        if (date != null) {
            this.createdAt = new Timestamp(date);
        } else {
            this.createdAt = null;
        }
    }
}
