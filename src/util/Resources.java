package src.util;

import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class Resources {
    private Resources() {
    }

    private static final Map<String, BufferedImage> imageCache = new ConcurrentHashMap<>();
    private static final Map<String, Clip> soundCache = new ConcurrentHashMap<>();

    // Normaliza rutas: acepta "/images/foo.png" o "images/foo.png"
    private static String normalize(String path) {
        if (path == null)
            return null;
        return path.startsWith("/") ? path.substring(1) : path;
    }

    public static BufferedImage loadAndCacheImage(String path) {
        String key = normalize(path);
        return imageCache.computeIfAbsent(key, k -> {
            try (InputStream is = Resources.class.getClassLoader().getResourceAsStream(k)) {
                if (is == null)
                    throw new IllegalArgumentException("Resource not found: " + path);
                return ImageIO.read(is);
            } catch (IOException e) {
                throw new RuntimeException("Failed to load image: " + path, e);
            }
        });
    }

    public static Clip loadAndCacheSound(String path) {
        String key = normalize(path);
        return soundCache.computeIfAbsent(key, k -> {
            try (InputStream is = Resources.class.getClassLoader().getResourceAsStream(k)) {
                if (is == null)
                    throw new IllegalArgumentException("Resource not found: " + path);
                AudioInputStream ais = AudioSystem.getAudioInputStream(is);
                Clip clip = AudioSystem.getClip();
                clip.open(ais);
                return clip;
            } catch (Exception e) {
                throw new RuntimeException("Failed to load sound: " + path, e);
            }
        });
    }

    public static void unload(String path) {
        String key = normalize(path);
        BufferedImage img = imageCache.remove(key);
        if (img != null) {
            // nothing to close for BufferedImage, GC will collect
        }
        Clip clip = soundCache.remove(key);
        if (clip != null) {
            clip.stop();
            clip.close();
        }
    }

    // Optional helpers
    public static BufferedImage getCachedImage(String path) {
        return imageCache.get(normalize(path));
    }

    public static Clip getCachedSound(String path) {
        return soundCache.get(normalize(path));
    }
}