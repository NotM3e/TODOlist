package com.example.todolist.fragment;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.todolist.R;
import com.example.todolist.TaskEditorActivity;
import com.example.todolist.adapter.TaskAdapter;
import com.example.todolist.database.AppDatabase;
import com.example.todolist.database.TaskDao;
import com.example.todolist.model.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class TaskListFragment extends Fragment implements TaskAdapter.OnTaskActionListener {

    private RecyclerView recyclerActive, recyclerCompleted;
    private TextView textActiveCount, textEmptyActive, textCompletedHeader;
    private TaskAdapter activeAdapter, completedAdapter;
    private TaskDao taskDao;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_task_list, container, false);

        taskDao = AppDatabase.getInstance(requireContext()).taskDao();

        initViews(view);
        setupRecyclerViews();
        setupSwipeGestures();

        // FAB – otwiera edytor w trybie tworzenia (bez task_id)
        FloatingActionButton fab = view.findViewById(R.id.fab_add);
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), TaskEditorActivity.class);
            startActivity(intent);
        });

        // TODO: Usunac po implementacji edytora (dzien 3)
        insertTestData();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadTasks();
    }

    // ========== INICJALIZACJA ==========

    private void initViews(View view) {
        recyclerActive = view.findViewById(R.id.recycler_active);
        recyclerCompleted = view.findViewById(R.id.recycler_completed);
        textActiveCount = view.findViewById(R.id.text_active_count);
        textEmptyActive = view.findViewById(R.id.text_empty_active);
        textCompletedHeader = view.findViewById(R.id.text_completed_header);
    }

    private void setupRecyclerViews() {
        activeAdapter = new TaskAdapter(new ArrayList<>(), this);
        completedAdapter = new TaskAdapter(new ArrayList<>(), this);

        recyclerActive.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerActive.setAdapter(activeAdapter);

        recyclerCompleted.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerCompleted.setAdapter(completedAdapter);
    }

    // ========== LADOWANIE DANYCH ==========

    private void loadTasks() {
        List<Task> activeTasks = taskDao.getAllActive();

        // Ukonczone zadania nie starsze niz 7 dni
        long cutoff = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000;
        List<Task> completedTasks = taskDao.getCompletedSince(cutoff);

        activeAdapter.updateTasks(activeTasks);
        completedAdapter.updateTasks(completedTasks);

        // Aktualizacja naglowkow
        textActiveCount.setText(activeTasks.size() + " Aktywnych");
        textCompletedHeader.setText(getString(R.string.tasks_completed)
                + " (" + completedTasks.size() + ")");

        // Pusty stan – widoczny gdy brak aktywnych zadan
        textEmptyActive.setVisibility(activeTasks.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerActive.setVisibility(activeTasks.isEmpty() ? View.GONE : View.VISIBLE);
    }

    // ========== GESTY SWIPE ==========

    private void setupSwipeGestures() {
        // ItemTouchHelper przechwytuje gesty przesuniecia na elementach RecyclerView
        ItemTouchHelper.SimpleCallback swipeCallback =
                new ItemTouchHelper.SimpleCallback(0,
                        ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

                    @Override
                    public boolean onMove(@NonNull RecyclerView recyclerView,
                                          @NonNull RecyclerView.ViewHolder viewHolder,
                                          @NonNull RecyclerView.ViewHolder target) {
                        return false; // nie obslugujemy przeciagania
                    }

                    @Override
                    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder,
                                         int direction) {
                        int position = viewHolder.getAdapterPosition();
                        Task task = activeAdapter.getTaskAt(position);

                        if (direction == ItemTouchHelper.LEFT) {
                            task.setStatus(1); // swipe w lewo = SKONCZONE
                        } else {
                            task.setStatus(2); // swipe w prawo = NIEUDANE
                        }

                        taskDao.update(task);
                        loadTasks();
                    }

                    // Rysowanie kolorowego tla widocznego podczas przesuwania karty
                    @Override
                    public void onChildDraw(@NonNull Canvas c,
                                            @NonNull RecyclerView recyclerView,
                                            @NonNull RecyclerView.ViewHolder viewHolder,
                                            float dX, float dY,
                                            int actionState, boolean isCurrentlyActive) {

                        View itemView = viewHolder.itemView;
                        Paint paint = new Paint();

                        if (dX < 0) {
                            // Swipe w lewo – zielone tlo (skonczone)
                            paint.setColor(ContextCompat.getColor(
                                    requireContext(), R.color.status_done));
                            c.drawRect(
                                    itemView.getRight() + dX, itemView.getTop(),
                                    itemView.getRight(), itemView.getBottom(), paint);
                        } else if (dX > 0) {
                            // Swipe w prawo – czerwone tlo (nieudane)
                            paint.setColor(ContextCompat.getColor(
                                    requireContext(), R.color.status_failed));
                            c.drawRect(
                                    itemView.getLeft(), itemView.getTop(),
                                    itemView.getLeft() + dX, itemView.getBottom(), paint);
                        }

                        super.onChildDraw(c, recyclerView, viewHolder,
                                dX, dY, actionState, isCurrentlyActive);
                    }
                };

        // Podpiecie gestow TYLKO do listy aktywnych zadan
        new ItemTouchHelper(swipeCallback).attachToRecyclerView(recyclerActive);
    }

    // ========== CALLBACKI ADAPTERA ==========

    @Override
    public void onTaskClick(Task task) {
        Intent intent = new Intent(requireContext(), TaskEditorActivity.class);
        intent.putExtra("task_id", task.getId());
        startActivity(intent);
    }

    @Override
    public void onCheckboxClick(Task task) {
        if (task.getStatus() == 0) {
            task.setStatus(1); // NIEZROBIONE -> SKONCZONE
        } else {
            task.setStatus(0); // SKONCZONE/NIEUDANE -> NIEZROBIONE
        }
        taskDao.update(task);
        loadTasks();
    }

    // ========== DANE TESTOWE ==========
    // TODO: Usunac cala metode po implementacji edytora (dzien 3)

    private void insertTestData() {
        if (!taskDao.getAllActive().isEmpty()) return;

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long today = cal.getTimeInMillis();
        long tomorrow = today + 86400000L;
        long dayAfter = today + 2 * 86400000L;

        cal.set(Calendar.HOUR_OF_DAY, 17);
        long time1700 = cal.getTimeInMillis();

        Task t1 = new Task("Pierwsza praca nad projektem", today);
        t1.setPriority(3);
        t1.setTime(time1700);
        taskDao.insert(t1);

        Task t2 = new Task("Dalsza pracja nad projektem", tomorrow);
        t2.setPriority(2);
        taskDao.insert(t2);

        Task t3 = new Task("Pokazanie projektu", dayAfter);
        t3.setPriority(1);
        taskDao.insert(t3);

        Task t4 = new Task("Przygotować propozycję projektu", today);
        t4.setStatus(1); // SKONCZONE
        taskDao.insert(t4);

        Task t5 = new Task("Zrobić salto", today);
        t5.setStatus(2); // NIEUDANE
        taskDao.insert(t5);
    }
}