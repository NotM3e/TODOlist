package com.example.todolist.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.widget.CompoundButtonCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.todolist.R;
import com.example.todolist.model.Task;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    // Interfejs do obsługi kliknięć, który implementuje Fragment
    public interface OnTaskActionListener {
        void onTaskClick(Task task);
        void onCheckboxClick(Task task);
    }

    private List<Task> taskList;
    private final OnTaskActionListener listener;

    public TaskAdapter(List<Task> taskList, OnTaskActionListener listener) {
        this.taskList = taskList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Pompowanie układu pojedynczego elementu listy
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        final Task task = taskList.get(position);
        final Context context = holder.itemView.getContext();

        // Ustawienie tytulu
        holder.title.setText(task.getTitle());

        // --- Obsługa priorytetu (kolorowe etykiety) ---
        if (task.getPriority() == 0 || task.getStatus() != 0) {
            holder.priority.setVisibility(View.GONE);
        } else {
            holder.priority.setVisibility(View.VISIBLE);
            int colorRes = 0;
            String label = "";

            if (task.getPriority() == 1) {
                colorRes = R.color.priority_low;
                label = "Niski";
            } else if (task.getPriority() == 2) {
                colorRes = R.color.priority_medium;
                label = "Średni";
            } else if (task.getPriority() == 3) {
                colorRes = R.color.priority_high;
                label = "Wysoki";
            }

            if (colorRes != 0) {
                holder.priority.setText(label);
                holder.priority.setBackgroundResource(R.drawable.bg_priority_chip);
                holder.priority.getBackground().mutate().setTint(ContextCompat.getColor(context, colorRes));
            } else {
                holder.priority.setVisibility(View.GONE);
            }
        }

        // --- Obsługa daty (formatowanie relatywne) ---
        if (task.getStatus() != 0) {
            holder.date.setVisibility(View.GONE);
        } else {
            holder.date.setVisibility(View.VISIBLE);
            
            // Logika formatowania daty (Dzisiaj, Jutro lub dd.MM)
            Calendar taskCal = Calendar.getInstance();
            taskCal.setTimeInMillis(task.getDate());

            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);

            Calendar tomorrow = (Calendar) today.clone();
            tomorrow.add(Calendar.DAY_OF_YEAR, 1);

            String dateStr;
            if (taskCal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                taskCal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {
                dateStr = "Dzisiaj";
            } else if (taskCal.get(Calendar.YEAR) == tomorrow.get(Calendar.YEAR) &&
                       taskCal.get(Calendar.DAY_OF_YEAR) == tomorrow.get(Calendar.DAY_OF_YEAR)) {
                dateStr = "Jutro";
            } else {
                SimpleDateFormat sdf = new SimpleDateFormat("dd.MM", Locale.getDefault());
                dateStr = sdf.format(new Date(task.getDate()));
            }

            // Dodanie godziny jeśli jest ustawiona
            if (task.getTime() != 0) {
                SimpleDateFormat tf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                dateStr += " " + tf.format(new Date(task.getTime()));
            }
            holder.date.setText(dateStr);
        }

        // --- Obsługa wyglądu w zależności od statusu (przekreślenie) ---
        if (task.getStatus() == 0) {
            // Aktywne
            holder.title.setTextColor(ContextCompat.getColor(context, R.color.text_primary));
            holder.title.setPaintFlags(holder.title.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            CompoundButtonCompat.setButtonTintList(holder.checkbox, ColorStateList.valueOf(ContextCompat.getColor(context, R.color.accent)));
        } else {
            // Zakończone lub Nieudane
            holder.title.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
            holder.title.setPaintFlags(holder.title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

            int statusColor;
            if (task.getStatus() == 1) statusColor = R.color.status_done;
            else statusColor = R.color.status_failed;
            
            CompoundButtonCompat.setButtonTintList(holder.checkbox, ColorStateList.valueOf(ContextCompat.getColor(context, statusColor)));
        }

        // --- Obsługa kliknięć ---

        // Kliknięcie w całą kartę
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onTaskClick(task);
                }
            }
        });

        // Obsługa checkboxa (zmiana statusu)
        holder.checkbox.setOnCheckedChangeListener(null); // Ważne: resetujemy listener przed zmianą stanu (recykling widoków)
        holder.checkbox.setChecked(task.getStatus() != 0);
        holder.checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (listener != null) {
                    listener.onCheckboxClick(task);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    // Metoda do odświeżania listy przez Fragment
    public void updateTasks(List<Task> tasks) {
        this.taskList = tasks;
        notifyDataSetChanged();
    }

    // Pobieranie zadania z konkretnej pozycji
    public Task getTaskAt(int position) {
        return taskList.get(position);
    }

    // Klasa przechowująca widoki pojedynczego elementu
    static class TaskViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkbox;
        TextView title, priority, date;

        TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            checkbox = itemView.findViewById(R.id.checkbox_status);
            title = itemView.findViewById(R.id.text_title);
            priority = itemView.findViewById(R.id.text_priority);
            date = itemView.findViewById(R.id.text_date);
        }
    }
}
