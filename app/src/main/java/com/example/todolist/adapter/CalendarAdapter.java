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

/**
 * Adapter odpowiedzialny za wyświetlanie siatki dni w kalendarzu.
 */
public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.DayViewHolder> {

    // Klasa reprezentująca dane jednego dnia w kalendarzu
    public static class CalendarDay {
        public int dayOfMonth;        // numer dnia (np. 1, 2, 3...)
        public long timestamp;        // dokładny czas w milisekundach (używany do bazy danych)
        public boolean isCurrentMonth; // czy dzień należy do obecnie wyświetlanego miesiąca
        public boolean isToday;        // czy ten dzień to dzisiaj
        public boolean isSelected;     // czy użytkownik kliknął w ten dzień
        public int taskCount;         // ile zadań jest zaplanowanych na ten dzień
    }

    // Interfejs do obsługi kliknięć, który zaimplementujemy we fragmencie
    public interface OnDayClickListener {
        void onDayClick(int position, CalendarDay day);
    }

    private List<CalendarDay> days;
    private final OnDayClickListener listener;

    // Konstruktor adaptera
    public CalendarAdapter(List<CalendarDay> days, OnDayClickListener listener) {
        this.days = days;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Pompowanie layoutu pojedynczej komórki dnia (kwadracik w kalendarzu)
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_calendar_day, parent, false);
        return new DayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final DayViewHolder holder, final int position) {
        // Pobieramy dane dnia dla konkretnej pozycji w liście
        final CalendarDay day = days.get(position);

        // Ustawiamy tekst z numerem dnia
        holder.textDay.setText(String.valueOf(day.dayOfMonth));

        // --- USTAWIANIE KOLORU TEKSTU ---
        // Sprawdzamy czy dzień należy do aktualnego miesiąca
        if (day.isCurrentMonth) {
            // Jeśli tak, tekst jest wyraźny (czarny/ciemny)
            holder.textDay.setTextColor(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.text_primary));
        } else {
            // Jeśli nie (dzień z poprzedniego lub następnego miesiąca), tekst jest wyszarzony
            holder.textDay.setTextColor(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.text_hint));
        }

        // --- USTAWIANIE TŁA KOMÓRKI (ZAZNACZENIE) ---
        // Najważniejsze jest to, co wybrał użytkownik
        if (day.isSelected) {
            // Ustawiamy specjalne tło dla zaznaczonego dnia
            holder.textDay.setBackgroundResource(R.drawable.bg_calendar_selected);
            // Zapewniamy, że tekst będzie dobrze widoczny na tym tle
            holder.textDay.setTextColor(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.text_primary));
        } else if (day.isToday) {
            // Jeśli dzień nie jest zaznaczony, ale jest dzisiejszy, dajemy mu ramkę/tło "dzisiaj"
            holder.textDay.setBackgroundResource(R.drawable.bg_calendar_today);
        } else {
            // W pozostałych przypadkach usuwamy jakiekolwiek tło
            holder.textDay.setBackground(null);
        }

        // --- KROPKA INFORMUJĄCA O ZADANIACH ---
        // Pokazujemy kropkę tylko jeśli są jakieś zadania i dzień należy do bieżącego miesiąca
        if (day.taskCount > 0 && day.isCurrentMonth) {
            holder.dotIndicator.setVisibility(View.VISIBLE);
        } else {
            holder.dotIndicator.setVisibility(View.INVISIBLE);
        }

        // --- OBSŁUGA KLIKNIĘCIA W DZIEŃ ---
        // Zamiast krótkiej lambdy, używamy pełnej klasy anonimowej
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Reagujemy na kliknięcie tylko jeśli to dzień z bieżącego miesiąca i mamy ustawiony listener
                if (listener != null) {
                    if (day.isCurrentMonth) {
                        listener.onDayClick(position, day);
                    }
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        // Zwracamy liczbę wszystkich komórek (zazwyczaj 35 lub 42)
        return days.size();
    }

    // Metoda do odświeżania całej listy dni
    public void updateDays(List<CalendarDay> newDays) {
        this.days = newDays;
        // Informujemy RecyclerView, że wszystkie dane się zmieniły i trzeba odrysować widok
        notifyDataSetChanged();
    }

    /**
     * Metoda zmieniająca zaznaczony dzień bez odświeżania całego kalendarza.
     * To jest bardziej wydajne, bo zmieniamy tylko dwie komórki.
     */
    public void setSelectedPosition(int oldPos, int newPos) {
        // Usuwamy zaznaczenie ze starej pozycji
        if (oldPos >= 0) {
            if (oldPos < days.size()) {
                days.get(oldPos).isSelected = false;
                // Odświeżamy tylko tę jedną komórkę
                notifyItemChanged(oldPos);
            }
        }
        
        // Dodajemy zaznaczenie na nową pozycję
        if (newPos >= 0) {
            if (newPos < days.size()) {
                days.get(newPos).isSelected = true;
                // Odświeżamy tylko tę jedną komórkę
                notifyItemChanged(newPos);
            }
        }
    }

    // Klasa ViewHolder, która trzyma referencje do widoków wewnątrz jednego elementu listy
    static class DayViewHolder extends RecyclerView.ViewHolder {
        TextView textDay;    // widok tekstu z numerem dnia
        View dotIndicator;   // mała kropka pod numerem dnia

        DayViewHolder(@NonNull View itemView) {
            super(itemView);
            // Znajdujemy widoki po ich ID zdefiniowanych w layout XML
            textDay = itemView.findViewById(R.id.text_day);
            dotIndicator = itemView.findViewById(R.id.dot_indicator);
        }
    }
}
