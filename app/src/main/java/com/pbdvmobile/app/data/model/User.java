package com.pbdvmobile.app.data.model;

import com.google.firebase.firestore.Exclude;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class User implements Serializable {

    private String uid; // Firebase Auth User ID - will be the document ID
    private int studentNum; // Keep if still relevant, otherwise can be removed or made optional
    private String firstName;
    private String lastName;
    private String email;
    // Password is NOT stored in Firestore document directly
    private String bio;
    private EduLevel educationLevel;
    private boolean isTutor;
    private TierLevel tierLevel;
    private double averageRatingAsTutor; // Calculated from sessions where this user was a tutor
    private double averageRatingAsTutee; // Calculated from sessions where this user was a tutee
    private String profileImageUrl;
    private double credits;
    private List<String> tutoredSubjectIds; // List of Firestore document IDs for Subjects
    private String bankDetails;

    public enum TierLevel {
        BASIC, PREMIUM, VIP
    }

    public enum EduLevel {
        HIGHER_CERF, BACHELOR, ADV_DIP, DIP, HONOURS, MASTER, PHD
    }

    // Firestore requires a public no-argument constructor for deserialization
    public User() {
        this.isTutor = false;
        this.averageRatingAsTutor = 0.0;
        this.averageRatingAsTutee = 0.0;
        this.tierLevel = TierLevel.BASIC;
        this.credits = 10.0; // Default starting credits
        this.tutoredSubjectIds = new ArrayList<>();
    }

    // Constructor for initial creation
    public User(String uid, int studentNum, String firstName, String lastName, String email) {
        this(); // Call no-arg constructor for defaults
        this.uid = uid;
        this.studentNum = studentNum; // Ensure this is set if used
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }

    // Getters and Setters (Public for Firestore)
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public int getStudentNum() { return studentNum; }
    public void setStudentNum(int studentNum) { this.studentNum = studentNum; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    @Exclude // Exclude password from being stored in Firestore
    public String getPassword() { return null; /* Managed by Firebase Auth */ }
    @Exclude
    public void setPassword(String password) { /* Not stored in Firestore */ }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public EduLevel getEducationLevel() { return educationLevel; }
    public void setEducationLevel(EduLevel educationLevel) { this.educationLevel = educationLevel; }

    public boolean isTutor() { return isTutor; } // Firestore maps this to "tutor" field if getter is isTutor()
    public void setTutor(boolean tutor) { isTutor = tutor; }

    public TierLevel getTierLevel() { return tierLevel; }
    public void setTierLevel(TierLevel tierLevel) { this.tierLevel = tierLevel; }

    public double getAverageRatingAsTutor() { return averageRatingAsTutor; }
    public void setAverageRatingAsTutor(double averageRatingAsTutor) { this.averageRatingAsTutor = averageRatingAsTutor; }

    public double getAverageRatingAsTutee() { return averageRatingAsTutee; }
    public void setAverageRatingAsTutee(double averageRatingAsTutee) { this.averageRatingAsTutee = averageRatingAsTutee; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

    public double getCredits() { return credits; }
    public void setCredits(double credits) { this.credits = credits; }

    public List<String> getTutoredSubjectIds() { return tutoredSubjectIds; }
    public void setTutoredSubjectIds(List<String> tutoredSubjectIds) { this.tutoredSubjectIds = tutoredSubjectIds; }

    @Exclude // This method was for local SQLite.
    public String getSubjects() { return null; /* Implement based on tutoredSubjectIds if needed for display by fetching Subject names */ }
    @Exclude
    public void setSubjects(String subjectsJson) { /* Parse JSON to tutoredSubjectIds or ignore if IDs are directly managed */ }

    public String getBankDetails() { return bankDetails; }
    public void setBankDetails(String bankDetails) { this.bankDetails = bankDetails; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(uid, user.uid); // UID is the unique identifier
    }

    @Override
    public int hashCode() {
        return Objects.hash(uid);
    }
}
