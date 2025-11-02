package com.goalrift.football.iapads;

public class AdsManager {
    public interface Listener { void onReward(); }
    public void init() {}
    public void showRewarded(Listener l) { if (l!=null) l.onReward(); }
}
