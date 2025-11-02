package com.goalrift.football.app.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class OwnedManager {
    private static final String PREF_NAME = "owned_items";
    private SharedPreferences prefs;

    public OwnedManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // Ürüne sahiplik ver
    public void setOwned(String itemKey, boolean owned) {
        prefs.edit().putBoolean(itemKey, owned).apply();
    }

    // Ürüne sahip mi?
    public boolean isOwned(String itemKey) {
        return prefs.getBoolean(itemKey, false);
    }
}
