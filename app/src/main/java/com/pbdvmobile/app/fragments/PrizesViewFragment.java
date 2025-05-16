package com.pbdvmobile.app.fragments;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast; // Added

import com.pbdvmobile.app.R;
import com.pbdvmobile.app.data.model.Prize; // Assuming your Prize model

import java.io.Serializable; // Added
import java.util.ArrayList;
import java.util.List;

public class PrizesViewFragment extends Fragment {

    private static final String ARG_PRIZES_LIST = "prizes_list";
    private List<Prize> prizesList;
    private RecyclerView recyclerViewPrizes;
    private PrizeAdapter prizeAdapter;
    private OnPrizeRedeemListener mListener;


    public interface OnPrizeRedeemListener {
        void onRedeemPrize(Prize prize);
    }

    public PrizesViewFragment() {
        // Required empty public constructor
    }

    public static PrizesViewFragment newInstance(List<Prize> prizes) {
        PrizesViewFragment fragment = new PrizesViewFragment();
        Bundle args = new Bundle();
        // RecyclerView items should be Parcelable or Serializable to pass in a Bundle
        // For simplicity, using Serializable. Consider Parcelable for performance with large lists.
        args.putSerializable(ARG_PRIZES_LIST, (Serializable) prizes);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            prizesList = (List<Prize>) getArguments().getSerializable(ARG_PRIZES_LIST);
        }
        if (prizesList == null) {
            prizesList = new ArrayList<>(); // Initialize to avoid null pointer
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_prizes_view, container, false);
        recyclerViewPrizes = view.findViewById(R.id.recyclerViewPrizes);
        recyclerViewPrizes.setLayoutManager(new LinearLayoutManager(getContext()));
        prizeAdapter = new PrizeAdapter(prizesList, mListener);
        recyclerViewPrizes.setAdapter(prizeAdapter);
        return view;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnPrizeRedeemListener) {
            mListener = (OnPrizeRedeemListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnPrizeRedeemListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    // --- PrizeAdapter Inner Class ---
    private static class PrizeAdapter extends RecyclerView.Adapter<PrizeAdapter.PrizeViewHolder> {
        private List<Prize> prizes;
        private OnPrizeRedeemListener redeemListener;

        PrizeAdapter(List<Prize> prizes, OnPrizeRedeemListener listener) {
            this.prizes = prizes;
            this.redeemListener = listener;
        }

        @NonNull
        @Override
        public PrizeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.list_item_prize, parent, false);
            return new PrizeViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull PrizeViewHolder holder, int position) {
            Prize currentPrize = prizes.get(position);
            holder.textViewPrizeName.setText(currentPrize.getPrizeName());
            // Assuming Prize model has a getCostInCredits() method.
            // You need to add a 'cost' field to your Prize model and DB table.
            // holder.textViewPrizeCost.setText("Cost: " + currentPrize.getCostInCredits() + " Credits");
            holder.textViewPrizeCost.setText("Cost: 100 Credits"); // Placeholder cost

            // Placeholder for prize icon - you might want different icons per prize type
            holder.imageViewPrizeIcon.setImageResource(R.drawable.ic_prize_trophy);

            holder.buttonRedeemPrize.setOnClickListener(v -> {
                if (redeemListener != null) {
                    redeemListener.onRedeemPrize(currentPrize);
                }
            });
        }

        @Override
        public int getItemCount() {
            return prizes.size();
        }

        static class PrizeViewHolder extends RecyclerView.ViewHolder {
            ImageView imageViewPrizeIcon;
            TextView textViewPrizeName;
            TextView textViewPrizeCost;
            Button buttonRedeemPrize;

            PrizeViewHolder(View itemView) {
                super(itemView);
                imageViewPrizeIcon = itemView.findViewById(R.id.imageViewPrizeIcon);
                textViewPrizeName = itemView.findViewById(R.id.textViewPrizeName);
                textViewPrizeCost = itemView.findViewById(R.id.textViewPrizeCost);
                buttonRedeemPrize = itemView.findViewById(R.id.buttonRedeemPrize);
            }
        }
    }
}