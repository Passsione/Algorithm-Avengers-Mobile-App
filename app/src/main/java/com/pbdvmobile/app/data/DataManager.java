package com.pbdvmobile.app.data;
import android.content.Context;
import android.graphics.BlendMode;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;
import com.pbdvmobile.app.data.dao.PrizeDao;
import com.pbdvmobile.app.data.dao.ResourceDao;
import com.pbdvmobile.app.data.dao.SessionDao;
import com.pbdvmobile.app.data.dao.SubjectDao;
import com.pbdvmobile.app.data.dao.UserDao;
import com.pbdvmobile.app.data.model.Subject;
import com.pbdvmobile.app.data.model.User;
import com.pbdvmobile.app.data.model.UserSubject;

import java.util.regex.Pattern;

public class DataManager {
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
        "ASDM401: Advanced Software Development and Management: [SEM1]",
        "NOPS201: NETWORKS AND OPERATING SYSTEMS II: [SEM1]",
        "SKDA201: SKILLS DEVELOPMENT IIA: [SEM1]",
        "SYSA201: SYSTEMS ANALYSIS II: [SEM1]",
        "SFEN301: SOFTWARE ENGINEERING III: [SEM1]",
        "MINF301: MANAGEMENT OF INFORMATION III: [SEM1]",
        "FPRM101: PROGRAMMING I: [YEAR]",
        "FCPR101: COMPUTER AND PROGRAMMING SKILLS I: [SEM1]",
        "ESYS401: EXPERT SYSTEMS IV: [YEAR]",
        "COPE102: COMPUTER OPERATING I: [YEAR]",
        "ARIN402: ARTIFICIAL INTELLIGENCE IV: [SEM1]",
        "RPDI681: RESEARCH PROJECT AND DISSERTATION (8TH REG): [P0]",
        "PROG103: PROGRAMMING I: [SEM1]",
        "ADDA401: Advanced Data Analytics: [SEM1]",
        "IPRO201: INTERNET PROGRAMMING II: [YEAR]: 2025",
        "BSAN302: BUSINESS ANALYSIS III: [YEAR]",
        "SYPM401: SYSTEMS AND PROJECT MANAGEMENT IV: [SEM1]",
        "SWSK121: SOFTWARE SKILLS I (MODULE 2): [SEM1]",
        "PRJA301: PROJECT IIIA: [SEM1]",
        "PRGM201: PROGRAMMING II: [SEM1]",
        "OSYS101: OPERATING SYSTEMS: [SEM1]",
        "ISYS304: INFORMATION SYSTEMS III: [SEM1]",
        "ISYS203: INFORMATION SYSTEMS II: [YEAR]",
        "ILGT101: IT LOGIC AND TECHNOLOGY I: [YEAR]",
        "IISY402: INTELLIGENT INDUSTRIAL SYSTEMS IV: [SEM1]",
        "FISY331: FINANCIAL INFORMATION SYSTEMS III (MOD 3): [SEM1]",
        "FISY301: FINANCIAL INFORMATION SYSTEMS III: [SEM1]",
        "FISY201: FINANCIAL INFORMATION SYSTEMS II: [SEM1]",
        "ADSW401: ADVANCED DEVELOPMENT SOFTWARE IV: [SEM1]",
        "CLCO401: Cloud computing: [SEM1]",
        "RPDI611: RESEARCH PROJECT AND DISSERTATION (1ST REG): [P0]",
        "PRGM101: PROGRAMMING I: [SEM1]",
        "WEBP101: WEB PROJECT I: [SEM1]",
        "WBTC102: WEB TECHNOLOGY: [SEM1]",
        "FISY101: FINANCIAL INFORMATION SYSTEMS I: [SEM1]: 2025",
        "APDB201: APPLICATIONS DEVELOPMENT IIB: [SEM2]",
        "IEXP101: INDUSTRY EXPOSURE: [SEM2]",
        "IPRO201: INTERNET PROGRAMMING II: [SEM2]",
        "SSFT213: SYSTEMS SOFTWARE II (MOD 1): [SEM2]",
        "ADSW401: ADVANCED DEVELOPMENT SOFTWARE IV: [SEM2]",
        "CNTW101: COMMUNICATION NETWORKS I: [SEM2]",
        "SWSK121: SOFTWARE SKILLS I (MODULE 2): [SEM2]",
        "SFEN301: SOFTWARE ENGINEERING III: [SEM2]",
        "RMIT121: RESEARCH METHODOLOGY (2ND REGISTRATION): [SEM2] ",
        "FCSL111: COMPUTER SKILLS I (MODULE 1): [SEM2]",
        "BISY201: BUSINESS INFORMATION SYSTEMS II: [SEM2]",
        "APRE402: Applied Research ICT: [SEM2]",
        "PROG103: PROGRAMMING I: [SEM2]",
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
        "ALDS201: ALGORITHMS AND DATA STRUCTURES II: [SEM2]"
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
        }


        // adds subjects to database
        if(instance.getSubjectDao().getAllSubjects().isEmpty()){
            for (String subject: instance.subjects) {
                instance.getSubjectDao().insertSubject(new Subject(subject));
            }
        }

        //assigns users random subjects
        if (instance.getSubjectDao().getAllUserSubjects().isEmpty()){
            for (User user: instance.getUserDao().getAllUsers()) {
                for(int s = 0; s < 8; s++) {
                    int subID = instance.randomIndex(instance.subjects.length);
                    instance.getSubjectDao().addUserSubject(
                            new UserSubject(user.getStudentNum(), subID, instance.randomIndex(100)));
                }
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

    // Sample usage in activity
    public void initialize() {

        // This method would be called once when the app starts
        // It can handle any initial database setup or data insertion


    }

    public void fillUserSubject(int studentNum){
        for(int s = 0; s < 8; s++) {
            getSubjectDao().addUserSubject(
                    new UserSubject(studentNum, randomIndex(subjects.length), randomIndex(100)));
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


}