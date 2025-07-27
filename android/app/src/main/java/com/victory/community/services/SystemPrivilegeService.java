package com.victorycommunity.overlay;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.os.Build;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Enhanced overlay service for rooted devices
 * Provides superior performance and capabilities through root access
 */
public class SystemPrivilegeService extends OverlayService {
    private static final String TAG = "SystemPrivilege";
    private boolean isRootAccessVerified = false;
    private Process rootProcess;
    private DataOutputStream rootOutputStream;
    
    // Enhanced root-specific features
    private boolean directFramebufferAccess = false;
    private boolean systemLevelInjection = false;
    private int enhancedRefreshRate = 120; // Target 120fps for root mode
    
    @Override
    public void onCreate() {
        super.onCreate();
        verifyRootAccess();
        if (isRootAccessVerified) {
            initializeRootFeatures();
            Log.d(TAG, "SystemPrivilegeService initialized with enhanced capabilities");
        } else {
            Log.w(TAG, "Root access not available, falling back to standard mode");
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (isRootAccessVerified && intent != null) {
            String action = intent.getAction();
            if ("ENABLE_DIRECT_FRAMEBUFFER".equals(action)) {
                enableDirectFramebufferAccess();
            } else if ("ENABLE_SYSTEM_INJECTION".equals(action)) {
                enableSystemLevelInjection();
            } else if ("SET_ENHANCED_REFRESH_RATE".equals(action)) {
                int refreshRate = intent.getIntExtra("refresh_rate", 120);
                setEnhancedRefreshRate(refreshRate);
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        cleanupRootResources();
        super.onDestroy();
    }

    /**
     * Verify if the device has root access and if we can utilize it
     */
    private void verifyRootAccess() {
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            
            os.writeBytes("id\n");
            os.writeBytes("exit\n");
            os.flush();
            
            String response = reader.readLine();
            if (response != null && response.toLowerCase().contains("uid=0")) {
                isRootAccessVerified = true;
                rootProcess = process;
                rootOutputStream = os;
                Log.d(TAG, "Root access verified successfully");
            } else {
                Log.w(TAG, "Root access verification failed");
            }
            
            reader.close();
        } catch (IOException e) {
            Log.e(TAG, "Failed to verify root access", e);
            isRootAccessVerified = false;
        }
    }

    /**
     * Initialize root-specific features and optimizations
     */
    private void initializeRootFeatures() {
        try {
            // Set high priority for overlay process
            executeRootCommand("renice -20 " + android.os.Process.myPid());
            
            // Enable hardware acceleration at system level
            executeRootCommand("setprop debug.hwui.renderer opengl");
            executeRootCommand("setprop debug.egl.hw 1");
            
            // Optimize memory management
            executeRootCommand("echo 1 > /proc/sys/vm/drop_caches");
            
            Log.d(TAG, "Root features initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize root features", e);
        }
    }

    /**
     * Enable direct framebuffer access for maximum performance
     */
    private void enableDirectFramebufferAccess() {
        if (!isRootAccessVerified) return;
        
        try {
            // Grant access to framebuffer device
            executeRootCommand("chmod 666 /dev/graphics/fb0");
            executeRootCommand("chmod 666 /dev/fb0");
            
            // Set optimal framebuffer settings
            executeRootCommand("echo 0 > /sys/class/graphics/fb0/blank");
            
            directFramebufferAccess = true;
            Log.d(TAG, "Direct framebuffer access enabled");
        } catch (Exception e) {
            Log.e(TAG, "Failed to enable direct framebuffer access", e);
            directFramebufferAccess = false;
        }
    }

    /**
     * Enable system-level overlay injection
     */
    private void enableSystemLevelInjection() {
        if (!isRootAccessVerified) return;
        
        try {
            // Modify system properties for enhanced overlay capabilities
            executeRootCommand("setprop persist.vendor.overlay.disable_skip_initramfs false");
            executeRootCommand("setprop ro.surface_composer.max_frame_buffer_acquired_buffers 3");
            
            // Enable advanced compositor features
            executeRootCommand("setprop debug.sf.enable_hwc_vds 1");
            executeRootCommand("setprop debug.sf.hw 1");
            
            systemLevelInjection = true;
            Log.d(TAG, "System-level injection enabled");
        } catch (Exception e) {
            Log.e(TAG, "Failed to enable system-level injection", e);
            systemLevelInjection = false;
        }
    }

    /**
     * Set enhanced refresh rate for root mode
     */
    private void setEnhancedRefreshRate(int refreshRate) {
        if (!isRootAccessVerified) return;
        
        try {
            // Attempt to set higher refresh rate
            executeRootCommand("settings put system peak_refresh_rate " + refreshRate);
            executeRootCommand("settings put system min_refresh_rate " + refreshRate);
            
            this.enhancedRefreshRate = refreshRate;
            Log.d(TAG, "Enhanced refresh rate set to: " + refreshRate + "Hz");
        } catch (Exception e) {
            Log.e(TAG, "Failed to set enhanced refresh rate", e);
        }
    }

    /**
     * Execute root command safely
     */
    private void executeRootCommand(String command) throws IOException {
        if (rootOutputStream != null) {
            rootOutputStream.writeBytes(command + "\n");
            rootOutputStream.flush();
        } else {
            throw new IOException("Root access not available");
        }
    }

    /**
     * Create enhanced overlay view with root optimizations
     */
    @Override
    protected void initializeOverlay() {
        if (isRootAccessVerified) {
            // Create enhanced overlay view
            View overlayView = new EnhancedRootOverlayView(this);
            
            // Configure enhanced window parameters
            int layoutFlag;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                layoutFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            } else {
                layoutFlag = WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;
            }

            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                100, 100,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            );

            params.gravity = Gravity.TOP | Gravity.LEFT;
            params.alpha = 0.8f;
            
            // Root-specific enhancements
            if (systemLevelInjection) {
                params.flags |= WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
                params.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
            }
            
            Log.d(TAG, "Enhanced overlay initialized with root optimizations");
        } else {
            // Fall back to standard overlay
            super.initializeOverlay();
        }
    }

    /**
     * Enhanced overlay view with root-specific optimizations
     */
    private class EnhancedRootOverlayView extends View {
        private Paint enhancedCrosshairPaint;
        private Paint performancePaint;
        private long lastDrawTime = 0;
        private int rootFrameCount = 0;
        private float rootAvgFps = 0;

        public EnhancedRootOverlayView(Context context) {
            super(context);
            initializeEnhancedPaints();
            
            // Set hardware acceleration explicitly
            setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }

        private void initializeEnhancedPaints() {
            // Enhanced crosshair with better anti-aliasing
            enhancedCrosshairPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            enhancedCrosshairPaint.setColor(Color.argb(255, 255, 0, 0));
            enhancedCrosshairPaint.setStrokeWidth(1.5f);
            enhancedCrosshairPaint.setStyle(Paint.Style.STROKE);
            enhancedCrosshairPaint.setStrokeCap(Paint.Cap.ROUND);
            
            // Performance indicator paint
            performancePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            performancePaint.setColor(Color.GREEN);
            performancePaint.setTextSize(10.0f);
            performancePaint.setFakeBoldText(true);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            
            // Enhanced performance tracking
            long currentTime = System.nanoTime();
            if (lastDrawTime != 0) {
                float fps = 1000000000.0f / (currentTime - lastDrawTime);
                rootAvgFps = (rootAvgFps * rootFrameCount + fps) / (rootFrameCount + 1);
                rootFrameCount++;
            }
            lastDrawTime = currentTime;
            
            int width = getWidth();
            int height = getHeight();
            int centerX = width / 2;
            int centerY = height / 2;
            
            // Draw enhanced crosshair
            drawEnhancedCrosshair(canvas, centerX, centerY, width, height);
            
            // Draw performance indicator
            if (rootFrameCount > 60) { // Show after stabilization
                String perfText = String.format("ROOT: %.1f FPS", rootAvgFps);
                if (directFramebufferAccess) perfText += " [FB]";
                if (systemLevelInjection) perfText += " [SI]";
                
                canvas.drawText(perfText, 5, 15, performancePaint);
            }
            
            // Trigger redraw at enhanced refresh rate
            if (enhancedRefreshRate > 60) {
                postInvalidateDelayed(1000 / enhancedRefreshRate);
            } else {
                invalidate();
            }
        }

        private void drawEnhancedCrosshair(Canvas canvas, int centerX, int centerY, int width, int height) {
            int crosshairSize = Math.min(width, height) / 2;
            
            // Outer crosshair
            canvas.drawLine(centerX - crosshairSize/2, centerY, 
                           centerX + crosshairSize/2, centerY, enhancedCrosshairPaint);
            canvas.drawLine(centerX, centerY - crosshairSize/2, 
                           centerX, centerY + crosshairSize/2, enhancedCrosshairPaint);
            
            // Inner precision crosshair
            int innerSize = crosshairSize / 4;
            enhancedCrosshairPaint.setStrokeWidth(0.8f);
            canvas.drawLine(centerX - innerSize, centerY, 
                           centerX + innerSize, centerY, enhancedCrosshairPaint);
            canvas.drawLine(centerX, centerY - innerSize, 
                           centerX, centerY + innerSize, enhancedCrosshairPaint);
            
            // Center dot with enhanced precision
            enhancedCrosshairPaint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(centerX, centerY, 1.5f, enhancedCrosshairPaint);
            enhancedCrosshairPaint.setStyle(Paint.Style.STROKE);
            enhancedCrosshairPaint.setStrokeWidth(1.5f);
        }
    }

    /**
     * Clean up root resources
     */
    private void cleanupRootResources() {
        try {
            if (rootOutputStream != null) {
                rootOutputStream.writeBytes("exit\n");
                rootOutputStream.flush();
                rootOutputStream.close();
            }
            if (rootProcess != null) {
                rootProcess.destroy();
            }
            Log.d(TAG, "Root resources cleaned up");
        } catch (IOException e) {
            Log.e(TAG, "Error cleaning up root resources", e);
        }
    }

    // Public methods for root-specific features
    // Public methods for system-privileged features
public static void startPrivilegedService(Context context) {
    Intent intent = new Intent(context, SystemPrivilegeService.class);
    intent.setAction("SHOW_OVERLAY");
    context.startService(intent);
}

public static void enableDirectFramebuffer(Context context) {
    Intent intent = new Intent(context, SystemPrivilegeService.class);
    intent.setAction("ENABLE_DIRECT_FRAMEBUFFER");
    context.startService(intent);
}

public static void enableSystemInjection(Context context) {
    Intent intent = new Intent(context, SystemPrivilegeService.class);
    intent.setAction("ENABLE_SYSTEM_INJECTION");
    context.startService(intent);
}

public static void setEnhancedRefreshRate(Context context, int refreshRate) {
    Intent intent = new Intent(context, SystemPrivilegeService.class);
    intent.setAction("SET_ENHANCED_REFRESH_RATE");
    intent.putExtra("refresh_rate", refreshRate);
    context.startService(intent);
}
    
    public boolean isRootAccessAvailable() {
        return isRootAccessVerified;
    }
    
    public boolean isDirectFramebufferEnabled() {
        return directFramebufferAccess;
    }
    
    public boolean isSystemInjectionEnabled() {
        return systemLevelInjection;
    }
}
