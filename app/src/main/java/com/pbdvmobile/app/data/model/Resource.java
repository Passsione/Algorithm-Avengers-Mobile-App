package com.pbdvmobile.app.data.model;

public class Resource {
    private int resourcesId;// Id set to AUTO INCREMENT
    private String resource; // url
    private int tutorId;

    // Constructor
    public Resource() {
    }

    public Resource(int tutorId, String resLink) {
        this.tutorId = tutorId;
        this.resource = resLink;
    }

    // Getters and Setters
    public int getResourcesId() {
        return resourcesId;
    }

    public void setResourcesId(int resourcesId) {
        this.resourcesId = resourcesId;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public int getTutorId() {
        return tutorId;
    }

    public void setTutorId(int tutorId) {
        this.tutorId = tutorId;
    }
}
