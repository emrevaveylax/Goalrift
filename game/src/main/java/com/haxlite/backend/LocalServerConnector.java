package com.goalrift.football.backend;

import static com.goalrift.football.game.core.Constants.MATCH_DURATION_MS;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import com.goalrift.football.game.core.Physics;
import com.goalrift.football.game.core.Player;
import com.goalrift.football.game.core.WorldState;
import com.goalrift.football.net.ClientConnector;
import com.goalrift.football.net.dto.InputMessage;
import com.goalrift.football.net.dto.Snapshot;

import java.util.ArrayDeque;
import java.util.Queue;

public class LocalServerConnector implements ClientConnector, Runnable {
    private Listener listener;

    // Simülasyon ayrı thread
    private HandlerThread simThread;
    private Handler simHandler;

    // Callback’ler UI thread’e
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final WorldState serverWorld;
    private volatile boolean running = false;
    private volatile boolean paused = false;

    private int lastSeqProcessed = 0;
    private final Queue<InputMessage> inputs = new ArrayDeque<>();

    // Maç saati
    private int matchRemainingMs = MATCH_DURATION_MS;
    private long lastTickTimeMs = 0L;

    public LocalServerConnector(int numPlayers){
        serverWorld = new WorldState(numPlayers);
        Physics.initKickoff(serverWorld); // countdown burada başlar
    }

    // ====== yeni: oyunu durdur/başlat ======
    public void setPaused(boolean p){
        paused = p;
    }

    @Override public void connect(Listener listener){
        this.listener = listener;
        simThread = new HandlerThread("ServerSim");
        simThread.start();
        simHandler = new Handler(simThread.getLooper());
        running = true;
        simHandler.post(this);
        if(listener!=null) mainHandler.post(listener::onConnected);
    }

    @Override public void sendInput(InputMessage msg){
        synchronized (inputs) { inputs.offer(msg); }
    }

    @Override public void run(){
        if(!running) return;

        long now = System.currentTimeMillis();
        if (lastTickTimeMs == 0L) lastTickTimeMs = now;
        long elapsed = now - lastTickTimeMs;
        lastTickTimeMs = now;

        // Zaman akışı: yalnızca PAUSE kapalı ve countdown bitmişse
        if (!paused && serverWorld.countdownMs <= 0) {
            matchRemainingMs = Math.max(0, matchRemainingMs - (int)elapsed);
        }

        // Maç bittiyse: hızlar sıfır, snapshot yolla
        if (matchRemainingMs <= 0){
            for (Player p : serverWorld.players) { p.vx=0; p.vy=0; p.shoot=false; }
            serverWorld.ball.vx=0; serverWorld.ball.vy=0;
            emitSnapshot();
            simHandler.postDelayed(this, 33);
            return;
        }

        if (!paused){
            // input’ları uygula
            InputMessage im;
            synchronized (inputs) {
                while((im=inputs.poll())!=null){
                    serverWorld.players[0].vx = im.moveX;
                    serverWorld.players[0].vy = im.moveY;
                    serverWorld.players[0].shoot = im.shoot;
                    lastSeqProcessed = im.seq;
                }
            }

            // bot AI (countdown bittiyse)
            Player bot = serverWorld.players.length>1 ? serverWorld.players[1] : null;
            if (bot != null && serverWorld.countdownMs <= 0) {
                float dx = serverWorld.ball.x - bot.x;
                float dy = serverWorld.ball.y - bot.y;
                float len = (float)Math.sqrt(dx*dx+dy*dy) + 1e-5f;
                float speed = 28f;
                bot.vx = dx/len*speed;
                bot.vy = dy/len*speed;
                bot.shoot = len < 5f;
            } else if (bot != null) {
                bot.vx = bot.vy = 0; bot.shoot = false;
            }

            // 60Hz sim (2 adım) — countdown aktifse Physics.step erken döner
            Physics.step(serverWorld);
            Physics.step(serverWorld);
        } else {
            // paused iken hızları yavaşça sıfıra çekmek istemiyorsak dokunma
            // sadece snapshot at
        }

        emitSnapshot();
        simHandler.postDelayed(this, 33); // ~30Hz net tick
    }

    private void emitSnapshot(){
        Snapshot s = new Snapshot();
        s.lastProcessedInputSeq = lastSeqProcessed;
        s.ballX=serverWorld.ball.x; s.ballY=serverWorld.ball.y;
        s.ballVX=serverWorld.ball.vx; s.ballVY=serverWorld.ball.vy;

        int n = serverWorld.players.length;
        s.px = new float[n]; s.py = new float[n]; s.pvx=new float[n]; s.pvy=new float[n];
        for(int i=0;i<n;i++){
            s.px[i]=serverWorld.players[i].x;
            s.py[i]=serverWorld.players[i].y;
            s.pvx[i]=serverWorld.players[i].vx;
            s.pvy[i]=serverWorld.players[i].vy;
        }
        s.scores = new int[]{ serverWorld.score[0], serverWorld.score[1] };
        s.serverTime = System.currentTimeMillis();
        s.countdownMs = serverWorld.countdownMs;
        s.matchRemainingMs = matchRemainingMs;

        if(listener!=null) {
            mainHandler.post(() -> listener.onSnapshot(s));
        }
    }
}
