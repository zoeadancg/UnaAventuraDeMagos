
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

public final class Resources {
    private Resources() {
    }

    public static final String IMG_HIELO = "Images/mago_hielo.png";
    public static final String IMG_FUEGO = "Images/mago_fuego.png";
    public static final String IMG_AGUA = "Images/mago_agua.png";
    public static final String IMG_ELECTRICIDAD = "Images/mago_electricidad.png";
    public static final String IMG_BOTON_JUGAR = "Images/boton_jugar.png";

    public static BufferedImage loadImage(String resourcePath) {
        try (InputStream is = Resources.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null)
                return null;
            return ImageIO.read(is);
        } catch (IOException e) {
            return null;
        }
    }
}