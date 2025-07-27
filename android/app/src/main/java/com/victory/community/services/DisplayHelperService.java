package com.android.systemhelper.services;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;
import android.util.Log;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Color;

public class DisplayHelperService extends Service {
    private static final String TAG = "DisplayHelper";
    private WindowManager windowManager;
    private View overlayView;
    private WindowManager.LayoutParams params;
    private boolean isOverlayVisible = false;
    
    // Overlay configuration
    private int overlayWidth = 100;
    private int overlayHeight = 100;
    private int overlayX = 0;
    private int overlayY = 0;
    private float overlayOpacity = 0.8f;
    
    // Performance tracking
    private long lastFrameTime = 0;
    private int frameCount = 0;
    private float avgFps = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        initializeOverlay();
        Log.d(TAG, "OverlayService created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if ("SHOW_OVERLAY".equals(action)) {
                showOverlay();
            } else if ("HIDE_OVERLAY".equals(action)) {
                hideOverlay();
            } else if ("UPDATE_CONFIG".equals(action)) {
                updateOverlayConfig(intent);
            }
        }
        return START_STICKY; // Keep service running
    }

    @Override
    public void onDestroy() {
        hideOverlay();
        Log.d(TAG, "OverlayService destroyed");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; // Not a bound service
    }

    private void initializeOverlay() {
        // Create custom overlay view
        overlayView = new OverlayView(this);
        
        // Configure window parameters
        int layoutFlag;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutFlag = WindowManager.LayoutParams.TYPE_PHONE;
        }

        params = new WindowManager.LayoutParams(
            overlayWidth,
            overlayHeight,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = overlayX;
        params.y = overlayY;
        params.alpha = overlayOpacity;
    }

    public void showOverlay() {
        if (!isOverlayVisible && overlayView != null) {
            try {
                windowManager.addView(overlayView, params);
                isOverlayVisible = true;
                Toast.makeText(this, "Display Helper Activated", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Overlay shown");
            } catch (Exception e) {
                Log.e(TAG, "Failed to show overlay", e);
                Toast.makeText(this, "Failed to show overlay. Check permissions.", Toast.LENGTH_LONG).show();
            }
        }
    }

    public void hideOverlay() {
        if (isOverlayVisible && overlayView != null) {
            try {
                windowManager.removeView(overlayView);
                isOverlayVisible = false;
                Toast.makeText(this, "Display Helper Deactivated", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Overlay hidden");
            } catch (Exception e) {
                Log.e(TAG, "Failed to hide overlay", e);
            }
        }
    }

    private void updateOverlayConfig(Intent intent) {
        boolean needsUpdate = false;
        
        if (intent.hasExtra("width")) {
            overlayWidth = intent.getIntExtra("width", overlayWidth);
            params.width = overlayWidth;
            needsUpdate = true;
        }
        
        if (intent.hasExtra("height")) {
            overlayHeight = intent.getIntExtra("height", overlayHeight);
            params.height = overlayHeight;
            needsUpdate = true;
        }
        
        if (intent.hasExtra("x")) {
            overlayX = intent.getIntExtra("x", overlayX);
            params.x = overlayX;
            needsUpdate = true;
        }
        
        if (intent.hasExtra("y")) {
            overlayY = intent.getIntExtra("y", overlayY);
            params.y = overlayY;
            needsUpdate = true;
        }
        
        if (intent.hasExtra("opacity")) {
            overlayOpacity = intent.getFloatExtra("opacity", overlayOpacity);
            params.alpha = overlayOpacity;
            needsUpdate = true;
        }
        
        if (needsUpdate && isOverlayVisible) {
            try {
                windowManager.updateViewLayout(overlayView, params);
                Log.d(TAG, "Overlay configuration updated");
            } catch (Exception e) {
                Log.e(TAG, "Failed to update overlay", e);
            }
        }
    }

    // Custom overlay view with crosshair rendering
    private class OverlayView extends View {
        private Paint crosshairPaint;
        private Paint backgroundPaint;
        private Paint textPaint;
        
        public OverlayView(Context context) {
            super(context);
            initializePaints();
            setOnTouchListener(new TouchListener());
        }

        private void initializePaints() {
            // Crosshair paint
            crosshairPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            crosshairPaint.setColor(Color.RED);
            crosshairPaint.setStrokeWidth(2.0f);
            crosshairPaint.setStyle(Paint.Style.STROKE);
            
            // Background paint (semi-transparent)
            backgroundPaint = new Paint();
            backgroundPaint.setColor(Color.argb(50, 0, 0, 0));
            backgroundPaint.setStyle(Paint.Style.FILL);
            
            // Text paint for FPS counter
            textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            textPaint.setColor(Color.WHITE);
            textPaint.setTextSize(12.0f);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            
            // Performance tracking
            long currentTime = System.currentTimeMillis();
            if (lastFrameTime != 0) {
                float fps = 1000.0f / (currentTime - lastFrameTime);
                avgFps = (avgFps * frameCount + fps) / (frameCount + 1);
                frameCount++;
            }
            lastFrameTime = currentTime;
            
            int width = getWidth();
            int height = getHeight();
            int centerX = width / 2;
            int centerY = height / 2;
            
            // Draw background
            canvas.drawRect(0, 0, width, height, backgroundPaint);
            
            // Draw crosshair
            int crosshairSize = Math.min(width, height) / 3;
            
            // Horizontal line
            canvas.drawLine(centerX - crosshairSize/2, centerY, 
                           centerX + crosshairSize/2, centerY, crosshairPaint);
            
            // Vertical line
            canvas.drawLine(centerX, centerY - crosshairSize/2, 
                           centerX, centerY + crosshairSize/2, crosshairPaint);
            
            // Draw center dot
            canvas.drawCircle(centerX, centerY, 2, crosshairPaint);
            
            // Draw FPS counter (for performance monitoring)
            if (frameCount > 30) { // Only show after some frames for accuracy
                canvas.drawText(String.format("FPS: %.1f", avgFps), 10, 20, textPaint);
            }
            
            // Trigger redraw for smooth animation
            invalidate();
        }
    }

    // Touch listener for overlay interaction
    private class TouchListener implements View.OnTouchListener {
        private int initialX, initialY;
        private float initialTouchX, initialTouchY;
        private boolean isDragging = false;
        private static final int DRAG_THRESHOLD = 10;

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    initialX = params.x;
                    initialY = params.y;
                    initialTouchX = event.getRawX();
                    initialTouchY = event.getRawY();
                    isDragging = false;
                    return true;

                case MotionEvent.ACTION_MOVE:
                    float deltaX = event.getRawX() - initialTouchX;
                    float deltaY = event.getRawY() - initialTouchY;
                    
                    if (!isDragging && (Math.abs(deltaX) > DRAG_THRESHOLD || Math.abs(deltaY) > DRAG_THRESHOLD)) {
                        isDragging = true;
                    }
                    
                    if (isDragging) {
                        params.x = initialX + (int) deltaX;
                        params.y = initialY + (int) deltaY;
                        
                        try {
                            windowManager.updateViewLayout(overlayView, params);
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to update overlay position", e);
                        }
                    }
                    return true;

                case MotionEvent.ACTION_UP:
                    if (!isDragging) {
                        // Single tap - could be used for configuration
                        Log.d(TAG, "Overlay tapped");
                        // TODO: Show configuration panel
                    }
                    return true;
            }
            return false;
        }
    }
    
    // Public methods for external control
    public static void startOverlay(Context context) {
        Intent intent = new Intent(context, OverlayService.class);
        intent.setAction("SHOW_OVERLAY");
        context.startService(intent);
    }
    
    public static void stopOverlay(Context context) {
        Intent intent = new Intent(context, OverlayService.class);
        intent.setAction("HIDE_OVERLAY");
        context.startService(intent);
    }
    
    public static void updateOverlay(Context context, int x, int y, int width, int height, float opacity) {
        Intent intent = new Intent(context, OverlayService.class);
        intent.setAction("UPDATE_CONFIG");
        intent.putExtra("x", x);
        intent.putExtra("y", y);
        intent.putExtra("width", width);
        intent.putExtra("height", height);
        intent.putExtra("opacity", opacity);
        context.startService(intent);
    }
}
