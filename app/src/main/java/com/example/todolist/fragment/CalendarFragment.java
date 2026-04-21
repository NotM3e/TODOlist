package com.example.todolist.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.todolist.R;
import com.example.todolist.TaskEditorActivity;
import com.example.todolist.adapter.CalendarAdapter;
import com.example.todolist.adapter.TaskAdapter;
import com.example.todolist.database.AppDatabase;
import com.example.todolist.database.TaskDao;
import com.example.todolist.model.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CalendarFragment extends Fragment
        implements CalendarAdapter.OnDayClickListener, TaskAdapter.OnTaskActionListener {

    private TextView textMonthYear, textSelectedDate, textTaskCount;
    private RecyclerView recyclerCalendar, recyclerDayTasks;
    private View textEmptyDay;

    private CalendarAdapter calendarAdapter;
    private TaskAdapter taskAdapter;
    private TaskDao taskDao;
    private int currentYear, currentMonth;

    // Zmienna przechowujaca pozycje zaznaczonego dnia w liscie
    private int selectedPosition = -1;
    // Zmienna przechowujaca czas w milisekundach dla zaznaczonego dnia
    private long selectedDayTimestamp = 0;

    // ========================================================
    //  Główna metoda tworząca widok fragmentu
    // ========================================================

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Pompowanie layoutu fragmentu
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);

        // Inicjalizacja bazy danych
        taskDao = AppDatabase.getInstance(requireContext()).taskDao();

        // --- Inicjalizacja wszystkich widoków (findViewById) ---
        // Tutaj przypisujemy komponenty z pliku XML do zmiennych w Javie
        textMonthYear    = view.findViewById(R.id.text_month_year);
        textSelectedDate = view.findViewById(R.id.text_selected_date);
        textTaskCount    = view.findViewById(R.id.text_task_count);
        recyclerCalendar = view.findViewById(R.id.recycler_calendar);
        recyclerDayTasks = view.findViewById(R.id.recycler_day_tasks);
        textEmptyDay     = view.findViewById(R.id.text_empty_day);

        // --- Konfiguracja głównego kalendarza (RecyclerView) ---
        // Tworzymy adapter z pustą listą na start
        calendarAdapter = new CalendarAdapter(new ArrayList<CalendarAdapter.CalendarDay>(), this);
        // Kalendarz to siatka, więc używamy GridLayoutManager z 7 kolumnami (7 dni tygodnia)
        recyclerCalendar.setLayoutManager(new GridLayoutManager(requireContext(), 7));
        recyclerCalendar.setAdapter(calendarAdapter);

        // --- Konfiguracja listy zadań dla wybranego dnia ---
        taskAdapter = new TaskAdapter(new ArrayList<Task>(), this);
        // Zadania wyświetlamy jedno pod drugim
        recyclerDayTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerDayTasks.setAdapter(taskAdapter);

        // --- Konfiguracja przycisków nawigacji (poprzedni/następny miesiąc) ---
        ImageButton btnPrev = view.findViewById(R.id.btn_prev_month);
        ImageButton btnNext = view.findViewById(R.id.btn_next_month);

        // Akcja dla przycisku w lewo
        btnPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Zmniejszamy numer miesiąca
                currentMonth--;
                // Jeśli wyjdziemy poza styczeń (0), to wracamy do grudnia (11) poprzedniego roku
                if (currentMonth < 0) {
                    currentMonth = 11;
                    currentYear--;
                }
                // Resetujemy zaznaczenie, bo w nowym miesiącu zaznaczymy coś innego
                selectedPosition = -1;
                selectedDayTimestamp = 0;
                // Odświeżamy widok kalendarza
                loadMonth();
            }
        });

        // Akcja dla przycisku w prawo
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Zwiększamy numer miesiąca
                currentMonth++;
                // Jeśli wyjdziemy poza grudzień (11), to przechodzimy do stycznia (0) następnego roku
                if (currentMonth > 11) {
                    currentMonth = 0;
                    currentYear++;
                }
                // Resetujemy zaznaczenie
                selectedPosition = -1;
                selectedDayTimestamp = 0;
                // Odświeżamy widok kalendarza
                loadMonth();
            }
        });

        // --- Ustawienie początkowej daty (dzisiejszy dzień) ---
        Calendar today = Calendar.getInstance();
        currentYear = today.get(Calendar.YEAR);
        currentMonth = today.get(Calendar.MONTH);

        // --- Konfiguracja przycisku plusa (FAB) ---
        FloatingActionButton fab = view.findViewById(R.id.fab_add_calendar);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Tworzymy intencję, żeby otworzyć okno dodawania zadania
                Intent intent = new Intent(requireContext(), TaskEditorActivity.class);
                // Jeśli mamy zaznaczony jakiś dzień w kalendarzu, przekazujemy jego datę
                if (selectedDayTimestamp != 0) {
                    intent.putExtra("prefill_date", selectedDayTimestamp);
                }
                startActivity(intent);
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Odświeżamy kalendarz za każdym razem, gdy użytkownik wraca do tego ekranu
        loadMonth();
    }

    // ========================================================
    //  Metoda odpowiedzialna za ładowanie danych dla danego miesiąca
    // ========================================================

    private void loadMonth() {
        // --- Ustawianie napisu z nazwą miesiąca i rokiem ---
        Calendar cal = Calendar.getInstance();
        cal.set(currentYear, currentMonth, 1);
        SimpleDateFormat sdf = new SimpleDateFormat("LLLL yyyy", Locale.getDefault());
        String monthName = sdf.format(cal.getTime());
        // Robimy pierwszą literę wielką (np. "wrzesień" -> "Wrzesień")
        textMonthYear.setText(monthName.substring(0, 1).toUpperCase() + monthName.substring(1));

        // --- Generowanie listy dni do wyświetlenia w siatce ---
        List<CalendarAdapter.CalendarDay> days = generateDays();

        // --- Pobieranie zadań z bazy i liczenie ich dla każdego dnia ---
        if (!days.isEmpty()) {
            // Początek zakresu to timestamp pierwszego elementu w liście (może być z poprz. miesiąca)
            long rangeStart = days.get(0).timestamp;
            // Koniec to ostatni dzień + 24 godziny (milisekundy), żeby złapać cały dzień
            long rangeEnd = days.get(days.size() - 1).timestamp + 86400000L;
            List<Task> monthTasks = taskDao.getByDateRange(rangeStart, rangeEnd);
            
            // Mapowanie zadań - tutaj liczymy ile zadań jest w każdym dniu
            mapTaskCountsToDays(days, monthTasks);
        }

        // --- Logika zaznaczania odpowiedniego dnia ---
        if (selectedDayTimestamp != 0) {
            // Jeśli już coś było zaznaczone, szukamy tego dnia w nowej liście
            boolean found = false;
            for (int i = 0; i < days.size(); i++) {
                CalendarAdapter.CalendarDay day = days.get(i);
                if (day.isCurrentMonth && day.timestamp == selectedDayTimestamp) {
                    day.isSelected = true;
                    selectedPosition = i;
                    found = true;
                    break;
                }
            }
            // Jeśli nie znaleźliśmy (bo np. zmieniliśmy miesiąc), wybieramy domyślny dzień
            if (!found) {
                selectedDayTimestamp = 0;
                autoSelectDay(days);
            }
        } else {
            // Jeśli nic nie było zaznaczone, wybieramy automatycznie
            autoSelectDay(days);
        }

        // Przekazujemy gotową listę dni do adaptera
        calendarAdapter.updateDays(days);

        // Jeśli mamy wybrany dzień, ładujemy dla niego zadania na dole ekranu
        if (selectedDayTimestamp != 0) {
            loadTasksForDay(selectedDayTimestamp);
        }
    }

    // ========================================================
    //  Matematyka generowania dni w kalendarzu
    // ========================================================

    private List<CalendarAdapter.CalendarDay> generateDays() {
        List<CalendarAdapter.CalendarDay> days = new ArrayList<CalendarAdapter.CalendarDay>();

        // Ustawiamy kalendarz na pierwszy dzień wybranego miesiąca
        Calendar cal = Calendar.getInstance();
        cal.set(currentYear, currentMonth, 1, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);

        // Sprawdzamy ile dni ma ten miesiąc (np. 30, 31 lub 28/29)
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        // --- OBLICZANIE OFFSETU (przesunięcia) ---
        // Sprawdzamy którym dniem tygodnia jest pierwszy dzień miesiąca
        int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        // W Androidzie niedziela to 1, poniedziałek to 2. My chcemy, żeby poniedziałek był pierwszy.
        // Ta formuła wylicza ile "pustych" komórek (z poprzedniego miesiąca) musimy dodać na początku.
        int offset = (firstDayOfWeek - Calendar.MONDAY + 7) % 7;

        // --- OBLICZANIE LICZBY WSZYSTKICH KOMÓREK ---
        // Kalendarz zawsze wyświetla pełne tygodnie (wiersze po 7 dni)
        int totalCells = offset + daysInMonth;
        // Obliczamy ile pełnych rzędów potrzebujemy
        int rows = (int) Math.ceil(totalCells / 7.0);
        // Ostateczna liczba komórek to wielokrotność 7
        totalCells = rows * 7;

        // Robimy kopię kalendarza i cofamy go o wyliczony offset, żeby zacząć od poprz. miesiąca
        Calendar dayCal = (Calendar) cal.clone();
        dayCal.add(Calendar.DAY_OF_MONTH, -offset);

        // Dzisiejsza data do porównania
        Calendar today = Calendar.getInstance();

        // Pętla tworząca obiekty dla każdej komórki w siatce
        for (int i = 0; i < totalCells; i++) {
            CalendarAdapter.CalendarDay day = new CalendarAdapter.CalendarDay();
            day.dayOfMonth = dayCal.get(Calendar.DAY_OF_MONTH);
            day.timestamp = dayCal.getTimeInMillis();
            // Sprawdzamy czy ten dzień należy do aktualnie przeglądanego miesiąca
            day.isCurrentMonth = (dayCal.get(Calendar.MONTH) == currentMonth
                    && dayCal.get(Calendar.YEAR) == currentYear);
            // Sprawdzamy czy to dzisiaj
            day.isToday = isSameDay(dayCal, today);

            // Dodajemy dzień do listy i przesuwamy kalendarz o jeden dzień do przodu
            days.add(day);
            dayCal.add(Calendar.DAY_OF_MONTH, 1);
        }

        return days;
    }

    // ========================================================
    //  Liczenie zadań dla każdego dnia
    // ========================================================

    private void mapTaskCountsToDays(List<CalendarAdapter.CalendarDay> days,
                                     List<Task> tasks) {
        // Mapa: klucz to data jako tekst "yyyy-MM-dd", wartość to liczba zadań
        Map<String, Integer> countMap = new HashMap<String, Integer>();
        SimpleDateFormat keyFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        // Przechodzimy przez wszystkie zadania i zliczamy je dla konkretnych dat
        for (int i = 0; i < tasks.size(); i++) {
            Task task = tasks.get(i);
            String key = keyFormat.format(new Date(task.getDate()));
            
            // Sprawdzamy czy mamy już taki klucz w mapie
            if (countMap.containsKey(key)) {
                // Jeśli tak, to pobieramy aktualną liczbę i dodajemy 1
                int currentCount = countMap.get(key);
                countMap.put(key, currentCount + 1);
            } else {
                // Jeśli nie, to wpisujemy 1 (pierwsze zadanie dla tej daty)
                countMap.put(key, 1);
            }
        }

        // Teraz przypisujemy policzone wartości do obiektów dni w kalendarzu
        for (int j = 0; j < days.size(); j++) {
            CalendarAdapter.CalendarDay day = days.get(j);
            String key = keyFormat.format(new Date(day.timestamp));
            
            Integer count = countMap.get(key);
            if (count != null) {
                day.taskCount = count;
            } else {
                day.taskCount = 0;
            }
        }
    }

    // ========================================================
    //  Wybieranie domyślnego dnia przy wejściu w miesiąc
    // ========================================================

    private void autoSelectDay(List<CalendarAdapter.CalendarDay> days) {
        Calendar today = Calendar.getInstance();
        // Sprawdzamy czy jesteśmy w aktualnym miesiącu i roku
        boolean isTodayMonth = (currentYear == today.get(Calendar.YEAR)
                && currentMonth == today.get(Calendar.MONTH));

        // Jeśli tak, zaznaczamy dzisiejszy numer dnia, jeśli nie - pierwszy dzień miesiąca
        int targetDay = 1;
        if (isTodayMonth) {
            targetDay = today.get(Calendar.DAY_OF_MONTH);
        }

        for (int i = 0; i < days.size(); i++) {
            CalendarAdapter.CalendarDay day = days.get(i);
            // Zaznaczamy tylko jeśli dzień należy do głównego miesiąca
            if (day.isCurrentMonth && day.dayOfMonth == targetDay) {
                day.isSelected = true;
                selectedPosition = i;
                selectedDayTimestamp = day.timestamp;
                break;
            }
        }
    }

    // Metoda wywoływana, gdy użytkownik kliknie w dzień na kalendarzu
    @Override
    public void onDayClick(int position, CalendarAdapter.CalendarDay day) {
        int oldPosition = selectedPosition;
        selectedPosition = position;
        selectedDayTimestamp = day.timestamp;

        // Informujemy adapter, że zmieniło się zaznaczenie, aby odświeżył widok
        calendarAdapter.setSelectedPosition(oldPosition, position);
        // Ładujemy zadania dla nowo wybranego dnia
        loadTasksForDay(day.timestamp);
    }

    // ========================================================
    //  Ładowanie listy zadań pod kalendarzem
    // ========================================================

    private void loadTasksForDay(long dayStart) {
        // Zakres 24h: od północy do północy następnego dnia
        long dayEnd = dayStart + 86400000L;
        List<Task> tasks = taskDao.getByDateRange(dayStart, dayEnd);

        // Formatowanie nagłówka z datą, np. "10 stycznia, niedziela"
        SimpleDateFormat sdf = new SimpleDateFormat("d MMMM, EEEE", Locale.getDefault());
        textSelectedDate.setText(sdf.format(new Date(dayStart)));

        // Ustawienie tekstu z liczbą zadań (używamy plurals dla poprawnej polskiej odmiany)
        textTaskCount.setText(
                getResources().getQuantityString(R.plurals.task_count, tasks.size(), tasks.size()));

        // Aktualizacja listy zadań w adapterze
        taskAdapter.updateTasks(tasks);

        // Jeśli nie ma zadań, pokazujemy napis "brak zadań", w przeciwnym razie pokazujemy listę
        if (tasks.isEmpty()) {
            recyclerDayTasks.setVisibility(View.GONE);
            textEmptyDay.setVisibility(View.VISIBLE);
        } else {
            recyclerDayTasks.setVisibility(View.VISIBLE);
            textEmptyDay.setVisibility(View.GONE);
        }
    }

    // ========================================================
    //  Akcje na zadaniach (kliknięcie lub zmiana statusu)
    // ========================================================

    @Override
    public void onTaskClick(Task task) {
        // Otwarcie edytora dla istniejącego zadania
        Intent intent = new Intent(requireContext(), TaskEditorActivity.class);
        intent.putExtra("task_id", task.getId());
        startActivity(intent);
    }

    @Override
    public void onCheckboxClick(Task task) {
        // Prosta zmiana statusu: 0 -> 1, 1 -> 0
        if (task.getStatus() == 0) {
            task.setStatus(1);
        } else {
            task.setStatus(0);
        }
        // Zapisanie zmiany w bazie danych
        taskDao.update(task);
        // Odświeżenie wszystkiego, żeby kropki na kalendarzu też się zgadzały
        loadMonth();
    }

    // Funkcja pomocnicza sprawdzająca czy dwie daty to ten sam dzień
    private boolean isSameDay(Calendar c1, Calendar c2) {
        boolean sameYear = c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR);
        boolean sameDayOfYear = c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR);
        return sameYear && sameDayOfYear;
    }
}
