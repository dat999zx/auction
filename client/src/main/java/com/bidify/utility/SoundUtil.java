package com.bidify.utility;

import javafx.scene.media.AudioClip;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.URL;

public class SoundUtil {
    private static final Logger logger = LoggerFactory.getLogger(SoundUtil.class);
    
    private static AudioClip successClip;
    private static AudioClip errorClip;

    private SoundUtil() {}

    // preload audio
    static {
        try {
            successClip = loadClip("success");
            errorClip = loadClip("error");
        }
        catch (Exception t) {
            logger.debug("Failed to initialize SoundUtil: {}", t.getMessage());
        }
    }

    // load audio clip from resources
    private static AudioClip loadClip(String baseName) {
        try {
            URL resource = SoundUtil.class.getResource("/audio/" + baseName + ".mp3");
            if (resource != null)
                return new AudioClip(resource.toExternalForm());
        }
        catch (Exception t) {
            logger.debug("Failed to load sound resource {}: {}", baseName, t.getMessage());
        }
        return null;
    }

    public static void success() {
        play(successClip);
    }

    public static void error() {
        play(errorClip);
    }

    // play audio clip in a separate thread
    private static void play(AudioClip clip) {
        if (clip == null) return;
        try {
            Thread playThread = new Thread(() -> {
                try {
                    clip.play();
                }
                catch (Exception t) {
                    logger.debug("Error playing audio clip: {}", t.getMessage());
                }
            });
            playThread.setDaemon(true);
            playThread.start();
        }
        catch (Exception t) {
            logger.debug("Failed to play audio clip thread: {}", t.getMessage());
        }
    }
}
