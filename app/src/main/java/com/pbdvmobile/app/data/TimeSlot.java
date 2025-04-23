package sum;

import java.time.LocalDateTime;

public class TimeSlot {
	  
          LocalDateTime startTime;
          LocalDateTime endTime;

          public TimeSlot(LocalDateTime startTime, LocalDateTime endTime) {
              this.startTime = startTime;
              this.endTime = endTime;
          }

          public LocalDateTime getStartTime() { return startTime; }
          public LocalDateTime getEndTime() { return endTime; }

          public boolean overlaps(TimeSlot other) {
              return (startTime.isBefore(other.endTime) && endTime.isAfter(other.startTime));
          }

          @Override
          public String toString() {
              return "TimeSlot{" + "start=" + startTime + ", end=" + endTime + '}';
          }
      }

