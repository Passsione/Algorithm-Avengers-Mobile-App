package com.pbdvmobile.app.data.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class User implements Serializable {
    public enum TierLevel {
        BASIC,
        PREMIUM,
        VIP
    }

    public enum EduLevel {
        HIGHER_CERF,
        BACHELOR,
        ADV_DIP,
        DIP,
        HONOURS,
        MASTER,
        PHD
    }

    private int studentNum;
    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private String bio;
    private EduLevel educationLevel;
    private boolean tutor;
    private TierLevel tierLevel;
    private double averageRating;
    private String profileImageUrl;
    private double credits;
    private String subjects; // JSON string of subjects
    private String bankDetails;

    // Constructor
    public User() {
        this.tutor = false;
        this.averageRating = -1; // Never been rated
        this.tierLevel = TierLevel.BASIC;
    }
    public User(int stuNum, String fName, String lName) {
        this.studentNum = stuNum;
        this.firstName = fName;
        this.lastName = lName;
        this.tutor = false;
        this.averageRating = -1;
        this.tierLevel = TierLevel.BASIC;
    }
    public int getStudentNum() {
        return studentNum;
    }

    public void setStudentNum(int studentNum) {
        this.studentNum = studentNum;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public EduLevel getEducationLevel() {
        return educationLevel;
    }

    public void setEducationLevel(EduLevel educationLevel) {
        this.educationLevel = educationLevel;
    }

    public boolean isTutor() {
        return tutor;
    }

    public void setTutor(boolean tutor) {
        this.tutor = tutor;
    }

    public TierLevel getTierLevel() {
        return tierLevel;
    }

    public void setTierLevel(TierLevel tierLevel) {
        this.tierLevel = tierLevel;
    }

    public double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(double averageRating) {
        this.averageRating = averageRating;
    }


    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public double getCredits() {
        return credits;
    }

    public void setCredits(double credits) {
        this.credits = credits;
    }

    public String getSubjects() {
        return subjects;
    }

    public void setSubjects(String subjects) {
        this.subjects = subjects;
    }

    public String getBankDetails() {
        return bankDetails;
    }

    public void setBankDetails(String bankDetails) {
        this.bankDetails = bankDetails;
    }
}