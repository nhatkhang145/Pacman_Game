package com.example.pacmangame.model;

import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * SoundManager – Quản lý toàn bộ âm thanh của game Pac-Man.
 *
 * Phân loại:
 *   - Nhạc nền (Music): intro (menu) + background (in-game) – phát lặp vô tận
 *   - Hiệu ứng (SFX)  : chomp, powerup, eatghost, death    – phát một lần
 *
 * Mỗi loại có thể bật/tắt độc lập qua SettingsManager.isMusicEnabled()
 * và SettingsManager.isSfxEnabled().
 */
public class SoundManager {

    private static SoundManager instance;

    // --- SFX clips ---
    private final Map<String, AudioClip> sfxClips = new HashMap<>();

    // --- Music players ---
    private MediaPlayer introPlayer;
    private MediaPlayer bgPlayer;
    private String currentMusic = "";   // "intro" | "background" | ""

    // --------------------------------------------------------
    // Singleton
    // --------------------------------------------------------

    private SoundManager() {
        loadSfx("chomp",    "sounds/chomp.wav");
        loadSfx("death",    "sounds/death.wav");
        loadSfx("eatghost", "sounds/eatghost.wav");
        loadSfx("powerup",  "sounds/powerup.wav");

        introPlayer = buildMusicPlayer("sounds/intro.wav");
        bgPlayer    = buildMusicPlayer("sounds/background.wav");

        // Intro chỉ phát 1 lần; background phát lặp vô tận
        if (introPlayer != null) {
            introPlayer.setCycleCount(1);
            introPlayer.setOnEndOfMedia(this::onIntroEnded);
        }
        if (bgPlayer != null) {
            bgPlayer.setCycleCount(MediaPlayer.INDEFINITE);
        }
    }

    public static SoundManager getInstance() {
        if (instance == null) {
            instance = new SoundManager();
        }
        return instance;
    }

    // --------------------------------------------------------
    // Nhạc nền (Music)
    // --------------------------------------------------------

    /**
     * Phát nhạc nền Intro (màn hình menu).
     * Nếu nhạc đang tắt hoặc đã đang phát intro → không làm gì.
     */
    public void playIntro() {
        if (!SettingsManager.getInstance().isMusicEnabled()) return;
        if ("intro".equals(currentMusic)) return;

        stopAllMusic();
        if (introPlayer == null) return;

        currentMusic = "intro";
        applyMusicVolume(introPlayer);
        introPlayer.play();
    }

    /**
     * Phát nhạc nền in-game (lặp vô tận).
     */
    public void playBackground() {
        if (!SettingsManager.getInstance().isMusicEnabled()) return;
        if ("background".equals(currentMusic)) return;

        stopAllMusic();
        if (bgPlayer == null) return;

        currentMusic = "background";
        applyMusicVolume(bgPlayer);
        bgPlayer.play();
    }

    /** Dừng toàn bộ nhạc nền, giữ nguyên vị trí */
    public void pauseMusic() {
        if (introPlayer != null) introPlayer.pause();
        if (bgPlayer    != null) bgPlayer.pause();
    }

    /** Tiếp tục nhạc nền (nếu đang pause) */
    public void resumeMusic() {
        if (!SettingsManager.getInstance().isMusicEnabled()) return;
        if ("intro".equals(currentMusic) && introPlayer != null) {
            applyMusicVolume(introPlayer);
            introPlayer.play();
        } else if ("background".equals(currentMusic) && bgPlayer != null) {
            applyMusicVolume(bgPlayer);
            bgPlayer.play();
        }
    }

    /** Dừng hẳn và reset về đầu */
    public void stopAllMusic() {
        if (introPlayer != null) { introPlayer.stop(); }
        if (bgPlayer    != null) { bgPlayer.stop(); }
        currentMusic = "";
    }

    /**
     * Gọi khi người dùng thay đổi trạng thái Music enable/disable.
     */
    public void onMusicSettingChanged() {
        boolean enabled = SettingsManager.getInstance().isMusicEnabled();
        if (!enabled) {
            pauseMusic();
        } else {
            // Phát lại nhạc tương ứng với trạng thái cuối
            resumeMusic();
        }
    }

    /**
     * Cập nhật volume nhạc nền khi người dùng kéo slider.
     */
    public void onVolumeChanged() {
        double vol = SettingsManager.getInstance().getVolume();
        if (introPlayer != null) introPlayer.setVolume(vol);
        if (bgPlayer    != null) bgPlayer.setVolume(vol);
    }

    // --------------------------------------------------------
    // SFX
    // --------------------------------------------------------

    /**
     * Phát hiệu ứng âm thanh một lần.
     * Kiểm tra sfxEnabled + muted trước khi phát.
     *
     * @param name tên clip: "chomp" | "powerup" | "eatghost" | "death"
     */
    public void playSound(String name) {
        SettingsManager sm = SettingsManager.getInstance();
        if (!sm.isSfxEnabled() || sm.isMuted()) return;

        AudioClip clip = sfxClips.get(name);
        if (clip == null) return;

        clip.setVolume(sm.getVolume());
        // Tránh chomp bị xếp chồng quá nhiều
        if ("chomp".equals(name) && clip.isPlaying()) return;
        clip.play();
    }

    // --------------------------------------------------------
    // Private helpers
    // --------------------------------------------------------

    private void loadSfx(String name, String relativePath) {
        try {
            File file = new File("src/main/resources/" + relativePath);
            if (file.exists()) {
                sfxClips.put(name, new AudioClip(file.toURI().toString()));
            } else {
                System.out.println("[SoundManager] SFX không tìm thấy: " + relativePath);
            }
        } catch (Exception e) {
            System.out.println("[SoundManager] Lỗi load SFX: " + relativePath);
        }
    }

    private MediaPlayer buildMusicPlayer(String relativePath) {
        try {
            File file = new File("src/main/resources/" + relativePath);
            if (!file.exists()) {
                System.out.println("[SoundManager] Nhạc không tìm thấy: " + relativePath);
                return null;
            }
            Media media = new Media(file.toURI().toString());
            return new MediaPlayer(media);
        } catch (Exception e) {
            System.out.println("[SoundManager] Lỗi load nhạc: " + relativePath);
            return null;
        }
    }

    private void applyMusicVolume(MediaPlayer player) {
        player.setVolume(SettingsManager.getInstance().getVolume());
    }

    /** Callback khi intro kết thúc: không tự chuyển sang background */
    private void onIntroEnded() {
        currentMusic = "";
    }
}
