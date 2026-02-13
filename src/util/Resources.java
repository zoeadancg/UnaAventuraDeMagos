package util;

import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.ImageIcon;

public final class Resources {
    private Resources() {
    }

    // Image path constants
    public static final String IMG_PORTADA = "Images/Una_Aventura_de_Magos_logo.png";
    public static final String IMG_BOTON_JUGAR = "Images/botonJugar.png";
    public static final String IMG_BOTON_CONTINUAR = "Images/boton_continuar.png";
    public static final String IMG_HIELO = "Images/mago_hielo.png";
    public static final String IMG_FUEGO = "Images/mago_fuego.png";
    public static final String IMG_AGUA = "Images/mago_agua.png";
    public static final String IMG_ELECTRICIDAD = "Images/mago_electricidad.png";
    public static final String GIF_CARGANDO = "Animaciones/cargando.gif";

    private static final Map<String, BufferedImage> imageCache = new ConcurrentHashMap<>();
    private static final Map<String, Clip> soundCache = new ConcurrentHashMap<>();

    /**
     * Convenience method: loads and caches an image, returns null on failure.
     */
    public static BufferedImage getImage(String path) {
        try {
            return loadAndCacheImage(path);
        } catch (Exception e) {
            System.err.println("Could not load image: " + path + " - " + e.getMessage());
            return null;
        }
    }

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

    public static ImageIcon getIcon(String path) {
        URL url = Resources.class.getClassLoader().getResource(normalize(path));
        if (url == null) {
            System.err.println("Could not find icon: " + path);
            return null;
        }
        return new ImageIcon(url);
    }

    // Optional helpers
    public static BufferedImage getCachedImage(String path) {
        return imageCache.get(normalize(path));
    }

    public static Clip getCachedSound(String path) {
        return soundCache.get(normalize(path));
    }
}