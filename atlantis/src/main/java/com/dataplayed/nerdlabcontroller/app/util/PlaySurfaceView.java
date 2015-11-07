package com.dataplayed.nerdlabcontroller.app.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.dataplayed.nerdlabcontroller.app.R;

import java.util.ArrayList;

public class PlaySurfaceView extends SurfaceView {
    public Bitmap bitmap;
    public Paint playerColor = new Paint(Color.WHITE);
    public int bitmapId = R.drawable.human0;
    public int imageSet = 0;
    public float x;
    public float y;
    //add display variables- modify through MainActivity
    public boolean holding;
    public boolean reacting;
    public float amplitude;
    public boolean audioEnabled;
    public float rotation = 0;
    public boolean allowChangesToCanvas = false;
    private SurfaceHolder surfaceHolder;
    private ArrayList<TapListener> listeners=new ArrayList<TapListener>();
    private boolean hasDrawn = false;


    public PlaySurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        System.out.println("Instantiated Surface View");
    }
    @Override
    public void draw(Canvas canvas) {
        if (!hasDrawn) {
            x = getWidth()/2;
            y = getHeight()/2;
            hasDrawn = true;
        }
        canvas.drawColor(Color.BLACK);
        canvas.rotate(rotation,getWidth()/2, getHeight()/2);
        bitmap = BitmapFactory.decodeResource(getResources(), bitmapId);
        Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setARGB(255, 255, 255, 255);

        if (holding) {
            x = getWidth()/2;
            y = getHeight()/2;
            canvas.drawCircle(getWidth() / 2 , getHeight() / 2 , getWidth() * 2, circlePaint);
        }

        if (audioEnabled) {
            x = getWidth()/2;
            y = getHeight()/2;
            canvas.drawCircle(x, y, amplitude*(getWidth()/6), circlePaint);
            System.out.println("AUDIO VISUALIZING!");
        }

        if (reacting) {
            System.out.println("Reacting to something");
            canvas.drawBitmap(bitmap, getWidth()/2, getHeight() / 2, circlePaint);
        }
        else {

            canvas.drawBitmap(bitmap, x - bitmap.getWidth() / 2, y - bitmap.getHeight() / 2, playerColor);
        }

    }



    public boolean onTouchEvent(MotionEvent event) {
        if (allowChangesToCanvas) {
            x = event.getX();
            y = event.getY();

            for (TapListener l : listeners) {
                l.onTap(event);
            }
        }
            updateBitmap();

        return true;
    }

    public void updateBitmap() {
        Canvas canvas = null;
        try {

            canvas =  getHolder().lockCanvas(null);
            synchronized (getHolder()) {
                this.draw(canvas);
            }
        }
        finally {
            if (canvas != null) {
                getHolder().unlockCanvasAndPost(canvas);
            }
        }
    }

    public void addTapListener(TapListener l) {
        listeners.add(l);
    }


    public void removeTapListener(TapListener l) {
        listeners.remove(l);
    }



    public interface TapListener {
        void onTap(MotionEvent event);
    }

}