package com.example.todolist.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.todolist.model.Task;

import java.util.List;

@Dao
public interface TaskDao {

    // --- Zapytania dla MainView ---

    @Query("SELECT * FROM tasks WHERE status = 0 ORDER BY date ASC")
    List<Task> getAllActive();

    @Query("SELECT * FROM tasks WHERE status != 0 AND date >= :cutoff ORDER BY date DESC")
    List<Task> getCompletedSince(long cutoff);

    // --- Zapytania dla CalendarView ---

    @Query("SELECT * FROM tasks WHERE date >= :start AND date < :end ORDER BY time ASC")
    List<Task> getByDateRange(long start, long end);

    // --- Zapytania dla edytora ---

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    Task getById(int taskId);

    // --- Operacje zapisu ---

    @Insert
    void insert(Task task);

    @Update
    void update(Task task);

    @Query("DELETE FROM tasks WHERE id = :taskId")
    void deleteById(int taskId);
}