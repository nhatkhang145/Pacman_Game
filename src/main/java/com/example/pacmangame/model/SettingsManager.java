package com.example.pacmangame.model;

import javafx.scene.input.KeyCode;


public class SettingsManager {


    private static SettingsManager instance;



    // Các ngôn ngữ được hỗ trợ trong giao diện game
    public enum Language {
        VI,
        EN
    }
    public enum InputDevice {
        KEYBOARD,
        GAMEPAD
    }
    private double volume = 1.0;
    private boolean isMuted = false;

    /** Bật/tắt nhạc nền (Intro + In-game music) */
    private boolean musicEnabled = true;

    /** Bật/tắt hiệu ứng âm thanh (waka, powerup, eatghost, death) */
    private boolean sfxEnabled = true;

    /** Ngôn ngữ hiện tại của giao diện */
    private Language language = Language.VI;

    // Trạng thái toàn màn hình
    private boolean fullscreen = false;

    //  Điều khiển (UC-14)
    private InputDevice inputDevice = InputDevice.KEYBOARD;

    private KeyCode keyUp    = KeyCode.UP;
    private KeyCode keyDown  = KeyCode.DOWN;
    private KeyCode keyLeft  = KeyCode.LEFT;
    private KeyCode keyRight = KeyCode.RIGHT;

    /**
     * UC-14 – Phím tạm dừng / tiếp tục game.
     * Mặc định là ESC; người chơi có thể đổi sang phím khác (ví dụ P).
     */
    private KeyCode keyPause = KeyCode.ESCAPE;




    private SettingsManager() {}

    public static SettingsManager getInstance() {
        if (instance == null) {
            instance = new SettingsManager();
        }
        return instance;
    }

    // Getter / Setter – Âm thanh

    /** @return mức âm lượng [0.0 – 1.0] */
    public double getVolume() { return volume; }

    public void setVolume(double volume) { this.volume = volume; }


    public boolean isMuted() { return isMuted; }

    public void setMuted(boolean muted) { this.isMuted = muted; }

    /** @return true nếu nhạc nền đang được bật */
    public boolean isMusicEnabled() { return musicEnabled; }

    /** Bật/tắt nhạc nền */
    public void setMusicEnabled(boolean musicEnabled) { this.musicEnabled = musicEnabled; }

    /** @return true nếu hiệu ứng âm thanh đang được bật */
    public boolean isSfxEnabled() { return sfxEnabled; }

    /** Bật/tắt hiệu ứng âm thanh */
    public void setSfxEnabled(boolean sfxEnabled) { this.sfxEnabled = sfxEnabled; }



    public Language getLanguage() { return language; }


    public void setLanguage(Language language) { this.language = language; }

    public boolean isFullscreen() { return fullscreen; }

    public void setFullscreen(boolean fullscreen) { this.fullscreen = fullscreen; }


    /**
     * UC-14 – Lấy thiết bị đầu vào đang được chọn.
     * @return KEYBOARD hoặc GAMEPAD
     */
    public InputDevice getInputDevice() { return inputDevice; }

    /**
     * UC-14 – Chọn thiết bị đầu vào.
     * @param inputDevice thiết bị mới
     */
    public void setInputDevice(InputDevice inputDevice) { this.inputDevice = inputDevice; }

    // --- Phím hướng ---
    public KeyCode getKeyUp() { return keyUp; }
    public void setKeyUp(KeyCode k) { this.keyUp = k; }
    public KeyCode getKeyDown() { return keyDown; }
    public void setKeyDown(KeyCode k) { this.keyDown = k; }
    public KeyCode getKeyLeft() { return keyLeft; }
    public void setKeyLeft(KeyCode k) { this.keyLeft = k; }
    public KeyCode getKeyRight() { return keyRight; }
    public void setKeyRight(KeyCode k) { this.keyRight = k; }

    // --- Phím Pause ---
    /** UC-14 @return phím tạm dừng hiện tại */
    public KeyCode getKeyPause() { return keyPause; }
    /** UC-14 @param k phím mới cho chức năng Pause */
    public void setKeyPause(KeyCode k) { this.keyPause = k; }


    /**
     * UC-17 – Khôi phục TẤT CẢ cài đặt về giá trị mặc định ban đầu.
     * Bao gồm: âm thanh, ngôn ngữ, theme, fullscreen, và điều khiển.
     */
    public void resetToDefaults() {
        volume        = 1.0;
        isMuted       = false;
        musicEnabled  = true;
        sfxEnabled    = true;
        language      = Language.VI;
        fullscreen    = false;
        inputDevice   = InputDevice.KEYBOARD;
        keyUp         = KeyCode.UP;
        keyDown       = KeyCode.DOWN;
        keyLeft       = KeyCode.LEFT;
        keyRight      = KeyCode.RIGHT;
        keyPause      = KeyCode.ESCAPE;
    }
}
