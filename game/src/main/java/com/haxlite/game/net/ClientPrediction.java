package com.goalrift.football.game.net;

import com.goalrift.football.game.core.WorldState;
import com.goalrift.football.game.core.Physics;
import com.goalrift.football.net.dto.InputMessage;

import java.util.ArrayList;
import java.util.List;

public class ClientPrediction {
    private final List<InputMessage> pending = new ArrayList<>();
    public int seq = 0;

    public InputMessage nextInput(float mx, float my, boolean shoot){
        InputMessage im = new InputMessage(++seq, System.currentTimeMillis(), mx, my, shoot);
        pending.add(im);
        return im;
    }
    public void applyPending(WorldState local, int lastProcessedSeq){
        int idx=0;
        while(idx < pending.size() && pending.get(idx).seq <= lastProcessedSeq) idx++;
        if(idx>0) pending.subList(0, idx).clear();
        // re-sim
        for(InputMessage im: pending){
            // simplistic: assume player 0 is us
            local.players[0].vx = im.moveX;
            local.players[0].vy = im.moveY;
            local.players[0].shoot = im.shoot;
            Physics.step(local);
        }
    }
}
