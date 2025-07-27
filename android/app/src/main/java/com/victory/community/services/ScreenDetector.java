package com.victorycommunity.overlay;

import android.accessibilityservice.AccessibilityService;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Detects game windows, screen properties, and provides optimal overlay positioning
 */
public class ScreenDetector {
    private static final String TAG = "VictoryScreenDetector";
    
    private Context context;
    private WindowManager windowManager;
    private DisplayManager displayManager;
    private ActivityManager activityManager;
    private PackageManager packageManager;
    
    // Screen properties
    private int screenWidth;
    private int screenHeight;
    private float screenDensity;
    private int screenOrientation;
    private List<Display> availableDisplays;
    
    // Game detection
    private Map<String, GameProfile> knownGames;
    private String currentGamePackage;
    private GameProfile currentGameProfile;
    private boolean isGameActive = false;
    
    // Callbacks
    private ScreenDetectionCallback callback;

    public ScreenDetector(Context context) {
        this.context = context;
        this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        this.displayManager = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
        this.activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        this.packageManager = context.getPackageManager();
        
        this.knownGames = new HashMap<>();
        this.availableDisplays = new ArrayList<>();
        
        initializeScreenProperties();
        initializeKnownGames();
        detectDisplays();
        
        Log.d(TAG, "ScreenDetector initialized");
    }

    /**
     * Game profile containing optimization settings
     */
    public static class GameProfile {
        public String packageName;
        public String gameName;
        public int targetWidth;
        public int targetHeight;
        public float aspectRatio;
        public boolean isFullscreen;
        public boolean hasNotch;
        public Point optimalOverlayPosition;
        public int overlaySize;
        public float overlayOpacity;
        public boolean requiresRootMode;
        public int refreshRate;
        
        public GameProfile(String packageName, String gameName) {
            this.packageName = packageName;
            this.gameName = gameName;
            this.targetWidth = 1920;
            this.targetHeight = 1080;
            this.aspectRatio = 16.0f / 9.0f;
            this.isFullscreen = true;
            this.hasNotch = false;
            this.optimalOverlayPosition = new Point(100, 100);
            this.overlaySize = 100;
            this.overlayOpacity = 0.8f;
            this.requiresRootMode = false;
            this.refreshRate = 60;
        }
    }

    /**
     * Screen detection callback interface
     */
    public interface ScreenDetectionCallback {
        void onGameDetected(GameProfile gameProfile);
        void onGameLost();
        void onScreenPropertiesChanged(int width, int height, int orientation);
        void onOptimalPositionCalculated(Point position, int size);
    }

    /**
     * Initialize screen properties
     */
    private void initializeScreenProperties() {
        Display display = windowManager.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getRealMetrics(metrics);
        
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;
        screenDensity = metrics.density;
        screenOrientation = display.getRotation();
        
        Log.d(TAG, String.format("Screen initialized: %dx%d, density=%.2f, orientation=%d", 
              screenWidth, screenHeight, screenDensity, screenOrientation));
    }

    /**
     * Initialize known game profiles - 8 Ball Pool ONLY
     */
    private void initializeKnownGames() {
        // SINGLE TARGET: 8 Ball Pool by Miniclip
        // Optimized specifically for pool game mechanics
        addGameProfile("com.miniclip.eightballpool", "8 Ball Pool", 1920, 1080, true, false,
                      new Point(200, 300), 70, 0.6f, false, 60);
        
        // Default fallback (if somehow not detected)
        GameProfile defaultProfile = new GameProfile("default", "8 Ball Pool Default");
        defaultProfile.optimalOverlayPosition = new Point(200, 300);
        defaultProfile.overlaySize = 70;
        defaultProfile.overlayOpacity = 0.6f;
        knownGames.put("default", defaultProfile);
        
        Log.d(TAG, "Initialized 8 Ball Pool specific profile");
    }

    /**
     * Add game profile to known games
     */
    private void addGameProfile(String packageName, String gameName, int width, int height,
                               boolean fullscreen, boolean hasNotch, Point overlayPos, 
                               int overlaySize, float opacity, boolean needsRoot, int refreshRate) {
        GameProfile profile = new GameProfile(packageName, gameName);
        profile.targetWidth = width;
        profile.targetHeight = height;
        profile.aspectRatio = (float) width / height;
        profile.isFullscreen = fullscreen;
        profile.hasNotch = hasNotch;
        profile.optimalOverlayPosition = overlayPos;
        profile.overlaySize = overlaySize;
        profile.overlayOpacity = opacity;
        profile.requiresRootMode = needsRoot;
        profile.refreshRate = refreshRate;
        
        knownGames.put(packageName, profile);
    }

    /**
     * Detect available displays
     */
    private void detectDisplays() {
        availableDisplays.clear();
        Display[] displays = displayManager.getDisplays();
        
        for (Display display : displays) {
            availableDisplays.add(display);
            DisplayMetrics metrics = new DisplayMetrics();
            display.getRealMetrics(metrics);
            
            Log.d(TAG, String.format("Display %d: %dx%d, density=%.2f", 
                  display.getDisplayId(), metrics.widthPixels, metrics.heightPixels, metrics.density));
        }
    }

    /**
     * Start game detection
     */
    public void startDetection() {
        // Start monitoring for game activities
        startGameMonitoring();
        Log.d(TAG, "Game detection started");
    }

    /**
     * Stop game detection
     */
    public void stopDetection() {
        // Stop monitoring
        isGameActive = false;
        currentGamePackage = null;
        currentGameProfile = null;
        Log.d(TAG, "Game detection stopped");
    }

    /**
     * Start monitoring for game activities
     */
    private void startGameMonitoring() {
        // This would typically use AccessibilityService or UsageStatsManager
        // For now, we'll simulate detection
        new Thread(() -> {
            while (true) {
                try {
                    String topPackage = getCurrentTopPackage();
                    if (topPackage != null && !topPackage.equals(currentGamePackage)) {
                        handlePackageChange(topPackage);
                    }
                    Thread.sleep(1000); // Check every second
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();
    }

    /**
     * Get current top package (requires proper permissions)
     */
    private String getCurrentTopPackage() {
        try {
            List<ActivityManager.RunningAppProcessInfo> processes = 
                activityManager.getRunningAppProcesses();
            
            if (processes != null && !processes.isEmpty()) {
                for (ActivityManager.RunningAppProcessInfo process : processes) {
                    if (process.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                        return process.processName;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get top package", e);
        }
        return null;
    }

    /**
     * Handle package change detection
     */
    private void handlePackageChange(String packageName) {
        if (isGamePackage(packageName)) {
            currentGamePackage = packageName;
            currentGameProfile = knownGames.getOrDefault(packageName, knownGames.get("default"));
            isGameActive = true;
            
            // Calculate optimal overlay position for current screen
            Point optimalPosition = calculateOptimalPosition(currentGameProfile);
            int optimalSize = calculateOptimalSize(currentGameProfile);
            
            if (callback != null) {
                callback.onGameDetected(currentGameProfile);
                callback.onOptimalPositionCalculated(optimalPosition, optimalSize);
            }
            
            Log.d(TAG, "Game detected: " + currentGameProfile.gameName);
        } else {
            if (isGameActive) {
                isGameActive = false;
                currentGamePackage = null;
                currentGameProfile = null;
                
                if (callback != null) {
                    callback.onGameLost();
                }
                
                Log.d(TAG, "Game lost");
            }
        }
    }

    /**
     * Check if package is a game
     */
    private boolean isGamePackage(String packageName) {
        try {
            ApplicationInfo appInfo = packageManager.getApplicationInfo(packageName, 0);
            
            // Check if it's a known game
            if (knownGames.containsKey(packageName)) {
                return true;
            }
            
            // Check if it's categorized as a game
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                return appInfo.category == ApplicationInfo.CATEGORY_GAME;
            }
            
            // Fallback: check app name for game keywords
            String appName = packageManager.getApplicationLabel(appInfo).toString().toLowerCase();
            return appName.contains("game") || appName.contains("play") || 
                   appName.contains("battle") || appName.contains("shooter");
                   
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /**
     * Calculate optimal overlay position based on game profile and current screen
     */
    private Point calculateOptimalPosition(GameProfile gameProfile) {
        if (gameProfile == null) {
            return calculateCenterPosition(100);
        }

        // Scale position based on current screen vs target resolution
        float scaleX = (float) screenWidth / gameProfile.targetWidth;
        float scaleY = (float) screenHeight / gameProfile.targetHeight;
        
        int scaledX = (int) (gameProfile.optimalOverlayPosition.x * scaleX);
        int scaledY = (int) (gameProfile.optimalOverlayPosition.y * scaleY);
        
        // Adjust for screen orientation
        if (screenOrientation == 1 || screenOrientation == 3) { // Portrait or reverse portrait
            // Swap coordinates for portrait mode
            int temp = scaledX;
            scaledX = scaledY;
            scaledY = temp;
        }
        
        // Ensure position is within screen bounds
        scaledX = Math.max(0, Math.min(scaledX, screenWidth - gameProfile.overlaySize));
        scaledY = Math.max(0, Math.min(scaledY, screenHeight - gameProfile.overlaySize));
        
        return new Point(scaledX, scaledY);
    }

    /**
     * Calculate optimal overlay size
     */
    private int calculateOptimalSize(GameProfile gameProfile) {
        if (gameProfile == null) {
            return 100;
        }

        // Scale size based on screen density and resolution
        float scaleFactor = Math.min((float) screenWidth / gameProfile.targetWidth,
                                   (float) screenHeight / gameProfile.targetHeight);
        
        int scaledSize = (int) (gameProfile.overlaySize * scaleFactor * screenDensity);
        
        // Clamp size to reasonable bounds
        return Math.max(50, Math.min(scaledSize, 200));
    }

    /**
     * Calculate center position for given overlay size
     */
    private Point calculateCenterPosition(int overlaySize) {
        int centerX = (screenWidth - overlaySize) / 2;
        int centerY = (screenHeight - overlaySize) / 2;
        return new Point(centerX, centerY);
    }

    /**
     * Get safe area bounds (avoiding notches, navigation bars)
     */
    public Rect getSafeAreaBounds() {
        Rect safeArea = new Rect(0, 0, screenWidth, screenHeight);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Display display = windowManager.getDefaultDisplay();
            if (display != null && display.getCutout() != null) {
                // Adjust for display cutout (notch)
                safeArea.top += display.getCutout().getSafeInsetTop();
                safeArea.bottom -= display.getCutout().getSafeInsetBottom();
                safeArea.left += display.getCutout().getSafeInsetLeft();
                safeArea.right -= display.getCutout().getSafeInsetRight();
            }
        }
        
        // Adjust for navigation bar (approximate)
        safeArea.bottom -= 48 * (int) screenDensity; // 48dp navigation bar height
        
        return safeArea;
    }

    /**
     * Get current screen properties
     */
    public ScreenProperties getCurrentScreenProperties() {
        ScreenProperties properties = new ScreenProperties();
        properties.width = screenWidth;
        properties.height = screenHeight;
        properties.density = screenDensity;
        properties.orientation = screenOrientation;
        properties.aspectRatio = (float) screenWidth / screenHeight;
        properties.safeArea = getSafeAreaBounds();
        return properties;
    }

    /**
     * Check if current screen is suitable for overlay
     */
    public boolean isScreenSuitableForOverlay() {
        // Check minimum screen size
        if (screenWidth < 720 || screenHeight < 1280) {
            return false;
        }
        
        // Check if we have enough space for overlay
        Rect safeArea = getSafeAreaBounds();
        int availableWidth = safeArea.width();
        int availableHeight = safeArea.height();
        
        return availableWidth > 200 && availableHeight > 200;
    }

    /**
     * Set detection callback
     */
    public void setCallback(ScreenDetectionCallback callback) {
        this.callback = callback;
    }

    /**
     * Get current game profile
     */
    public GameProfile getCurrentGameProfile() {
        return currentGameProfile;
    }

    /**
     * Get all known games
     */
    public Map<String, GameProfile> getKnownGames() {
        return new HashMap<>(knownGames);
    }

    /**
     * Add custom game profile
     */
    public void addCustomGameProfile(GameProfile profile) {
        knownGames.put(profile.packageName, profile);
        Log.d(TAG, "Added custom game profile: " + profile.gameName);
    }

    /**
     * Screen properties data class
     */
    public static class ScreenProperties {
        public int width;
        public int height;
        public float density;
        public int orientation;
        public float aspectRatio;
        public Rect safeArea;
    }
}

