package com.pbdvmobile.app.data.model;

import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Session implements Serializable{
    public enum Status {
        PENDING,
        CONFIRMED,
        CANCELLED,
        DECLINED,
        COMPLETED
    }

    private int id; // Id set to AUTO INCREMENT
    private int tutorId;
//    private int tuteeId;
    private String tuteeIdsJson;
    private int subjectId;
    private String location; // lat and long JSON
    private Date startTime;
    private Date endTime;
    private Status status;
    private String tutorReview;
    private String tuteeReview;
    private Double tutorRating;
    private Double tuteeRating;
    private  int maxStudents = 10;
    // Constructor
    public Session() {
        this.tuteeIdsJson = new Gson().toJson(new ArrayList<Integer>()); // Initialize with empty list JSON
    }

    public Session(int tutorId, int subId) {
        this.tutorId = tutorId;
//        this.tuteeId = tuteeId;
        this.subjectId = subId;
        this.tuteeIdsJson = new Gson().toJson(new ArrayList<Integer>()); // Initialize with empty list JSON
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

    /*public int getTuteeId() {
        return tuteeId;
    }

    public void setTuteeId(int tuteeId) {
        this.tuteeId = tuteeId;
    }
*/
    public int getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(int subjectId) {
        this.subjectId = subjectId;
    }
    public int getSessionMax() {
        return maxStudents;
    }

    public void setSessionMax(int maxStudents) {
        this.maxStudents = maxStudents;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public List<Integer> getTuteeIds() {
        if (tuteeIdsJson == null || tuteeIdsJson.isEmpty()) {
            return new ArrayList<>();
        }
        Type type = new TypeToken<ArrayList<Integer>>() {}.getType();
        return new Gson().fromJson(tuteeIdsJson, type);
    }

    /**
     * Trims the List if there are most then maxStudents
     * **/

    public void setTuteeIds(List<Integer> tuteeIds) {
        if (tuteeIds.size() > maxStudents) {
            List<Integer> trimmedList = tuteeIds.subList(0, maxStudents - 1);
            this.tuteeIdsJson = new Gson().toJson(trimmedList);
        }else
            this.tuteeIdsJson = new Gson().toJson(tuteeIds);
    }

    // Example methods to manage tutees in the list
    public boolean addTutee(int tuteeId) {
        List<Integer> currentTutees = getTuteeIds();
        if (currentTutees.size() < maxStudents && !currentTutees.contains(tuteeId)) {
            currentTutees.add(tuteeId);
            setTuteeIds(currentTutees);
            return true;
        }
        return false; // Either full or tutee already booked
    }

    public boolean removeTutee(int tuteeId) {
        List<Integer> currentTutees = getTuteeIds();
        if (currentTutees.remove(Integer.valueOf(tuteeId))) {
            setTuteeIds(currentTutees);
            return true;
        }
        return false;
    }
    // other fields like tutorId, sessionTime, location, subject, etc.
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