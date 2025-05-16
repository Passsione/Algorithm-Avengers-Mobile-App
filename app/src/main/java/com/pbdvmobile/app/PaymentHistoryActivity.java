package com.pbdvmobile.app;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

// Placeholder Model
class PaymentTransaction {
    String id;
    double amount;
    long timestamp;
    String status; // e.g., "Successful", "Failed", "Pending"
    String description;

    public PaymentTransaction(String id, double amount, long timestamp, String status, String description) {
        this.id = id;
        this.amount = amount;
        this.timestamp = timestamp;
        this.status = status;
        this.description = description;
    }

    public String getFormattedDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
}

public class PaymentHistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerViewPaymentHistory;
    private TextView textViewNoPayments;
    private PaymentHistoryAdapter adapter;
    private List<PaymentTransaction> transactionList = new ArrayList<>(); // Populate this from DB

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_payment_history);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        recyclerViewPaymentHistory = findViewById(R.id.recyclerViewPaymentHistory);
        textViewNoPayments = findViewById(R.id.textViewNoPayments);

        recyclerViewPaymentHistory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PaymentHistoryAdapter(transactionList);
        recyclerViewPaymentHistory.setAdapter(adapter);

        loadPaymentHistory();
    }

    private void loadPaymentHistory() {
        // TODO: Replace with actual data fetching from your database
        transactionList.clear();
        // Dummy Data
        Random random = new Random();
        for (int i = 0; i < 10; i++) {
            double amount = 50 + random.nextInt(200);
            long time = System.currentTimeMillis() - (random.nextInt(30) * 24L * 60 * 60 * 1000); // Random time in last 30 days
            String status = random.nextBoolean() ? "Successful" : "Failed";
            transactionList.add(new PaymentTransaction("TRN00" + i, amount, time, status, "Credit Purchase - " + (int)(amount * 10) + " Credits"));
        }


        if (transactionList.isEmpty()) {
            textViewNoPayments.setVisibility(View.VISIBLE);
            recyclerViewPaymentHistory.setVisibility(View.GONE);
        } else {
            textViewNoPayments.setVisibility(View.GONE);
            recyclerViewPaymentHistory.setVisibility(View.VISIBLE);
            adapter.notifyDataSetChanged();
        }
    }

    // --- PaymentHistoryAdapter Inner Class ---
    private static class PaymentHistoryAdapter extends RecyclerView.Adapter<PaymentHistoryAdapter.TransactionViewHolder> {
        private List<PaymentTransaction> transactions;

        PaymentHistoryAdapter(List<PaymentTransaction> transactions) {
            this.transactions = transactions;
        }

        @NonNull
        @Override
        public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.list_item_payment_transaction, parent, false);
            return new TransactionViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
            PaymentTransaction transaction = transactions.get(position);
            holder.textViewTransactionAmount.setText(String.format(Locale.getDefault(), "R %.2f", transaction.amount));
            holder.textViewTransactionDate.setText(transaction.getFormattedDate());
            holder.textViewTransactionStatus.setText(transaction.status);
            holder.textViewTransactionDescription.setText(transaction.description);

            // Set status color (similar to your session status logic)
            Context context = holder.itemView.getContext();
            if ("Successful".equalsIgnoreCase(transaction.status)) {
                holder.textViewTransactionStatus.setBackgroundResource(R.drawable.status_background_completed);
                holder.textViewTransactionStatus.setTextColor(ContextCompat.getColor(context, R.color.status_completed));
            } else if ("Failed".equalsIgnoreCase(transaction.status)) {
                holder.textViewTransactionStatus.setBackgroundResource(R.drawable.status_background_cancelled);
                holder.textViewTransactionStatus.setTextColor(ContextCompat.getColor(context, R.color.status_cancelled));
            } else { // Pending or other
                holder.textViewTransactionStatus.setBackgroundResource(R.drawable.status_background_generic);
                holder.textViewTransactionStatus.setTextColor(ContextCompat.getColor(context, R.color.status_generic_gray));
            }
        }

        @Override
        public int getItemCount() {
            return transactions.size();
        }

        static class TransactionViewHolder extends RecyclerView.ViewHolder {
            ImageView imageViewTransactionIcon;
            TextView textViewTransactionAmount;
            TextView textViewTransactionDate;
            TextView textViewTransactionStatus;
            TextView textViewTransactionDescription;

            TransactionViewHolder(View itemView) {
                super(itemView);
                imageViewTransactionIcon = itemView.findViewById(R.id.imageViewTransactionIcon);
                textViewTransactionAmount = itemView.findViewById(R.id.textViewTransactionAmount);
                textViewTransactionDate = itemView.findViewById(R.id.textViewTransactionDate);
                textViewTransactionStatus = itemView.findViewById(R.id.textViewTransactionStatus);
                textViewTransactionDescription = itemView.findViewById(R.id.textViewTransactionDescription);
            }
        }
    }
}