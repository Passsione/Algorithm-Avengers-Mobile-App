package com.pbdvmobile.app.data.model;
import java.util.ArrayList;
import java.util.List;

public class User {

    public enum UserType {
        TUTOR,
        TUTEE
    }

    public enum TierLevel {
        BASIC,
        PREMIUM,
        VIP
    }

    private String id;
    private String fullName;
    private String email;
    private String bio;
    private String studentId;
    private String educationLevel;
    private UserType userType;
    private TierLevel tierLevel;
    private double hourlyRate; // no needed
    private double averageRating;
    private String profileImageUrl;
    private int credits;
    private List<Subject> subjects;

    public User() {
        subjects = new ArrayList<>();
        credits = 100; // Default starting credits
        tierLevel = TierLevel.BASIC; // Default tier
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getEducationLevel() {
        return educationLevel;
    }

    public void setEducationLevel(String educationLevel) {
        this.educationLevel = educationLevel;
    }

    public UserType getUserType() {
        return userType;
    }

    public void setUserType(UserType userType) {
        this.userType = userType;
    }

    public TierLevel getTierLevel() {
        return tierLevel;
    }

    public void setTierLevel(TierLevel tierLevel) {
        this.tierLevel = tierLevel;
    }

    public double getHourlyRate() {
        return hourlyRate;
    }

    public void setHourlyRate(double hourlyRate) {
        this.hourlyRate = hourlyRate;
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

    public int getCredits() {
        return credits;
    }

    public void setCredits(int credits) {
        this.credits = credits;
    }

    public List<Subject> getSubjects() {
        return subjects;
    }

    public void setSubjects(List<Subject> subjects) {
        this.subjects = subjects;
    }
}