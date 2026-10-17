package com.yingtu.plus.setting;

import com.yingtu.plus.util.Prefers;

public class PlayerSetting {
    public static final int EXO = 0;
    public static final int IJK = 1;
    public static final int MPV = 2;
    public static final int RENDER_SURFACE = 0;
    public static final int RENDER_TEXTURE = 1;
    
    // Audio background constants
    public static final int AUDIO_BACKGROUND_ARTWORK = 0;
    public static final int AUDIO_BACKGROUND_DARK_NEON = 1;
    public static final int AUDIO_BACKGROUND_BLACK_GOLD = 2;
    public static final int AUDIO_BACKGROUND_RANDOM = 12;

    public static int getPlayer() { 
        int p = Prefers.getInt("player", EXO); 
        return isPlayer(p) ? p : EXO; 
    }
    
    public static void putPlayer(int p) { Prefers.put("player", sanitizePlayer(p)); }
    
    public static boolean isPlayer(int p) {
        return p >= EXO && p <= MPV;
    }
    
    public static int sanitizePlayer(int p) {
        return Math.max(EXO, Math.min(p, MPV));
    }
    
    public static int nextPlayer() {
        int current = getPlayer();
        return (current + 1) % 3;
    }

    public static int getRender() { return Prefers.getInt("render", RENDER_SURFACE); }
    public static int getScale() { return Prefers.getInt("scale", 1); }
    public static void putScale(int s) { Prefers.putInt("scale", s); }
    
    public static int getBuffer() { return Prefers.getInt("buffer", 1024 * 1024); }
    public static long getBufferBytes() { return getBuffer(); }
    
    public static int getBackBufferOption() { return Prefers.getInt("back_buffer_option", 0); }
    public static int getBackBufferMs() { return Prefers.getInt("back_buffer_ms", 0); }
    
    public static double getDefaultSpeed() { return Prefers.getDouble("default_speed", 1.0); }
    public static void putDefaultSpeed(double d) { Prefers.put("default_speed", d); }
    
    public static double getSpeed() { return Prefers.getDouble("speed", 1.0); }
    
    public static boolean isAutoChange() { return Prefers.getBoolean("auto_change", false); }
    public static boolean isCaption() { return Prefers.getBoolean("caption", false); }
    public static boolean isBackgroundOff() { return Prefers.getBoolean("background_off", false); }
    public static boolean isBlurayMenu() { return Prefers.getBoolean("bluray_menu", false); }
    public static boolean isTunnel() { return Prefers.getBoolean("tunnel", false); }
    public static boolean isPreferAAC() { return Prefers.getBoolean("prefer_aac", false); }
    public static boolean isAudioPassThrough() { return Prefers.getBoolean("audio_passthrough", false); }
    public static boolean isAudioBackgroundDecorated() { return Prefers.getBoolean("audio_background_decorated", true); }
    public static boolean isImmersiveAudioMode() { return Prefers.getBoolean("immersive_audio_mode", false); }
    
    public static int getAudioBackground() { return Prefers.getInt("audio_background", AUDIO_BACKGROUND_ARTWORK); }
    public static void putAudioBackground(int v) { Prefers.put("audio_background", v); }
    public static int getAudioBackgroundSeed() { return Prefers.getInt("audio_background_seed", 0x5A17B3); }
    public static void putAudioBackgroundSeed(int seed) { Prefers.put("audio_background_seed", seed); }
    public static int getAudioBackgroundDecorationSeed() { return Prefers.getInt("audio_background_decoration_seed", getAudioBackgroundSeed()); }
    public static void putAudioBackgroundDecorationSeed(int seed) { Prefers.put("audio_background_decoration_seed", seed); }
    public static boolean isAudioBackgroundLightEffect() { return Prefers.getBoolean("audio_background_light_effect", false); }
    public static void putAudioBackgroundLightEffect(boolean v) { Prefers.put("audio_background_light_effect", v); }
    public static void putAudioBackgroundDecorated(boolean v) { Prefers.put("audio_background_decorated", v); }

    public static int getSubtitlePosition() { return Prefers.getInt("subtitle_position", 0); }
    public static float getSubtitleTextSize() { return Prefers.getFloat("subtitle_text_size", 1.0f); }
    
    public static int getEpisodesColumn() { return Prefers.getInt("episode_column", 2); }
    public static void putEpisodesColumn(int n) { Prefers.put("episode_column", n); }
    
    public static boolean isDesktopLyrics() { return Prefers.getBoolean("desktop_lyrics", false); }
    public static void putDesktopLyrics(boolean v) { Prefers.put("desktop_lyrics", v); }
    public static int getDesktopLyricsX() { return Prefers.getInt("desktop_lyrics_x", 0); }
    public static int getDesktopLyricsY() { return Prefers.getInt("desktop_lyrics_y", 0); }
    public static void putDesktopLyricsPosition(int x, int y) { 
        Prefers.put("desktop_lyrics_x", x); 
        Prefers.put("desktop_lyrics_y", y); 
    }
    
    public static int getLyricsRows() { return Prefers.getInt("lyrics_rows", 3); }
    public static void putLyricsRows(int n) { Prefers.put("lyrics_rows", n); }
    public static int getLyricsTextSizeOption() { return Prefers.getInt("lyrics_text_size", 0); }
    public static void putLyricsTextSizeOption(int o) { Prefers.put("lyrics_text_size", o); }
    public static int getLyricsTimeOffsetMs() { return Prefers.getInt("lyrics_time_offset", 0); }
    public static void putLyricsTimeOffsetMs(int ms) { Prefers.put("lyrics_time_offset", ms); }
    
    public static boolean isKaraokeMode() { return Prefers.getBoolean("karaoke_mode", false); }
    public static void putKaraokeMode(boolean v) { Prefers.put("karaoke_mode", v); }
    public static int getKaraokeDifficulty() { return Prefers.getInt("karaoke_difficulty", 0); }
    public static void putKaraokeDifficulty(int d) { Prefers.put("karaoke_difficulty", d); }
    public static boolean isKaraokeBasicPitchTflite() { return Prefers.getBoolean("karaoke_basic_pitch_tflite", false); }
    public static void putKaraokeBasicPitchTflite(boolean v) { Prefers.put("karaoke_basic_pitch_tflite", v); }
    public static int getKaraokeMicDelayMs() { return Prefers.getInt("karaoke_mic_delay", 0); }
    public static void putKaraokeMicDelayMs(int ms) { Prefers.put("karaoke_mic_delay", ms); }
    public static float getKaraokeToleranceSemitones() { return Prefers.getFloat("karaoke_tolerance_semitones", 3.0f); }
    public static void putKaraokeToleranceSemitones(float f) { Prefers.put("karaoke_tolerance_semitones", f); }
    
    public static boolean isOsdDiagnostics() { return Prefers.getBoolean("osd_diagnostics", false); }
    public static void putOsdDiagnostics(boolean v) { Prefers.put("osd_diagnostics", v); }
    
    public static int getPlayCacheSize() { return Prefers.getInt("play_cache_size", 32 * 1024 * 1024); }
    public static boolean isVideoPrefer() { return Prefers.getBoolean("video_prefer", false); }
    public static boolean isAudioPrefer() { return Prefers.getBoolean("audio_prefer", false); }
    
    public static String getMpvRender() { return Prefers.getString("mpv_render", "gpu"); }
    
    // Helper for Prefers integration
    public static void putInt(String key, int value) { Prefers.put(key, value); }
}
