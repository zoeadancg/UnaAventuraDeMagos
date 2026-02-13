
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * ImageUtils
 * Utilidades estáticas para manipular imágenes (escalado pixel art, escalado
 * suave,
 * carga desde recursos y corte de spritesheets).
 */
public final class ImageUtils {

    private ImageUtils() {
        /* utilitario */ }

    /**
     * Carga una imagen desde el classpath (ruta con /images/...). Devuelve null si
     * falla.
     */
    public static BufferedImage loadImage(String resourcePath) {
        if (resourcePath == null)
            return null;
        try (InputStream is = ImageUtils.class.getResourceAsStream(resourcePath)) {
            if (is == null)
                return null;
            return ImageIO.read(is);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /** Asegura que una Image sea BufferedImage (con alpha). */
    public static BufferedImage toBufferedImage(Image img) {
        if (img == null)
            return null;
        if (img instanceof BufferedImage)
            return (BufferedImage) img;
        int w = img.getWidth(null);
        int h = img.getHeight(null);
        if (w <= 0 || h <= 0)
            return null;
        BufferedImage b = createCompatibleImage(w, h, Transparency.TRANSLUCENT);
        Graphics2D g = b.createGraphics();
        g.setComposite(AlphaComposite.Src);
        g.drawImage(img, 0, 0, null);
        g.dispose();
        return b;
    }

    /** Crea BufferedImage compatible con el sistema (mejor rendimiento). */
    public static BufferedImage createCompatibleImage(int w, int h, int transparency) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = ge.getDefaultScreenDevice();
        GraphicsConfiguration gc = gd.getDefaultConfiguration();
        BufferedImage img = gc.createCompatibleImage(Math.max(1, w), Math.max(1, h), transparency);
        return img;
    }

    /**
     * Escala pixel art usando nearest neighbor. scale >= 1. Si scale == 1 devuelve
     * la misma imagen.
     */
    public static BufferedImage scalePixelArt(BufferedImage src, int scale) {
        if (src == null)
            return null;
        if (scale <= 1)
            return src;
        int w = src.getWidth() * scale;
        int h = src.getHeight() * scale;
        BufferedImage dst = createCompatibleImage(w, h, Transparency.TRANSLUCENT);
        AffineTransform at = AffineTransform.getScaleInstance(scale, scale);
        AffineTransformOp op = new AffineTransformOp(at, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        op.filter(src, dst);
        return dst;
    }

    /** Escala suavemente (bilinear) para UI o fondos. scale can be fractional. */
    public static BufferedImage scaleSmooth(BufferedImage src, int targetW, int targetH) {
        if (src == null)
            return null;
        if (targetW <= 0 || targetH <= 0)
            return src;
        BufferedImage dst = createCompatibleImage(targetW, targetH, Transparency.TRANSLUCENT);
        Graphics2D g = dst.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setComposite(AlphaComposite.Src);
        g.drawImage(src, 0, 0, targetW, targetH, null);
        g.dispose();
        return dst;
    }

    /**
     * Corta un spritesheet en tiles de tileW x tileH y devuelve la lista de frames.
     */
    public static List<BufferedImage> sliceSpriteSheet(BufferedImage sheet, int tileW, int tileH) {
        List<BufferedImage> frames = new ArrayList<>();
        if (sheet == null || tileW <= 0 || tileH <= 0)
            return frames;
        int cols = sheet.getWidth() / tileW;
        int rows = sheet.getHeight() / tileH;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                BufferedImage sub = sheet.getSubimage(c * tileW, r * tileH, tileW, tileH);
                frames.add(toBufferedImage(sub));
            }
        }
        return frames;
    }

    /** Voltea horizontalmente una imagen (útil para reutilizar sprites). */
    public static BufferedImage flipHorizontal(BufferedImage src) {
        if (src == null)
            return null;
        AffineTransform tx = AffineTransform.getScaleInstance(-1, 1);
        tx.translate(-src.getWidth(), 0);
        AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        return op.filter(src, null);
    }

    /** Rota la imagen en grados (90, 180, 270 o cualquier valor). */
    public static BufferedImage rotate(BufferedImage src, double degrees) {
        if (src == null)
            return null;
        double rads = Math.toRadians(degrees);
        double sin = Math.abs(Math.sin(rads)), cos = Math.abs(Math.cos(rads));
        int w = src.getWidth(), h = src.getHeight();
        int newW = (int) Math.floor(w * cos + h * sin);
        int newH = (int) Math.floor(h * cos + w * sin);
        BufferedImage result = createCompatibleImage(newW, newH, Transparency.TRANSLUCENT);
        Graphics2D g = result.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.translate((newW - w) / 2.0, (newH - h) / 2.0);
        g.rotate(rads, w / 2.0, h / 2.0);
        g.drawRenderedImage(src, null);
        g.dispose();
        return result;
    }
}
