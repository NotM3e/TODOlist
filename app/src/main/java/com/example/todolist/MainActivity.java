package com.example.todolist;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.todolist.fragment.CalendarFragment;
import com.example.todolist.fragment.TaskListFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // --- Inicjalizacja widoków ---
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        // Domyślnie przy starcie aplikacji pokazujemy listę zadań
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new TaskListFragment())
                    .commit();
        }

        // --- Obsługa dolnej nawigacji ---
        // Używamy klasy anonimowej zamiast lambdy, aby kod był bardziej czytelny dla początkujących
        bottomNav.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment fragment = null;
                int itemId = item.getItemId();

                // Sprawdzanie ID klikniętego elementu menu
                if (itemId == R.id.nav_tasks) {
                    fragment = new TaskListFragment();
                } else if (itemId == R.id.nav_calendar) {
                    fragment = new CalendarFragment();
                }

                // Podmiana fragmentu w kontenerze, jeśli został wybrany
                if (fragment != null) {
                    getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragment_container, fragment)
                            .commit();
                }
                
                return true;
            }
        });
    }
}
