package com.example.todolist.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
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

    // Interfejs komunikacji z fragmentem – fragment implementuje te metody
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
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = taskList.get(position);
        Context context = holder.itemView.getContext();

        holder.title.setText(task.getTitle());
        bindPriority(holder, task, context);
        bindDate(holder, task);
        bindStatus(holder, task, context);

        // --- Obsluga klikniec ---

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onTaskClick(task);
        });

        // Usuniecie listener PRZED ustawieniem stanu checkboxa.
        holder.checkbox.setOnCheckedChangeListener(null);
        holder.checkbox.setChecked(task.getStatus() != 0);
        holder.checkbox.setOnCheckedChangeListener((btn, isChecked) -> {
            if (listener != null) listener.onCheckboxClick(task);
        });

        // Klikniecie krzyzyka (nieudane) tez przywraca status
        holder.iconFailed.setOnClickListener(v -> {
            if (listener != null) listener.onCheckboxClick(task);
        });
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    // --- Metody publiczne dla fragmentu ---

    public void updateTasks(List<Task> tasks) {
        this.taskList = tasks;
        notifyDataSetChanged();
    }

    public Task getTaskAt(int position) {
        return taskList.get(position);
    }

    // ========== Metody stylowania ==========

    private void bindPriority(TaskViewHolder holder, Task task, Context context) {
        // Ukrywanie priorytetu dla ukonczonych zadan lub gdy brak priorytetu
        if (task.getPriority() == 0 || task.getStatus() != 0) {
            holder.priority.setVisibility(View.GONE);
            return;
        }
        holder.priority.setVisibility(View.VISIBLE);

        int colorRes;
        String label;
        switch (task.getPriority()) {
            case 1:
                colorRes = R.color.priority_low;
                label = context.getString(R.string.priority_low);
                break;
            case 2:
                colorRes = R.color.priority_medium;
                label = context.getString(R.string.priority_medium);
                break;
            case 3:
                colorRes = R.color.priority_high;
                label = context.getString(R.string.priority_high);
                break;
            default:
                holder.priority.setVisibility(View.GONE);
                return;
        }

        holder.priority.setText(label);
        holder.priority.setBackgroundResource(R.drawable.bg_priority_chip);
        // mutate() tworzy kopie drawable, dzieki czemu zmiana koloru
        // nie wplywa na inne karty ktore uzywa tego samego drawable
        holder.priority.getBackground().mutate().setTint(
                ContextCompat.getColor(context, colorRes));
    }

    private void bindDate(TaskViewHolder holder, Task task) {
        if (task.getStatus() != 0) {
            holder.date.setVisibility(View.GONE);
            return;
        }
        holder.date.setVisibility(View.VISIBLE);
        holder.date.setText(formatDate(task));
    }

    private void bindStatus(TaskViewHolder holder, Task task, Context context) {
        if (task.getStatus() == 0) {
            // Aktywne – normalny wyglad
            holder.title.setTextColor(ContextCompat.getColor(context, R.color.text_primary));
            holder.title.setPaintFlags(
                    holder.title.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            holder.checkbox.setVisibility(View.VISIBLE);
            holder.iconFailed.setVisibility(View.GONE);
            CompoundButtonCompat.setButtonTintList(holder.checkbox,
                    ColorStateList.valueOf(ContextCompat.getColor(context, R.color.accent)));
        } else {
            // Ukonczone/nieudane – przekreslony tekst
            holder.title.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
            holder.title.setPaintFlags(
                    holder.title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

            if (task.getStatus() == 1) {
                // Skonczone – zielony checkbox z ptaszkiem
                holder.checkbox.setVisibility(View.VISIBLE);
                holder.iconFailed.setVisibility(View.GONE);
                CompoundButtonCompat.setButtonTintList(holder.checkbox,
                        ColorStateList.valueOf(ContextCompat.getColor(context, R.color.status_done)));
            } else {
                // Nieudane – czerwony krzyzyk zamiast checkboxa
                holder.checkbox.setVisibility(View.GONE);
                holder.iconFailed.setVisibility(View.VISIBLE);
            }
        }
    }

    // ========== Formatowanie daty ==========

    private String formatDate(Task task) {
        Calendar taskCal = Calendar.getInstance();
        taskCal.setTimeInMillis(task.getDate());

        Calendar todayCal = Calendar.getInstance();
        todayCal.set(Calendar.HOUR_OF_DAY, 0);
        todayCal.set(Calendar.MINUTE, 0);
        todayCal.set(Calendar.SECOND, 0);
        todayCal.set(Calendar.MILLISECOND, 0);

        Calendar tomorrowCal = (Calendar) todayCal.clone();
        tomorrowCal.add(Calendar.DAY_OF_YEAR, 1);

        // Wyswietlenie relatywnej daty lub formatu dd.MM
        String dateStr;
        if (isSameDay(taskCal, todayCal)) {
            dateStr = "Dzisiaj";
        } else if (isSameDay(taskCal, tomorrowCal)) {
            dateStr = "Jutro";
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM", Locale.getDefault());
            dateStr = sdf.format(new Date(task.getDate()));
        }

        // Dodanie godziny jesli ustawiona
        if (task.getTime() != 0) {
            SimpleDateFormat tf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            dateStr += " " + tf.format(new Date(task.getTime()));
        }

        return dateStr;
    }

    private boolean isSameDay(Calendar c1, Calendar c2) {
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)
                && c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR);
    }

    // ========== ViewHolder ==========

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkbox;
        ImageView iconFailed;
        TextView title, priority, date;

        TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            checkbox = itemView.findViewById(R.id.checkbox_status);
            iconFailed = itemView.findViewById(R.id.icon_failed);
            title = itemView.findViewById(R.id.text_title);
            priority = itemView.findViewById(R.id.text_priority);
            date = itemView.findViewById(R.id.text_date);
        }
    }
}