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

        // Widoki:
        recyclerActive = view.findViewById(R.id.recycler_active);
        recyclerCompleted = view.findViewById(R.id.recycler_completed);
        textActiveCount = view.findViewById(R.id.text_active_count);
        textEmptyActive = view.findViewById(R.id.text_empty_active);
        textCompletedHeader = view.findViewById(R.id.text_completed_header);

        // Konfiguracja RecyclerView (Active):
        activeAdapter = new TaskAdapter(new ArrayList<Task>(), this);
        recyclerActive.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerActive.setAdapter(activeAdapter);

        // Konfiguracja RecyclerView (Completed):
        completedAdapter = new TaskAdapter(new ArrayList<Task>(), this);
        recyclerCompleted.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerCompleted.setAdapter(completedAdapter);

        // Przycisk FAB:
        FloatingActionButton fab = view.findViewById(R.id.fab_add);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(requireContext(), TaskEditorActivity.class);
                startActivity(intent);
            }
        });

        // Obsługa gestów swipe:
        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Task task = activeAdapter.getTaskAt(position);

                if (direction == ItemTouchHelper.LEFT) {
                    // Udane
                    task.setStatus(1);
                } else if (direction == ItemTouchHelper.RIGHT) {
                    // Nieudane
                    task.setStatus(2);
                }

                taskDao.update(task);
                loadTasksFromDatabase();
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                View itemView = viewHolder.itemView;
                Paint paint = new Paint();

                if (dX < 0) {
                    paint.setColor(ContextCompat.getColor(requireContext(), R.color.status_done));
                    c.drawRect(itemView.getRight() + dX, itemView.getTop(), itemView.getRight(), itemView.getBottom(), paint);
                } else if (dX > 0) {
                    paint.setColor(ContextCompat.getColor(requireContext(), R.color.status_failed));
                    c.drawRect(itemView.getLeft(), itemView.getTop(), itemView.getLeft() + dX, itemView.getBottom(), paint);
                }

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        };

        // Podpięcie gestów do listy aktywnych zadań
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(swipeCallback);
        itemTouchHelper.attachToRecyclerView(recyclerActive);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Odświeżenie listy zadań przy każdym powrocie do fragmentu
        loadTasksFromDatabase();
    }

    // Pobieranie i wyświetlanie zadań z bazy danych:
    private void loadTasksFromDatabase() {
        List<Task> activeTasks = taskDao.getAllActive();

        // Pobieranie ukończonych zadań z ostatnich 7 dni
        long sevenDaysMillis = 7L * 24 * 60 * 60 * 1000;
        long cutoffTime = System.currentTimeMillis() - sevenDaysMillis;
        List<Task> completedTasks = taskDao.getCompletedSince(cutoffTime);

        // Aktualizacja adapterów
        activeAdapter.updateTasks(activeTasks);
        completedAdapter.updateTasks(completedTasks);

        // Aktualizacja tekstów liczników i nagłówków
        textActiveCount.setText(activeTasks.size() + " Aktywnych");
        textCompletedHeader.setText("Zakończone (" + completedTasks.size() + ")");

        // Wyświetlanie informacji o braku zadań
        if (activeTasks.isEmpty()) {
            textEmptyActive.setVisibility(View.VISIBLE);
            recyclerActive.setVisibility(View.GONE);
        } else {
            textEmptyActive.setVisibility(View.GONE);
            recyclerActive.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onTaskClick(Task task) {
        Intent intent = new Intent(requireContext(), TaskEditorActivity.class);
        intent.putExtra("task_id", task.getId());
        startActivity(intent);
    }

    @Override
    public void onCheckboxClick(Task task) {
        if (task.getStatus() == 0) {
            // Udane
            task.setStatus(1);
        } else {
            // Nieudane
            task.setStatus(0);
        }
        taskDao.update(task);
        loadTasksFromDatabase();
    }
}
