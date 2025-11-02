package com.goalrift.football.app.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class ActiveItemManager {
    private static final String PREF_NAME = "active_items";
    private SharedPreferences prefs;

    public ActiveItemManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void setActive(String category, String itemKey) {
        prefs.edit().putString(category, itemKey).apply();
    }

    public String getActive(String category) {
        return prefs.getString(category, "default");
    }
}
