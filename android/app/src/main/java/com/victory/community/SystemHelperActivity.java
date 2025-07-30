package com.victory.community;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.victory.community.services.OverlayService;
import com.victory.community.services.RootOverlayService;
import com.victory.community.utils.RootUtils;
import com.victory.community.utils.PermissionUtils;
import com.victory.community.utils.ScreenUtils;

/**
 * Victory Community - System Helper Activity
 * Entry point for the application with permission handling and service management
 */
public class SystemHelperActivity extends AppCompatActivity {
    
    private static final String TAG = "VictoryMain";
    private static final int REQUEST_OVERLAY_PERMISSION = 1001;
    private static final int REQUEST_STORAGE_PERMISSION = 1002;
    
    // Native library loading
    static {
        try {
            System.loadLibrary("victory_core");
        } catch (UnsatisfiedLinkError e) {
            // Native library is optional for basic functionality
            android.util.Log.w(TAG, "Native library not found, some features may be disabled");
        }
    }
    
    // Native methods (optional, for advanced features)
    public native void initializeNativeCore(String cachePath);
    public native boolean isNativeSupported();
    
    // UI Components
    private SeekBar slideButton;
    private Switch rootModeSwitch;
    private TextView systemStatusText;
    private TextView detectionStatusText;
    private TextView gameStatusText;
    private ImageView systemIndicator;
    private ImageView detectionIndicator;
    private ImageView gameIndicator;
    private ImageView rootIndicator;
    private TextView feedbackLink;
    
    private boolean isRootMode = false;
    private boolean overlayServiceRunning = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Initialize native core if available
        initializeNativeIfAvailable();
        
        // Check device capabilities
        checkDeviceCapabilities();
        
        // Request necessary permissions
        requestRequiredPermissions();
        
        // Initialize UI
        initializeUI();
    }
    
    /**
     * Initialize native core library if available
     */
    private void initializeNativeIfAvailable() {
        try {
            if (isNativeSupported()) {
                String cachePath = getCacheDir().getAbsolutePath();
                initializeNativeCore(cachePath);
                android.util.Log.i(TAG, "Native core initialized successfully");
            }
        } catch (UnsatisfiedLinkError e) {
            android.util.Log.w(TAG, "Native core not available: " + e.getMessage());
        }
    }
    
    /**
     * Check device capabilities and determine optimal mode
     */
    private void checkDeviceCapabilities() {
        // Check if device is rooted
        isRootMode = RootUtils.isDeviceRooted() && RootUtils.hasRootAccess();
        
        // Log device info
        android.util.Log.i(TAG, "Device Info:");
        android.util.Log.i(TAG, "- Android Version: " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")");
        android.util.Log.i(TAG, "- Root Access: " + isRootMode);
        android.util.Log.i(TAG, "- Screen Size: " + ScreenUtils.getScreenSize(this));
        android.util.Log.i(TAG, "- Overlay Permission: " + PermissionUtils.hasOverlayPermission(this));
    }
    
    /**
     * Request all required permissions
     */
    private void requestRequiredPermissions() {
        // Check overlay permission (required for both modes)
        if (!PermissionUtils.hasOverlayPermission(this)) {
            requestOverlayPermission();
            return;
        }
        
        // Check storage permission
        if (!PermissionUtils.hasStoragePermission(this)) {
            requestStoragePermission();
            return;
        }
        
        // All permissions granted, initialize overlay service
        initializeOverlayService();
    }
    
    /**
     * Request overlay permission
     */
    private void requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Overlay permission required for Victory Community", Toast.LENGTH_LONG).show();
                
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION);
            }
        }
    }
    
    /**
     * Request storage permission
     */
    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String[] permissions = {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
            
            ActivityCompat.requestPermissions(this, permissions, REQUEST_STORAGE_PERMISSION);
        }
    }
    
    /**
     * Initialize overlay service based on device capabilities
     */
    private void initializeOverlayService() {
        if (overlayServiceRunning) {
            return;
        }
        
        Intent serviceIntent;
        
        if (isRootMode) {
            // Use root overlay service for better performance
            serviceIntent = new Intent(this, RootOverlayService.class);
            android.util.Log.i(TAG, "Starting Root Overlay Service");
        } else {
            // Use standard overlay service
            serviceIntent = new Intent(this, OverlayService.class);
            android.util.Log.i(TAG, "Starting Standard Overlay Service");
        }
        
        // Start foreground service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        
        overlayServiceRunning = true;
        updateUI();
    }
    
    /**
     * Stop overlay service
     */
    private void stopOverlayService() {
        if (!overlayServiceRunning) {
            return;
        }
        
        Intent serviceIntent;
        
        if (isRootMode) {
            serviceIntent = new Intent(this, RootOverlayService.class);
        } else {
            serviceIntent = new Intent(this, OverlayService.class);
        }
        
        stopService(serviceIntent);
        overlayServiceRunning = false;
        updateUI();
    }
    
    /**
     * Initialize UI components
     */
    private void initializeUI() {
        // Find UI components using correct IDs from activity_main.xml
        slideButton = findViewById(R.id.slide_button);
        rootModeSwitch = findViewById(R.id.switch_root_mode);
        systemStatusText = findViewById(R.id.tv_system_status);
        detectionStatusText = findViewById(R.id.tv_detection_status);
        gameStatusText = findViewById(R.id.tv_game_status);
        systemIndicator = findViewById(R.id.iv_system_indicator);
        detectionIndicator = findViewById(R.id.iv_detection_indicator);
        gameIndicator = findViewById(R.id.iv_game_indicator);
        rootIndicator = findViewById(R.id.iv_root_indicator);
        feedbackLink = findViewById(R.id.tv_feedback_link);
        
        // Set up slide button listener
        if (slideButton != null) {
            slideButton.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    // Handle slide progress
                }
                
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    // Handle slide start
                }
                
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    // Handle slide completion
                    if (seekBar.getProgress() > 80) {
                        // Slide to start
                        toggleOverlayService();
                        seekBar.setProgress(100);
                    } else if (seekBar.getProgress() < 20) {
                        // Slide to stop
                        if (overlayServiceRunning) {
                            toggleOverlayService();
                        }
                        seekBar.setProgress(0);
                    } else {
                        // Return to original position
                        seekBar.setProgress(overlayServiceRunning ? 100 : 0);
                    }
                }
            });
        }
        
        // Set up root mode switch
        if (rootModeSwitch != null) {
            rootModeSwitch.setChecked(isRootMode);
            rootModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked && !RootUtils.hasRootAccess()) {
                    Toast.makeText(this, "Root access not available", Toast.LENGTH_SHORT).show();
                    buttonView.setChecked(false);
                } else {
                    isRootMode = isChecked;
                    if (overlayServiceRunning) {
                        // Restart service with new mode
                        stopOverlayService();
                        initializeOverlayService();
                    }
                    updateUI();
                }
            });
        }
        
        // Set up feedback link
        if (feedbackLink != null) {
            feedbackLink.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://t.me/victory_community"));
                startActivity(intent);
            });
        }
        
        // Set up hamburger menu
        findViewById(R.id.btn_hamburger_menu).setOnClickListener(v -> openSettings());
        
        // Update UI state
        updateUI();
    }
    
    /**
     * Toggle overlay service on/off
     */
    private void toggleOverlayService() {
        if (overlayServiceRunning) {
            stopOverlayService();
            Toast.makeText(this, "Victory overlay stopped", Toast.LENGTH_SHORT).show();
        } else {
            if (PermissionUtils.hasOverlayPermission(this)) {
                initializeOverlayService();
                Toast.makeText(this, "Victory overlay started", Toast.LENGTH_SHORT).show();
            } else {
                requestOverlayPermission();
            }
        }
    }
    
    /**
     * Open settings activity
     */
    private void openSettings() {
        // For now, show a simple dialog
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Settings")
                .setMessage("Settings panel coming soon!\n\nCurrent Status:\n" +
                          "- Mode: " + (isRootMode ? "Root" : "Standard") + "\n" +
                          "- Overlay: " + (overlayServiceRunning ? "Running" : "Stopped"))
                .setPositiveButton("OK", null)
                .show();
    }
    
    /**
     * Show about dialog
     */
    private void showAbout() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Victory Community")
                .setMessage("Open source overlay assistance app\n\nVersion: 1.0.0\nMode: " + (isRootMode ? "Root" : "Standard"))
                .setPositiveButton("OK", null)
                .show();
    }
    
    /**
     * Update UI based on current state
     */
    private void updateUI() {
        // Update slide button position
        if (slideButton != null) {
            slideButton.setProgress(overlayServiceRunning ? 100 : 0);
        }
        
        // Update root indicator visibility
        if (rootIndicator != null) {
            rootIndicator.setVisibility(isRootMode ? android.view.View.VISIBLE : android.view.View.GONE);
        }
        
        // Update status texts
        if (systemStatusText != null) {
            systemStatusText.setText(overlayServiceRunning ? "System Active" : "System Ready");
        }
        
        if (detectionStatusText != null) {
            detectionStatusText.setText("Detection Ready");
        }
        
        if (gameStatusText != null) {
            gameStatusText.setText("Waiting for 8 Ball Pool");
        }
        
        // Update status indicators
        updateStatusIndicator(systemIndicator, overlayServiceRunning);
        updateStatusIndicator(detectionIndicator, true);
        updateStatusIndicator(gameIndicator, false);
    }
    
    /**
     * Update status indicator color
     */
    private void updateStatusIndicator(ImageView indicator, boolean active) {
        if (indicator != null) {
            int colorRes = active ? R.color.status_active : R.color.status_inactive;
            indicator.setColorFilter(ContextCompat.getColor(this, colorRes));
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            if (PermissionUtils.hasOverlayPermission(this)) {
                Toast.makeText(this, "Overlay permission granted", Toast.LENGTH_SHORT).show();
                requestRequiredPermissions();
            } else {
                Toast.makeText(this, "Overlay permission denied. App may not work properly.", Toast.LENGTH_LONG).show();
            }
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            if (allGranted) {
                Toast.makeText(this, "Storage permissions granted", Toast.LENGTH_SHORT).show();
                initializeOverlayService();
            } else {
                Toast.makeText(this, "Storage permissions denied. Some features may not work.", Toast.LENGTH_LONG).show();
            }
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up if needed
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
    }
}
