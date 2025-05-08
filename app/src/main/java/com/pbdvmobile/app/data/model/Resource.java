package com.pbdvmobile.app.data.model;

import java.io.Serializable;

public class Resource implements Serializable {
    private int resourcesId;// Id set to AUTO INCREMENT
    private String resource; // url
    private String name; // Display Name

    private int tutorId;
    private int subjectId;

    // Constructor
    public Resource() {
    }

    public Resource(int tutorId, int subjectId, String resLink, String name) {
        this.tutorId = tutorId;
        this.resource = resLink;
        this.subjectId = subjectId;
        this.name = name;
    }

    // Getters and Setters
    public int getResourcesId() {
        return resourcesId;
    }

    public void setResourcesId(int resourcesId) {
        this.resourcesId = resourcesId;
    }
    public int getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(int subId) {
        this.subjectId = subId;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getTutorId() {
        return tutorId;
    }

    public void setTutorId(int tutorId) {
        this.tutorId = tutorId;
    }
}
