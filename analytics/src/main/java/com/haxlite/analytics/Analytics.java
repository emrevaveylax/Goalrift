package com.goalrift.football.analytics;

import android.util.Log;

public class Analytics {
    private static String TAG = "Analytics";
    public static void event(String name) {
        Log.d(TAG, "event: " + name);
    }
    public static void event(String name, String kv) {
        Log.d(TAG, "event: " + name + " " + kv);
    }
}
