package com.example.todolist.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "tasks")
public class Task {

    @PrimaryKey(autoGenerate = true)
    private int id;         // inkrementowany, -1 = nowo utworzony

    @NonNull
    private String title;

    private String description;

    private long date;       // timestamp w milisekundach, pole wymagane

    private long time;       // timestamp godziny, 0 = brak godziny

    private int priority;    // 0=brak, 1=niski, 2=sredni, 3=wysoki

    private int status;      // 0=NIEZROBIONE, 1=SKONCZONE, 2=NIEUDANE

    private long createdAt;

    // --- Konstruktor (wymagane pola: tytul i data) ---

    public Task(@NonNull String title, long date) {
        this.title = title;
        this.date = date;
        this.status = 0;
        this.priority = 0;
        this.time = 0;
        this.createdAt = System.currentTimeMillis();
    }

    // --- Gettery i Settery ---

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    @NonNull
    public String getTitle() { return title; }
    public void setTitle(@NonNull String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public long getDate() { return date; }
    public void setDate(long date) { this.date = date; }

    public long getTime() { return time; }
    public void setTime(long time) { this.time = time; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}