package com.pbdvmobile.app.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.pbdvmobile.app.fragments.DashboardFragment;
import com.pbdvmobile.app.fragments.ExplorerFragment;
import com.pbdvmobile.app.fragments.ResourcesFragment;
import com.pbdvmobile.app.fragments.TutorDashboardFragment;

public class DashboardAdapter extends FragmentStateAdapter {

    private int itemCount;

    public DashboardAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
      switch (position){
          case 0:
              return new DashboardFragment();
          case 1:
              return new ExplorerFragment();
          case 2:
              return new ResourcesFragment();
          case 3:
              return new TutorDashboardFragment();
          default:
              return new DashboardFragment();
      }
    }

    @Override
    public int getItemCount() {
        return itemCount;
    }

    public void setItemCount(int count){
        itemCount = count;
    }
    public void setItemCount(){
        itemCount = 3;
    }
}
