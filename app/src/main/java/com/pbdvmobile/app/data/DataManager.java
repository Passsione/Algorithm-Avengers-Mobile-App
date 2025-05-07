package com.pbdvmobile.app.data;
import android.content.Context;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.pbdvmobile.app.data.dao.PrizeDao;
import com.pbdvmobile.app.data.dao.ResourceDao;
import com.pbdvmobile.app.data.dao.SessionDao;
import com.pbdvmobile.app.data.dao.SubjectDao;
import com.pbdvmobile.app.data.dao.UserDao;
import com.pbdvmobile.app.data.model.Session;
import com.pbdvmobile.app.data.model.Subject;
import com.pbdvmobile.app.data.model.User;
import com.pbdvmobile.app.data.model.UserSubject;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

public class DataManager implements Serializable {
    private static DataManager instance;
    private final SqlOpenHelper dbHelper;
    private final UserDao userDao;
    private final SubjectDao subjectDao;
    private final SessionDao sessionDao;
    private final ResourceDao resourceDao;
    private final PrizeDao prizeDao;

    private final String[] subjects = {
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
        userDao = new UserDao(dbHelper);
        subjectDao = new SubjectDao(dbHelper);
        sessionDao = new SessionDao(dbHelper);
        resourceDao = new ResourceDao(dbHelper);
        prizeDao = new PrizeDao(dbHelper);

    }

    public static synchronized DataManager getInstance(Context context) {
        if (instance == null) {
            instance = new DataManager(context.getApplicationContext());
            // adds subjects to database
            if (instance.getSubjectDao().getAllSubjects().isEmpty()) {
                for (String subject : instance.subjects) {
                    instance.getSubjectDao().insertSubject(new Subject(subject));
                }
            }
            // dumby Tutor
            if (instance.getUserDao().getUserByStudentNum(11111111) == null) {
                User dumbyTutee = new User(11111111, "Dumby", "Tutor");
                dumbyTutee.setEmail("11111111@dut4life.ac.za");
                dumbyTutee.setPassword("password1");
                dumbyTutee.setTutor(true);
                dumbyTutee.setEducationLevel(User.EduLevel.values()[instance.randomIndex(User.EduLevel.values().length)]);
                instance.getUserDao().insertUser(dumbyTutee);
            }
            // main account
            if (instance.getUserDao().getUserByStudentNum(22323809) == null) {
                User mainUser = new User(22323809, "Mogale", "Tshehla");
                mainUser.setEmail("22323809@dut4life.ac.za");
                mainUser.setPassword("password1");
                mainUser.setEducationLevel(User.EduLevel.BACHELOR);
                instance.getUserDao().insertUser(mainUser);
            }
            //assigns users random subjects, marks and qualifies them
            if (instance.getSubjectDao().getAllUserSubjects().isEmpty()) {
                List<User> users = instance.getUserDao().getAllUsers();
                if (!users.isEmpty())
                    for (User user : users) {
                        for (int s = 0; s < 8; s++) {
                            int subID = s + 1;
                            UserSubject userSubject = new UserSubject(user.getStudentNum(), subID, instance.randomIndex(101));
                            userSubject.setTutoring(instance.qualifies(userSubject, user));
                            instance.getSubjectDao().addUserSubject(userSubject);
                        }
                    }
            }
            // dumby session
            if(instance.getSessionDao().getSessionsByTuteeId(22323809).isEmpty()) {
                List<UserSubject> userSubject = instance.getSubjectDao().getUserSubjects(22323809);
                int subId = userSubject.get(instance.randomIndex(userSubject.size())).getSubjectId();
                Session session = new Session(11111111, subId);
                session.setTuteeIds(List.of(22323809));
                Date startTime = new Date();
                // Set start time to 34 hours from now
                startTime.setTime(startTime.getTime() + 34L * 60 * 60 * 1000);
                Date endTime = new Date();
                // Set end time to 2 hours after the start time
                endTime.setTime(startTime.getTime() + 2L * 60 * 60 * 1000);

                session.setStartTime(startTime);
                session.setEndTime(endTime);
                session.setStatus(Session.Status.CONFIRMED);
                // Make sure insertSession returns a meaningful value (e.g., new row ID or 1 for success)
                long insertResult = instance.getSessionDao().insertSession(session);
                Toast.makeText(context, "Dummy session creation result: " + insertResult, Toast.LENGTH_SHORT).show();

            }

    }

        return instance;
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

    public void fillUserSubject(int studentNum){
        for(int s = 0; s < 8; s++) {
            int mark = randomIndex(101);
            UserSubject userSubject = new UserSubject(studentNum, 1 + randomIndex(subjects.length), mark);
            userSubject.setTutoring(qualifies(userSubject, getUserDao().getUserByStudentNum(studentNum)));
            getSubjectDao().addUserSubject(userSubject);
        }
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

    public void displayError(View v, TextView anchor, String error){
        Snackbar.make(v, error, Snackbar.LENGTH_LONG)
                .setAnchorView(anchor)
                .setAction("Action", null).show();
    }
    public boolean validDut(String email){
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
    public boolean qualifies(UserSubject userSubject, User tutor) {
        if(userSubject.getMark() >= 65){
            return true;
        }
        else{
            return false;
        }
    }

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