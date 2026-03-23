package com.example.todolist.fragment;

import android.content.Intent;
import android.os.Bundle;
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

    // Aktualnie wyswietlany miesiac
    private int currentYear, currentMonth;

    // Zaznaczony dzien
    private int selectedPosition = -1;
    private long selectedDayTimestamp = 0;

    // ========================================================
    //  CYKL ZYCIA
    // ========================================================

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);

        taskDao = AppDatabase.getInstance(requireContext()).taskDao();

        initViews(view);
        setupCalendar();
        setupTaskList();
        setupNavigation(view);

        // Ustawienie biezacego miesiaca
        Calendar today = Calendar.getInstance();
        currentYear = today.get(Calendar.YEAR);
        currentMonth = today.get(Calendar.MONTH);

        // FAB – otwiera edytor z wstepnie wypelniona data zaznaczonego dnia
        FloatingActionButton fab = view.findViewById(R.id.fab_add_calendar);
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), TaskEditorActivity.class);
            if (selectedDayTimestamp != 0) {
                intent.putExtra("prefill_date", selectedDayTimestamp);
            }
            startActivity(intent);
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Odswieza kalendarz przy kazdym powrocie (np. po edycji zadania)
        loadMonth();
    }

    // ========================================================
    //  INICJALIZACJA
    // ========================================================

    private void initViews(View view) {
        textMonthYear    = view.findViewById(R.id.text_month_year);
        textSelectedDate = view.findViewById(R.id.text_selected_date);
        textTaskCount    = view.findViewById(R.id.text_task_count);
        recyclerCalendar = view.findViewById(R.id.recycler_calendar);
        recyclerDayTasks = view.findViewById(R.id.recycler_day_tasks);
        textEmptyDay     = view.findViewById(R.id.text_empty_day);
    }

    private void setupCalendar() {
        calendarAdapter = new CalendarAdapter(new ArrayList<>(), this);
        // GridLayoutManager z 7 kolumnami – jeden na kazdy dzien tygodnia
        recyclerCalendar.setLayoutManager(new GridLayoutManager(requireContext(), 7));
        recyclerCalendar.setAdapter(calendarAdapter);
    }

    private void setupTaskList() {
        taskAdapter = new TaskAdapter(new ArrayList<>(), this);
        recyclerDayTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerDayTasks.setAdapter(taskAdapter);
    }

    private void setupNavigation(View view) {
        ImageButton btnPrev = view.findViewById(R.id.btn_prev_month);
        ImageButton btnNext = view.findViewById(R.id.btn_next_month);

        btnPrev.setOnClickListener(v -> {
            currentMonth--;
            if (currentMonth < 0) {
                currentMonth = 11;
                currentYear--;
            }
            // Reset zaznaczenia – nowy miesiac, autoSelectDay wybierze dzien
            selectedPosition = -1;
            selectedDayTimestamp = 0;
            loadMonth();
        });

        btnNext.setOnClickListener(v -> {
            currentMonth++;
            if (currentMonth > 11) {
                currentMonth = 0;
                currentYear++;
            }
            selectedPosition = -1;
            selectedDayTimestamp = 0;
            loadMonth();
        });
    }

    // ========================================================
    //  LADOWANIE MIESIACA
    // ========================================================

    private void loadMonth() {
        // --- Naglowek miesiaca (np. "Wrzesień 2026") ---
        Calendar cal = Calendar.getInstance();
        cal.set(currentYear, currentMonth, 1);
        SimpleDateFormat sdf = new SimpleDateFormat("LLLL yyyy", Locale.getDefault());
        String monthName = sdf.format(cal.getTime());
        textMonthYear.setText(monthName.substring(0, 1).toUpperCase() + monthName.substring(1));

        // --- Generowanie komorek kalendarza ---
        List<CalendarAdapter.CalendarDay> days = generateDays();

        // --- Pobranie liczby zadan dla widocznego zakresu dat ---
        if (!days.isEmpty()) {
            long rangeStart = days.get(0).timestamp;
            long rangeEnd = days.get(days.size() - 1).timestamp + 86400000L;
            List<Task> monthTasks = taskDao.getByDateRange(rangeStart, rangeEnd);
            mapTaskCountsToDays(days, monthTasks);
        }

        // --- Zaznaczenie dnia ---
        if (selectedDayTimestamp != 0) {
            // Proba ponownego zaznaczenia tego samego dnia (po powrocie z edytora)
            boolean reselected = false;
            for (int i = 0; i < days.size(); i++) {
                CalendarAdapter.CalendarDay day = days.get(i);
                if (day.isCurrentMonth && day.timestamp == selectedDayTimestamp) {
                    day.isSelected = true;
                    selectedPosition = i;
                    reselected = true;
                    break;
                }
            }
            if (!reselected) {
                selectedDayTimestamp = 0;
                autoSelectDay(days);
            }
        } else {
            autoSelectDay(days);
        }

        calendarAdapter.updateDays(days);

        // --- Zaladowanie zadan dla zaznaczonego dnia ---
        if (selectedDayTimestamp != 0) {
            loadTasksForDay(selectedDayTimestamp);
        }
    }

    // ========================================================
    //  GENEROWANIE DNI MIESIACA
    // ========================================================

    private List<CalendarAdapter.CalendarDay> generateDays() {
        List<CalendarAdapter.CalendarDay> days = new ArrayList<>();

        Calendar cal = Calendar.getInstance();
        cal.set(currentYear, currentMonth, 1, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);

        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        // Oblicz offset – ile komorek z poprzedniego miesiaca trzeba dodac
        // aby pierwszy rzad zaczynał sie od poniedzialku.
        // Java: SUNDAY=1, MONDAY=2, ..., SATURDAY=7
        // Nasz uklad: Pn=0, Wt=1, ..., Nd=6
        int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        int offset = (firstDayOfWeek - Calendar.MONDAY + 7) % 7;

        // Uzupelnienie do pelnych wierszy (kazdy po 7 komorek)
        int totalCells = offset + daysInMonth;
        int rows = (int) Math.ceil(totalCells / 7.0);
        totalCells = rows * 7;

        // Cofnij kalendarz o offset dni (na ostatnie dni poprzedniego miesiaca)
        Calendar dayCal = (Calendar) cal.clone();
        dayCal.add(Calendar.DAY_OF_MONTH, -offset);

        Calendar today = Calendar.getInstance();

        for (int i = 0; i < totalCells; i++) {
            CalendarAdapter.CalendarDay day = new CalendarAdapter.CalendarDay();
            day.dayOfMonth = dayCal.get(Calendar.DAY_OF_MONTH);
            day.timestamp = dayCal.getTimeInMillis();
            day.isCurrentMonth = (dayCal.get(Calendar.MONTH) == currentMonth
                    && dayCal.get(Calendar.YEAR) == currentYear);
            day.isToday = isSameDay(dayCal, today);

            days.add(day);
            dayCal.add(Calendar.DAY_OF_MONTH, 1);
        }

        return days;
    }

    // ========================================================
    //  MAPOWANIE ZADAN NA DNI
    // ========================================================

    /**
     * Liczy ile zadan przypada na kazdy dzien widoczny w kalendarzu.
     * Wynik zapisuje w polu taskCount kazdego CalendarDay.
     */
    private void mapTaskCountsToDays(List<CalendarAdapter.CalendarDay> days,
                                     List<Task> tasks) {
        // Klucz: data w formacie "yyyy-MM-dd", wartosc: liczba zadan
        Map<String, Integer> countMap = new HashMap<>();
        SimpleDateFormat keyFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        for (Task task : tasks) {
            String key = keyFormat.format(new Date(task.getDate()));
            countMap.merge(key, 1, Integer::sum);
        }

        for (CalendarAdapter.CalendarDay day : days) {
            String key = keyFormat.format(new Date(day.timestamp));
            Integer count = countMap.get(key);
            day.taskCount = (count != null) ? count : 0;
        }
    }

    // ========================================================
    //  ZAZNACZANIE DNIA
    // ========================================================

    /**
     * Automatycznie zaznacza dzien:
     * - dzisiejszy, jesli kalendarz pokazuje biezacy miesiac
     * - pierwszy dzien miesiaca w przeciwnym wypadku
     */
    private void autoSelectDay(List<CalendarAdapter.CalendarDay> days) {
        Calendar today = Calendar.getInstance();
        boolean isTodayMonth = (currentYear == today.get(Calendar.YEAR)
                && currentMonth == today.get(Calendar.MONTH));

        int targetDay = isTodayMonth ? today.get(Calendar.DAY_OF_MONTH) : 1;

        for (int i = 0; i < days.size(); i++) {
            CalendarAdapter.CalendarDay day = days.get(i);
            if (day.isCurrentMonth && day.dayOfMonth == targetDay) {
                day.isSelected = true;
                selectedPosition = i;
                selectedDayTimestamp = day.timestamp;
                break;
            }
        }
    }

    @Override
    public void onDayClick(int position, CalendarAdapter.CalendarDay day) {
        int oldPosition = selectedPosition;
        selectedPosition = position;
        selectedDayTimestamp = day.timestamp;

        // Aktualizacja zaznaczenia (tylko dwie komorki, nie cala siatka)
        calendarAdapter.setSelectedPosition(oldPosition, position);
        loadTasksForDay(day.timestamp);
    }

    // ========================================================
    //  LISTA ZADAN WYBRANEGO DNIA
    // ========================================================

    private void loadTasksForDay(long dayStart) {
        long dayEnd = dayStart + 86400000L; // +24 godziny
        List<Task> tasks = taskDao.getByDateRange(dayStart, dayEnd);

        // Naglowek: data w formacie "20 września, piątek"
        SimpleDateFormat sdf = new SimpleDateFormat("d MMMM, EEEE", Locale.getDefault());
        textSelectedDate.setText(sdf.format(new Date(dayStart)));

        // Licznik z poprawna odmiana ("1 Zadanie", "3 Zadania", "5 Zadań")
        textTaskCount.setText(
                getResources().getQuantityString(R.plurals.task_count, tasks.size(), tasks.size()));

        taskAdapter.updateTasks(tasks);

        // Przelaczenie miedzy lista a komunikatem pustego stanu
        boolean empty = tasks.isEmpty();
        recyclerDayTasks.setVisibility(empty ? View.GONE : View.VISIBLE);
        textEmptyDay.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    // ========================================================
    //  CALLBACKI TASK ADAPTERA
    // ========================================================

    @Override
    public void onTaskClick(Task task) {
        Intent intent = new Intent(requireContext(), TaskEditorActivity.class);
        intent.putExtra("task_id", task.getId());
        startActivity(intent);
    }

    @Override
    public void onCheckboxClick(Task task) {
        if (task.getStatus() == 0) {
            task.setStatus(1);
        } else {
            task.setStatus(0);
        }
        taskDao.update(task);
        loadMonth();
    }

    // ========================================================
    //  POMOC
    // ========================================================

    private boolean isSameDay(Calendar c1, Calendar c2) {
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)
                && c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR);
    }
}