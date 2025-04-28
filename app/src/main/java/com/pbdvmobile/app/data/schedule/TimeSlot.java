package com.pbdvmobile.app.data.schedule;

import java.util.Date;

public class TimeSlot {

    Date startTime;
    Date endTime;
    // padding (15 minutes in milliseconds) around the sessions times
    long timePadding = 15 * 60 * 1000;

    public TimeSlot(Date startTime, Date endTime) {
        startTime.setTime(startTime.getTime() - timePadding);
        endTime.setTime(endTime.getTime() + timePadding);
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public Date getStartTime() { return startTime; }
    public Date getEndTime() { return endTime; }

    public boolean overlaps(TimeSlot other) {
      return (startTime.before(other.endTime) && endTime.after(other.startTime));
    }

    @Override
    public String toString() {
      return "TimeSlot{" + "start=" + startTime + ", end=" + endTime + '}';
    }
}

