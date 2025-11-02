package com.goalrift.football.game.math;

public class Vec2 {
    public float x, y;
    public Vec2(){}
    public Vec2(float x, float y){this.x=x;this.y=y;}
    public Vec2 set(float x, float y){this.x=x;this.y=y;return this;}
    public Vec2 add(Vec2 o){x+=o.x;y+=o.y;return this;}
    public Vec2 add(float ax,float ay){x+=ax;y+=ay;return this;}
    public Vec2 sub(Vec2 o){x-=o.x;y-=o.y;return this;}
    public Vec2 mul(float s){x*=s;y*=s;return this;}
    public float len(){return (float)Math.sqrt(x*x+y*y);}
    public Vec2 nor(){float l=len(); if(l>1e-6f){x/=l;y/=l;} return this;}
    public Vec2 cpy(){return new Vec2(x,y);}
}
