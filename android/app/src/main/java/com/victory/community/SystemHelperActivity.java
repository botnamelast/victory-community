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
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;
import android.util.Log;
import android.graphics.Color;
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
 * System Helper - Main Activity with Full Functionality
 * Overlay assistance app for system interaction
 */
public class SystemHelperActivity extends AppCompatActivity {
    
    private static final String TAG = "SystemHelper";
    private static final int REQUEST_OVERLAY_PERMISSION = 1001;
    private static final int REQUEST_STORAGE_PERMISSION = 1002;
    
    // UI Components
    private TextView tvStatus;
    private Button btnToggleOverlay;
    private Button btnSettings;
    private Button btnAbout;
    private Switch switchRootMode;
    
    // Status Indicators
    private View indicatorOverlay;
    private View indicatorPermissions;
    private View indicatorRoot;
    
    // State variables
    private boolean isOverlayActive = false;
    private boolean isRootMode = false;
    private boolean hasOverlayPermission = false;
    private boolean hasRootAccess = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_system_helper);
        
        // Initialize UI components
        initializeViews();
        
        // Check system capabilities
        checkSystemCapabilities();
        
        // Setup event listeners
        setupEventListeners();
        
        // Update initial UI state
        updateUI();
        
        Log.i(TAG, "SystemHelperActivity initialized");
    }
    
    /**
     * Initialize all UI views
     */
    private void initializeViews() {
        // Main elements
        tvStatus = findViewById(R.id.tv_status);
        btnToggleOverlay = findViewById(R.id.btn_toggle_overlay);
        btnSettings = findViewById(R.id.btn_settings);
        btnAbout = findViewById(R.id.btn_about);
        switchRootMode = findViewById(R.id.switch_root_mode);
        
        // Status indicators
        indicatorOverlay = findViewById(R.id.indicator_overlay);
        indicatorPermissions = findViewById(R.id.indicator_permissions);
        indicatorRoot = findViewById(R.id.indicator_root);
        
        Log.i(TAG, "UI views initialized");
    }
    
    /**
     * Check system capabilities and permissions
     */
    private void checkSystemCapabilities() {
        // Check overlay permission
        hasOverlayPermission = PermissionUtils.hasOverlayPermission(this);
        
        // Check root access
        hasRootAccess = RootUtils.isDeviceRooted() && RootUtils.hasRootAccess();
        
        // Enable/disable root mode switch
        if (switchRootMode != null) {
            switchRootMode.setEnabled(hasRootAccess);
            if (!hasRootAccess) {
                switchRootMode.setChecked(false);
                isRootMode = false;
            }
        }
        
        Log.i(TAG, "System capabilities checked - Overlay: " + hasOverlayPermission + ", Root: " + hasRootAccess);
    }
    
    /**
     * Setup all event listeners
     */
    private void setupEventListeners() {
        // Main toggle button
        if (btnToggleOverlay != null) {
            btnToggleOverlay.setOnClickListener(v -> toggleOverlayService());
        }
        
        // Settings button
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> openSettings());
        }
        
        // About button
        if (btnAbout != null) {
            btnAbout.setOnClickListener(v -> showAbout());
        }
        
        // Root mode switch
        if (switchRootMode != null) {
            switchRootMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked && !hasRootAccess) {
                    Toast.makeText(this, "Root access not available on this device", Toast.LENGTH_SHORT).show();
                    switchRootMode.setChecked(false);
                    return;
                }
                
                isRootMode = isChecked;
                updateUI();
                
                // If overlay is active, restart with new mode
                if (isOverlayActive) {
                    stopOverlayService();
                    startOverlayService();
                }
            });
        }
    }
    
    /**
     * Toggle overlay service on/off
     */
    private void toggleOverlayService() {
        if (isOverlayActive) {
            stopOverlayService();
        } else {
            if (!hasOverlayPermission) {
                requestOverlayPermission();
                return;
            }
            startOverlayService();
        }
    }
    
    /**
     * Start the overlay service
     */
    private void startOverlayService() {
        try {
            Intent serviceIntent;
            
            if (isRootMode && hasRootAccess) {
                // Use root privileged service
                serviceIntent = new Intent(this, SystemPrivilegeService.class);
                Log.i(TAG, "Starting SystemPrivilegeService (Root Mode)");
            } else {
                // Use standard display helper service
                serviceIntent = new Intent(this, DisplayHelperService.class);
                Log.i(TAG, "Starting DisplayHelperService (Standard Mode)");
            }
            
            serviceIntent.setAction("SHOW_OVERLAY");
            
            // Start foreground service
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
            
            isOverlayActive = true;
            updateUI();
            
            Toast.makeText(this, "System Helper activated", Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to start overlay service", e);
            Toast.makeText(this, "Failed to start overlay service: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * Stop the overlay service
     */
    private void stopOverlayService() {
        try {
            Intent serviceIntent;
            
            if (isRootMode && hasRootAccess) {
                serviceIntent = new Intent(this, SystemPrivilegeService.class);
            } else {
                serviceIntent = new Intent(this, DisplayHelperService.class);
            }
            
            serviceIntent.setAction("HIDE_OVERLAY");
            startService(serviceIntent);
            
            isOverlayActive = false;
            updateUI();
            
            Toast.makeText(this, "System Helper deactivated", Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to stop overlay service", e);
            Toast.makeText(this, "Failed to stop overlay service: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * Request overlay permission
     */
    private void requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Please grant overlay permission to use System Helper", Toast.LENGTH_LONG).show();
                
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION);
            }
        } else {
            // For older Android versions, permission is granted automatically
            hasOverlayPermission = true;
            updateUI();
        }
    }
    
    /**
     * Open settings activity
     */
    private void openSettings() {
        try {
            Intent intent = new Intent(this, DeviceSettingsActivity.class);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to open settings", e);
            Toast.makeText(this, "Settings not available", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Show about dialog
     */
    private void showAbout() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        
        String aboutText = "System Helper v1.0.0\n\n" +
                          "A system overlay assistance tool\n\n" +
                          "Features:\n" +
                          "• Screen overlay assistance\n" +
                          "• Root mode support\n" +
                          "• Customizable settings\n\n" +
                          "Mode: " + (isRootMode ? "Root Privileged" : "Standard") + "\n" +
                          "Android: " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")";
        
        builder.setTitle("About System Helper")
                .setMessage(aboutText)
                .setPositiveButton("OK", null)
                .setNeutralButton("GitHub", (dialog, which) -> {
                    // TODO: Open GitHub repository if needed
                    Toast.makeText(this, "GitHub repository: github.com/victory-community", Toast.LENGTH_SHORT).show();
                })
                .show();
    }
    
    /**
     * Update UI based on current state
     */
    private void updateUI() {
        // Update main status text
        if (tvStatus != null) {
            StringBuilder statusText = new StringBuilder();
            statusText.append("Status: ").append(isOverlayActive ? "ACTIVE" : "INACTIVE").append("\n");
            
            if (!hasOverlayPermission) {
                statusText.append("⚠ Overlay permission required\n");
            }
            
            if (hasRootAccess) {
                statusText.append("Mode: ").append(isRootMode ? "ROOT PRIVILEGED" : "STANDARD").append("\n");
                statusText.append("Root access: Available");
            } else {
                statusText.append("Mode: STANDARD\n");
                statusText.append("Root access: Not available");
            }
            
            tvStatus.setText(statusText.toString());
        }
        
        // Update toggle button
        if (btnToggleOverlay != null) {
            btnToggleOverlay.setText(isOverlayActive ? "Stop System Helper" : "Start System Helper");
            
            // Change button color based on state
            if (isOverlayActive) {
                btnToggleOverlay.setBackgroundColor(Color.parseColor("#DA3633")); // Red
            } else if (hasOverlayPermission) {
                btnToggleOverlay.setBackgroundColor(Color.parseColor("#238636")); // Green
            } else {
                btnToggleOverlay.setBackgroundColor(Color.parseColor("#FD7E14")); // Orange
            }
        }
        
        // Update status indicators
        updateStatusIndicators();
        
        Log.d(TAG, "UI updated - Active: " + isOverlayActive + ", Root: " + isRootMode + ", Permission: " + hasOverlayPermission);
    }
    
    /**
     * Update visual status indicators
     */
    private void updateStatusIndicators() {
        // Overlay indicator
        if (indicatorOverlay != null) {
            if (isOverlayActive) {
                indicatorOverlay.setBackgroundColor(Color.parseColor("#238636")); // Green - Active
            } else if (hasOverlayPermission) {
                indicatorOverlay.setBackgroundColor(Color.parseColor("#FD7E14")); // Orange - Ready
            } else {
                indicatorOverlay.setBackgroundColor(Color.parseColor("#DA3633")); // Red - No permission
            }
        }
        
        // Permissions indicator
        if (indicatorPermissions != null) {
            indicatorPermissions.setBackgroundColor(hasOverlayPermission ? 
                Color.parseColor("#238636") : Color.parseColor("#DA3633"));
        }
        
        // Root indicator
        if (indicatorRoot != null) {
            if (hasRootAccess && isRootMode) {
                indicatorRoot.setBackgroundColor(Color.parseColor("#238636")); // Green - Active
            } else if (hasRootAccess) {
                indicatorRoot.setBackgroundColor(Color.parseColor("#FD7E14")); // Orange - Available
            } else {
                indicatorRoot.setBackgroundColor(Color.parseColor("#6F7681")); // Gray - Not available
            }
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            hasOverlayPermission = PermissionUtils.hasOverlayPermission(this);
            
            if (hasOverlayPermission) {
                Toast.makeText(this, "Overlay permission granted! You can now start the helper.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Overlay permission denied. The app won't work without this permission.", Toast.LENGTH_LONG).show();
            }
            
            updateUI();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        // Recheck permissions and capabilities when returning to app
        checkSystemCapabilities();
        updateUI();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Stop overlay service when app is destroyed
        if (isOverlayActive) {
            stopOverlayService();
        }
        
        Log.i(TAG, "SystemHelperActivity destroyed");
    }
}
