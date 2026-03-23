package com.example.todolist.util;

import android.content.Context;
import android.content.SharedPreferences;

public class DraftManager {

    private static final String PREF_NAME = "task_draft";
    private final SharedPreferences prefs;

    public DraftManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveDraft(String title, String description,
                          long date, long time, int priority) {
        prefs.edit()
                .putString("title", title)
                .putString("description", description)
                .putLong("date", date)
                .putLong("time", time)
                .putInt("priority", priority)
                .putBoolean("has_draft", true)
                .apply();
    }

    public boolean hasDraft() {
        return prefs.getBoolean("has_draft", false);
    }

    public String getTitle()       { return prefs.getString("title", ""); }
    public String getDescription() { return prefs.getString("description", ""); }
    public long   getDate()        { return prefs.getLong("date", 0); }
    public long   getTime()        { return prefs.getLong("time", 0); }
    public int    getPriority()    { return prefs.getInt("priority", 0); }

    public void clearDraft() {
        prefs.edit().clear().apply();
    }
}