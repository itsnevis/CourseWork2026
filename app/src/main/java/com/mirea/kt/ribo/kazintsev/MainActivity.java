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

        BottomNavigationView nav = findViewById(R.id.bottom_navigation);

        // По умолчанию открываем Политику
        loadFragment(NewsFragment.newInstance("Политика"));

        nav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int id = item.getItemId();

            if (id == R.id.nav_politics) {
                selectedFragment = NewsFragment.newInstance("Политика");
            } else if (id == R.id.nav_society) {
                selectedFragment = NewsFragment.newInstance("Общество");
            } else if (id == R.id.nav_sport) {
                selectedFragment = NewsFragment.newInstance("Спорт");
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