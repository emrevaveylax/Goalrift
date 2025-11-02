package com.goalrift.football.net.dto;

public class InputMessage {
    public int seq;
    public long clientTime;
    public float moveX, moveY;
    public boolean shoot;
    public InputMessage() {}
    public InputMessage(int seq, long clientTime, float moveX, float moveY, boolean shoot) {
        this.seq = seq; this.clientTime = clientTime; this.moveX=moveX; this.moveY=moveY; this.shoot=shoot;
    }
}
