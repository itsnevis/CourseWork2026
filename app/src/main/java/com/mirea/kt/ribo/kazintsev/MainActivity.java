package com.mirea.kt.ribo.kazintsev;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        loadFragment(NewsFragment.newInstance(NetworkUtils.CATEGORY_POLITICS));

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int id = item.getItemId();

            if (id == R.id.nav_politics) {
                selectedFragment = NewsFragment.newInstance(NetworkUtils.CATEGORY_POLITICS);
            } else if (id == R.id.nav_society) {
                selectedFragment = NewsFragment.newInstance(NetworkUtils.CATEGORY_SOCIETY);
            } else if (id == R.id.nav_sport) {
                selectedFragment = NewsFragment.newInstance(NetworkUtils.CATEGORY_SPORT);
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment);
            }
            return true;
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}