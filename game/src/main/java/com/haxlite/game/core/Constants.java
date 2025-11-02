package com.goalrift.football.game.core;

public class Constants {
    public static final int TICK_RATE = 60; // 60 Hz fixed
    public static final float DT = 1f / TICK_RATE;

    public static final float FIELD_W = 120f;
    public static final float FIELD_H = 70f;
    public static final float GOAL_W = 8f;

    public static final float PLAYER_R = 2.5f;
    public static final float BALL_R = 1.5f;

    public static final float PLAYER_SPEED = 36f;
    public static final float SHOT_IMPULSE = 32f;

    public static final float FRIC = 0.985f;
    public static final float BOUNCE = 0.9f;

    // YENİ: maç süresi ve kickoff ayarları
    public static final int MATCH_DURATION_MS = 180_000;      // 3 dakika
    public static final int KICKOFF_COUNTDOWN_MS = 3_000;     // 3 sn geri sayım
    public static final float KICKOFF_OFFSET_X = 18f;         // merkezden sağ/sol mesafe
}
