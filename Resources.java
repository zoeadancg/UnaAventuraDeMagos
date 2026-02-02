
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

public final class Resources {
    private Resources() {
    }

    // Nombres de archivo en carpeta raíz "Images"
    public static final String IMG_HIELO = "Images/mago_hielo.png";
    public static final String IMG_FUEGO = "Images/mago_fuego.png";
    public static final String IMG_AGUA = "Images/mago_agua.png";
    public static final String IMG_ELECTRICIDAD = "Images/mago_electricidad.png";
    public static final String IMG_BOTON_JUGAR = "Images/botonJugar.png";
    public static final String IMG_PORTADA = "Images/Una_Aventura_de_Magos_logo.png";
    public static final String IMG_BOTON_CONTINUAR = "Images/boton_continuar.png";

    public static BufferedImage loadImage(String path) {
        try {
            File file = new File(path);
            if (!file.exists()) {
                System.err.println("Imagen no encontrada: " + file.getAbsolutePath());
                return null;
            }
            return ImageIO.read(file);
        } catch (Exception e) {
            System.err.println("Error al cargar imagen: " + path);
            return null;
        }
    }
}
