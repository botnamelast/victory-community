package com.android.systemhelper.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import android.preference.PreferenceManager;
import java.util.ArrayList;
import java.util.List;
import com.android.systemhelper.utils.ProfileManager;
import com.android.systemhelper.services.DisplayHelperService;
import com.android.systemhelper.services.SystemPrivilegeService;

/**
 * Modern settings activity with real-time preview and profile management
 */
public class DeviceSettingsActivity extends Activity {
    private static final String TAG = "DeviceSettings";
    
    // UI Components
    private LinearLayout mainLayout;
    private ScrollView scrollView;
    private Button toggleOverlayButton;
    private Button rootModeButton;
    private SeekBar opacitySeekBar;
    private SeekBar sizeSeekBar;
    private SeekBar positionXSeekBar;
    private SeekBar positionYSeekBar;
    private Spinner colorSpinner;
    private Spinner profileSpinner;
    private Switch rootModeSwitch;
    private Switch framebufferSwitch;
    private Switch systemInjectionSwitch;
    private TextView previewText;
    private View previewOverlay;
    
    // Settings
    private SharedPreferences prefs;
    private ProfileManager profileManager;
    private boolean isOverlayActive = false;
    private boolean isRootMode = false;
    
    // Current settings
    private int currentOpacity = 80;
    private int currentSize = 100;
    private int currentX = 0;
    private int currentY = 100;
    private int currentColor = Color.RED;
    private String currentProfile = "Default";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        profileManager = new ProfileManager(this);
        
        createUI();
        loadSettings();
        setupEventListeners();
        
        Log.d(TAG, "DeviceSettingsActivity created");
    }

    private void createUI() {
        // Create main layout
        mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(20, 20, 20, 20);
        mainLayout.setBackgroundColor(Color.parseColor("#1E1E1E"));
        
        scrollView = new ScrollView(this);
        scrollView.addView(mainLayout);
        setContentView(scrollView);
        
        // Title
        TextView titleText = new TextView(this);
        titleText.setText("Victory Community Overlay Settings");
        titleText.setTextSize(24);
        titleText.setTextColor(Color.WHITE);
        titleText.setPadding(0, 0, 0, 30);
        mainLayout.addView(titleText);
        
        // Profile section
        createProfileSection();
        
        // Control buttons
        createControlSection();
        
        // Basic settings
        createBasicSettingsSection();
        
        // Root mode settings
        createRootModeSection();
        
        // Preview section
        createPreviewSection();
        
        // Action buttons
        createActionButtonsSection();
    }

    private void createProfileSection() {
        // Profile section header
        TextView profileHeader = createSectionHeader("Profile Management");
        mainLayout.addView(profileHeader);
        
        // Profile spinner
        LinearLayout profileLayout = new LinearLayout(this);
        profileLayout.setOrientation(LinearLayout.HORIZONTAL);
        profileLayout.setPadding(0, 10, 0, 20);
        
        TextView profileLabel = createLabel("Current Profile:");
        profileSpinner = new Spinner(this);
        setupProfileSpinner();
        
        Button saveProfileButton = new Button(this);
        saveProfileButton.setText("Save");
        saveProfileButton.setBackgroundColor(Color.parseColor("#4CAF50"));
        saveProfileButton.setTextColor(Color.WHITE);
        
        Button loadProfileButton = new Button(this);
        loadProfileButton.setText("Load");
        loadProfileButton.setBackgroundColor(Color.parseColor("#2196F3"));
        loadProfileButton.setTextColor(Color.WHITE);
        
        profileLayout.addView(profileLabel);
        profileLayout.addView(profileSpinner);
        profileLayout.addView(saveProfileButton);
        profileLayout.addView(loadProfileButton);
        mainLayout.addView(profileLayout);
        
        // Profile action listeners
        saveProfileButton.setOnClickListener(v -> saveCurrentProfile());
        loadProfileButton.setOnClickListener(v -> loadSelectedProfile());
    }

    private void createControlSection() {
        TextView controlHeader = createSectionHeader("Overlay Control");
        mainLayout.addView(controlHeader);
        
        LinearLayout controlLayout = new LinearLayout(this);
        controlLayout.setOrientation(LinearLayout.HORIZONTAL);
        controlLayout.setPadding(0, 10, 0, 20);
        
        toggleOverlayButton = new Button(this);
        toggleOverlayButton.setText("Start Overlay");
        toggleOverlayButton.setBackgroundColor(Color.parseColor("#4CAF50"));
        toggleOverlayButton.setTextColor(Color.WHITE);
        
        rootModeButton = new Button(this);
        rootModeButton.setText("Enable Root Mode");
        rootModeButton.setBackgroundColor(Color.parseColor("#FF9800"));
        rootModeButton.setTextColor(Color.WHITE);
        
        controlLayout.addView(toggleOverlayButton);
        controlLayout.addView(rootModeButton);
        mainLayout.addView(controlLayout);
    }

    private void createBasicSettingsSection() {
        TextView basicHeader = createSectionHeader("Basic Settings");
        mainLayout.addView(basicHeader);
        
        // Opacity setting
        createSeekBarSetting("Opacity", 0, 100, currentOpacity, (opacity) -> {
            currentOpacity = opacity;
            updatePreview();
            if (isOverlayActive) {
                updateOverlaySettings();
            }
        }, (seekBar) -> opacitySeekBar = seekBar);
        
        // Size setting
        createSeekBarSetting("Size", 50, 300, currentSize, (size) -> {
            currentSize = size;
            updatePreview();
            if (isOverlayActive) {
                updateOverlaySettings();
            }
        }, (seekBar) -> sizeSeekBar = seekBar);
        
        // Position X setting
        createSeekBarSetting("Position X", 0, 1000, currentX, (x) -> {
            currentX = x;
            updatePreview();
            if (isOverlayActive) {
                updateOverlaySettings();
            }
        }, (seekBar) -> positionXSeekBar = seekBar);
        
        // Position Y setting
        createSeekBarSetting("Position Y", 0, 2000, currentY, (y) -> {
            currentY = y;
            updatePreview();
            if (isOverlayActive) {
                updateOverlaySettings();
            }
        }, (seekBar) -> positionYSeekBar = seekBar);
        
        // Color setting
        LinearLayout colorLayout = new LinearLayout(this);
        colorLayout.setOrientation(LinearLayout.HORIZONTAL);
        colorLayout.setPadding(0, 10, 0, 10);
        
        TextView colorLabel = createLabel("Color:");
        colorSpinner = new Spinner(this);
        setupColorSpinner();
        
        colorLayout.addView(colorLabel);
        colorLayout.addView(colorSpinner);
        mainLayout.addView(colorLayout);
    }

    private void createRootModeSection() {
        TextView rootHeader = createSectionHeader("Root Mode Settings");
        mainLayout.addView(rootHeader);
        
        // Root mode switch
        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setPadding(0, 10, 0, 20);
        
        rootModeSwitch = new Switch(this);
        rootModeSwitch.setText("Enable Root Mode");
        rootModeSwitch.setTextColor(Color.WHITE);
        rootModeSwitch.setEnabled(false); // Will be enabled if root is available
        
        framebufferSwitch = new Switch(this);
        framebufferSwitch.setText("Direct Framebuffer Access");
        framebufferSwitch.setTextColor(Color.WHITE);
        framebufferSwitch.setEnabled(false);
        
        systemInjectionSwitch = new Switch(this);
        systemInjectionSwitch.setText("System-Level Injection");
        systemInjectionSwitch.setTextColor(Color.WHITE);
        systemInjectionSwitch.setEnabled(false);
        
        rootLayout.addView(rootModeSwitch);
        rootLayout.addView(framebufferSwitch);
        rootLayout.addView(systemInjectionSwitch);
        mainLayout.addView(rootLayout);
    }

    private void createPreviewSection() {
        TextView previewHeader = createSectionHeader("Live Preview");
        mainLayout.addView(previewHeader);
        
        // Preview container
        LinearLayout previewContainer = new LinearLayout(this);
        previewContainer.setOrientation(LinearLayout.VERTICAL);
        previewContainer.setPadding(10, 10, 10, 10);
        previewContainer.setBackgroundColor(Color.parseColor("#2E2E2E"));
        
        previewText = new TextView(this);
        previewText.setText("Preview will appear here");
        previewText.setTextColor(Color.WHITE);
        previewText.setPadding(0, 0, 0, 10);
        
        previewOverlay = new View(this);
        previewOverlay.setBackgroundColor(currentColor);
        previewOverlay.setAlpha(currentOpacity / 100.0f);
        
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(
            currentSize, currentSize);
        previewOverlay.setLayoutParams(previewParams);
        
        previewContainer.addView(previewText);
        previewContainer.addView(previewOverlay);
        mainLayout.addView(previewContainer);
    }

    private void createActionButtonsSection() {
        LinearLayout actionLayout = new LinearLayout(this);
        actionLayout.setOrientation(LinearLayout.HORIZONTAL);
        actionLayout.setPadding(0, 30, 0, 0);
        
        Button resetButton = new Button(this);
        resetButton.setText("Reset to Default");
        resetButton.setBackgroundColor(Color.parseColor("#F44336"));
        resetButton.setTextColor(Color.WHITE);
        
        Button exportButton = new Button(this);
        exportButton.setText("Export Settings");
        exportButton.setBackgroundColor(Color.parseColor("#9C27B0"));
        exportButton.setTextColor(Color.WHITE);
        
        Button importButton = new Button(this);
        importButton.setText("Import Settings");
        importButton.setBackgroundColor(Color.parseColor("#607D8B"));
        importButton.setTextColor(Color.WHITE);
        
        actionLayout.addView(resetButton);
        actionLayout.addView(exportButton);
        actionLayout.addView(importButton);
        mainLayout.addView(actionLayout);
        
        // Action listeners
        resetButton.setOnClickListener(v -> resetToDefaults());
        exportButton.setOnClickListener(v -> exportSettings());
        importButton.setOnClickListener(v -> importSettings());
    }

    private TextView createSectionHeader(String text) {
        TextView header = new TextView(this);
        header.setText(text);
        header.setTextSize(18);
        header.setTextColor(Color.parseColor("#4CAF50"));
        header.setPadding(0, 20, 0, 10);
        header.setAllCaps(true);
        return header;
    }

    private TextView createLabel(String text) {
        TextView label = new TextView(this);
        label.setText(text);
        label.setTextColor(Color.WHITE);
        label.setPadding(0, 0, 20, 0);
        label.setMinWidth(120);
        return label;
    }

    private void createSeekBarSetting(String label, int min, int max, int current, 
                                     SeekBarChangeListener listener, SeekBarCallback callback) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setPadding(0, 10, 0, 10);
        
        TextView labelView = createLabel(label + ":");
        SeekBar seekBar = new SeekBar(this);
        seekBar.setMax(max - min);
        seekBar.setProgress(current - min);
        
        TextView valueView = new TextView(this);
        valueView.setText(String.valueOf(current));
        valueView.setTextColor(Color.WHITE);
        valueView.setMinWidth(80);
        valueView.setPadding(20, 0, 0, 0);
        
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int value = progress + min;
                valueView.setText(String.valueOf(value));
                listener.onChanged(value);
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        layout.addView(labelView);
        layout.addView(seekBar);
        layout.addView(valueView);
        mainLayout.addView(layout);
        
        callback.onSeekBarCreated(seekBar);
    }

    private void setupProfileSpinner() {
        List<String> profiles = profileManager.getProfileNames();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_spinner_item, profiles);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        profileSpinner.setAdapter(adapter);
    }

    private void setupColorSpinner() {
        String[] colorNames = {"Red", "Green", "Blue", "Yellow", "Cyan", "Magenta", "White"};
        int[] colorValues = {Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, 
                           Color.CYAN, Color.MAGENTA, Color.WHITE};
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_spinner_item, colorNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        colorSpinner.setAdapter(adapter);
        
        colorSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentColor = colorValues[position];
                updatePreview();
                if (isOverlayActive) {
                    updateOverlaySettings();
                }
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupEventListeners() {
        toggleOverlayButton.setOnClickListener(v -> toggleOverlay());
        rootModeButton.setOnClickListener(v -> toggleRootMode());

        rootModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isRootMode = isChecked;
            framebufferSwitch.setEnabled(isChecked);
            systemInjectionSwitch.setEnabled(isChecked);
        });

        framebufferSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Menggunakan SystemPrivilegeService, bukan DisplayHelperService
                SystemPrivilegeService.enableDirectFramebuffer(this);
            }
            // Jika perlu, bisa juga handle switch off logic
        });

        systemInjectionSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Menggunakan SystemPrivilegeService, bukan DisplayHelperService
                SystemPrivilegeService.enableSystemInjection(this);
            }
            // Jika perlu, bisa juga handle switch off logic
        });
    }

    private void loadSettings() {
        currentOpacity = prefs.getInt("overlay_opacity", 80);
        currentSize = prefs.getInt("overlay_size", 100);
        currentX = prefs.getInt("overlay_x", 0);
        currentY = prefs.getInt("overlay_y", 100);
        currentColor = prefs.getInt("overlay_color", Color.RED);
        currentProfile = prefs.getString("current_profile", "Default");
        isRootMode = prefs.getBoolean("root_mode", false);
        
        updateUI();
    }

    private void saveSettings() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("overlay_opacity", currentOpacity);
        editor.putInt("overlay_size", currentSize);
        editor.putInt("overlay_x", currentX);
        editor.putInt("overlay_y", currentY);
        editor.putInt("overlay_color", currentColor);
        editor.putString("current_profile", currentProfile);
        editor.putBoolean("root_mode", isRootMode);
        editor.apply();
    }

    private void updateUI() {
        if (opacitySeekBar != null) opacitySeekBar.setProgress(currentOpacity);
        if (sizeSeekBar != null) sizeSeekBar.setProgress(currentSize - 50);
        if (positionXSeekBar != null) positionXSeekBar.setProgress(currentX);
        if (positionYSeekBar != null) positionYSeekBar.setProgress(currentY);
        rootModeSwitch.setChecked(isRootMode);
        updatePreview();
    }

    private void updatePreview() {
        if (previewOverlay != null) {
            previewOverlay.setBackgroundColor(currentColor);
            previewOverlay.setAlpha(currentOpacity / 100.0f);
            
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                currentSize, currentSize);
            previewOverlay.setLayoutParams(params);
            
            previewText.setText(String.format(
                "Size: %dx%d, Position: (%d,%d), Opacity: %d%%", 
                currentSize, currentSize, currentX, currentY, currentOpacity));
        }
    }

    private void toggleOverlay() {
        if (isOverlayActive) {
            if (isRootMode) {
                // STOP root mode overlay
                stopService(new Intent(this, SystemPrivilegeService.class));
            } else {
                DisplayHelperService.stopOverlay(this);
            }
            toggleOverlayButton.setText("Start Overlay");
            toggleOverlayButton.setBackgroundColor(Color.parseColor("#4CAF50"));
            isOverlayActive = false;
        } else {
            if (isRootMode) {
                // START root mode overlay, gunakan method yang benar dan ada
                SystemPrivilegeService.startPrivilegedService(this);
            } else {
                DisplayHelperService.startOverlay(this);
            }
            toggleOverlayButton.setText("Stop Overlay");
            toggleOverlayButton.setBackgroundColor(Color.parseColor("#F44336"));
            isOverlayActive = true;
            updateOverlaySettings();
        }
    }

    private void toggleRootMode() {
        isRootMode = !isRootMode;
        rootModeSwitch.setChecked(isRootMode);
        rootModeButton.setText(isRootMode ? "Disable Root Mode" : "Enable Root Mode");
        rootModeButton.setBackgroundColor(isRootMode ? 
            Color.parseColor("#F44336") : Color.parseColor("#FF9800"));
    }

    private void updateOverlaySettings() {
        if (isRootMode) {
            // Update root overlay settings
            // This would require extending RootOverlayService with update methods
        } else {
            DisplayHelperService.updateOverlay(this, currentX, currentY, 
                currentSize, currentSize, currentOpacity / 100.0f);
        }
    }

    private void saveCurrentProfile() {
        profileManager.saveProfile(currentProfile, currentOpacity, currentSize, 
            currentX, currentY, currentColor);
        Toast.makeText(this, "Profile saved: " + currentProfile, Toast.LENGTH_SHORT).show();
    }

    private void loadSelectedProfile() {
        String selectedProfile = (String) profileSpinner.getSelectedItem();
        if (profileManager.loadProfile(selectedProfile)) {
            // Load profile data and update UI
            currentProfile = selectedProfile;
            loadSettings();
            Toast.makeText(this, "Profile loaded: " + selectedProfile, Toast.LENGTH_SHORT).show();
        }
    }

    private void resetToDefaults() {
        currentOpacity = 80;
        currentSize = 100;
        currentX = 0;
        currentY = 100;
        currentColor = Color.RED;
        updateUI();
        saveSettings();
        Toast.makeText(this, "Settings reset to defaults", Toast.LENGTH_SHORT).show();
    }

    private void exportSettings() {
        // TODO: Implement settings export functionality
        Toast.makeText(this, "Export functionality coming soon", Toast.LENGTH_SHORT).show();
    }

    private void importSettings() {
        // TODO: Implement settings import functionality
        Toast.makeText(this, "Import functionality coming soon", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveSettings();
    }

    // Interfaces
    private interface SeekBarChangeListener {
        void onChanged(int value);
    }

    private interface SeekBarCallback {
        void onSeekBarCreated(SeekBar seekBar);
    }
}
