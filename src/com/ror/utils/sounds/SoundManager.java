package com.ror.utils.sounds;

import java.io.File;
import java.net.URL;
import javax.sound.sampled.*;

public class SoundManager {
    private SoundManager() {} // instantiation prevention yay

    private static Clip bgmClip;
    private static Clip sfxClip;
    private static int masterVolume = 100;
    private static String currentMusicPath;

    public static void playMusic(String path) { //music loops
        if (path != null && path.equals(currentMusicPath) && bgmClip != null && bgmClip.isOpen()) {
            if (!bgmClip.isRunning()) {
                bgmClip.loop(Clip.LOOP_CONTINUOUSLY);
                bgmClip.start();
            }
            return;
        }
        stopMusic();
        bgmClip = loadClip(path);
        if (bgmClip != null) {
            currentMusicPath = path;
            applyVolume(bgmClip);
            bgmClip.loop(Clip.LOOP_CONTINUOUSLY);
            bgmClip.start();
        }
    }

    public static void playSfx(String path) {
        stopSfx();
        sfxClip = loadClip(path);
        if (sfxClip != null) {
            applyVolume(sfxClip);
            sfxClip.start();
        }
    }

    private static Clip loadClip(String path) {
        try {
            AudioInputStream ais = null;
            
            URL url = SoundManager.class.getResource(path);
            if (url != null) {
                ais = AudioSystem.getAudioInputStream(url);
            } else {
                String fsPath = path.startsWith("/") ? path.substring(1) : path;
                File file = new File(fsPath);
                if (file.exists()) {
                    ais = AudioSystem.getAudioInputStream(file);
                }
            }

            if (ais == null) {
                System.err.println("AUDIO ERROR: Could not find file at " + path);
                return null;
            }

            Clip clip = AudioSystem.getClip();
            clip.open(ais);
            
            //debug thing, the original clip is 15mins long.
            // if (path.toLowerCase().contains("title")) {
            //     clip.loop(Clip.LOOP_CONTINUOUSLY);
            // }
            return clip;
        } catch (Exception e) {
            System.err.println("AUDIO SYSTEM CRASH!");
            e.printStackTrace();
            return null;
        }

        
    }

    public static void stopMusic() {
        if (bgmClip != null) {
            bgmClip.stop();
            bgmClip.close();
            bgmClip = null;
        }
        currentMusicPath = null;
    }

    public static void stopSfx() {
        if (sfxClip != null) {
            sfxClip.stop();
            sfxClip.close();
        }
    }

    public static void shutdownSound() {
        stopMusic();
        stopSfx();
    }

    public static int getMasterVolume() {
        return masterVolume;
    }

    public static void setMasterVolume(int volume) {
        masterVolume = Math.max(0, Math.min(100, volume));
        applyVolume(bgmClip);
        applyVolume(sfxClip);
    }

    private static void applyVolume(Clip clip) {
        if (clip == null || !clip.isOpen()) {
            return;
        }
        if (!clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            return;
        }

        FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
        if (masterVolume <= 0) {
            gainControl.setValue(gainControl.getMinimum());
            return;
        }

        float gain = (float) (20.0 * Math.log10(masterVolume / 100.0));
        float clampedGain = Math.max(gainControl.getMinimum(), Math.min(gainControl.getMaximum(), gain));
        gainControl.setValue(clampedGain);
    }
}
