package com.android.systemhelper.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;

/**
 * Manages overlay configuration profiles
 * Handles saving, loading, importing, and exporting overlay settings
 */
public class ProfileManager {
    private static final String TAG = "VictoryProfileManager";
    private static final String PREFS_NAME = "victory_profiles";
    private static final String PROFILES_KEY = "saved_profiles";
    private static final String DEFAULT_PROFILE = "Default";
    
    private Context context;
    private SharedPreferences prefs;
    private List<OverlayProfile> profiles;

    public ProfileManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.profiles = new ArrayList<>();
        loadProfiles();
        
        // Ensure default profile exists
        if (!hasProfile(DEFAULT_PROFILE)) {
            createDefaultProfile();
        }
    }

    /**
     * Overlay profile data class
     */
    public static class OverlayProfile {
        public String name;
        public int opacity;
        public int size;
        public int positionX;
        public int positionY;
        public int color;
        public boolean rootMode;
        public boolean directFramebuffer;
        public boolean systemInjection;
        public int refreshRate;
        public long createdTime;
        public long modifiedTime;

        public OverlayProfile(String name) {
            this.name = name;
            this.opacity = 80;
            this.size = 100;
            this.positionX = 0;
            this.positionY = 100;
            this.color = Color.RED;
            this.rootMode = false;
            this.directFramebuffer = false;
            this.systemInjection = false;
            this.refreshRate = 60;
            this.createdTime = System.currentTimeMillis();
            this.modifiedTime = this.createdTime;
        }

        public JSONObject toJSON() throws JSONException {
            JSONObject json = new JSONObject();
            json.put("name", name);
            json.put("opacity", opacity);
            json.put("size", size);
            json.put("positionX", positionX);
            json.put("positionY", positionY);
            json.put("color", color);
            json.put("rootMode", rootMode);
            json.put("directFramebuffer", directFramebuffer);
            json.put("systemInjection", systemInjection);
            json.put("refreshRate", refreshRate);
            json.put("createdTime", createdTime);
            json.put("modifiedTime", modifiedTime);
            return json;
        }

        public static OverlayProfile fromJSON(JSONObject json) throws JSONException {
            OverlayProfile profile = new OverlayProfile(json.getString("name"));
            profile.opacity = json.getInt("opacity");
            profile.size = json.getInt("size");
            profile.positionX = json.getInt("positionX");
            profile.positionY = json.getInt("positionY");
            profile.color = json.getInt("color");
            profile.rootMode = json.optBoolean("rootMode", false);
            profile.directFramebuffer = json.optBoolean("directFramebuffer", false);
            profile.systemInjection = json.optBoolean("systemInjection", false);
            profile.refreshRate = json.optInt("refreshRate", 60);
            profile.createdTime = json.optLong("createdTime", System.currentTimeMillis());
            profile.modifiedTime = json.optLong("modifiedTime", profile.createdTime);
            return profile;
        }
    }

    /**
     * Save a new profile or update existing one
     */
    public void saveProfile(String name, int opacity, int size, int x, int y, int color) {
        OverlayProfile profile = findProfile(name);
        if (profile == null) {
            profile = new OverlayProfile(name);
            profiles.add(profile);
        }
        
        profile.opacity = opacity;
        profile.size = size;
        profile.positionX = x;
        profile.positionY = y;
        profile.color = color;
        profile.modifiedTime = System.currentTimeMillis();
        
        saveProfiles();
        Log.d(TAG, "Profile saved: " + name);
    }

    /**
     * Save profile with advanced settings
     */
    public void saveAdvancedProfile(String name, int opacity, int size, int x, int y, int color,
                                   boolean rootMode, boolean directFramebuffer, 
                                   boolean systemInjection, int refreshRate) {
        OverlayProfile profile = findProfile(name);
        if (profile == null) {
            profile = new OverlayProfile(name);
            profiles.add(profile);
        }
        
        profile.opacity = opacity;
        profile.size = size;
        profile.positionX = x;
        profile.positionY = y;
        profile.color = color;
        profile.rootMode = rootMode;
        profile.directFramebuffer = directFramebuffer;
        profile.systemInjection = systemInjection;
        profile.refreshRate = refreshRate;
        profile.modifiedTime = System.currentTimeMillis();
        
        saveProfiles();
        Log.d(TAG, "Advanced profile saved: " + name);
    }

    /**
     * Load a profile by name
     */
    public boolean loadProfile(String name) {
        OverlayProfile profile = findProfile(name);
        if (profile != null) {
            // Apply profile settings to SharedPreferences
            SharedPreferences appPrefs = context.getSharedPreferences("victory_settings", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = appPrefs.edit();
            
            editor.putInt("overlay_opacity", profile.opacity);
            editor.putInt("overlay_size", profile.size);
            editor.putInt("overlay_x", profile.positionX);
            editor.putInt("overlay_y", profile.positionY);
            editor.putInt("overlay_color", profile.color);
            editor.putBoolean("root_mode", profile.rootMode);
            editor.putBoolean("direct_framebuffer", profile.directFramebuffer);
            editor.putBoolean("system_injection", profile.systemInjection);
            editor.putInt("refresh_rate", profile.refreshRate);
            editor.putString("current_profile", name);
            
            editor.apply();
            Log.d(TAG, "Profile loaded: " + name);
            return true;
        }
        return false;
    }

    /**
     * Delete a profile
     */
    public boolean deleteProfile(String name) {
        if (DEFAULT_PROFILE.equals(name)) {
            Log.w(TAG, "Cannot delete default profile");
            return false;
        }
        
        OverlayProfile profile = findProfile(name);
        if (profile != null) {
            profiles.remove(profile);
            saveProfiles();
            Log.d(TAG, "Profile deleted: " + name);
            return true;
        }
        return false;
    }

    /**
     * Get profile by name
     */
    public OverlayProfile getProfile(String name) {
        return findProfile(name);
    }

    /**
     * Get all profile names
     */
    public List<String> getProfileNames() {
        List<String> names = new ArrayList<>();
        for (OverlayProfile profile : profiles) {
            names.add(profile.name);
        }
        return names;
    }

    /**
     * Get all profiles
     */
    public List<OverlayProfile> getAllProfiles() {
        return new ArrayList<>(profiles);
    }

    /**
     * Check if profile exists
     */
    public boolean hasProfile(String name) {
        return findProfile(name) != null;
    }

    /**
     * Duplicate a profile with new name
     */
    public boolean duplicateProfile(String sourceName, String newName) {
        if (hasProfile(newName)) {
            Log.w(TAG, "Profile already exists: " + newName);
            return false;
        }
        
        OverlayProfile source = findProfile(sourceName);
        if (source != null) {
            OverlayProfile duplicate = new OverlayProfile(newName);
            duplicate.opacity = source.opacity;
            duplicate.size = source.size;
            duplicate.positionX = source.positionX;
            duplicate.positionY = source.positionY;
            duplicate.color = source.color;
            duplicate.rootMode = source.rootMode;
            duplicate.directFramebuffer = source.directFramebuffer;
            duplicate.systemInjection = source.systemInjection;
            duplicate.refreshRate = source.refreshRate;
            
            profiles.add(duplicate);
            saveProfiles();
            Log.d(TAG, "Profile duplicated: " + sourceName + " -> " + newName);
            return true;
        }
        return false;
    }

    /**
     * Export profiles to JSON file
     */
    public boolean exportProfiles(String filePath) {
        try {
            JSONArray jsonArray = new JSONArray();
            for (OverlayProfile profile : profiles) {
                jsonArray.put(profile.toJSON());
            }
            
            JSONObject exportData = new JSONObject();
            exportData.put("version", 1);
            exportData.put("exportTime", System.currentTimeMillis());
            exportData.put("profiles", jsonArray);
            
            File file = new File(filePath);
            FileWriter writer = new FileWriter(file);
            writer.write(exportData.toString(2));
            writer.close();
            
            Log.d(TAG, "Profiles exported to: " + filePath);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to export profiles", e);
            return false;
        }
    }

    /**
     * Import profiles from JSON file
     */
    public boolean importProfiles(String filePath, boolean overwrite) {
        try {
            File file = new File(filePath);
            BufferedReader reader = new BufferedReader(new FileReader(file));
            StringBuilder jsonBuilder = new StringBuilder();
            String line;
            
            while ((line = reader.readLine()) != null) {
                jsonBuilder.append(line);
            }
            reader.close();
            
            JSONObject importData = new JSONObject(jsonBuilder.toString());
            JSONArray jsonArray = importData.getJSONArray("profiles");
            
            int importedCount = 0;
            int skippedCount = 0;
            
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject profileJson = jsonArray.getJSONObject(i);
                OverlayProfile profile = OverlayProfile.fromJSON(profileJson);
                
                if (hasProfile(profile.name) && !overwrite) {
                    skippedCount++;
                    continue;
                }
                
                // Remove existing profile if overwriting
                if (hasProfile(profile.name)) {
                    profiles.removeIf(p -> p.name.equals(profile.name));
                }
                
                profiles.add(profile);
                importedCount++;
            }
            
            saveProfiles();
            Log.d(TAG, String.format("Import completed: %d imported, %d skipped", 
                  importedCount, skippedCount));
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to import profiles", e);
            return false;
        }
    }

    /**
     * Get profile statistics
     */
    public ProfileStats getProfileStats() {
        ProfileStats stats = new ProfileStats();
        stats.totalProfiles = profiles.size();
        stats.rootModeProfiles = 0;
        stats.oldestProfile = Long.MAX_VALUE;
        stats.newestProfile = 0;
        
        for (OverlayProfile profile : profiles) {
            if (profile.rootMode) {
                stats.rootModeProfiles++;
            }
            if (profile.createdTime < stats.oldestProfile) {
                stats.oldestProfile = profile.createdTime;
                stats.oldestProfileName = profile.name;
            }
            if (profile.createdTime > stats.newestProfile) {
                stats.newestProfile = profile.createdTime;
                stats.newestProfileName = profile.name;
            }
        }
        
        return stats;
    }

    /**
     * Clean up old profiles (older than specified days)
     */
    public int cleanupOldProfiles(int daysOld) {
        long cutoffTime = System.currentTimeMillis() - (daysOld * 24 * 60 * 60 * 1000L);
        List<OverlayProfile> toRemove = new ArrayList<>();
        
        for (OverlayProfile profile : profiles) {
            if (!DEFAULT_PROFILE.equals(profile.name) && profile.modifiedTime < cutoffTime) {
                toRemove.add(profile);
            }
        }
        
        profiles.removeAll(toRemove);
        if (!toRemove.isEmpty()) {
            saveProfiles();
        }
        
        Log.d(TAG, "Cleaned up " + toRemove.size() + " old profiles");
        return toRemove.size();
    }

    /**
     * Create default profile
     */
    private void createDefaultProfile() {
        OverlayProfile defaultProfile = new OverlayProfile(DEFAULT_PROFILE);
        profiles.add(defaultProfile);
        saveProfiles();
        Log.d(TAG, "Default profile created");
    }

    /**
     * Find profile by name
     */
    private OverlayProfile findProfile(String name) {
        for (OverlayProfile profile : profiles) {
            if (profile.name.equals(name)) {
                return profile;
            }
        }
        return null;
    }

    /**
     * Load profiles from SharedPreferences
     */
    private void loadProfiles() {
        profiles.clear();
        String profilesJson = prefs.getString(PROFILES_KEY, "[]");
        
        try {
            JSONArray jsonArray = new JSONArray(profilesJson);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject profileJson = jsonArray.getJSONObject(i);
                OverlayProfile profile = OverlayProfile.fromJSON(profileJson);
                profiles.add(profile);
            }
            Log.d(TAG, "Loaded " + profiles.size() + " profiles");
        } catch (JSONException e) {
            Log.e(TAG, "Failed to load profiles", e);
        }
    }

    /**
     * Save profiles to SharedPreferences
     */
    private void saveProfiles() {
        try {
            JSONArray jsonArray = new JSONArray();
            for (OverlayProfile profile : profiles) {
                jsonArray.put(profile.toJSON());
            }
            
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(PROFILES_KEY, jsonArray.toString());
            editor.apply();
            
            Log.d(TAG, "Saved " + profiles.size() + " profiles");
        } catch (JSONException e) {
            Log.e(TAG, "Failed to save profiles", e);
        }
    }

    /**
     * Profile statistics class
     */
    public static class ProfileStats {
        public int totalProfiles;
        public int rootModeProfiles;
        public long oldestProfile;
        public long newestProfile;
        public String oldestProfileName;
        public String newestProfileName;
    }
}
