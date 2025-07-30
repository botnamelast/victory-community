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
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;
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
    
    // UI Components - supports both simple and complex layouts
    private Button btnToggleHelper;
    private Button btnSettings;
    private Button btnAbout;
    private Switch switchRootMode;
    private TextView tvSystemStatus, tvDetectionStatus, tvGameStatus;
    
    // Legacy UI components (for complex layout compatibility)
    private ImageButton btnHamburgerMenu;
    private SeekBar slideButton;
    private ImageView ivRootIndicator, ivSystemIndicator, ivDetectionIndicator, ivGameIndicator;
    private TextView tvFeedbackLink;
    
    // State variables
    private boolean isRootMode = false;
    private boolean overlayServiceRunning = false;
    private boolean systemHelperActive = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_system_helper); // FIXED: Use correct layout
        
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
     * Initialize all UI views - using actual layout elements
     */
    private void initializeViews() {
        // Get elements from activity_system_helper.xml
        TextView tvStatus = findViewById(R.id.tv_status);
        Button btnToggleOverlay = findViewById(R.id.btn_toggle_overlay);
        btnSettings = findViewById(R.id.btn_settings);
        btnAbout = findViewById(R.id.btn_about);
        
        // Map to our internal variables for consistency
        tvSystemStatus = tvStatus; // Use tv_status as system status display
        btnToggleHelper = btnToggleOverlay; // Use btn_toggle_overlay as main toggle
        
        Log.i(TAG, "Using activity_system_helper.xml layout");
        
        // Try to find additional elements (these might not exist, that's OK)
        try {
            switchRootMode = findViewById(R.id.switch_root_mode);
            tvDetectionStatus = findViewById(R.id.tv_detection_status);
            tvGameStatus = findViewById(R.id.tv_game_status);
        } catch (Exception e) {
            Log.d(TAG, "Optional UI elements not found: " + e.getMessage());
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
     * Setup all event listeners - for activity_system_helper.xml
     */
    private void setupEventListeners() {
        // Main toggle button (btn_toggle_overlay)
        if (btnToggleHelper != null) {
            btnToggleHelper.setOnClickListener(v -> {
                if (systemHelperActive) {
                    deactivateSystemHelper();
                } else {
                    activateSystemHelper();
                }
            });
        }
        
        // Settings button
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> openSettings());
        }
        
        // About button
        if (btnAbout != null) {
            btnAbout.setOnClickListener(v -> showAbout());
        }
        
        // Root mode switch (if exists in layout)
        if (switchRootMode != null) {
            switchRootMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
                boolean hasRoot = RootUtils.isDeviceRooted() && RootUtils.hasRootAccess();
                if (isChecked && !hasRoot) {
                    Toast.makeText(this, "Root access not available", Toast.LENGTH_SHORT).show();
                    switchRootMode.setChecked(false);
                } else {
                    isRootMode = isChecked;
                    updateUI();
                }
            });
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
     * Show about dialog
     */
    private void showAbout() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("System Helper")
                .setMessage("Open source system helper app\n\nVersion: 1.0.0\nMode: " + (isRootMode ? "Root" : "Standard"))
                .setPositiveButton("OK", null)
                .show();
    }
    
    /**
     * Update UI based on current state - simplified for activity_system_helper.xml
     */
    private void updateUI() {
        // Update main toggle button
        if (btnToggleHelper != null) {
            btnToggleHelper.setText(systemHelperActive ? "Stop System Helper" : "Start System Helper");
        }
        
        // Update status text (tv_status)
        if (tvSystemStatus != null) {
            String statusText = "Status: " + (systemHelperActive ? "Active" : "Not Active");
            
            // Add permission info
            if (!PermissionUtils.hasOverlayPermission(this)) {
                statusText += "\n(Overlay permission required)";
            }
            
            // Add root info
            boolean hasRoot = RootUtils.isDeviceRooted() && RootUtils.hasRootAccess();
            if (hasRoot && isRootMode) {
                statusText += "\nMode: Root";
            } else if (hasRoot) {
                statusText += "\nMode: Standard (Root available)";
            } else {
                statusText += "\nMode: Standard";
            }
            
            tvSystemStatus.setText(statusText);
        }
        
        // Update additional status texts if they exist
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
        
        // Note: No complex UI indicators to update since we're using simple layout
        Log.d(TAG, "UI updated - Active: " + systemHelperActive + ", Root: " + isRootMode);
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
