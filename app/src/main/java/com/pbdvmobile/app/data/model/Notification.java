package com.pbdvmobile.app.data.model;

import java.util.Date;
import java.util.Objects;

public class Notification {
    private int noteId;         // Primary Key, AUTOINCREMENT
    private Integer studentNum; // Foreign Key to User (student_num), nullable for system-wide notifications
    private String text;        // Changed from 'message' for consistency with previous suggestions
    private Status status;
    private Date date;
    private boolean remember;
    // private String type; // Optional: if you want different types of notifications (e.g., "session", "reminder", "system")
    // private int relatedItemId; // Optional: ID of related item (e.g., session_id)


    public enum Status {
        OPENED,
        SEALED
    }

    // Constructors
    public Notification() {
        this.date = new Date(); // Default to now
        this.status = Status.SEALED; // Default to new
    }

    public Notification(Integer studentNum, String text, boolean remember) {
        this(); // Call default constructor
        this.studentNum = studentNum;
        this.text = text;
        this.remember = remember;
    }
    public Notification(Integer studentNum, String text, Status status, Date date, boolean remember) {
        this.studentNum = studentNum;
        this.text = text;
        this.status = status;
        this.date = date;
        this.remember = remember;
    }


    // Getters
    public int getNoteId() {
        return noteId;
    }

    public Integer getStudentNum() {
        return studentNum;
    }

    public String getText() {
        return text;
    }

    public Status getStatus() {
        return status;
    }

    public Date getDate() {
        return date;
    }

    public boolean isRemember() {
        return remember;
    }

    // Setters
    public void setNoteId(int noteId) {
        this.noteId = noteId;
    }

    public void setStudentNum(Integer studentNum) {
        this.studentNum = studentNum;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public void setRemember(boolean remember) {
        this.remember = remember;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Notification that = (Notification) o;
        return noteId == that.noteId &&
                remember == that.remember &&
                Objects.equals(studentNum, that.studentNum) &&
                Objects.equals(text, that.text) &&
                status == that.status &&
                Objects.equals(date, that.date);
    }

    @Override
    public int hashCode() {
        return Objects.hash(noteId, studentNum, text, status, date, remember);
    }

}
