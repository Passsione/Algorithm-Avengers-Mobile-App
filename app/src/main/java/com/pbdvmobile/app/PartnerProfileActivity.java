package com.pbdvmobile.app;

import android.os.Bundle;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.pbdvmobile.app.data.DataManager;
import com.pbdvmobile.app.data.LogInUser;
import com.pbdvmobile.app.data.model.User;

public class PartnerProfileActivity extends AppCompatActivity {


    DataManager dataManager = DataManager.getInstance(this);
    LogInUser current_user = LogInUser.getInstance(dataManager);
    ImageView partnerProfileImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_partner_profile);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        User partner = (User) getIntent().getSerializableExtra("tutor");

        initial_element();

        if(partner != null)set_data(partner);

    }

    private void set_data(User partner) {
        Glide.with(this)
                .load(partner.getProfileImageUrl())
                .placeholder(R.mipmap.ic_launcher_round)
                .error(R.mipmap.ic_launcher_round)
                .circleCrop()
                .into(partnerProfileImage);

    }

    private void initial_element(){

        partnerProfileImage = findViewById(R.id.imageViewPartner);
    }
}