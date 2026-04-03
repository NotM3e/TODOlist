package com.example.todolist;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;

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

    // Widoki:
    private EditText inputTitle, inputDescription;
    private TextView textDate, textTime;
    private MaterialButtonToggleGroup togglePriority, toggleStatus;
    private MaterialButton btnPriorityLow, btnPriorityMed, btnPriorityHigh;
    private MaterialButton btnStatusTodo, btnStatusFailed, btnStatusDone;
    private MaterialButton btnConfirm;
    private View sectionStatus;
    private TextView badgeSaved;

    // Dane:
    private TaskDao taskDao;
    private DraftManager draftManager;
    private Task currentTask;       // null w trybie tworzenia
    private boolean isEditMode;

    // Wybrane wartości:
    private long selectedDate = 0;
    private long selectedTime = 0;
    private int selectedPriority = 0;
    private int selectedStatus = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_editor);

        taskDao = AppDatabase.getInstance(this).taskDao();
        draftManager = new DraftManager(this);

        inputTitle = findViewById(R.id.input_title);
        inputDescription = findViewById(R.id.input_description);
        textDate = findViewById(R.id.text_date);
        textTime = findViewById(R.id.text_time);
        togglePriority = findViewById(R.id.toggle_priority);
        toggleStatus = findViewById(R.id.toggle_status);
        btnPriorityLow = findViewById(R.id.btn_priority_low);
        btnPriorityMed = findViewById(R.id.btn_priority_medium);
        btnPriorityHigh = findViewById(R.id.btn_priority_high);
        btnStatusTodo = findViewById(R.id.btn_status_todo);
        btnStatusFailed = findViewById(R.id.btn_status_failed);
        btnStatusDone = findViewById(R.id.btn_status_done);
        btnConfirm = findViewById(R.id.btn_confirm);
        sectionStatus = findViewById(R.id.section_status);
        badgeSaved = findViewById(R.id.badge_saved);

        // Sprawdzanie trybu
        int taskId = getIntent().getIntExtra("task_id", -1);
        isEditMode = (taskId != -1);

        if (isEditMode) {
            currentTask = taskDao.getById(taskId);
            sectionStatus.setVisibility(View.VISIBLE);
            badgeSaved.setVisibility(View.VISIBLE);
            btnConfirm.setText(R.string.btn_save);
            
            // Ładowanie danych istniejących
            loadTask();
        } else {
            // Sprawdzanie czy jest zapisany szkic
            if (draftManager.hasDraft()) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.draft_dialog_title);
                builder.setMessage(R.string.draft_dialog_message);
                builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        loadDraft();
                    }
                });
                builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        draftManager.clearDraft();
                    }
                });
                builder.setCancelable(false);
                builder.show();
            }
        }

        findViewById(R.id.card_date).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicker();
            }
        });

        View cardTime = findViewById(R.id.card_time);
        cardTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTimePicker();
            }
        });
        
        // Długie przytrzymanie godziny powoduje jej wyczyszczenie
        cardTime.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                selectedTime = 0;
                updateTimeDisplay();
                return true;
            }
        });

        togglePriority.addOnButtonCheckedListener(new MaterialButtonToggleGroup.OnButtonCheckedListener() {
            @Override
            public void onButtonChecked(MaterialButtonToggleGroup group, int checkedId, boolean isChecked) {
                int selected = group.getCheckedButtonId();
                if (selected == R.id.btn_priority_low)         selectedPriority = 1;
                else if (selected == R.id.btn_priority_medium)  selectedPriority = 2;
                else if (selected == R.id.btn_priority_high)    selectedPriority = 3;
                else                                            selectedPriority = 0;
                updatePriorityColors();
            }
        });

        // Wybór statusu - tylko w edycji
        toggleStatus.addOnButtonCheckedListener(new MaterialButtonToggleGroup.OnButtonCheckedListener() {
            @Override
            public void onButtonChecked(MaterialButtonToggleGroup group, int checkedId, boolean isChecked) {
                int selected = group.getCheckedButtonId();
                if (selected == R.id.btn_status_todo)        selectedStatus = 0;
                else if (selected == R.id.btn_status_done)   selectedStatus = 1;
                else if (selected == R.id.btn_status_failed) selectedStatus = 2;
                updateStatusColors();
            }
        });

        // Sprawdzanie czy tytuł nie jest pusty
        inputTitle.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                updateConfirmButton();
            }
        });

        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveTask();
            }
        });

        findViewById(R.id.btn_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleExit();
            }
        });

        findViewById(R.id.btn_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleExit();
            }
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleExit();
            }
        });

        // Na końcu ustawiamy stan przycisku zapisu
        updateConfirmButton();
    }

    // Ustawienie danych z bazy danych do widoków
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

    // Ustawienie danych z szkica do edytora
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

    // Funkcja wywoływana przy wychodzeniu z edytora
    private void handleExit() {
        // Zapisz szkic tylko jeśli to nowe zadanie i coś wpisano
        if (!isEditMode && hasAnyContent()) {
            draftManager.saveDraft(
                    inputTitle.getText().toString().trim(),
                    inputDescription.getText().toString().trim(),
                    selectedDate, selectedTime, selectedPriority
            );
        }
        finish();
    }

    // Sprawdza czy użytkownik wpisał cokolwiek w formularzu
    private boolean hasAnyContent() {
        return !inputTitle.getText().toString().trim().isEmpty()
                || !inputDescription.getText().toString().trim().isEmpty()
                || selectedDate != 0
                || selectedTime != 0
                || selectedPriority != 0;
    }

    // Wyświetla okno wyboru daty
    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        if (selectedDate != 0) cal.setTimeInMillis(selectedDate);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                Calendar selected = Calendar.getInstance();
                selected.set(year, month, dayOfMonth, 0, 0, 0);
                selected.set(Calendar.MILLISECOND, 0);
                selectedDate = selected.getTimeInMillis();
                updateDateDisplay();
                updateConfirmButton();
            }
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        
        datePickerDialog.show();
    }

    // Wyświetla okno wyboru godziny
    private void showTimePicker() {
        Calendar cal = Calendar.getInstance();
        if (selectedTime != 0) cal.setTimeInMillis(selectedTime);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                Calendar selected = Calendar.getInstance();
                selected.set(Calendar.HOUR_OF_DAY, hourOfDay);
                selected.set(Calendar.MINUTE, minute);
                selected.set(Calendar.SECOND, 0);
                selected.set(Calendar.MILLISECOND, 0);
                selectedTime = selected.getTimeInMillis();
                updateTimeDisplay();
            }
        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true);
        
        timePickerDialog.show();
    }

    // Zapisuje zadanie do bazy danych
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

    // Aktualizacja tekstu z datą na ekranie
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

    // Aktualizacja tekstu z godziną na ekranie
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

    // Sprawdza czy przycisk zapisu powinien być aktywny
    private void updateConfirmButton() {
        boolean valid = !inputTitle.getText().toString().trim().isEmpty() && selectedDate != 0;
        btnConfirm.setEnabled(valid);
        btnConfirm.setAlpha(valid ? 1.0f : 0.5f);
    }

    private void setPriorityChecked() {
        togglePriority.clearChecked();
        if (selectedPriority == 1) togglePriority.check(R.id.btn_priority_low);
        else if (selectedPriority == 2) togglePriority.check(R.id.btn_priority_medium);
        else if (selectedPriority == 3) togglePriority.check(R.id.btn_priority_high);
        
        updatePriorityColors();
    }

    private void setStatusChecked() {
        if (selectedStatus == 0) toggleStatus.check(R.id.btn_status_todo);
        else if (selectedStatus == 1) toggleStatus.check(R.id.btn_status_done);
        else if (selectedStatus == 2) toggleStatus.check(R.id.btn_status_failed);
        
        updateStatusColors();
    }

    private void updatePriorityColors() {
        resetButton(btnPriorityLow);
        resetButton(btnPriorityMed);
        resetButton(btnPriorityHigh);

        if (selectedPriority == 1) activateButton(btnPriorityLow,  R.color.priority_low);
        else if (selectedPriority == 2) activateButton(btnPriorityMed,  R.color.priority_medium);
        else if (selectedPriority == 3) activateButton(btnPriorityHigh, R.color.priority_high);
    }

    private void updateStatusColors() {
        resetButton(btnStatusTodo);
        resetButton(btnStatusDone);
        resetButton(btnStatusFailed);

        if (selectedStatus == 0) activateButton(btnStatusTodo,   R.color.accent);
        else if (selectedStatus == 1) activateButton(btnStatusDone,   R.color.status_done);
        else if (selectedStatus == 2) activateButton(btnStatusFailed, R.color.status_failed);
    }

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
        btn.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.text_hint)));
    }
}
