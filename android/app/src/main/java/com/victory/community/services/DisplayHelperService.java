package com.victory.community.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.victory.community.SystemHelperActivity;

/**
 * Display Helper Service - Real Overlay Functionality
 * Provides system overlay assistance with crosshair and touch detection
 */
public class DisplayHelperService extends Service {
    private static final String TAG = "DisplayHelper";
    private static final String CHANNEL_ID = "overlay_service_channel";
    private static final int NOTIFICATION_ID = 1001;
    
    private WindowManager windowManager;
    private View overlayView;
    private WindowManager.LayoutParams overlayParams;
    private boolean isOverlayVisible = false;
    
    // Overlay configuration
    private int overlaySize = 100;
    private float overlayOpacity = 0.8f;
    private int overlayColor = Color.RED;
    private int overlayX = 100;
    private int overlayY = 200;
    
    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        createNotificationChannel();
        Log.d(TAG, "DisplayHelperService created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            Log.d(TAG, "Received action: " + action);
            
            switch (action != null ? action : "") {
                case "SHOW_OVERLAY":
                    startForegroundService();
                    showOverlay();
                    break;
                case "HIDE_OVERLAY":
                    hideOverlay();
                    stopSelf();
                    break;
                case "UPDATE_OVERLAY":
                    updateOverlayConfig(intent);
                    break;
                default:
                    startForegroundService();
                    showOverlay();
                    break;
            }
        }
        
        return START_STICKY; // Keep service running
    }

    @Override
    public void onDestroy() {
        hideOverlay();
        Log.d(TAG, "DisplayHelperService destroyed");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; // Not a bound service
    }
    
    /**
     * Create notification channel for foreground service
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "System Helper Overlay",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("System Helper overlay service is running");
            channel.setShowBadge(false);
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
    
    /**
     * Start as foreground service with notification
     */
    private void startForegroundService() {
        Intent notificationIntent = new Intent(this, SystemHelperActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, 
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0
        );

        Notification notification = new Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("System Helper Active")
            .setContentText("Overlay assistance is running")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build();

        startForeground(NOTIFICATION_ID, notification);
        Log.d(TAG, "Started as foreground service");
    }

    /**
     * Show the overlay
     */
    private void showOverlay() {
        if (isOverlayVisible || overlayView != null) {
            Log.w(TAG, "Overlay already visible");
            return;
        }
        
        try {
            // Create overlay view
            overlayView = new OverlayView(this);
            
            // Configure window parameters
            int layoutFlag;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                layoutFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            } else {
                layoutFlag = WindowManager.LayoutParams.TYPE_PHONE;
            }

            overlayParams = new WindowManager.LayoutParams(
                overlaySize,
                overlaySize,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT
            );

            overlayParams.gravity = Gravity.TOP | Gravity.LEFT;
            overlayParams.x = overlayX;
            overlayParams.y = overlayY;
            overlayParams.alpha = overlayOpacity;
            
            // Add overlay to window
            windowManager.addView(overlayView, overlayParams);
            isOverlayVisible = true;
            
            Toast.makeText(this, "System Helper overlay activated", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Overlay shown at position (" + overlayX + ", " + overlayY + ")");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to show overlay", e);
            Toast.makeText(this, "Failed to show overlay. Check permissions.", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Hide the overlay
     */
    private void hideOverlay() {
        if (!isOverlayVisible || overlayView == null) {
            return;
        }
        
        try {
            windowManager.removeView(overlayView);
            overlayView = null;
            isOverlayVisible = false;
            
            Toast.makeText(this, "System Helper overlay deactivated", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Overlay hidden");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to hide overlay", e);
        }
    }

    /**
     * Update overlay configuration
     */
    private void updateOverlayConfig(Intent intent) {
        boolean needsUpdate = false;
        
        if (intent.hasExtra("size")) {
            overlaySize = intent.getIntExtra("size", overlaySize);
            overlayParams.width = overlaySize;
            overlayParams.height = overlaySize;
            needsUpdate = true;
        }
        
        if (intent.hasExtra("x")) {
            overlayX = intent.getIntExtra("x", overlayX);
            overlayParams.x = overlayX;
            needsUpdate = true;
        }
        
        if (intent.hasExtra("y")) {
            overlayY = intent.getIntExtra("y", overlayY);
            overlayParams.y = overlayY;
            needsUpdate = true;
        }
        
        if (intent.hasExtra("opacity")) {
            overlayOpacity = intent.getFloatExtra("opacity", overlayOpacity);
            overlayParams.alpha = overlayOpacity;
            needsUpdate = true;
        }
        
        if (intent.hasExtra("color")) {
            overlayColor = intent.getIntExtra("color", overlayColor);
            if (overlayView != null) {
                overlayView.invalidate(); // Redraw with new color
            }
            needsUpdate = true;
        }
        
        if (needsUpdate && isOverlayVisible && overlayView != null) {
            try {
                windowManager.updateViewLayout(overlayView, overlayParams);
                Log.d(TAG, "Overlay configuration updated");
            } catch (Exception e) {
                Log.e(TAG, "Failed to update overlay", e);
            }
        }
    }

    /**
     * Custom overlay view with crosshair and interactive features
     */
    private class OverlayView extends View {
        private Paint crosshairPaint;
        private Paint backgroundPaint;
        private Paint centerDotPaint;
        private Paint textPaint;
        
        // Touch handling
        private boolean isDragging = false;
        private float lastTouchX, lastTouchY;
        private int initialX, initialY;
        
        public OverlayView(Context context) {
            super(context);
            initializePaints();
            setOnTouchListener(new OverlayTouchListener());
        }

        private void initializePaints() {
            // Crosshair paint
            crosshairPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            crosshairPaint.setColor(overlayColor);
            crosshairPaint.setStrokeWidth(3.0f);
            crosshairPaint.setStyle(Paint.Style.STROKE);
            
            // Background paint (semi-transparent circle)
            backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            backgroundPaint.setColor(Color.argb(60, 0, 0, 0));
            backgroundPaint.setStyle(Paint.Style.FILL);
            
            // Center dot paint
            centerDotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            centerDotPaint.setColor(overlayColor);
            centerDotPaint.setStyle(Paint.Style.FILL);
            
            // Text paint for coordinates
            textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            textPaint.setColor(Color.WHITE);
            textPaint.setTextSize(20.0f);
            textPaint.setTextAlign(Paint.Align.CENTER);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            
            int width = getWidth();
            int height = getHeight();
            int centerX = width / 2;
            int centerY = height / 2;
            int radius = Math.min(width, height) / 2 - 10;
            
            // Draw background circle
            canvas.drawCircle(centerX, centerY, radius, backgroundPaint);
            
            // Draw crosshair lines
            int crosshairLength = radius - 15;
            
            // Horizontal line
            canvas.drawLine(centerX - crosshairLength, centerY, 
                           centerX + crosshairLength, centerY, crosshairPaint);
            
            // Vertical line
            canvas.drawLine(centerX, centerY - crosshairLength, 
                           centerX, centerY + crosshairLength, crosshairPaint);
            
            // Draw center dot
            canvas.drawCircle(centerX, centerY, 4, centerDotPaint);
            
            // Draw coordinate text
            String coordinates = "(" + overlayParams.x + ", " + overlayParams.y + ")";
            canvas.drawText(coordinates, centerX, centerY - radius - 10, textPaint);
            
            // Update paint colors in case they changed
            crosshairPaint.setColor(overlayColor);
            centerDotPaint.setColor(overlayColor);
        }
    }

    /**
     * Touch listener for overlay interaction
     */
    private class OverlayTouchListener implements View.OnTouchListener {
        private static final int DRAG_THRESHOLD = 10;

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // Record initial position
                    initialX = overlayParams.x;
                    initialY = overlayParams.y;
                    lastTouchX = event.getRawX();
                    lastTouchY = event.getRawY();
                    isDragging = false;
                    return true;

                case MotionEvent.ACTION_MOVE:
                    float deltaX = event.getRawX() - lastTouchX;
                    float deltaY = event.getRawY() - lastTouchY;
                    
                    // Check if this is a drag gesture
                    if (!isDragging && (Math.abs(deltaX) > DRAG_THRESHOLD || Math.abs(deltaY) > DRAG_THRESHOLD)) {
                        isDragging = true;
                    }
                    
                    if (isDragging) {
                        // Update overlay position
                        overlayParams.x = initialX + (int) deltaX;
                        overlayParams.y = initialY + (int) deltaY;
                        
                        // Keep overlay on screen
                        overlayParams.x = Math.max(0, Math.min(overlayParams.x, 
                            getResources().getDisplayMetrics().widthPixels - overlaySize));
                        overlayParams.y = Math.max(0, Math.min(overlayParams.y, 
                            getResources().getDisplayMetrics().heightPixels - overlaySize));
                        
                        try {
                            windowManager.updateViewLayout(overlayView, overlayParams);
                            overlayView.invalidate(); // Redraw with new coordinates
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to update overlay position", e);
                        }
                    }
                    return true;

                case MotionEvent.ACTION_UP:
                    if (!isDragging) {
                        // Single tap - show current position
                        Toast.makeText(DisplayHelperService.this, 
                            "Position: (" + overlayParams.x + ", " + overlayParams.y + ")", 
                            Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Overlay tapped at position (" + overlayParams.x + ", " + overlayParams.y + ")");
                    } else {
                        // Save final position
                        overlayX = overlayParams.x;
                        overlayY = overlayParams.y;
                        Log.d(TAG, "Overlay moved to position (" + overlayX + ", " + overlayY + ")");
                    }
                    return true;
            }
            return false;
        }
    }
    
    // Static methods for external control
    public static void startOverlay(Context context) {
        Intent intent = new Intent(context, DisplayHelperService.class);
        intent.setAction("SHOW_OVERLAY");
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
        
        Log.d(TAG, "Start overlay requested");
    }
    
    public static void stopOverlay(Context context) {
        Intent intent = new Intent(context, DisplayHelperService.class);
        intent.setAction("HIDE_OVERLAY");
        context.startService(intent);
        
        Log.d(TAG, "Stop overlay requested");
    }
    
    public static void updateOverlay(Context context, int x, int y, int width, int height, float opacity) {
        Intent intent = new Intent(context, DisplayHelperService.class);
        intent.setAction("UPDATE_OVERLAY");
        intent.putExtra("x", x);
        intent.putExtra("y", y);
        intent.putExtra("size", Math.max(width, height)); // Use larger dimension
        intent.putExtra("opacity", opacity);
        context.startService(intent);
        
        Log.d(TAG, "Update overlay requested: (" + x + ", " + y + ") size=" + Math.max(width, height));
    }
    
    public static void updateOverlayColor(Context context, int color) {
        Intent intent = new Intent(context, DisplayHelperService.class);
        intent.setAction("UPDATE_OVERLAY");
        intent.putExtra("color", color);
        context.startService(intent);
        
        Log.d(TAG, "Update overlay color requested: " + color);
    }
}
