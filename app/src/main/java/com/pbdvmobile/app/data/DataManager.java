package com.pbdvmobile.app.data;
import static android.content.ContentValues.TAG;

import static java.security.AccessController.getContext;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.pbdvmobile.app.data.dao.NotificationDao;
import com.pbdvmobile.app.data.dao.PrizeDao;
import com.pbdvmobile.app.data.dao.ResourceDao;
import com.pbdvmobile.app.data.dao.SessionDao;
import com.pbdvmobile.app.data.dao.SubjectDao;
import com.pbdvmobile.app.data.dao.UserDao;
import com.pbdvmobile.app.data.model.Notification;
import com.pbdvmobile.app.data.model.Session;
import com.pbdvmobile.app.data.model.Subject;
import com.pbdvmobile.app.data.model.User;
import com.pbdvmobile.app.data.model.UserSubject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

public class DataManager implements Serializable {
    public static final double CREDIT_AI_QUIZ = 5;
    public static final double CREDIT_AI_SUMMARIZER = 3;

    // 5 minutes before the start of the session
    public static final long START_SESSION_PADDING = 5 * 60 * 1000;
    private static DataManager instance;
    private final SqlOpenHelper dbHelper;
    private final UserDao userDao;
    private final SubjectDao subjectDao;
    private final SessionDao sessionDao;
    private final ResourceDao resourceDao;
    private final PrizeDao prizeDao;
    private final NotificationDao notificationDao;
    private final Context context;

    public final String[] subjects = {
        "RESK401: RESEARCH SKILLS: [SEM1]",
        "PRGM301: PROGRAMMING III: [SEM1]",
        "RESK301: RESEARCH SKILLS: [SEM1]",
        "TPRG211: TECHNICAL PROGRAMMING II (MODULE 1): [SEM1]",
        "ISYS213: INFORMATION SYSTEMS II (MODULE 1): [SEM1]",
        "FISY321: FINANCIAL INFORMATION SYSTEMS III (MOD 2): [SEM1]",
        "RPDI681: RESEARCH PROJECT AND DISSERTATION (8TH REG): [P0]",
        "PROG103: PROGRAMMING I: [SEM1]",
       /* "ADDA401: Advanced Data Analytics: [SEM1]",
        "IPRO201: INTERNET PROGRAMMING II: [YEAR]: 2025",
        "BSAN302: BUSINESS ANALYSIS III: [YEAR]",
        "SYPM401: SYSTEMS AND PROJECT MANAGEMENT IV: [SEM1]",
        "APDB301: APPLICATIONS DEVELOPMENT IIIB: [SEM2]",
        "CMPG101: COMMERCIAL PROGRAMMING I: [SEM2]",
        "WBTC102: WEB TECHNOLOGY: [SEM2]",
        "PRGM201: PROGRAMMING II: [SEM2]",
        "SWSP101: SOFTWARE SUPPORT I: [SEM2]",
        "FISY311: FINANCIAL INFORMATION SYSTEMS III (MOD 1): [SEM2]",
        "NWRK101: NETWORKING I: [SEM2]",
        "NETW404: NETWORKS IV: [SEM2]",
        "ISYS324: INFORMATION SYSTEMS III (MODULE 2): [SEM2]",
        "ISYS304: INFORMATION SYSTEMS III: [SEM2]",
        "FISY201: FINANCIAL INFORMATION SYSTEMS II: [SEM2]",
        "ALDS201: ALGORITHMS AND DATA STRUCTURES II: [SEM2]"*/
    };

    private DataManager(Context context) {
        dbHelper = SqlOpenHelper.getInstance(context);
        userDao = new UserDao();
        subjectDao = new SubjectDao();
        sessionDao = new SessionDao();
        resourceDao = new ResourceDao();
        prizeDao = new PrizeDao(dbHelper);
        notificationDao = new NotificationDao(dbHelper);
        this.context = context;

    }

    public static synchronized DataManager getInstance(Context context) {
        if (instance == null) {
            instance = new DataManager(context.getApplicationContext());
//            instance.populateInitialSubjects();
    }

        return instance;
    }

    // Method to populate initial subjects into Firestore (call cautiously, e.g., on first run or from admin)
    public void populateInitialSubjects() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        // Check if subjects collection is empty or has few items before populating
        db.collection(SubjectDao.SUBJECTS_COLLECTION).limit(1).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && (task.getResult() == null || task.getResult().isEmpty())) {
                Log.d(TAG, "Populating initial subjects into Firestore...");
                for (String subjectName : subjects) {
                    Subject newSubject = new Subject(subjectName);
                    subjectDao.insertSubject(newSubject)
                            .addOnSuccessListener(docRef -> Log.i(TAG, "Added subject: " + subjectName + " with ID: " + docRef.getId()))
                            .addOnFailureListener(e -> Log.e(TAG, "Error adding subject: " + subjectName, e));
                }
            } else if (task.isSuccessful()) {
                Log.d(TAG, "Subjects collection already has data. Skipping initial population.");
            } else {
                Log.e(TAG, "Error checking subjects collection before population.", task.getException());
            }
        });
    }

    public UserDao getUserDao() {
        return userDao;
    }

    public SubjectDao getSubjectDao() {
        return subjectDao;
    }

    public SessionDao getSessionDao() {
        return sessionDao;
    }

    public ResourceDao getResourceDao() {
        return resourceDao;
    }

    public PrizeDao getPrizeDao() {
        return prizeDao;
    }

    public NotificationDao getNotificationDao() {
        return notificationDao;
    }



    /**
     * Fills a user's profile with randomly assigned subjects they can tutor based on simulated marks.
     * This is an asynchronous operation.
     * @param studentNum The student number of the user to update.
     */
    public void fillUserSubject(int studentNum, boolean applyingTutor) {
        Log.d(TAG, "Attempting to fill subjects for studentNum: " + studentNum);

        Task<QuerySnapshot> userTask = userDao.getUserByStudentNum(studentNum);
        Task<QuerySnapshot> subjectsTask = subjectDao.getAllSubjects();

        Tasks.whenAllComplete(userTask, subjectsTask).addOnCompleteListener(allTasks -> {
            if (!allTasks.isSuccessful() || getContext() == null) {
                Log.e(TAG, "Failed to fetch user or subjects for fillUserSubject.");
                displayToast("Error: Could not fetch initial data to assign subjects.");
                // Check individual task failures for more specific error messages if needed
                if (!userTask.isSuccessful()) Log.e(TAG, "User fetch failed: ", userTask.getException());
                if (!subjectsTask.isSuccessful()) Log.e(TAG, "Subjects fetch failed: ", subjectsTask.getException());
                return;
            }

            QuerySnapshot userQuerySnapshot = userTask.getResult();
            User userToUpdate;
            if (userQuerySnapshot != null && !userQuerySnapshot.isEmpty()) {
                userToUpdate = userQuerySnapshot.getDocuments().get(0).toObject(User.class);
                if (userToUpdate != null) {
                    userToUpdate.setUid(userQuerySnapshot.getDocuments().get(0).getId()); // Ensure UID is set
                }
            } else {
                userToUpdate = null;
            }

            if (userToUpdate == null) {
                Log.e(TAG, "User with studentNum " + studentNum + " not found.");
                displayToast("Error: User not found.");
                return;
            }

            QuerySnapshot subjectQuerySnapshot = subjectsTask.getResult();
            List<Subject> availableSubjects = new ArrayList<>();
            if (subjectQuerySnapshot != null && !subjectQuerySnapshot.isEmpty()) {
                for (DocumentSnapshot doc : subjectQuerySnapshot.getDocuments()) {
                    Subject subject = doc.toObject(Subject.class);
                    if (subject != null) {
                        subject.setId(doc.getId()); // Ensure subject ID is set
                        availableSubjects.add(subject);
                    }
                }
            }

            if (availableSubjects.isEmpty()) {
                Log.w(TAG, "No subjects available in the database to assign.");
                displayToast("Warning: No subjects available to assign for tutoring.");
                // Update user as not a tutor if they previously were and now have no subjects
                if (userToUpdate.isTutor()) {
                    userToUpdate.setTutor(false);
                    userToUpdate.setTutoredSubjectIds(new ArrayList<>());
                    userDao.updateUser(userToUpdate)
                            .addOnSuccessListener(aVoid -> Log.d(TAG, "User " + studentNum + " updated (no longer tutor due to no available subjects)."))
                            .addOnFailureListener(e -> Log.e(TAG, "Failed to update user " + studentNum, e));
                }
                return;
            }

            List<String> qualifiedSubjectIdsForUser = new ArrayList<>();
            Random random = new Random();
            final int NUMBER_OF_SUBJECTS_TO_ASSIGN = Math.min(8, availableSubjects.size()); // Assign up to 8 or total available

            Log.d(TAG, "Assigning " + NUMBER_OF_SUBJECTS_TO_ASSIGN + " subjects to user " + studentNum);

            for (int i = 0; i < NUMBER_OF_SUBJECTS_TO_ASSIGN; i++) {
                Subject randomSubject = availableSubjects.get(random.nextInt(availableSubjects.size()));
                int mark = random.nextInt(101); // 0-100

                Log.d(TAG, "  Subject: " + randomSubject.getSubjectName() + " (ID: " + randomSubject.getId() + "), Mark: " + mark);

                if (randomSubject.getId() != null && !qualifiedSubjectIdsForUser.contains(randomSubject.getId())) {
                    if (qualifies(randomSubject.getId(), mark)) { // Qualification criteria
                    qualifiedSubjectIdsForUser.add(randomSubject.getId());
                    Log.d(TAG, "    Qualified for: " + randomSubject.getSubjectName());
                }
                }
            }

            if(applyingTutor)userToUpdate.setTutoredSubjectIds(qualifiedSubjectIdsForUser);
            userToUpdate.setTutor(!qualifiedSubjectIdsForUser.isEmpty() && applyingTutor);

            Log.d(TAG, "User " + studentNum + " isTutor: " + userToUpdate.isTutor() + ", Tutored IDs: " + qualifiedSubjectIdsForUser.toString());

            userDao.updateUser(userToUpdate)
                    .addOnSuccessListener(aVoid -> {
                        Log.i(TAG, "User " + studentNum + " profile updated successfully with tutored subjects.");
                        displayToast("User subjects assigned and profile updated.");
                        if (userToUpdate.isTutor() && qualifiedSubjectIdsForUser.isEmpty()) {
                            // This case should be handled by setTutor logic, but as a safeguard
                            displayToast("Warning: User is tutor but has no qualified subjects assigned.");
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to update user " + studentNum + " with tutored subjects.", e);
                        displayToast("Error: Failed to save subject assignments.");
                    });
        });
    }

    public boolean required(EditText...args){
        boolean fill = true;
        for (EditText et: args) {
            if(et.getText().toString().isEmpty()){
                et.setError("Required field");
                fill = false;
            }
        }
        return fill;
    }

    public void displayToast(String error){
        Toast.makeText(instance.context, error, Toast.LENGTH_LONG).show();
    }
    public boolean validDut(String email){
        if (email == null) return false;
        Pattern pattern = Pattern.compile("[0-9]{8}+@dut4life.ac.za");
        return pattern.matcher(email).matches();
    }

    public int randomIndex(int length){
        return (int)(Math.random() * (length));
    }
    public int retrieveStudentNum(String sEmail) {
        return Integer.parseInt(sEmail.split("@")[0]);
    }


    // the rule for qualifying as a tutor
    public boolean qualifies(String id , double mark) {
        if(mark >= 65){
            return true;
        }
        else{
            return false;
        }
    }

    /**
     * *
     * index = 0
     * @return  dow, mon dd yyyy
     * index = 1
     *@return  hh:mm:ss
     * index = 2
     * @return  hh:mm
     */
    public String[] formatDateTime(String date) {
        String[] result = new String[3];
        // dow, mon dd yyyy
        result[0] =date.split(" ")[0]+", "
                + date.split(" ")[1] +
                " "+date.split(" ")[2]
                +", "+date.split(" ")[5];
        // hh:mm:ss
        result[2] = date.split(" ")[3];
        // hh:mm
        result[1] = result[2].split(":")[0] +":"+result[2].split(":")[1];


        return result;
    }
}