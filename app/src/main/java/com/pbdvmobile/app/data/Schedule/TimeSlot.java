package com.pbdvmobile.app.data.Schedule;

import android.util.Log;

import java.io.Serializable;
import java.util.Date;

public class TimeSlot implements Serializable {

    Date internalStartTime;
    Date internalEndTime;
    // padding (15 minutes in milliseconds) around the sessions times
    public static final int DEFAULT_TIME_PADDING_MIN = 15;
    public static final long DEFAULT_TIME_PADDING = DEFAULT_TIME_PADDING_MIN * 60 * 1000;
    Date actualStartTime; // Store actual unpadded start time
    Date actualEndTime;   // Store actual unpadded end time

    public TimeSlot(Date actualStartTime, Date actualEndTime) {
        this(actualStartTime, actualEndTime, DEFAULT_TIME_PADDING);
    }

    public TimeSlot(Date actualStartTime, Date actualEndTime, long customTimePadding) {
        if (actualStartTime == null || actualEndTime == null) {
            throw new IllegalArgumentException("Start time and end time cannot be null.");
        }
        if (actualEndTime.before(actualStartTime)) {
            // Or handle as an invalid slot, perhaps by not applying padding
            // For now, let's assume valid inputs or that this state is caught elsewhere.
            Log.w("TimeSlot", "EndTime is before StartTime for actual times. Padding might behave unexpectedly.");
        }

        // Store copies of actual times
        this.actualStartTime = new Date(actualStartTime.getTime());
        this.actualEndTime = new Date(actualEndTime.getTime());

        // Apply padding to internal representations for overlap checks
        this.internalStartTime = new Date(actualStartTime.getTime() - customTimePadding);
        this.internalEndTime = new Date(actualEndTime.getTime() + customTimePadding);
    }

    public Date getInternalStartTime() { return internalStartTime; }
    public Date getInternalEndTime() { return internalEndTime; }

    public boolean overlaps(TimeSlot other) {
      return (internalStartTime.before(other.internalEndTime) && internalEndTime.after(other.internalStartTime));
    }

    @Override
    public String toString() {
      return "TimeSlot{" + "start=" + internalStartTime + ", end=" + internalEndTime + '}';
    }

    public Date getActualStartTime() {
        return actualStartTime;
    }

    public Date getActualEndTime() {
        return actualEndTime;
    }
}

