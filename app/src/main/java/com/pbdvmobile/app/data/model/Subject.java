package com.pbdvmobile.app.data.model;

import com.google.firebase.firestore.Exclude;
import java.io.Serializable;

public class Subject implements Serializable {
    @Exclude // The document ID will serve as the ID
    private String id; // Firestore Document ID
    private String subjectName;

    // Firestore requires a public no-argument constructor
    public Subject() {
    }

    public Subject(String subjectName) {
        this.subjectName = subjectName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }
}
