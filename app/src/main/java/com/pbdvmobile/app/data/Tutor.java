package sum;

import java.util.ArrayList;
import java.util.List;

public class Tutor {
    
        private String name;
        private List<TimeSlot> availableSlots = new ArrayList<>();

        public Tutor(String name) {
            this.name = name;
        }

        public void addAvailability(TimeSlot slot) {
            availableSlots.add(slot);
        }

        public List<TimeSlot> getAvailableSlots() {
            return availableSlots;
        }

        public String getName() {
            return name;
        }
    }

