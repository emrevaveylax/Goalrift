package com.goalrift.football.net.dto;

public class Snapshot {
    public int lastProcessedInputSeq;
    public float ballX, ballY, ballVX, ballVY;
    public float[] px, py, pvx, pvy;
    public int[] scores; // [home, away]
    public long serverTime;

    // YENİ: geri sayım ve kalan maç süresi
    public int countdownMs;
    public int matchRemainingMs;
}
