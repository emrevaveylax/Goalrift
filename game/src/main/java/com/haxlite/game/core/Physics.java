package com.goalrift.football.game.core;

import static com.goalrift.football.game.core.Constants.*;

public class Physics {

    public static void initKickoff(com.goalrift.football.game.core.WorldState w){
        // Topu merkeze al ve durdur
        w.ball.x=FIELD_W/2f; w.ball.y=FIELD_H/2f; w.ball.vx=0; w.ball.vy=0;

        // Geri sayımı başlat
        w.countdownMs = KICKOFF_COUNTDOWN_MS;

        // Oyuncuları eşit mesafede konumlandır (1v1 öncelik)
        if (w.players.length == 2) {
            // Takımları ata
            w.players[0].team = 0;
            w.players[1].team = 1;

            // Merkeze eşit uzaklıkta, aynı hizada
            w.players[0].x = FIELD_W/2f - KICKOFF_OFFSET_X; w.players[0].y = FIELD_H/2f;
            w.players[1].x = FIELD_W/2f + KICKOFF_OFFSET_X; w.players[1].y = FIELD_H/2f;

            w.players[0].vx=w.players[0].vy=0;
            w.players[1].vx=w.players[1].vy=0;
            w.players[0].shoot=w.players[1].shoot=false;
            return;
        }

        // Çoklu oyuncu için basit simetrik dağıtım (takım 0 sol, takım 1 sağ)
        for(int i=0;i<w.players.length;i++){
            com.goalrift.football.game.core.Player p = w.players[i];
            p.team = i%2;
            float side = (p.team==0)? -1f : +1f;
            float rank = (i/2); // aynı takımda yukarı-aşağı dağılım
            float yOff = (rank%2==0? -1: +1) * (6f + 6f*(rank/2));
            p.x = FIELD_W/2f + side*KICKOFF_OFFSET_X;
            p.y = FIELD_H/2f + yOff;
            p.vx=p.vy=0; p.shoot=false;
        }
    }

    public static void step(com.goalrift.football.game.core.WorldState w){
        // Geri sayım sırasında fizik dondur
        if (w.countdownMs > 0) {
            w.countdownMs -= (int)(DT * 1000f);
            if (w.countdownMs < 0) w.countdownMs = 0;
            return; // hiçbir şeyi oynatma
        }

        // oyuncular
        for(com.goalrift.football.game.core.Player p: w.players){
            p.x += p.vx*DT;
            p.y += p.vy*DT;
            p.vx *= FRIC; p.vy *= FRIC;
            if(p.x < PLAYER_R){p.x=PLAYER_R;p.vx*=-BOUNCE;}
            if(p.x > FIELD_W-PLAYER_R){p.x=FIELD_W-PLAYER_R;p.vx*=-BOUNCE;}
            if(p.y < PLAYER_R){p.y=PLAYER_R;p.vy*=-BOUNCE;}
            if(p.y > FIELD_H-PLAYER_R){p.y=FIELD_H-PLAYER_R;p.vy*=-BOUNCE;}
        }

        // top
        w.ball.x += w.ball.vx*DT;
        w.ball.y += w.ball.vy*DT;
        w.ball.vx *= FRIC; w.ball.vy *= FRIC;

        // üst/alt duvarlar
        if(w.ball.y < BALL_R){ w.ball.y=BALL_R; w.ball.vy*=-BOUNCE; }
        if(w.ball.y > FIELD_H-BALL_R){ w.ball.y=FIELD_H-BALL_R; w.ball.vy*=-BOUNCE; }

        // sol/sağ duvarlar (orta bölgede kale boşluğu)
        boolean inGoalY = w.ball.y > FIELD_H/2f-6 && w.ball.y < FIELD_H/2f+6;
        if(!inGoalY){
            if(w.ball.x < BALL_R){ w.ball.x=BALL_R; w.ball.vx*=-BOUNCE; }
            if(w.ball.x > FIELD_W-BALL_R){ w.ball.x=FIELD_W-BALL_R; w.ball.vx*=-BOUNCE; }
        } else {
            if(w.ball.x < 0){ w.score[1]++; initKickoff(w); } // GOL: kickoff + geri sayım
            if(w.ball.x > FIELD_W){ w.score[0]++; initKickoff(w); }
        }

        // oyuncu-top çarpışma
        for(Player p: w.players){
            float dx = w.ball.x - p.x, dy = w.ball.y - p.y;
            float r = (PLAYER_R+BALL_R);
            float d2 = dx*dx+dy*dy;
            if(d2 < r*r){
                float d = (float)Math.sqrt(Math.max(1e-5, d2));
                float nx = dx/d, ny = dy/d;
                float pen = r - d;
                w.ball.x += nx*pen; w.ball.y += ny*pen;

                float relvx = w.ball.vx - p.vx;
                float relvy = w.ball.vy - p.vy;
                float vn = relvx*nx + relvy*ny;
                if(vn < 0){
                    w.ball.vx -= (1+BOUNCE)*vn*nx;
                    w.ball.vy -= (1+BOUNCE)*vn*ny;
                }
                if(p.shoot){
                    w.ball.vx += nx*SHOT_IMPULSE;
                    w.ball.vy += ny*SHOT_IMPULSE;
                }
            }
        }
    }
}
