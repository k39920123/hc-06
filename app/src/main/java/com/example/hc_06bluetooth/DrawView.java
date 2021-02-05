package com.example.hc_06bluetooth;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;


public class DrawView extends View {
    int drawstart=0,drawend=-180; //onDraw半圓的起始角度和橫跨角度
    double r1 = 0,r0=0,rotation;//r1:手勢和圓心的角度 r0:手勢和圓心的角度 rotation:r0到r1旋轉的角度(微量)
    int startangle=0,endangle=0;//start指針角度  end指針角度(距離180)
    private PointF lastPoint = new PointF();//最後紀錄手指的xy
    private PointF strmid = new PointF(785,590);//start指針中點xy
    private PointF endmid = new PointF(295,590);//end指針中點xy
    int mode=0;//0:控制start,1:控制end
    static int R=490;
    //園心:x=540,y=590,r=490

    public DrawView(Context context,@Nullable AttributeSet attrs) {
        super(context,attrs);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Paint p = new Paint();
        p.setColor(Color.BLACK);
        RectF oval2 = new RectF(50, 100, 1030, 1080);// 设置个新的长方形，扫描测量
        canvas.drawArc(oval2, drawstart, drawend, true, p);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getAction() & MotionEvent.ACTION_MASK){
            case MotionEvent.ACTION_DOWN:
                lastPoint.set(e.getX(), e.getY());
                double strd=Math.pow(lastPoint.x-strmid.x,2)+Math.pow(lastPoint.y-strmid.y,2); //手指起點和start中點距離
                double endd=Math.pow(lastPoint.x-endmid.x,2)+Math.pow(lastPoint.y-endmid.y,2); //手指起點和end中點距離
                if(strd<=endd )//控制start
                {
                    mode=0;
                }
                else if(strd>endd)//控制end
                {
                    mode=1;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                r0= Math.toDegrees(Math.atan2( lastPoint.x - 540 , 590 - lastPoint.y));
                r1= Math.toDegrees(Math.atan2(e.getX() - 540 , 590 - e.getY()));
                rotation=r1-r0;
                caltheangle(mode);
                break;
        }
        doublecheck();
        invalidate();
        //rotation=0;
        lastPoint.set(e.getX(), e.getY());
        return true;
    }

    public PointF changemidxy(int angle){
        double dx=R/2*Math.cos(Math.toRadians(angle));
        double dy=590-R/2*Math.sin(Math.toRadians(angle));
        if(mode==0){
            dx=dx+540;
        }
        else if(mode==1){
            dx=540-dx;
        }
        PointF q=new PointF((float)dx,(float)dy);
        return q;
    }

    public void doublecheck()
    {
        if(startangle<0||drawstart>0){
            drawstart=0;
            startangle=0;
        }
        if(endangle<0||drawend<-180){
            drawend=-180+startangle;
            endangle=0;
        }
    }

    public void caltheangle(int mode)
    {
        if(mode==0)//操控start
        {
            if(180-endangle-startangle>=5) { //start和end角度>=5(限制最小距離)
                if(startangle>=0||drawstart<=0) { //start角度>=0(限制start最小值)
                    drawstart=-startangle;
                    drawend=-180+startangle+endangle;
                }
                startangle = startangle + (int) (-rotation);
                strmid = changemidxy(startangle);//改變strmid
            }
            else if(180-endangle-startangle<5){ //start和end角度<5(限制最小距離)
                startangle=175-endangle;
            }
        }
        else if(mode==1)
        {
            if(180-endangle-startangle>=5) { //start和end角度>=5(限制最小距離)
                if(endangle>=0||drawend>=-180) { //end角度>=0(限制start最小值)
                    drawend = -180+endangle+startangle;
                }
                endangle = endangle + (int) rotation;
                endmid = changemidxy(endangle);//改變endmid
            }
            else if(180-endangle-startangle<5){ //start和end角度<5(限制最小距離)
                endangle=175-startangle;
            }
        }
        doublecheck();
        rotation=0;
    }

}
