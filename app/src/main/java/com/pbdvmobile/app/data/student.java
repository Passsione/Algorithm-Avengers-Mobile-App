package sum;

public class student {

        private String name;

        public student(String name) {
            this.name = name;
        }

        public boolean requestSession(Tutor tutor, TimeSlot requestedSlot) {
            for (TimeSlot slot : tutor.getAvailableSlots()) {
                if (!slot.overlaps(requestedSlot)) continue;

                System.out.println("Booking confirmed for student " + name + " with tutor " + tutor.getName() + " at " + requestedSlot);
                tutor.getAvailableSlots().remove(slot);
                return true;
            }
            System.out.println("Requested time is not available for tutor " + tutor.getName());
            return false;
        }
    }

