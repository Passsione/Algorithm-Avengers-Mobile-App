package com.pbdvmobile.app;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.pbdvmobile.app.data.DataManager;
import com.pbdvmobile.app.data.LogInUser;
import com.pbdvmobile.app.data.model.Prize;
import com.pbdvmobile.app.data.model.RedeemPrize;
import com.pbdvmobile.app.data.model.User;
import com.pbdvmobile.app.fragments.PrizesViewFragment; // Assuming this exists

import java.util.List;

public class RedeemCreditActivity extends AppCompatActivity implements PrizesViewFragment.OnPrizeRedeemListener {

    private TextView textViewUserCreditsValue;
    private TextView textViewNoPrizes;
    private DataManager dataManager;
    private LogInUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_redeem_credit);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        textViewUserCreditsValue = findViewById(R.id.textViewUserCreditsValue);
        textViewNoPrizes = findViewById(R.id.textViewNoPrizes);

        dataManager = DataManager.getInstance(this);
        currentUser = LogInUser.getInstance();

        if (!currentUser.isLoggedIn()) {
            // Redirect to login or handle appropriately
            Toast.makeText(this, "Please log in to redeem credits.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        updateUserCreditsDisplay();
        loadPrizesFragment();
    }

    private void updateUserCreditsDisplay() {
        if (currentUser.getUser() != null) {
            textViewUserCreditsValue.setText(String.valueOf(currentUser.getUser().getCredits()));
        } else {
            textViewUserCreditsValue.setText("N/A");
        }
    }

    private void loadPrizesFragment() {
        List<Prize> prizes = dataManager.getPrizeDao().getAllPrizes(); // Assuming Prize model has cost

        if (prizes == null || prizes.isEmpty()) {
            textViewNoPrizes.setVisibility(View.VISIBLE);
            findViewById(R.id.prizesFragmentContainer).setVisibility(View.GONE);
        } else {
            textViewNoPrizes.setVisibility(View.GONE);
            findViewById(R.id.prizesFragmentContainer).setVisibility(View.VISIBLE);

            PrizesViewFragment fragment = PrizesViewFragment.newInstance(prizes);
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.prizesFragmentContainer, fragment);
            fragmentTransaction.commit();
        }
    }

    @Override
    public void onRedeemPrize(Prize prizeToRedeem) {
        User user = currentUser.getUser();
        if (user == null) {
            Toast.makeText(this, "User data not found.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Assume Prize object has a getCostInCredits() method
        // int prizeCost = prizeToRedeem.getCostInCredits(); // You'll need to add this field to your Prize model & DB

        // For now, let's assume a placeholder cost or that this logic is handled
        // by a check before calling this (e.g., button disabled if not enough credits)
        // Or, if PrizeDao handles cost, or you add a 'cost' field to Prize model:
        // For example:
        int placeholderPrizeCost = 100; // Replace with actual prize.getCost()

        if (user.getCredits() >= placeholderPrizeCost) {
            // 1. Deduct credits
            user.setCredits(user.getCredits() - placeholderPrizeCost);
            dataManager.getUserDao().updateUser(user); // Update user in DB

            // 2. Record redemption
            RedeemPrize redemption = new RedeemPrize();
            redemption.setStudentNum(user.getStudentNum());
            redemption.setPrizeId(prizeToRedeem.getPrizeId());
            redemption.setStatus(RedeemPrize.Status.PENDING); // Or COMPLETED if immediate
            long redemptionId = dataManager.getPrizeDao().insertRedeemPrize(redemption);

            if (redemptionId > 0) {
                Toast.makeText(this, prizeToRedeem.getPrizeName() + " redeemed successfully!", Toast.LENGTH_LONG).show();
                updateUserCreditsDisplay();
                // Optionally, refresh the prize list or update the UI for that specific prize
            } else {
                Toast.makeText(this, "Failed to record redemption.", Toast.LENGTH_SHORT).show();
                // Rollback credit deduction if necessary, though less critical if only local DB
            }
        } else {
            Toast.makeText(this, "Not enough credits to redeem " + prizeToRedeem.getPrizeName(), Toast.LENGTH_SHORT).show();
        }
    }
}