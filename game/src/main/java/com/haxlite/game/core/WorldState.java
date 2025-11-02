package com.goalrift.football.game.core;

public class WorldState {
    public Ball ball = new Ball();
    public Player[] players;
    public int[] score = new int[]{0,0};

    // YENİ: kickoff geri sayımı (ms)
    public int countdownMs = 0;

    public WorldState(int numPlayers){
        players = new Player[numPlayers];
        for(int i=0;i<numPlayers;i++){ players[i]=new Player(); }
    }
    public WorldState copy(){
        WorldState w = new WorldState(players.length);
        w.ball.x=ball.x; w.ball.y=ball.y; w.ball.vx=ball.vx; w.ball.vy=ball.vy;
        for(int i=0;i<players.length;i++){
            Player sp=players[i], dp=w.players[i];
            dp.x=sp.x; dp.y=sp.y; dp.vx=sp.vx; dp.vy=sp.vy; dp.team=sp.team; dp.shoot=sp.shoot;
        }
        w.score[0]=score[0]; w.score[1]=score[1];
        w.countdownMs = countdownMs;
        return w;
    }
}
