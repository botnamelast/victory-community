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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.victory.community.services.DisplayHelperService;
import com.victory.community.services.SystemPrivilegeService;
import com.victory.community.ui.DeviceSettingsActivity;
import com.victory.community.utils.RootUtils;
import com.victory.community.utils.PermissionUtils;
import com.victory.community.utils.ScreenUtils;

/**
 * System Helper - Main Activity
 * Entry point for the application with permission handling and service management
 */
public class SystemHelperActivity extends AppCompatActivity {
    
    private static final String TAG = "SystemHelper";
    private static final int REQUEST_OVERLAY_PERMISSION = 1001;
    private static final int REQUEST_STORAGE_PERMISSION = 1002;
    
    // UI Components
    private ImageButton btnHamburgerMenu;
    private SeekBar slideButton;
    private Switch switchRootMode;
    private TextView tvSystemStatus, tvDetectionStatus, tvGameStatus;
    private ImageView ivRootIndicator, ivSystemIndicator, ivDetectionIndicator, ivGameIndicator;
    private TextView tvFeedbackLink;
    
    // State variables
    private boolean isRootMode = false;
    private boolean overlayServiceRunning = false;
    private boolean systemHelperActive = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Initialize UI components
        initializeViews();
        
        // Check device capabilities
        checkDeviceCapabilities();
        
        // Request necessary permissions
        requestRequiredPermissions();
        
        // Setup event listeners
        setupEventListeners();
        
        // Update initial UI state
        updateUI();
    }
    
    /**
     * Initialize all UI views - with safe null checks
     */
    private void initializeViews() {
        // Try to find UI elements, but don't crash if they don't exist
        try {
            btnHamburgerMenu = findViewById(R.id.btn_hamburger_menu);
        } catch (Exception e) { 
            Log.w(TAG, "btn_hamburger_menu not found in layout");
        }
        
        try {
            ivRootIndicator = findViewById(R.id.iv_root_indicator);
        } catch (Exception e) { 
            Log.w(TAG, "iv_root_indicator not found in layout");
        }
        
        try {
            slideButton = findViewById(R.id.slide_button);
        } catch (Exception e) { 
            Log.w(TAG, "slide_button not found in layout");
        }
        
        try {
            tvSystemStatus = findViewById(R.id.tv_system_status);
            tvDetectionStatus = findViewById(R.id.tv_detection_status);
            tvGameStatus = findViewById(R.id.tv_game_status);
        } catch (Exception e) { 
            Log.w(TAG, "Status TextViews not found in layout");
        }
        
        try {
            ivSystemIndicator = findViewById(R.id.iv_system_indicator);
            ivDetectionIndicator = findViewById(R.id.iv_detection_indicator);
            ivGameIndicator = findViewById(R.id.iv_game_indicator);
        } catch (Exception e) { 
            Log.w(TAG, "Status indicators not found in layout");
        }
        
        try {
            switchRootMode = findViewById(R.id.switch_root_mode);
        } catch (Exception e) { 
            Log.w(TAG, "switch_root_mode not found in layout");
        }
        
        try {
            tvFeedbackLink = findViewById(R.id.tv_feedback_link);
        } catch (Exception e) { 
            Log.w(TAG, "tv_feedback_link not found in layout");
        }
        
        // If main UI elements are not found, create simple fallback
        if (slideButton == null && btnHamburgerMenu == null) {
            createSimpleUI();
        }
    }
    
    /**
     * Create simple fallback UI if main layout elements are missing
     */
    private void createSimpleUI() {
        Log.i(TAG, "Creating simple fallback UI");
        
        // Create a simple button as fallback
        android.widget.Button simpleToggle = new android.widget.Button(this);
        simpleToggle.setText("Toggle System Helper");
        simpleToggle.setOnClickListener(v -> {
            if (systemHelperActive) {
                deactivateSystemHelper();
            } else {
                activateSystemHelper();
            }
        });
        
        // Try to add to main layout
        try {
            android.view.ViewGroup mainView = findViewById(android.R.id.content);
            if (mainView instanceof android.widget.LinearLayout) {
                ((android.widget.LinearLayout) mainView).addView(simpleToggle);
            } else if (mainView instanceof android.widget.FrameLayout) {
                ((android.widget.FrameLayout) mainView).addView(simpleToggle);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to add simple UI", e);
        }
    }
    
    /**
     * Setup all event listeners
     */
    private void setupEventListeners() {
        // Hamburger menu
        if (btnHamburgerMenu != null) {
            btnHamburgerMenu.setOnClickListener(v -> openSettings());
        }
        
        // Main slide button
        if (slideButton != null) {
            slideButton.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser && progress > 80) {
                        // Activate helper when slid to the end
                        if (!systemHelperActive) {
                            activateSystemHelper();
                        }
                    } else if (fromUser && progress < 20) {
                        // Deactivate helper when slid to start
                        if (systemHelperActive) {
                            deactivateSystemHelper();
                        }
                    }
                }
                
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}
                
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    // Snap to position based on current state
                    if (systemHelperActive) {
                        seekBar.setProgress(100);
                    } else {
                        seekBar.setProgress(0);
                    }
                }
            });
        }
        
        // Root mode switch
        if (switchRootMode != null) {
            switchRootMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked && !isRootMode) {
                    Toast.makeText(this, "Root access not available", Toast.LENGTH_SHORT).show();
                    switchRootMode.setChecked(false);
                } else {
                    isRootMode = isChecked;
                    updateUI();
                }
            });
        }
        
        // Feedback link
        if (tvFeedbackLink != null) {
            tvFeedbackLink.setOnClickListener(v -> openFeedback());
        }
    }
    
    /**
     * Check device capabilities and determine optimal mode
     */
    private void checkDeviceCapabilities() {
        // Check if device is rooted
        boolean hasRoot = RootUtils.isDeviceRooted() && RootUtils.hasRootAccess();
        
        // Update UI to show root availability
        if (ivRootIndicator != null) {
            ivRootIndicator.setVisibility(hasRoot ? android.view.View.VISIBLE : android.view.View.GONE);
        }
        
        if (switchRootMode != null) {
            switchRootMode.setEnabled(hasRoot);
        }
        
        // Log device info
        android.util.Log.i(TAG, "Device Info:");
        android.util.Log.i(TAG, "- Android Version: " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")");
        android.util.Log.i(TAG, "- Root Access: " + hasRoot);
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
        
        // All permissions granted
        updateUI();
    }
    
    /**
     * Request overlay permission
     */
    private void requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Overlay permission required for System Helper", Toast.LENGTH_LONG).show();
                
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
     * Activate system helper
     */
    private void activateSystemHelper() {
        if (!PermissionUtils.hasOverlayPermission(this)) {
            requestOverlayPermission();
            return;
        }
        
        Intent serviceIntent;
        
        if (isRootMode && RootUtils.hasRootAccess()) {
            // Use root overlay service for better performance
            serviceIntent = new Intent(this, SystemPrivilegeService.class);
            android.util.Log.i(TAG, "Starting Root Privilege Service");
        } else {
            // Use standard overlay service
            serviceIntent = new Intent(this, DisplayHelperService.class);
            android.util.Log.i(TAG, "Starting Display Helper Service");
        }
        
        serviceIntent.setAction("SHOW_OVERLAY");
        
        // Start foreground service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        
        systemHelperActive = true;
        overlayServiceRunning = true;
        
        Toast.makeText(this, "System Helper Activated", Toast.LENGTH_SHORT).show();
        updateUI();
    }
    
    /**
     * Deactivate system helper
     */
    private void deactivateSystemHelper() {
        Intent serviceIntent;
        
        if (isRootMode) {
            serviceIntent = new Intent(this, SystemPrivilegeService.class);
        } else {
            serviceIntent = new Intent(this, DisplayHelperService.class);
        }
        
        serviceIntent.setAction("HIDE_OVERLAY");
        startService(serviceIntent);
        
        systemHelperActive = false;
        overlayServiceRunning = false;
        
        Toast.makeText(this, "System Helper Deactivated", Toast.LENGTH_SHORT).show();
        updateUI();
    }
    
    /**
     * Open settings activity
     */
    private void openSettings() {
        Intent intent = new Intent(this, DeviceSettingsActivity.class);
        startActivity(intent);
    }
    
    /**
     * Open feedback
     */
    private void openFeedback() {
        Toast.makeText(this, "Feedback feature coming soon", Toast.LENGTH_SHORT).show();
        // TODO: Implement feedback mechanism
    }
    
    /**
     * Update UI based on current state
     */
    private void updateUI() {
        // Update slide button position
        if (slideButton != null) {
            slideButton.setProgress(systemHelperActive ? 100 : 0);
        }
        
        // Update status texts
        if (tvSystemStatus != null) {
            tvSystemStatus.setText(systemHelperActive ? 
                getString(R.string.status_active) : 
                getString(R.string.waiting_for_game));
        }
        
        if (tvDetectionStatus != null) {
            tvDetectionStatus.setText(PermissionUtils.hasOverlayPermission(this) ? 
                getString(R.string.detection_ready) : 
                "Permission required");
        }
        
        if (tvGameStatus != null) {
            tvGameStatus.setText(systemHelperActive ? 
                "Helper active" : 
                getString(R.string.game_not_detected));
        }
        
        // Update status indicators
        int activeColor = ContextCompat.getColor(this, android.R.color.holo_green_light);
        int inactiveColor = ContextCompat.getColor(this, android.R.color.darker_gray);
        int readyColor = ContextCompat.getColor(this, android.R.color.holo_blue_light);
        
        if (ivSystemIndicator != null) {
            ivSystemIndicator.setColorFilter(systemHelperActive ? activeColor : inactiveColor);
        }
        
        if (ivDetectionIndicator != null) {
            ivDetectionIndicator.setColorFilter(PermissionUtils.hasOverlayPermission(this) ? 
                readyColor : inactiveColor);
        }
        
        if (ivGameIndicator != null) {
            ivGameIndicator.setColorFilter(systemHelperActive ? activeColor : inactiveColor);
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            if (PermissionUtils.hasOverlayPermission(this)) {
                Toast.makeText(this, "Overlay permission granted", Toast.LENGTH_SHORT).show();
                updateUI();
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
            } else {
                Toast.makeText(this, "Storage permissions denied. Some features may not work.", Toast.LENGTH_LONG).show();
            }
            updateUI();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up if needed
    }
}
