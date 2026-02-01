import java.awt.image.BufferedImage;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;

final class ImageUtils {
    private ImageUtils() {
    }

    public static BufferedImage scalePixelArt(BufferedImage src, int scale) {
        if (src == null || scale <= 1)
            return src;
        int w = src.getWidth() * scale;
        int h = src.getHeight() * scale;
        BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        AffineTransform at = AffineTransform.getScaleInstance(scale, scale);
        AffineTransformOp op = new AffineTransformOp(at, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        op.filter(src, dst);
        return dst;
    }
}
