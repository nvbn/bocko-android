package com.nvbn.bockoandroid;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;

import java.util.Vector;
import java.util.concurrent.LinkedBlockingQueue;

class BockoView extends SurfaceView implements SurfaceHolder.Callback {
    interface DrawCall {
        void call(Canvas canvas, Paint paint);
    }

    class SetRGB implements DrawCall {
        int r;
        int g;
        int b;

        public SetRGB(int r, int g, int b) {
            this.r = r;
            this.b = b;
            this.g = g;
        }

        public void call(Canvas canvas, Paint paint) {
            paint.setARGB(255, r, g, b);
        }
    }

    class DrawRect implements DrawCall {
        float left;
        float top;
        float right;
        float bottom;

        public DrawRect(float left, float top, float right, float bottom) {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }

        public void call(Canvas canvas, Paint paint) {
            try {
                canvas.drawRect(left, top, right, bottom, paint);
            } catch (NullPointerException e) {
                Log.e(mLogTag, "Canvas or Paint is null: " + e);
            }
        }
    }

    class DrawerThread extends Thread {
        Paint mPaint;
        SurfaceHolder mSurfaceHolder;
        Context mContext;
        Handler mHandler;
        Boolean running = true;

        public DrawerThread(SurfaceHolder surfaceHolder, Context context,
                            Handler handler) {
            mPaint = new Paint();
            mSurfaceHolder = surfaceHolder;
            mContext = context;
            mHandler = handler;
        }

        public void planToStop() {
            running = false;
        }

        @Override
        public void run() {
            while (running) {
                Canvas canvas = null;
                try {
                    Vector<DrawCall> calls = mQueue.take();
                    canvas = mSurfaceHolder.lockCanvas(null);
                    synchronized (mSurfaceHolder) {
                        for (DrawCall call : calls)
                            call.call(canvas, mPaint);
                    }
                } catch (InterruptedException e) {
                } finally {
                    if (canvas != null) mSurfaceHolder.unlockCanvasAndPost(canvas);
                }
            }
        }
    }

    class JSObject {
        Vector<DrawCall> mBuffer = new Vector<>();

        @JavascriptInterface
        public void setRGB(int r, int g, int b) {
            mBuffer.addElement(new SetRGB(r, g, b));
        }

        @JavascriptInterface
        public void drawRect(float left, float top, float right, float bottom) {
            mBuffer.addElement(new DrawRect(left, top, right, bottom));
        }

        @JavascriptInterface
        public void flush(){
            try {
                mQueue.put((Vector<DrawCall>) mBuffer.clone());
                mBuffer.clear();
            } catch (InterruptedException e) {
                Log.e(mLogTag, "Can't flush: " + e.toString());
            }
        }

        @JavascriptInterface
        public int width() {
            return mWidth;
        }

        @JavascriptInterface
        public int height() {
            return mHeight;
        }
    }

    LinkedBlockingQueue<Vector<DrawCall>> mQueue = new LinkedBlockingQueue<>();
    String mLogTag = "BockoView";
    DrawerThread mThread;
    WebView mWebView;
    int mWidth;
    int mHeight;

    public BockoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mWebView = new WebView(context);
        mWebView.setWillNotDraw(true);
        final WebSettings webSettings = mWebView.getSettings();
        webSettings.setAppCacheEnabled(false);
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDefaultTextEncodingName("utf-8");
        mWebView.loadUrl(getUrl());
        mWebView.addJavascriptInterface(new JSObject(), "android");
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);
    }

    String getUrl() {
         return "http://192.168.0.107:3449/"; // Use something similar for figwheel
//        return "file:///android_asset/index.html"; // For bundled
    }

    public void surfaceCreated(SurfaceHolder holder) {
        mThread = new DrawerThread(holder, getContext(), new Handler());
        mThread.start();
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        mWidth = width;
        mHeight = height;
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        mThread.planToStop();
    }
}
