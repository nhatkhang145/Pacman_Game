package com.example.pacmangame;

import javafx.scene.media.AudioClip;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class SoundManager {
    private static SoundManager instance;
    private Map<String, AudioClip> clips;
    
    private SoundManager() {
        clips = new HashMap<>();
        loadSound("chomp", "sounds/chomp.wav");
        loadSound("death", "sounds/death.wav");
        loadSound("eatghost", "sounds/eatghost.wav");
        loadSound("powerup", "sounds/powerup.wav");
    }
    
    public static SoundManager getInstance() {
        if (instance == null) {
            instance = new SoundManager();
        }
        return instance;
    }
    
    private void loadSound(String name, String filePath) {
        try {
            File file = new File("src/main/resources/" + filePath);
            if (file.exists()) {
                AudioClip clip = new AudioClip(file.toURI().toString());
                clips.put(name, clip);
            } else {
                System.out.println("Warning: Sound file not found: " + filePath + ". Please add it to hear sounds.");
            }
        } catch (Exception e) {
            System.out.println("Could not load sound: " + filePath);
        }
    }
    
    public void playSound(String name) {
        if (SettingsManager.getInstance().isMuted()) return;
        
        AudioClip clip = clips.get(name);
        if (clip != null) {
            clip.setVolume(SettingsManager.getInstance().getVolume());
            // Prevent ear-rape by not playing if it's already playing (useful for chomp)
            if (name.equals("chomp") && clip.isPlaying()) return;
            clip.play();
        }
    }
}
