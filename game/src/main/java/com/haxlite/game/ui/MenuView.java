package com.goalrift.football.game.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Build;
import android.util.TypedValue;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.goalrift.football.analytics.Analytics;
import com.goalrift.football.game.ui.play.GameActivity;

import java.util.ArrayList;
import java.util.List;

import com.goalrift.football.game.ui.store.StoreActivity;


/** Ana menü (arka plan cache + ayarlar dişlisi) */
public class MenuView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint small = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<MenuButton> buttons = new ArrayList<>();

    private float w, h, dp;
    private float panelRadius;
    private float buttonHeight;
    private float buttonGap;
    private float sidePadding;
    private float topPadding;
    private float titleSize;
    private float subtitleSize;

    // dekor
    private float t = 0f;
    private final RectF tmpRect = new RectF();

    // arka plan cache
    private Bitmap bgCache;
    private final Rect srcRect = new Rect();
    private final Rect dstRect = new Rect();

    // AYARLAR dişlisi
    private final RectF gearRect = new RectF();
    private float gearSize;

    public MenuView(Context c){
        super(c);
        setFocusable(true);
        setClickable(true);
        setHapticFeedbackEnabled(true);

        dp = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics());
        panelRadius = 20 * dp;
        buttonHeight = 78 * dp;
        buttonGap = 14 * dp;
        sidePadding = 24 * dp;
        topPadding = 28 * dp;
        titleSize = 48 * dp;
        subtitleSize = 16 * dp;

        text.setColor(0xFFFFFFFF);
        text.setTextSize(titleSize);
        text.setShadowLayer(8*dp, 0, 3*dp, 0x88000000);

        small.setColor(0xDDFFFFFF);
        small.setTextSize(subtitleSize);

        setBackgroundColor(0xFF0A6E30);
    }

    private static class MenuButton {
        RectF frame = new RectF();
        String title, subtitle;
        int bgColor;
        Runnable action;
        boolean pressed = false;
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldw, int oldh){
        super.onSizeChanged(width, height, oldw, oldh);
        w = width; h = height;

        buttons.clear();

        float columnGap = 20 * dp;
        float usableW = w - sidePadding*2 - columnGap;
        float leftW = usableW * 0.58f;
        float rightW = usableW - leftW;

        float leftX = sidePadding;
        float rightX = sidePadding + leftW + columnGap;

        float baseY = h * 0.35f;

        MenuButton quick = new MenuButton();
        quick.title = "Hızlı Maç";
        quick.subtitle = "Offline 1v1 • Bot’a karşı";
        quick.bgColor = 0xFF1E88E5;
        quick.frame.set(leftX, baseY, leftX + leftW, baseY + buttonHeight*1.3f);
        quick.action = () -> {
            Analytics.event("menu_quick_match");
            getContext().startActivity(new Intent(getContext(), GameActivity.class));
        };
        buttons.add(quick);

        MenuButton ranked = new MenuButton();
        ranked.title = "Dereceli";
        ranked.subtitle = "MMR • Ligler • Sezon";
        ranked.bgColor = 0xFFF4511E;
        ranked.frame.set(rightX, baseY, rightX + rightW, baseY + buttonHeight);
        ranked.action = () -> Toast.makeText(getContext(), "Yakında: Dereceli", Toast.LENGTH_SHORT).show();
        buttons.add(ranked);

        MenuButton priv = new MenuButton();
        priv.title = "Özel Oda";
        priv.subtitle = "Oda kodu / Davet linki";
        priv.bgColor = 0xFF43A047;
        priv.frame.set(rightX, ranked.frame.bottom + buttonGap, rightX + rightW, ranked.frame.bottom + buttonGap + buttonHeight);
        priv.action = () -> Toast.makeText(getContext(), "Yakında: Özel Oda", Toast.LENGTH_SHORT).show();
        buttons.add(priv);

        MenuButton store = new MenuButton();
        store.title = "Mağaza";
        store.subtitle = "Kozmetik • Top • Forma";
        store.bgColor = 0xFF6D4C41;
        store.frame.set(rightX, priv.frame.bottom + buttonGap, rightX + rightW, priv.frame.bottom + buttonGap + buttonHeight);
        store.action = () -> {
            Context ctx = getContext();
            Intent intent = new Intent(ctx, StoreActivity.class);
            ctx.startActivity(intent);
        };

        buttons.add(store);

        // dişli konumu — sağ üst, ekran içinde garanti
        gearSize = 48 * dp;
        float gy = Math.max(topPadding, 12*dp);
        gearRect.set(w - sidePadding - gearSize, gy, w - sidePadding, gy + gearSize);

        buildBackgroundCache();
    }

    @Override
    protected void onDraw(Canvas c){
        super.onDraw(c);

        if (bgCache != null) {
            srcRect.set(0, 0, bgCache.getWidth(), bgCache.getHeight());
            dstRect.set(0, 0, (int) w, (int) h);
            c.drawBitmap(bgCache, srcRect, dstRect, null);
        } else {
            drawGrass(c); drawPitchLines(c); drawLights(c);
        }

        String title = "Goalrift";
        text.setTextSize(titleSize);
        float tw = text.measureText(title);
        float tx = sidePadding;
        float ty = topPadding + titleSize;
        c.drawText(title, tx, ty, text);
        small.setTextSize(subtitleSize);
        c.drawText("Arcade Futbol • 60 FPS • Otoriter Sunucu", tx, ty + 10*dp + subtitleSize, small);
        c.drawText("1v1 / 2v2 / 3v3 • Sezon & Kozmetik (yakında)", tx, ty + 10*dp + subtitleSize*2 + 6*dp, small);
        drawBallDecoration(c, tx + tw + 28*dp, ty - 10*dp);

        for (MenuButton b : buttons) drawButton(c, b);

        // dişliyi EN SON çiz (üstte kalsın)
        drawGear(c);

        t += 0.016f;
        postInvalidateOnAnimation();

        SharedPreferences prefs = getContext().getSharedPreferences("HaxPrefs", Context.MODE_PRIVATE);
        String nick = prefs.getString("nickname", "Player");

// mevcut kod: title ve küçük yazılar çiziliyor
        c.drawText("Hoşgeldin, " + nick, tx, ty + 10*dp + subtitleSize*3 + 12*dp, small);

    }

    private void buildBackgroundCache(){
        if (w <= 0 || h <= 0) return;
        if (bgCache != null) { bgCache.recycle(); bgCache = null; }
        bgCache = Bitmap.createBitmap((int) w, (int) h, Bitmap.Config.ARGB_8888);
        Canvas cc = new Canvas(bgCache);
        drawGrass(cc);
        drawPitchLines(cc);
        drawLights(cc);
    }

    // ---------- çizim yardımcıları ----------

    private void drawGrass(Canvas c){
        int top = 0xFF0E8C3C, mid = 0xFF0A7533, bot = 0xFF075F29;
        paint.setShader(new LinearGradient(0, 0, 0, h,
                new int[]{top, mid, bot}, new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h, paint);
        paint.setShader(null);

        paint.setStyle(Paint.Style.FILL);
        int rows = 10, cols = 18;
        float cw = w / cols, rh = h / rows;
        for(int r=0;r<rows;r++){
            for(int col=0;col<cols;col++){
                if(((r+col)&1)==0){
                    paint.setColor(0x1A000000);
                    c.drawRect(col*cw, r*rh, (col+1)*cw, (r+1)*rh, paint);
                }
            }
        }
        paint.setColor(0x11FFFFFF);
        for(int i=0;i<12;i++){
            float x0 = i*(w/12f);
            c.drawRect(x0, 0, x0 + w/24f, h, paint);
        }
    }

    private void drawPitchLines(Canvas c){
        paint.setStyle(Paint.Style.STROKE);
        float stroke = Math.max(w, h) / 180f;
        paint.setStrokeWidth(stroke);
        paint.setColor(0xEEFFFFFF);

        float margin = 36*dp;
        RectF field = new RectF(margin, margin*1.3f, w - margin, h - margin*1.05f);
        c.drawRoundRect(field, 22*dp, 22*dp, paint);

        c.drawLine(field.centerX(), field.top, field.centerX(), field.bottom, paint);
        c.drawCircle(field.centerX(), field.centerY(), Math.min(field.width(), field.height())*0.13f, paint);

        float goalGap = field.height()*0.20f;
        float gyTop = field.centerY() - goalGap/2f;
        float gyBot = field.centerY() + goalGap/2f;
        float goalDepth = 28*dp;

        c.drawLine(field.left, gyTop, field.left - goalDepth, gyTop, paint);
        c.drawLine(field.left - goalDepth, gyTop, field.left - goalDepth, gyBot, paint);
        c.drawLine(field.left - goalDepth, gyBot, field.left, gyBot, paint);
        c.drawLine(field.right, gyTop, field.right + goalDepth, gyTop, paint);
        c.drawLine(field.right + goalDepth, gyTop, field.right + goalDepth, gyBot, paint);
        c.drawLine(field.right, gyBot, field.right + goalDepth, gyBot, paint);

        float boxW = field.width()*0.14f, boxH = field.height()*0.42f;
        RectF leftBox = new RectF(field.left, field.centerY()-boxH/2f, field.left+boxW, field.centerY()+boxH/2f);
        RectF rightBox = new RectF(field.right-boxW, field.centerY()-boxH/2f, field.right, field.centerY()+boxH/2f);
        c.drawRect(leftBox, paint);
        c.drawRect(rightBox, paint);
    }

    private void drawLights(Canvas c){
        paint.setStyle(Paint.Style.FILL);
        RadialGradient rg;
        rg = new RadialGradient(0, 0, Math.max(w, h)*0.7f, new int[]{0x33FFFFFF, 0x00000000}, new float[]{0f, 1f}, Shader.TileMode.CLAMP);
        paint.setShader(rg); c.drawRect(0,0,w,h,paint);
        rg = new RadialGradient(w, 0, Math.max(w, h)*0.7f, new int[]{0x22FFFFFF, 0x00000000}, new float[]{0f, 1f}, Shader.TileMode.CLAMP);
        paint.setShader(rg); c.drawRect(0,0,w,h,paint);
        rg = new RadialGradient(0, h, Math.max(w, h)*0.7f, new int[]{0x11FFFFFF, 0x00000000}, new float[]{0f, 1f}, Shader.TileMode.CLAMP);
        paint.setShader(rg); c.drawRect(0,0,w,h,paint);
        rg = new RadialGradient(w, h, Math.max(w, h)*0.7f, new int[]{0x11FFFFFF, 0x00000000}, new float[]{0f, 1f}, Shader.TileMode.CLAMP);
        paint.setShader(rg); c.drawRect(0,0,w,h,paint);
        paint.setShader(null);
    }

    private void drawBallDecoration(Canvas c, float cx, float cy){
        float amp = 8*dp;
        float y = cy + (float)Math.sin(t*2.2f)*amp;
        float r = 10*dp;

        paint.setColor(0x66000000);
        c.drawCircle(cx + 3*dp, y + 3*dp, r, paint);

        paint.setColor(0xFFFFFFFF);
        c.drawCircle(cx, y, r, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(0xFF222222);
        paint.setStrokeWidth(1.6f*dp);
        c.drawCircle(cx, y, r, paint);

        Path pent = new Path();
        for(int i=0;i<5;i++){
            double ang = Math.toRadians(-90 + i*72);
            float px = cx + (float)Math.cos(ang)*r*0.55f;
            float py = y  + (float)Math.sin(ang)*r*0.55f;
            if(i==0) pent.moveTo(px, py); else pent.lineTo(px, py);
        }
        pent.close();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFF333333);
        c.drawPath(pent, paint);
    }

    private void drawButton(Canvas c, MenuButton b){
        paint.setStyle(Paint.Style.FILL);
        int bg = b.pressed ? darken(b.bgColor, 0.85f) : b.bgColor;
        paint.setColor(bg);
        c.drawRoundRect(b.frame, panelRadius, panelRadius, paint);

        paint.setColor(0x22FFFFFF);
        tmpRect.set(b.frame.left, b.frame.top, b.frame.right, b.frame.top + b.frame.height()*0.45f);
        c.drawRoundRect(tmpRect, panelRadius, panelRadius, paint);

        text.setColor(0xFFFFFFFF);
        text.setTextSize(24*dp);
        small.setColor(0xEEFFFFFF);
        small.setTextSize(14*dp);

        float tx = b.frame.left + 22*dp;
        float ty = b.frame.centerY() - 6*dp;
        c.drawText(b.title, tx, ty, text);
        c.drawText(b.subtitle, tx, ty + 20*dp, small);

        float r = Math.min(18*dp, b.frame.height()*0.28f);
        float cx = b.frame.right - 24*dp;
        float cy = b.frame.centerY();
        paint.setColor(0x33000000);
        c.drawCircle(cx+2*dp, cy+2*dp, r, paint);
        paint.setColor(0xFFFFFFFF);
        c.drawCircle(cx, cy, r, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(0xFF222222);
        paint.setStrokeWidth(1.4f*dp);
        c.drawCircle(cx, cy, r, paint);
    }

    private void drawGear(Canvas c){
        // gölge + arka daire
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xAA000000);
        c.drawCircle(gearRect.centerX()+3*dp, gearRect.centerY()+3*dp, gearRect.width()/2f, paint);
        paint.setColor(0xFFFFFFFF);
        c.drawCircle(gearRect.centerX(), gearRect.centerY(), gearRect.width()/2f, paint);

        // dişli kolları
        paint.setColor(0xFF333333);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.2f*dp);
        float r = gearRect.width()*0.26f;
        for(int i=0;i<8;i++){
            double a = Math.toRadians(i*45);
            float x1 = gearRect.centerX() + (float)Math.cos(a)*(r+6*dp);
            float y1 = gearRect.centerY() + (float)Math.sin(a)*(r+6*dp);
            float x2 = gearRect.centerX() + (float)Math.cos(a)*(r+12*dp);
            float y2 = gearRect.centerY() + (float)Math.sin(a)*(r+12*dp);
            c.drawLine(x1,y1,x2,y2,paint);
        }
        paint.setStyle(Paint.Style.FILL);
        c.drawCircle(gearRect.centerX(), gearRect.centerY(), r, paint);
    }

    private int darken(int color, float factor){
        int a = (color >>> 24) & 0xFF;
        int r = (color >>> 16) & 0xFF; // <-- 0xFF olmalıydı
        int g = (color >>> 8)  & 0xFF;
        int b =  color         & 0xFF;

        r = Math.max(0, Math.min(255, (int)(r * factor)));
        g = Math.max(0, Math.min(255, (int)(g * factor)));
        b = Math.max(0, Math.min(255, (int)(b * factor)));

        return (a << 24) | (r << 16) | (g << 8) | b;
    }


    @Override
    public boolean onTouchEvent(MotionEvent e){
        int action = e.getActionMasked();

        // Önce dişli: DOWN/UP ikisinde de yakala
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_UP){
            if (gearRect.contains(e.getX(), e.getY())){
                if (action == MotionEvent.ACTION_UP){
                    try {
                        Intent i = new Intent();
                        i.setClassName(getContext().getPackageName(), "com.goalrift.football.app.SettingsActivity");
                        getContext().startActivity(i);
                    } catch (Exception ex) {
                        Toast.makeText(getContext(), "Ayarlar açılamadı", Toast.LENGTH_SHORT).show();
                    }
                    if (Build.VERSION.SDK_INT >= 28) {
                        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                    } else {
                        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                    }
                }
                return true;
            }
        }

        boolean handled = false;
        if(action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE){
            handled = true;
            for (MenuButton b : buttons){
                b.pressed = b.frame.contains(e.getX(), e.getY());
            }
            invalidate();
        } else if(action == MotionEvent.ACTION_UP){
            for (MenuButton b : buttons){
                boolean inside = b.frame.contains(e.getX(), e.getY());
                b.pressed = false;
                if(inside){
                    if (Build.VERSION.SDK_INT >= 28) {
                        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                    } else {
                        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                    }
                    if(b.action!=null) b.action.run();
                    Analytics.event("menu_click", b.title);
                    invalidate();
                    return true;
                }
            }
            invalidate();
        }
        return handled || super.onTouchEvent(e);
    }
}
