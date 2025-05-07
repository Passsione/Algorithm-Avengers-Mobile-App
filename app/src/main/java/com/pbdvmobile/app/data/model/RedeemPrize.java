package com.pbdvmobile.app.data.model;

public class RedeemPrize {
    public enum Status {
        PENDING,
        CONFIRMED,
        CANCELLED,
        DECLINED
    }

    private int redeemId;// Id set to AUTO INCREMENT
    private int studentNum;
    private int prizeId;
    private Status status;

    // Constructor
    public RedeemPrize() {
        this.status = Status.PENDING;
    }
    public RedeemPrize(int stuName, int prizeId) {
        this.status = Status.PENDING;
        this.studentNum = stuName;
        this.prizeId = prizeId;
    }

    public int getRedeemId() {
        return redeemId;
    }

    public void setRedeemId(int redeemId) {
        this.redeemId = redeemId;
    }

    public int getStudentNum() {
        return studentNum;
    }

    public void setStudentNum(int studentNum) {
        this.studentNum = studentNum;
    }

    public int getPrizeId() {
        return prizeId;
    }

    public void setPrizeId(int prizeId) {
        this.prizeId = prizeId;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    /*
    This table tracks which user has unlocked which prize.
Fields should include:
redeemId (PK)
userId (FK to User)
prizeId (FK to Prize)
redeemTimestamp (long)
expirationTimestamp (long, calculated based on Prize.durationMinutes if applicable)
usesRemaining (int, if Prize.maxUses > 0)**/
}