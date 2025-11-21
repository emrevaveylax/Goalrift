package com.goalrift.football.app.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class JetonManager {
    private static final String PREF_NAME = "jeton_prefs";
    private static final String KEY_JETON = "jeton";
    private SharedPreferences prefs;

    public JetonManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // Kaç jetonun var
    public int getJeton() {
        int current = prefs.getInt(KEY_JETON, 0);

        // Eğer kayıtlı jeton 99.999'dan azsa, otomatik olarak 99.999 yap
        if (current < 99999) {
            current = 99999;
            prefs.edit().putInt(KEY_JETON, current).apply();
        }

        return current;
    }

    // Jeton ekle
    public void addJeton(int amount) {
        int current = getJeton();
        prefs.edit().putInt(KEY_JETON, current + amount).apply();
    }

    // Jeton sıfırla
    public void resetJeton() {
        prefs.edit().putInt(KEY_JETON, 0).apply();
    }
}
