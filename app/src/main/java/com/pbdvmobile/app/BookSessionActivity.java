package com.pbdvmobile.app;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.pbdvmobile.app.R;
import com.pbdvmobile.app.data.model.Session;
import com.pbdvmobile.app.data.model.Subject;
import com.pbdvmobile.app.data.model.User;
import com.pbdvmobile.app.services.SessionService;
import com.pbdvmobile.app.services.UserService;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class BookSessionActivity extends AppCompatActivity {

    private TextView tvTutorName, tvDate, tvStartTime, tvEndTime, tvTotalPrice;
    private Spinner spinnerSubject, spinnerLocation;
    private Button btnSelectDate, btnSelectStartTime, btnSelectEndTime, btnBook;

    private Calendar dateCalendar, startTimeCalendar, endTimeCalendar;
    private SimpleDateFormat dateFormat, timeFormat;
    private UserService userService;
    private SessionService sessionService;
    private User tutor;
    private String tutorId;
    private double hourlyRate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_session);

        tutorId = getIntent().getStringExtra("tutor_id");
        userService = new UserService();
        sessionService = new SessionService();

        dateFormat = new SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault());
        timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());

        dateCalendar = Calendar.getInstance();
        startTimeCalendar = Calendar.getInstance();
        endTimeCalendar = Calendar.getInstance();
        endTimeCalendar.add(Calendar.HOUR_OF_DAY, 1); // Default 1-hour session

        tvTutorName = findViewById(R.id.tv_tutor_name);
        tvDate = findViewById(R.id.tv_date);
        tvStartTime = findViewById(R.id.tv_start_time);
        tvEndTime = findViewById(R.id.tv_end_time);
        tvTotalPrice = findViewById(R.id.tv_total_price);
        spinnerSubject = findViewById(R.id.spinner_subject);
        spinnerLocation = findViewById(R.id.spinner_location);
        btnSelectDate = findViewById(R.id.btn_select_date);
        btnSelectStartTime = findViewById(R.id.btn_select_start_time);
        btnSelectEndTime = findViewById(R.id.btn_select_end_time);
        btnBook = findViewById(R.id.btn_book);

        // Load tutor info
        userService.getUserById(tutorId, new UserService.UserDetailCallback() {
            @Override
            public void onSuccess(User user) {
                tutor = user;
                tvTutorName.setText("Book a session with " + tutor.getFullName());
                hourlyRate = tutor.getHourlyRate();
                updateTotalPrice();

                // Populate subject spinner with tutor's subjects
                ArrayAdapter<String> subjectAdapter = new ArrayAdapter<>(
                        BookSessionActivity.this,
                        android.R.layout.simple_spinner_item);
                subjectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

                for (Subject subject : tutor.getSubjects()) {
                    subjectAdapter.add(subject.getName());
                }

                spinnerSubject.setAdapter(subjectAdapter);
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(BookSessionActivity.this, "Error loading tutor details: " + error, Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        // Setup location spinner
        ArrayAdapter<CharSequence> locationAdapter = ArrayAdapter.createFromResource(this,
                R.array.locations, android.R.layout.simple_spinner_item);
        locationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLocation.setAdapter(locationAdapter);

        // Update displayed date and times
        updateDateDisplay();
        updateTimeDisplay();

        btnSelectDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicker();
            }
        });

        btnSelectStartTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTimePicker(true);
            }
        });

        btnSelectEndTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTimePicker(false);
            }
        });

        btnBook.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bookSession();
            }
        });
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        dateCalendar.set(Calendar.YEAR, year);
                        dateCalendar.set(Calendar.MONTH, month);
                        dateCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        updateDateDisplay();
                    }
                },
                dateCalendar.get(Calendar.YEAR),
                dateCalendar.get(Calendar.MONTH),
                dateCalendar.get(Calendar.DAY_OF_MONTH)
        );

        // Set min date to today
        Calendar minDate = Calendar.getInstance();
        datePickerDialog.getDatePicker().setMinDate(minDate.getTimeInMillis());

        datePickerDialog.show();
    }

    private void showTimePicker(final boolean isStartTime) {
        Calendar calendar = isStartTime ? startTimeCalendar : endTimeCalendar;

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        if (isStartTime) {
                            startTimeCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                            startTimeCalendar.set(Calendar.MINUTE, minute);

                            // Ensure end time is later than start time
                            if (endTimeCalendar.before(startTimeCalendar)) {
                                endTimeCalendar.setTime(startTimeCalendar.getTime());
                                endTimeCalendar.add(Calendar.HOUR_OF_DAY, 1);
                            }
                        } else {
                            endTimeCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                            endTimeCalendar.set(Calendar.MINUTE, minute);

                            // Ensure end time is later than start time
                            if (endTimeCalendar.before(startTimeCalendar)) {
                                Toast.makeText(BookSessionActivity.this, "End time must be after start time", Toast.LENGTH_SHORT).show();
                                endTimeCalendar.setTime(startTimeCalendar.getTime());
                                endTimeCalendar.add(Calendar.HOUR_OF_DAY, 1);
                            }
                        }

                        updateTimeDisplay();
                        updateTotalPrice();
                    }
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                false
        );

        timePickerDialog.show();
    }

    private void updateDateDisplay() {
        tvDate.setText(dateFormat.format(dateCalendar.getTime()));
    }

    private void updateTimeDisplay() {
        tvStartTime.setText(timeFormat.format(startTimeCalendar.getTime()));
        tvEndTime.setText(timeFormat.format(endTimeCalendar.getTime()));
    }

    private void updateTotalPrice() {
        long durationMillis = endTimeCalendar.getTimeInMillis() - startTimeCalendar.getTimeInMillis();
        float durationHours = durationMillis / (1000.0f * 60 * 60);
        double totalPrice = hourlyRate * durationHours;

        tvTotalPrice.setText(String.format(Locale.getDefault(), "Total: $%.2f", totalPrice));
    }

    private void bookSession() {
        String subject = spinnerSubject.getSelectedItem().toString();
        String location = spinnerLocation.getSelectedItem().toString();

        // Set date and time components
        Calendar sessionStart = Calendar.getInstance();
        sessionStart.set(
                dateCalendar.get(Calendar.YEAR),
                dateCalendar.get(Calendar.MONTH),
                dateCalendar.get(Calendar.DAY_OF_MONTH),
                startTimeCalendar.get(Calendar.HOUR_OF_DAY),
                startTimeCalendar.get(Calendar.MINUTE)
        );

        Calendar sessionEnd = Calendar.getInstance();
        sessionEnd.set(
                dateCalendar.get(Calendar.YEAR),
                dateCalendar.get(Calendar.MONTH),
                dateCalendar.get(Calendar.DAY_OF_MONTH),
                endTimeCalendar.get(Calendar.HOUR_OF_DAY),
                endTimeCalendar.get(Calendar.MINUTE)
        );

        // Check if booking is in the future
        if (sessionStart.before(Calendar.getInstance())) {
            Toast.makeText(this, "Cannot book sessions in the past", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create session object
        Session session = new Session();
        session.setTutorId(tutorId);
        session.setTuteeId(userService.getCurrentUser().getId());
        session.setSubject(subject);
        session.setLocation(location);
        session.setStartTime(sessionStart.getTime());
        session.setEndTime(sessionEnd.getTime());
        session.setStatus(Session.Status.PENDING);

        // Book the session
        sessionService.bookSession(session, new SessionService.SessionCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(BookSessionActivity.this, "Session booked successfully", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(BookSessionActivity.this, "Error booking session: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
