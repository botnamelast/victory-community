package com.victory.community.services;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.graphics.Rect;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import java.util.List;

/**
 * Victory Community Accessibility Service
 * Enhanced game detection and UI analysis based on EasyVictory insights
 */
public class AccessibilityHelperService extends AccessibilityService {
    private static final String TAG = "AccessibilityHelper";
    
    // Game detection callback
    private GameDetectionCallback gameCallback;
    
    // Current game state
    private String currentGamePackage;
    private boolean isGameInForeground = false;
    private Rect gameWindowBounds;
    
    // Performance tracking
    private long lastEventTime = 0;
    private int eventCount = 0;

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        
        // Configure accessibility service
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED |
                         AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED |
                         AccessibilityEvent.TYPE_VIEW_FOCUSED;
        
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS |
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
        
        // Polling interval for better performance (inspired by EasyVictory)
        info.notificationTimeout = 100; // 100ms for responsive detection
        
        setServiceInfo(info);
        
        Log.d(TAG, "AccessibilityHelperService connected");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Performance throttling
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastEventTime < 50) { // Max 20 events/second
            return;
        }
        lastEventTime = currentTime;
        eventCount++;
        
        try {
            processAccessibilityEvent(event);
        } catch (Exception e) {
            Log.e(TAG, "Error processing accessibility event", e);
        }
    }

    /**
     * Process accessibility events for game detection
     */
    private void processAccessibilityEvent(AccessibilityEvent event) {
        switch (event.getEventType()) {
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                handleWindowStateChanged(event);
                break;
                
            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
                handleWindowContentChanged(event);
                break;
                
            case AccessibilityEvent.TYPE_VIEW_FOCUSED:
                handleViewFocused(event);
                break;
        }
    }

    /**
     * Handle window state changes - primary game detection method
     */
    private void handleWindowStateChanged(AccessibilityEvent event) {
        CharSequence packageName = event.getPackageName();
        CharSequence className = event.getClassName();
        
        if (packageName == null) return;
        
        String packageStr = packageName.toString();
        
        // Check if this is a game package
        if (isGamePackage(packageStr)) {
            if (!packageStr.equals(currentGamePackage)) {
                // New game detected
                currentGamePackage = packageStr;
                isGameInForeground = true;
                analyzeGameWindow();
                
                if (gameCallback != null) {
                    gameCallback.onGameDetected(packageStr, gameWindowBounds);
                }
                
                Log.d(TAG, "Game detected: " + packageStr);
            }
        } else {
            // Non-game app in foreground
            if (isGameInForeground) {
                isGameInForeground = false;
                currentGamePackage = null;
                
                if (gameCallback != null) {
                    gameCallback.onGameLost();
                }
                
                Log.d(TAG, "Game lost focus");
            }
        }
    }

    /**
     * Handle window content changes - for UI element detection
     */
    private void handleWindowContentChanged(AccessibilityEvent event) {
        if (!isGameInForeground) return;
        
        // Analyze game UI elements for optimal overlay positioning
        AccessibilityNodeInfo rootNode = event.getSource();
        if (rootNode != null) {
            analyzeGameUIElements(rootNode);
            rootNode.recycle();
        }
    }

    /**
     * Handle view focus changes - for interactive element detection
     */
    private void handleViewFocused(AccessibilityEvent event) {
        if (!isGameInForeground) return;
        
        // Track focused elements to avoid overlay conflicts
        AccessibilityNodeInfo focusedNode = event.getSource();
        if (focusedNode != null) {
            Rect bounds = new Rect();
            focusedNode.getBoundsInScreen(bounds);
            
            if (gameCallback != null) {
                gameCallback.onUIElementFocused(bounds);
            }
            
            focusedNode.recycle();
        }
    }

    /**
     * Analyze game window properties
     */
    private void analyzeGameWindow() {
        List<AccessibilityWindowInfo> windows = getWindows();
        
        for (AccessibilityWindowInfo window : windows) {
            if (window.getType() == AccessibilityWindowInfo.TYPE_APPLICATION) {
                AccessibilityNodeInfo rootNode = window.getRoot();
                if (rootNode != null && currentGamePackage.equals(rootNode.getPackageName())) {
                    // Found game window
                    gameWindowBounds = new Rect();
                    rootNode.getBoundsInScreen(gameWindowBounds);
                    
                    Log.d(TAG, "Game window bounds: " + gameWindowBounds.toString());
                    rootNode.recycle();
                    break;
                }
            }
        }
    }

    /**
     * Analyze game UI elements for smart overlay positioning
     */
    private void analyzeGameUIElements(AccessibilityNodeInfo rootNode) {
        // Recursive analysis of UI elements
        analyzeNodeRecursive(rootNode, 0);
    }

    /**
     * Recursively analyze accessibility nodes
     */
    private void analyzeNodeRecursive(AccessibilityNodeInfo node, int depth) {
        if (node == null || depth > 10) return; // Prevent deep recursion
        
        // Analyze clickable elements (buttons, controls)
        if (node.isClickable()) {
            Rect bounds = new Rect();
            node.getBoundsInScreen(bounds);
            
            // Report UI element to avoid overlay conflicts
            if (gameCallback != null) {
                gameCallback.onUIElementDetected(bounds, node.getClassName().toString());
            }
        }
        
        // Analyze child nodes
        int childCount = node.getChildCount();
        for (int i = 0; i < childCount; i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                analyzeNodeRecursive(child, depth + 1);
                child.recycle();
            }
        }
    }

    /**
     * Check if package is 8 Ball Pool (focused detection)
     */
    private boolean isGamePackage(String packageName) {
        // ONLY target: 8 Ball Pool by Miniclip
        return "com.miniclip.eightballpool".equals(packageName);
    }

    /**
     * Get optimal overlay position based on UI analysis
     */
    public Rect getOptimalOverlayZone() {
        if (gameWindowBounds == null) {
            return new Rect(100, 100, 200, 200); // Default fallback
        }
        
        // Calculate safe zone (avoiding detected UI elements)
        int safeX = gameWindowBounds.left + (gameWindowBounds.width() / 6);
        int safeY = gameWindowBounds.top + (gameWindowBounds.height() / 3);
        int safeSize = Math.min(gameWindowBounds.width(), gameWindowBounds.height()) / 8;
        
        return new Rect(safeX, safeY, safeX + safeSize, safeY + safeSize);
    }

    /**
     * Get current game detection statistics
     */
    public GameDetectionStats getDetectionStats() {
        return new GameDetectionStats(
            currentGamePackage,
            isGameInForeground,
            eventCount,
            gameWindowBounds
        );
    }

    @Override
    public void onInterrupt() {
        Log.w(TAG, "AccessibilityHelperService interrupted");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "AccessibilityHelperService destroyed");
    }

    /**
     * Set game detection callback
     */
    public void setGameDetectionCallback(GameDetectionCallback callback) {
        this.gameCallback = callback;
    }

    /**
     * Game detection callback interface
     */
    public interface GameDetectionCallback {
        void onGameDetected(String packageName, Rect windowBounds);
        void onGameLost();
        void onUIElementDetected(Rect bounds, String elementType);
        void onUIElementFocused(Rect bounds);
    }

    /**
     * Game detection statistics
     */
    public static class GameDetectionStats {
        public final String currentGame;
        public final boolean isGameActive;
        public final int totalEvents;
        public final Rect gameWindow;
        
        public GameDetectionStats(String currentGame, boolean isGameActive, 
                                int totalEvents, Rect gameWindow) {
            this.currentGame = currentGame;
            this.isGameActive = isGameActive;
            this.totalEvents = totalEvents;
            this.gameWindow = gameWindow;
        }
    }
}

