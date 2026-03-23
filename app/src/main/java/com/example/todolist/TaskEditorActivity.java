package com.example.todolist;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.todolist.database.AppDatabase;
import com.example.todolist.database.TaskDao;
import com.example.todolist.model.Task;
import com.example.todolist.util.DraftManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class TaskEditorActivity extends AppCompatActivity {

    // ========== Widoki ==========
    private EditText inputTitle, inputDescription;
    private TextView textDate, textTime;
    private MaterialButtonToggleGroup togglePriority, toggleStatus;
    private MaterialButton btnPriorityLow, btnPriorityMed, btnPriorityHigh;
    private MaterialButton btnStatusTodo, btnStatusFailed, btnStatusDone;
    private MaterialButton btnConfirm;
    private View sectionStatus;
    private TextView badgeSaved;

    // ========== Dane ==========
    private TaskDao taskDao;
    private DraftManager draftManager;
    private Task currentTask;       // null w trybie tworzenia
    private boolean isEditMode;

    // Aktualnie wybrane wartosci
    private long selectedDate = 0;
    private long selectedTime = 0;
    private int selectedPriority = 0;
    private int selectedStatus = 0;

    // ========================================================
    //  INICJALIZACJA
    // ========================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_editor);

        taskDao = AppDatabase.getInstance(this).taskDao();
        draftManager = new DraftManager(this);

        initViews();
        determineMode();
        setupListeners();
        setupBackNavigation();

        if (isEditMode) {
            loadTask();
        } else {
            checkDraft();
        }

        updateConfirmButton();
    }

    private void initViews() {
        inputTitle       = findViewById(R.id.input_title);
        inputDescription = findViewById(R.id.input_description);
        textDate         = findViewById(R.id.text_date);
        textTime         = findViewById(R.id.text_time);
        togglePriority   = findViewById(R.id.toggle_priority);
        toggleStatus     = findViewById(R.id.toggle_status);
        btnPriorityLow   = findViewById(R.id.btn_priority_low);
        btnPriorityMed   = findViewById(R.id.btn_priority_medium);
        btnPriorityHigh  = findViewById(R.id.btn_priority_high);
        btnStatusTodo    = findViewById(R.id.btn_status_todo);
        btnStatusFailed  = findViewById(R.id.btn_status_failed);
        btnStatusDone    = findViewById(R.id.btn_status_done);
        btnConfirm       = findViewById(R.id.btn_confirm);
        sectionStatus    = findViewById(R.id.section_status);
        badgeSaved       = findViewById(R.id.badge_saved);
    }

    private void determineMode() {
        int taskId = getIntent().getIntExtra("task_id", -1);
        isEditMode = (taskId != -1);

        if (isEditMode) {
            currentTask = taskDao.getById(taskId);
            sectionStatus.setVisibility(View.VISIBLE);
            badgeSaved.setVisibility(View.VISIBLE);
            btnConfirm.setText(R.string.btn_save);
        } else {
            // Wstepne wypelnienie daty (np. z widoku kalendarza)
            long prefillDate = getIntent().getLongExtra("prefill_date", 0);
            if (prefillDate != 0) {
                selectedDate = prefillDate;
                updateDateDisplay();
            }
        }
    }

    // ========================================================
    //  LADOWANIE DANYCH
    // ========================================================

    private void loadTask() {
        inputTitle.setText(currentTask.getTitle());
        inputDescription.setText(currentTask.getDescription());
        selectedDate = currentTask.getDate();
        selectedTime = currentTask.getTime();
        selectedPriority = currentTask.getPriority();
        selectedStatus = currentTask.getStatus();

        updateDateDisplay();
        updateTimeDisplay();
        setPriorityChecked();
        setStatusChecked();
    }

    // ========================================================
    //  DRAFT – szkic nowego zadania
    // ========================================================

    private void checkDraft() {
        if (!draftManager.hasDraft()) return;

        new AlertDialog.Builder(this)
                .setTitle(R.string.draft_dialog_title)
                .setMessage(R.string.draft_dialog_message)
                .setPositiveButton(R.string.yes, (d, w) -> loadDraft())
                .setNegativeButton(R.string.no, (d, w) -> draftManager.clearDraft())
                .setCancelable(false)
                .show();
    }

    private void loadDraft() {
        inputTitle.setText(draftManager.getTitle());
        inputDescription.setText(draftManager.getDescription());
        selectedDate = draftManager.getDate();
        selectedTime = draftManager.getTime();
        selectedPriority = draftManager.getPriority();

        updateDateDisplay();
        updateTimeDisplay();
        setPriorityChecked();
        updateConfirmButton();
    }

    /**
     * Zapisuje szkic tylko w trybie tworzenia i tylko gdy
     * uzytkownik wpisal jakiekolwiek dane.
     */
    private void saveDraftIfNeeded() {
        if (isEditMode) return;
        if (!hasAnyContent()) return;

        draftManager.saveDraft(
                inputTitle.getText().toString().trim(),
                inputDescription.getText().toString().trim(),
                selectedDate, selectedTime, selectedPriority
        );
    }

    private boolean hasAnyContent() {
        return !inputTitle.getText().toString().trim().isEmpty()
                || !inputDescription.getText().toString().trim().isEmpty()
                || selectedDate != 0
                || selectedTime != 0
                || selectedPriority != 0;
    }

    // ========================================================
    //  LISTENERY
    // ========================================================

    private void setupListeners() {

        // --- Karta daty – otwiera DatePickerDialog ---
        findViewById(R.id.card_date).setOnClickListener(v -> showDatePicker());

        // --- Karta godziny – klikniecie otwiera picker,
        //     dlugie przytrzymanie czysci godzine ---
        View cardTime = findViewById(R.id.card_time);
        cardTime.setOnClickListener(v -> showTimePicker());
        cardTime.setOnLongClickListener(v -> {
            selectedTime = 0;
            updateTimeDisplay();
            return true;
        });

        // --- Priorytet ---
        togglePriority.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            int selected = group.getCheckedButtonId();
            if (selected == R.id.btn_priority_low)         selectedPriority = 1;
            else if (selected == R.id.btn_priority_medium)  selectedPriority = 2;
            else if (selected == R.id.btn_priority_high)    selectedPriority = 3;
            else                                            selectedPriority = 0;
            updatePriorityColors();
        });

        // --- Status (tylko tryb edycji) ---
        toggleStatus.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            int selected = group.getCheckedButtonId();
            if (selected == R.id.btn_status_todo)        selectedStatus = 0;
            else if (selected == R.id.btn_status_done)   selectedStatus = 1;
            else if (selected == R.id.btn_status_failed) selectedStatus = 2;
            updateStatusColors();
        });

        // --- Walidacja tytulu w czasie rzeczywistym ---
        inputTitle.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) {}
            public void afterTextChanged(Editable s) { updateConfirmButton(); }
        });

        // --- Przyciski akcji ---
        btnConfirm.setOnClickListener(v -> saveTask());
        findViewById(R.id.btn_cancel).setOnClickListener(v -> handleExit());
        findViewById(R.id.btn_back).setOnClickListener(v -> handleExit());
    }

    /**
     * Obsluga systemowego przycisku "wstecz" (gest lub przycisk na dole telefonu).
     * OnBackPressedCallback to nowszy sposob obslugujacy wszystkie wersje Androida.
     */
    private void setupBackNavigation() {
        getOnBackPressedDispatcher().addCallback(this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        handleExit();
                    }
                });
    }

    private void handleExit() {
        saveDraftIfNeeded();
        finish();
    }

    // ========================================================
    //  DIALOGI WYBORU DATY I GODZINY
    // ========================================================

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        if (selectedDate != 0) cal.setTimeInMillis(selectedDate);

        new DatePickerDialog(this, (view, year, month, day) -> {
            Calendar selected = Calendar.getInstance();
            selected.set(year, month, day, 0, 0, 0);
            selected.set(Calendar.MILLISECOND, 0);
            selectedDate = selected.getTimeInMillis();
            updateDateDisplay();
            updateConfirmButton();
        },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void showTimePicker() {
        Calendar cal = Calendar.getInstance();
        if (selectedTime != 0) cal.setTimeInMillis(selectedTime);

        new TimePickerDialog(this, (view, hour, minute) -> {
            Calendar selected = Calendar.getInstance();
            selected.set(Calendar.HOUR_OF_DAY, hour);
            selected.set(Calendar.MINUTE, minute);
            selected.set(Calendar.SECOND, 0);
            selected.set(Calendar.MILLISECOND, 0);
            selectedTime = selected.getTimeInMillis();
            updateTimeDisplay();
        },
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                true   // format 24h
        ).show();
    }

    // ========================================================
    //  ZAPIS ZADANIA
    // ========================================================

    private void saveTask() {
        String title = inputTitle.getText().toString().trim();
        String desc  = inputDescription.getText().toString().trim();

        if (isEditMode) {
            currentTask.setTitle(title);
            currentTask.setDescription(desc.isEmpty() ? null : desc);
            currentTask.setDate(selectedDate);
            currentTask.setTime(selectedTime);
            currentTask.setPriority(selectedPriority);
            currentTask.setStatus(selectedStatus);
            taskDao.update(currentTask);
        } else {
            Task task = new Task(title, selectedDate);
            task.setDescription(desc.isEmpty() ? null : desc);
            task.setTime(selectedTime);
            task.setPriority(selectedPriority);
            taskDao.insert(task);
            draftManager.clearDraft();
        }

        finish();
    }

    // ========================================================
    //  AKTUALIZACJA WYSWIETLANIA
    // ========================================================

    private void updateDateDisplay() {
        if (selectedDate == 0) {
            textDate.setText(R.string.select_date);
            textDate.setTextColor(ContextCompat.getColor(this, R.color.text_hint));
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            textDate.setText(sdf.format(new Date(selectedDate)));
            textDate.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        }
    }

    private void updateTimeDisplay() {
        if (selectedTime == 0) {
            textTime.setText(R.string.no_time);
            textTime.setTextColor(ContextCompat.getColor(this, R.color.text_hint));
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            textTime.setText(sdf.format(new Date(selectedTime)));
            textTime.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        }
    }

    private void updateConfirmButton() {
        boolean valid = isFormValid();
        btnConfirm.setEnabled(valid);
        btnConfirm.setAlpha(valid ? 1.0f : 0.5f);
    }

    private boolean isFormValid() {
        return !inputTitle.getText().toString().trim().isEmpty()
                && selectedDate != 0;
    }

    // ========================================================
    //  KOLORY PRZYCISKOW TOGGLE
    // ========================================================

    /**
     * Programowo zaznacza przycisk priorytetu odpowiadajacy
     * wartosci selectedPriority i aktualizuje kolory.
     */
    private void setPriorityChecked() {
        togglePriority.clearChecked();
        switch (selectedPriority) {
            case 1: togglePriority.check(R.id.btn_priority_low);    break;
            case 2: togglePriority.check(R.id.btn_priority_medium); break;
            case 3: togglePriority.check(R.id.btn_priority_high);   break;
        }
        updatePriorityColors();
    }

    private void setStatusChecked() {
        switch (selectedStatus) {
            case 0: toggleStatus.check(R.id.btn_status_todo);   break;
            case 1: toggleStatus.check(R.id.btn_status_done);   break;
            case 2: toggleStatus.check(R.id.btn_status_failed); break;
        }
        updateStatusColors();
    }

    private void updatePriorityColors() {
        resetButton(btnPriorityLow);
        resetButton(btnPriorityMed);
        resetButton(btnPriorityHigh);

        switch (selectedPriority) {
            case 1: activateButton(btnPriorityLow,  R.color.priority_low);    break;
            case 2: activateButton(btnPriorityMed,  R.color.priority_medium); break;
            case 3: activateButton(btnPriorityHigh, R.color.priority_high);   break;
        }
    }

    private void updateStatusColors() {
        resetButton(btnStatusTodo);
        resetButton(btnStatusDone);
        resetButton(btnStatusFailed);

        switch (selectedStatus) {
            case 0: activateButton(btnStatusTodo,   R.color.accent);        break;
            case 1: activateButton(btnStatusDone,   R.color.status_done);   break;
            case 2: activateButton(btnStatusFailed, R.color.status_failed); break;
        }
    }

    // --- Pomocnicze metody do zmiany wygladu przyciskow ---

    private void activateButton(MaterialButton btn, int colorRes) {
        int color = ContextCompat.getColor(this, colorRes);
        btn.setBackgroundTintList(ColorStateList.valueOf(color));
        btn.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        btn.setStrokeColor(ColorStateList.valueOf(color));
    }

    private void resetButton(MaterialButton btn) {
        int bgColor = ContextCompat.getColor(this, R.color.background_card);
        btn.setBackgroundTintList(ColorStateList.valueOf(bgColor));
        btn.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        btn.setStrokeColor(ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.text_hint)));
    }
}