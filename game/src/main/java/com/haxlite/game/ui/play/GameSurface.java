package com.goalrift.football.game.ui.play;

import static com.goalrift.football.game.core.Constants.BALL_R;
import static com.goalrift.football.game.core.Constants.FIELD_H;
import static com.goalrift.football.game.core.Constants.FIELD_W;
import static com.goalrift.football.game.core.Constants.GOAL_W;
import static com.goalrift.football.game.core.Constants.KICKOFF_COUNTDOWN_MS;
import static com.goalrift.football.game.core.Constants.MATCH_DURATION_MS;
import static com.goalrift.football.game.core.Constants.PLAYER_R;
import static com.goalrift.football.game.core.Constants.PLAYER_SPEED;
import static com.goalrift.football.game.core.Constants.TICK_RATE;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import androidx.preference.PreferenceManager;

import com.goalrift.football.app.utils.ActiveItemManager;
import com.goalrift.football.app.utils.JetonManager;
import com.goalrift.football.backend.LocalServerConnector;
import com.goalrift.football.game.core.Physics;
import com.goalrift.football.game.core.WorldState;
import com.goalrift.football.game.net.ClientPrediction;
import com.goalrift.football.net.ClientConnector;
import com.goalrift.football.net.dto.InputMessage;
import com.goalrift.football.net.dto.Snapshot;


/**
 * Oyun yüzeyi:
 * - 60 Hz fixed timestep (prediction) + ~30 Hz snapshot
 * - Pause butonu (sol üst), modal (Devam Et / Ana Menü)
 * - Skor + süre HUD (üst-orta)
 * - Kickoff 3sn geri sayım + "GO!" flaşı
 * - Maç bitişinde sonuç modalı (Yeniden Oyna / Ana Menü)
 * - Ayarlar entegrasyonu: hassasiyet, solak düzen, grafik kalitesi, titreşim
 */
public class GameSurface extends SurfaceView implements SurfaceHolder.Callback, Runnable, ClientConnector.Listener {

    // --- Loop/Rendering ---
    private Thread thread;
    private volatile boolean running;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float scale = 12f, ox = 50, oy = 50;
    private float uiMargin = 40f;
    private float dp;

    // --- Game state / net ---
    private final WorldState local = new WorldState(2);
    private final ClientPrediction prediction = new ClientPrediction();
    private ClientConnector connector;
    private LocalServerConnector simRef;

    // --- Timers ---
    private int countdownMs = KICKOFF_COUNTDOWN_MS;
    private int matchRemainingMs = MATCH_DURATION_MS;
    private long goFlashUntilMs = 0L; // GO! flaşı bitiş zamanı

    // --- Input / Controls ---
    private final Joystick stick = new Joystick();
    private boolean shootPressed = false;
    private boolean prevShootPressed = false;
    private int shotsTaken = 0;
    private float shootBtnX, shootBtnY, shootBtnR;

    // --- Pause UI ---
    private boolean isPaused = false;
    private float pauseBtnX, pauseBtnY, pauseBtnR;
    private final RectF modalRect = new RectF();
    private final RectF btnResume = new RectF();
    private final RectF btnMenu = new RectF();

    // --- Result UI ---
    private boolean showResult = false;
    private final RectF resultRect = new RectF();
    private final RectF btnReplay = new RectF();
    private final RectF btnMenu2 = new RectF();

    // --- Settings / Prefs ---
    private SharedPreferences prefs;
    private float prefSensitivity = 1.0f;    // 0.5x..2.0x
    private boolean prefLeftHanded = false;  // joystick sağda?
    private String prefGfx = "medium";
    private boolean prefVibration = true;

    // --- Nicknames ---
    private String[] playerNicks;
    private Paint nickPaint;

    private String activeForma;
    private String activeTop;
    private String activeArkaPlan;


    // Bot isimleri listesi
    private final String[] botNames = {
            "Bot Ronaldo", "Bot Messi", "Bot Neymar",
            "Bot Mbappe", "Bot Salah", "Bot Haaland"
    };

    // --- Kozmetik sistemi ---
    private ActiveItemManager activeItemManager;
    private String topKey;
    private String formaKey;
    private String fieldKey;








    public GameSurface(Context ctx){
        super(ctx);
        getHolder().addCallback(this);
        setFocusable(true);

        dp = getResources().getDisplayMetrics().density;
        shootBtnR = 110f * dp;

        Physics.initKickoff(local);

        // local authoritative server-sim
        simRef = new LocalServerConnector(2);
        connector = simRef;
        connector.connect(this);

        // prefs
        prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        loadPrefs();
        prefs.registerOnSharedPreferenceChangeListener(prefListener);

        SharedPreferences sp = ctx.getSharedPreferences("HaxPrefs", Context.MODE_PRIVATE);
        String myNick = sp.getString("nickname", "Player");

// Oyuncu sayısına göre nickleri doldur
        playerNicks = new String[local.players.length];
        playerNicks[0] = myNick; // senin nick

        for (int i = 1; i < playerNicks.length; i++) {
            int index = (int)(Math.random() * botNames.length);
            playerNicks[i] = botNames[index];
        }

// Nick yazı ayarı
        nickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        nickPaint.setColor(0xFFFFFFFF);
        nickPaint.setTextSize(14f * dp);
        nickPaint.setTextAlign(Paint.Align.CENTER);

        // Aktif kozmetikleri yükle
        activeItemManager = new ActiveItemManager(ctx);
        topKey = activeItemManager.getActive("top");
        formaKey = activeItemManager.getActive("forma");
        fieldKey = activeItemManager.getActive("arka_plan");


    }


    // ---------------- Surface lifecycle ----------------

    @Override public void surfaceCreated(SurfaceHolder holder){
        running = true;
        thread = new Thread(this, "GameLoop");
        thread.start();
    }

    @Override public void surfaceDestroyed(SurfaceHolder holder){
        running = false;
        try { if (thread != null) thread.join(); } catch (InterruptedException ignored) {}
    }

    @Override public void surfaceChanged(SurfaceHolder holder, int format, int w, int h){
        float scaleX = (w - 2 * uiMargin) / FIELD_W;
        float scaleY = (h - 2 * uiMargin) / FIELD_H;
        scale = Math.min(scaleX, scaleY);
        ox = (w - FIELD_W * scale) * 0.5f;
        oy = (h - FIELD_H * scale) * 0.5f;

        applyControlLayout(w, h);

        // pause butonu
        pauseBtnR = 36f * dp;
        pauseBtnX = uiMargin + pauseBtnR + 8f * dp;
        pauseBtnY = uiMargin + pauseBtnR + 8f * dp;

        // pause modal
        float mw = Math.min(w * 0.56f, 520f * dp);
        float mh = 220f * dp;
        modalRect.set(w/2f - mw/2f, h/2f - mh/2f, w/2f + mw/2f, h/2f + mh/2f);
        float pad = 18f * dp;
        float bw = (mw - pad * 3f) / 2f;
        float bh = 60f * dp;
        float by = modalRect.bottom - pad - bh;
        btnResume.set(modalRect.left + pad, by, modalRect.left + pad + bw, by + bh);
        btnMenu.set(modalRect.right - pad - bw, by, modalRect.right - pad, by + bh);

        // result modal
        float rmw = Math.min(w * 0.62f, 560f * dp);
        float rmh = 280f * dp;
        resultRect.set(w/2f - rmw/2f, h/2f - rmh/2f, w/2f + rmw/2f, h/2f + rmh/2f);
        float rpad = 18f * dp;
        float rbw = (rmw - rpad * 3f) / 2f;
        float rbh = 60f * dp;
        float rby = resultRect.bottom - rpad - rbh;
        btnReplay.set(resultRect.left + rpad, rby, resultRect.left + rpad + rbw, rby + rbh);
        btnMenu2.set(resultRect.right - rpad - rbw, rby, resultRect.right - rpad, rby + rbh);
    }

    @Override protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (prefs != null) prefs.unregisterOnSharedPreferenceChangeListener(prefListener);
    }

    // ---------------- Game loop ----------------

    @Override public void run(){
        long nsPerTick = (long)(1_000_000_000L / TICK_RATE);
        long last = System.nanoTime();
        long acc = 0L;

        while (running){
            long now = System.nanoTime();
            acc += now - last; last = now;

            while (acc >= nsPerTick){
                boolean canPlay = !isPaused && !showResult && (countdownMs <= 0) && (matchRemainingMs > 0);

                if (canPlay) {
                    float mx = stick.ax() * PLAYER_SPEED * prefSensitivity;
                    float my = stick.ay() * PLAYER_SPEED * prefSensitivity;

                    // shoot rising edge → şut say
                    boolean shoot = shootPressed;
                    if (shoot && !prevShootPressed) shotsTaken++;
                    prevShootPressed = shoot;

                    InputMessage im = prediction.nextInput(mx, my, shoot);
                    connector.sendInput(im);

                    // local prediction
                    local.players[0].vx = mx; local.players[0].vy = my; local.players[0].shoot = shoot;
                    Physics.step(local);
                }
                acc -= nsPerTick;
            }

            Canvas c = getHolder().lockCanvas();
            if (c != null){
                drawFrame(c);
                getHolder().unlockCanvasAndPost(c);
            }
        }
    }

    // ---------------- Rendering ----------------

    private void drawFrame(Canvas c) {
        if (c == null) return;

        // --- Aktif kozmetikleri her karede güncelle (anlık değişimleri yakalar) ---
        if (activeItemManager == null) {
            activeItemManager = new ActiveItemManager(getContext());
        }
        activeForma = activeItemManager.getActive("forma");
        activeTop = activeItemManager.getActive("top");
        activeArkaPlan = activeItemManager.getActive("arka_plan");

        // --- Arka plan (saha) ---
        if ("field_blue".equals(activeArkaPlan)) {
            c.drawColor(0xFF0B6FAF); // Mavi saha
        } else if ("field_dark".equals(activeArkaPlan)) {
            c.drawColor(0xFF083B18); // Koyu yeşil saha
        } else {
            c.drawColor(0xFF0B8F3A); // Varsayılan yeşil saha
        }

        // --- Grafik kalitesine göre çizgi kalınlığı ---
        float stroke = 4f;
        if ("low".equals(prefGfx)) stroke = 3f;
        else if ("high".equals(prefGfx)) stroke = 5.5f;

        // --- Saha çizgileri ---
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(stroke);
        p.setColor(0xFFFFFFFF);
        rect(c, 0, 0, FIELD_W, FIELD_H);
        line(c, FIELD_W / 2, 0, FIELD_W / 2, FIELD_H);
        circle(c, FIELD_W / 2, FIELD_H / 2, 10);

        // --- Kaleler ---
        p.setColor(0xFFFFC107);
        line(c, 0, FIELD_H / 2 - 6, -GOAL_W, FIELD_H / 2 - 6);
        line(c, -GOAL_W, FIELD_H / 2 - 6, -GOAL_W, FIELD_H / 2 + 6);
        line(c, -GOAL_W, FIELD_H / 2 + 6, 0, FIELD_H / 2 + 6);
        line(c, FIELD_W, FIELD_H / 2 - 6, FIELD_W + GOAL_W, FIELD_H / 2 - 6);
        line(c, FIELD_W + GOAL_W, FIELD_H / 2 - 6, FIELD_W + GOAL_W, FIELD_H / 2 + 6);
        line(c, FIELD_W, FIELD_H / 2 + 6, FIELD_W + GOAL_W, FIELD_H / 2 + 6);

        // --- Top ---
        p.setStyle(Paint.Style.FILL);
        if ("gold_top".equals(activeTop)) {
            p.setColor(0xFFFFD700); // Altın top
        } else if ("black_top".equals(activeTop)) {
            p.setColor(0xFF000000); // Siyah top
        } else {
            p.setColor(0xFFFFFFFF); // Beyaz top (varsayılan)
        }
        circle(c, local.ball.x, local.ball.y, BALL_R);

        // --- Oyuncular ---
        for (int i = 0; i < local.players.length; i++) {
            int col;

            if (local.players[i].team == 0) {
                // Oyuncunun kendi takımı (senin takım)
                if ("gold_forma".equals(activeForma)) {
                    col = 0xFFFFC107; // Altın forma
                } else if ("forma_blue".equals(activeForma)) {
                    col = 0xFF2196F3; // Mavi forma
                } else if ("forma_red".equals(activeForma)) {
                    col = 0xFFE53935; // Kırmızı forma
                } else {
                    col = 0xFF4CAF50; // Yeşil varsayılan forma
                }
            } else {
                // Rakip takım
                col = 0xFFE91E63;
            }

            p.setColor(col);
            circle(c, local.players[i].x, local.players[i].y, PLAYER_R);

            // Oyuncu ismini çiz (mevcut sistemde playerNicks varsa)
            if (playerNicks != null && i < playerNicks.length) {
                p.setTextSize(12f * dp);
                p.setColor(0xFFFFFFFF);
                c.drawText(playerNicks[i],
                        ox + local.players[i].x * scale,
                        oy + local.players[i].y * scale - PLAYER_R * scale - 8f * dp,
                        p);
            }
        }

        // --- HUD ---
        drawScoreHud(c);

        // --- Kontroller ---
        drawShootControl(c);
        drawPauseButton(c);

        // --- Kickoff geri sayım overlay ---
        if (countdownMs > 0) {
            p.setColor(0x88000000);
            p.setStyle(Paint.Style.FILL);
            c.drawRect(0, 0, getWidth(), getHeight(), p);

            int sec = (int) Math.ceil(countdownMs / 1000.0);
            p.setColor(0xFFFFFFFF);
            p.setTextSize(36f * dp);
            String s = String.valueOf(sec);
            float tw = p.measureText(s);
            c.drawText(s, getWidth() / 2f - tw / 2f, getHeight() / 2f + 12f * dp, p);
        }

        // --- GO! flaşı ---
        if (goFlashUntilMs > SystemClock.uptimeMillis()) {
            p.setColor(0x22FFFFFF);
            p.setStyle(Paint.Style.FILL);
            c.drawRect(0, 0, getWidth(), getHeight(), p);
            p.setColor(0xFFFFFFFF);
            p.setTextSize(28f * dp);
            String s = "GO!";
            float tw = p.measureText(s);
            c.drawText(s, getWidth() / 2f - tw / 2f, getHeight() / 2f, p);
        }

        // --- Pause modal ---
        if (isPaused) drawPauseModal(c);

        // --- Sonuç modal ---
        if (showResult) drawResultModal(c);
    }


    private void drawShootControl(Canvas c){
        // joystick kendi çizimini yapıyor
        stick.draw(c, p);

        // buton dairesi
        p.setStyle(Paint.Style.FILL);
        p.setColor(shootPressed ? 0xAAFF4444 : 0x66FFFFFF);
        c.drawCircle(shootBtnX, shootBtnY, shootBtnR, p);

        // metin ortalı ve sığdırılmış
        p.setColor(0xFFFFFFFF);
        p.setTextAlign(Paint.Align.CENTER);
        String shootLabel = "SHOOT";
        float maxWidth = shootBtnR * 1.6f;
        float ts = 22f * dp;
        p.setTextSize(ts);
        float wLabel = p.measureText(shootLabel);
        if (wLabel > maxWidth) { ts *= (maxWidth / wLabel); p.setTextSize(ts); }
        Paint.FontMetrics fm = p.getFontMetrics();
        float baseline = shootBtnY - (fm.ascent + fm.descent) / 2f;
        c.drawText(shootLabel, shootBtnX, baseline, p);
        p.setTextAlign(Paint.Align.LEFT);
    }

    private void drawScoreHud(Canvas c){
        String timeTxt = formatTime(matchRemainingMs);
        String scoreTxt = local.score[0] + " - " + local.score[1];

        float scoreTs = 22f * dp;
        float timeTs  = 16f * dp;

        p.setTextSize(scoreTs);
        float scoreW = p.measureText(scoreTxt);
        p.setTextSize(timeTs);
        float timeW = p.measureText(timeTxt);

        float contentW = Math.max(scoreW, timeW) + 32f * dp;
        float cx = getWidth() / 2f;
        float top = uiMargin;
        RectF hud = new RectF(cx - contentW/2f, top, cx + contentW/2f, top + 64f * dp);

        // gölge
        p.setStyle(Paint.Style.FILL);
        p.setColor(0x55000000);
        c.drawRoundRect(hud.left + 3f*dp, hud.top + 4f*dp, hud.right + 3f*dp, hud.bottom + 4f*dp, 16f*dp, 16f*dp, p);

        // panel
        p.setColor(0xCC1B5E20);
        c.drawRoundRect(hud, 16f*dp, 16f*dp, p);

        // skor
        p.setColor(0xFFFFFFFF);
        p.setTextSize(scoreTs);
        c.drawText(scoreTxt, cx - p.measureText(scoreTxt)/2f, hud.top + 26f*dp, p);

        // saat
        p.setTextSize(timeTs);
        c.drawText(timeTxt, cx - p.measureText(timeTxt)/2f, hud.top + 26f*dp + 20f*dp, p);
    }

    private void drawPauseButton(Canvas c){
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xAA000000);
        c.drawCircle(pauseBtnX + 3f*dp, pauseBtnY + 3f*dp, pauseBtnR, p);
        p.setColor(0xFFFFFFFF);
        c.drawCircle(pauseBtnX, pauseBtnY, pauseBtnR, p);

        // "||" simgesi
        p.setColor(0xFF333333);
        float barW = 4.5f * dp, barH = pauseBtnR * 0.9f;
        c.drawRect(pauseBtnX - 8f*dp - barW, pauseBtnY - barH/2f, pauseBtnX - 8f*dp + barW, pauseBtnY + barH/2f, p);
        c.drawRect(pauseBtnX + 8f*dp - barW, pauseBtnY - barH/2f, pauseBtnX + 8f*dp + barW, pauseBtnY + barH/2f, p);
    }

    private void drawPauseModal(Canvas c){
        // karartma
        p.setColor(0x99000000);
        p.setStyle(Paint.Style.FILL);
        c.drawRect(0,0,getWidth(),getHeight(),p);

        // kart + gölge
        p.setColor(0x55000000);
        c.drawRoundRect(modalRect.left+6f*dp, modalRect.top+8f*dp, modalRect.right+6f*dp, modalRect.bottom+8f*dp, 24f*dp, 24f*dp, p);
        p.setColor(0xFFF5F5F5);
        c.drawRoundRect(modalRect, 24f*dp, 24f*dp, p);

        // başlık
        p.setColor(0xFF212121);
        p.setTextSize(18f*dp);
        String t = "Oyun Durdu";
        float tw = p.measureText(t);
        c.drawText(t, modalRect.centerX() - tw/2f, modalRect.top + 32f*dp, p);

        // butonlar
        drawButton(c, btnResume, 0xFF1E88E5, "Devam Et");
        drawButton(c, btnMenu,   0xFFE53935, "Ana Menü");
    }

    private void drawResultModal(Canvas c){
        // karartma
        p.setColor(0x99000000);
        p.setStyle(Paint.Style.FILL);
        c.drawRect(0,0,getWidth(),getHeight(),p);

        // kart
        p.setColor(0x55000000);
        c.drawRoundRect(resultRect.left+6f*dp, resultRect.top+8f*dp, resultRect.right+6f*dp, resultRect.bottom+8f*dp, 24f*dp, 24f*dp, p);
        p.setColor(0xFFFFFFFF);
        c.drawRoundRect(resultRect, 24f*dp, 24f*dp, p);

        // başlık
        int me = local.score[0], opp = local.score[1];
        String title = (me>opp) ? "Kazandın!" : (me<opp) ? "Kaybettin" : "Berabere";
        p.setColor(0xFF212121);
        p.setTextSize(20f*dp);
        float tw = p.measureText(title);
        c.drawText(title, resultRect.centerX() - tw/2f, resultRect.top + 32f*dp, p);

        // istatistik
        String line1 = "Skor: " + me + " - " + opp;
        String line2 = "Şut: " + shotsTaken;
        p.setTextSize(14f*dp);
        float l1w = p.measureText(line1);
        float l2w = p.measureText(line2);
        c.drawText(line1, resultRect.centerX() - l1w/2f, resultRect.top + 32f*dp + 28f*dp, p);
        c.drawText(line2, resultRect.centerX() - l2w/2f, resultRect.top + 32f*dp + 28f*dp + 22f*dp, p);

        // butonlar
        drawButton(c, btnReplay, 0xFF1E88E5, "Yeniden Oyna");
        drawButton(c, btnMenu2,  0xFFE53935, "Ana Menü");
    }

    private void drawButton(Canvas c, RectF r, int color, String label){
        p.setColor(0x33000000);
        c.drawRoundRect(r.left+4f*dp, r.top+5f*dp, r.right+4f*dp, r.bottom+5f*dp, 20f*dp, 20f*dp, p);
        p.setColor(color);
        c.drawRoundRect(r, 20f*dp, 20f*dp, p);
        p.setColor(0xFFFFFFFF);
        p.setTextSize(12f*dp);
        float tw = p.measureText(label);
        c.drawText(label, r.centerX() - tw/2f, r.centerY() + 4f*dp, p);
    }

    // ---------------- Input ----------------

    @Override public boolean onTouchEvent(MotionEvent e){
        int action = e.getActionMasked();
        float x = e.getX(), y = e.getY();

        // Sonuç modal açıksa
        if (showResult){
            if (action == MotionEvent.ACTION_UP){
                if (btnReplay.contains(x,y)){
                    Activity a = (Activity)getContext();
                    a.recreate(); // yeniden başlat
                    return true;
                }
                if (btnMenu2.contains(x,y)){
                    Activity a = (Activity)getContext();
                    a.finish();
                    return true;
                }
            }
            return true; // modal açıkken diğer inputları yut
        }

        // Pause modal açıksa
        if (isPaused){
            if (action == MotionEvent.ACTION_UP){
                if (btnResume.contains(x,y)){
                    isPaused = false;
                    if (simRef != null) simRef.setPaused(false);
                    return true;
                }
                if (btnMenu.contains(x,y)){
                    Activity a = (Activity)getContext();
                    a.finish();
                    return true;
                }
            }
            return true;
        }

        // Pause butonu — DOWN veya UP’ta tetikle (kaçmasın)
        float dx = x - pauseBtnX, dy = y - pauseBtnY;
        boolean onPause = (dx*dx + dy*dy) <= (pauseBtnR*pauseBtnR);
        if (onPause && (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_UP)){
            isPaused = true;
            if (simRef != null) simRef.setPaused(true);
            if (prefVibration) performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);
            return true;
        }

        // Normal oyun inputları
        boolean handled = stick.onTouch(e);

        if (action==MotionEvent.ACTION_DOWN || action==MotionEvent.ACTION_POINTER_DOWN || action==MotionEvent.ACTION_MOVE){
            shootPressed = false;
            for (int i=0;i<e.getPointerCount();i++){
                float ddx = e.getX(i) - shootBtnX, ddy = e.getY(i) - shootBtnY;
                float d = (float)Math.sqrt(ddx*ddx + ddy*ddy);
                if (d < shootBtnR){ shootPressed = true; handled = true; break; }
            }
        }
        return handled || super.onTouchEvent(e);
    }

    // ---------------- Net callbacks ----------------

    @Override public void onConnected(){}

    @Override public void onSnapshot(Snapshot s){
        // authoritative state
        local.ball.x=s.ballX; local.ball.y=s.ballY; local.ball.vx=s.ballVX; local.ball.vy=s.ballVY;
        int n = Math.min(local.players.length, s.px.length);
        for (int i=0;i<n;i++){
            local.players[i].x=s.px[i]; local.players[i].y=s.py[i];
            local.players[i].vx=s.pvx[i]; local.players[i].vy=s.pvy[i];
        }
        local.score[0]=s.scores[0]; local.score[1]=s.scores[1];

        // GO! flaşı tetikle
        if (countdownMs > 0 && s.countdownMs <= 0) {
            goFlashUntilMs = SystemClock.uptimeMillis() + 600L; // 0.6 sn
        }

        countdownMs = s.countdownMs;
        matchRemainingMs = s.matchRemainingMs;

        // süre bitti → sonuç modal
        if (!showResult && countdownMs <= 0 && matchRemainingMs <= 0){
            showResult = true;
            isPaused = false;
            if (simRef != null) simRef.setPaused(true);

            // --- Jeton ödülü ---
            int myScore = local.score[0];
            int oppScore = local.score[1];

            JetonManager jetonManager = new JetonManager(getContext());
            int reward;

            if (myScore > oppScore) {
                reward = 100; // Galibiyet
            } else if (myScore < oppScore) {
                reward = 70;  // Mağlubiyet
            } else {
                reward = 50;  // Beraberlik
            }

            jetonManager.addJeton(reward);
            Toast.makeText(getContext(), reward + " Jeton kazandın!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override public void onDisconnected(String reason){}

    // ---------------- Helpers ----------------

    private String formatTime(int ms){
        if (ms < 0) ms = 0;
        int totalSec = ms / 1000;
        int min = totalSec / 60;
        int sec = totalSec % 60;
        return String.format("%d:%02d", min, sec);
    }

    private void circle(Canvas c, float x, float y, float r){
        c.drawCircle(ox + x*scale, oy + y*scale, r*scale, p);
    }
    private void rect(Canvas c, float x, float y, float w, float h){
        float l=ox + x*scale, t=oy + y*scale, r=l + w*scale, b=t + h*scale;
        c.drawRect(l, t, r, b, p);
    }
    private void line(Canvas c, float x1, float y1, float x2, float y2){
        c.drawLine(ox + x1*scale, oy + y1*scale, ox + x2*scale, oy + y2*scale, p);
    }

    // --- Preferences ---

    private final SharedPreferences.OnSharedPreferenceChangeListener prefListener =
            (sp, key) -> {
                loadPrefs();
                applyControlLayout(getWidth(), getHeight());
            };

    private void loadPrefs(){
        int iv = prefs.getInt("joystick_sensitivity", 100);
        prefSensitivity = Math.max(0.5f, Math.min(2.0f, iv / 100f));
        prefLeftHanded = prefs.getBoolean("left_handed", false);
        prefGfx = prefs.getString("graphics_quality", "medium");
        prefVibration = prefs.getBoolean("vibration_enabled", true);
    }

    private void applyControlLayout(int w, int h){
        if (w <= 0 || h <= 0) return;
        if (!prefLeftHanded){
            // klasik: joystick sol, şut sağ
            stick.layout(uiMargin + 160f, h - uiMargin - 160f);
            shootBtnX = w - uiMargin - 160f;
            shootBtnY = h - uiMargin - 160f;
        } else {
            // solak: joystick sağ, şut sol
            stick.layout(w - uiMargin - 160f, h - uiMargin - 160f);
            shootBtnX = uiMargin + 160f;
            shootBtnY = h - uiMargin - 160f;
        }
    }
}
