package com.goalrift.football.game.ui.play;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;

public class Joystick {
    private float cx, cy, baseR=120, hatR=50;
    private float ax, ay; // -1..1
    private int pointerId = -1;
    public float sensitivity = 1.0f;

    public void layout(float x, float y){ this.cx=x; this.cy=y; }
    public boolean onTouch(MotionEvent e){
        int action = e.getActionMasked();
        if(action==MotionEvent.ACTION_DOWN || action==MotionEvent.ACTION_POINTER_DOWN){
            int i = e.getActionIndex();
            float tx = e.getX(i), ty = e.getY(i);
            if(distance(tx,ty,cx,cy) < baseR*1.2f){
                pointerId = e.getPointerId(i);
                update(tx,ty);
                return true;
            }
        } else if(action==MotionEvent.ACTION_MOVE){
            if(pointerId!=-1){
                int i = e.findPointerIndex(pointerId);
                if(i>=0){ update(e.getX(i), e.getY(i)); return true; }
            }
        } else if(action==MotionEvent.ACTION_UP || action==MotionEvent.ACTION_POINTER_UP || action==MotionEvent.ACTION_CANCEL){
            int i = e.getActionIndex();
            if(pointerId==e.getPointerId(i)){
                pointerId=-1; ax=ay=0;
                return true;
            }
        }
        return false;
    }
    private void update(float x, float y){
        float dx = (x-cx)/baseR, dy = (y-cy)/baseR;
        float len = (float)Math.sqrt(dx*dx+dy*dy);
        if(len>1){ dx/=len; dy/=len; }
        ax = dx*sensitivity; ay = dy*sensitivity;
    }
    private float distance(float x1,float y1,float x2,float y2){
        float dx=x1-x2, dy=y1-y2; return (float)Math.sqrt(dx*dx+dy*dy);
    }
    public float ax(){ return ax; }
    public float ay(){ return ay; }
    public void draw(Canvas c, Paint p){
        p.setColor(0x44FFFFFF); c.drawCircle(cx, cy, baseR, p);
        p.setColor(0xAAFFFFFF); c.drawCircle(cx+ax*baseR, cy+ay*baseR, hatR, p);
    }
}
