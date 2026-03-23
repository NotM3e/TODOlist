package com.example.todolist.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.todolist.R;

import java.util.List;

public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.DayViewHolder> {

    // Model pojedynczego dnia w siatce kalendarza
    public static class CalendarDay {
        public int dayOfMonth;
        public long timestamp;        // polnoc danego dnia (milisekundy)
        public boolean isCurrentMonth;
        public boolean isToday;
        public boolean isSelected;
        public int taskCount;
    }

    public interface OnDayClickListener {
        void onDayClick(int position, CalendarDay day);
    }

    private List<CalendarDay> days;
    private final OnDayClickListener listener;

    public CalendarAdapter(List<CalendarDay> days, OnDayClickListener listener) {
        this.days = days;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_calendar_day, parent, false);
        return new DayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        CalendarDay day = days.get(position);

        holder.textDay.setText(String.valueOf(day.dayOfMonth));

        // --- Kolor tekstu ---
        // Dni spoza biezacego miesiaca sa przygaszone
        if (day.isCurrentMonth) {
            holder.textDay.setTextColor(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.text_primary));
        } else {
            holder.textDay.setTextColor(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.text_hint));
        }

        // --- Tlo komorki ---
        // Priorytet: zaznaczony > dzisiejszy > domyslne (brak)
        if (day.isSelected) {
            holder.textDay.setBackgroundResource(R.drawable.bg_calendar_selected);
            holder.textDay.setTextColor(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.text_primary));
        } else if (day.isToday) {
            holder.textDay.setBackgroundResource(R.drawable.bg_calendar_today);
        } else {
            holder.textDay.setBackground(null);
        }

        // --- Kropka zadan ---
        // Widoczna tylko dla dni biezacego miesiaca z zadaniami
        holder.dotIndicator.setVisibility(
                (day.taskCount > 0 && day.isCurrentMonth) ? View.VISIBLE : View.INVISIBLE);

        // --- Klikniecie ---
        // Reaguje tylko na dni biezacego miesiaca
        holder.itemView.setOnClickListener(v -> {
            if (listener != null && day.isCurrentMonth) {
                listener.onDayClick(position, day);
            }
        });
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    public void updateDays(List<CalendarDay> newDays) {
        this.days = newDays;
        notifyDataSetChanged();
    }

    /**
     * Aktualizuje zaznaczenie – zdejmuje z poprzedniego dnia,
     * naklada na nowy. Uzywa notifyItemChanged zeby
     * odswiezyc tylko dwie komorki zamiast calej siatki.
     */
    public void setSelectedPosition(int oldPos, int newPos) {
        if (oldPos >= 0 && oldPos < days.size()) {
            days.get(oldPos).isSelected = false;
            notifyItemChanged(oldPos);
        }
        if (newPos >= 0 && newPos < days.size()) {
            days.get(newPos).isSelected = true;
            notifyItemChanged(newPos);
        }
    }

    static class DayViewHolder extends RecyclerView.ViewHolder {
        TextView textDay;
        View dotIndicator;

        DayViewHolder(@NonNull View itemView) {
            super(itemView);
            textDay = itemView.findViewById(R.id.text_day);
            dotIndicator = itemView.findViewById(R.id.dot_indicator);
        }
    }
}