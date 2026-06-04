package com.example.pacmangame.model;

public class SettingsManager {
    private static SettingsManager instance;

    public enum Language {
        VI, EN
    }

    private double volume = 1.0;
    private boolean isMuted = false;
    private Language language = Language.VI;

    private SettingsManager() {}

    public static SettingsManager getInstance() {
        if (instance == null) {
            instance = new SettingsManager();
        }
        return instance;
    }

    public double getVolume() { return volume; }
    public void setVolume(double volume) { this.volume = volume; }
    
    public boolean isMuted() { return isMuted; }
    public void setMuted(boolean muted) { this.isMuted = muted; }
    
    public Language getLanguage() { return language; }
    public void setLanguage(Language language) { this.language = language; }
}
