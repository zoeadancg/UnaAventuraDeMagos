import javax.swing.*;
import java.awt.*;

public class PantallaCarga extends JFrame {
    public PantallaCarga(Runnable siguienteAccion) {
        // Configuración de la ventana
        setTitle("Cargando...");
        setSize(1200, 700);
        setLocationRelativeTo(null);
        setUndecorated(true);
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(10, 18, 40));

        // GIF de carga
        ImageIcon gif = new ImageIcon(getClass().getResource("Animaciones/cargando.gif"));
        JLabel animacion = new JLabel(gif);
        animacion.setHorizontalAlignment(SwingConstants.CENTER);
        add(animacion, BorderLayout.CENTER);

        setVisible(true);

        // Simula carga
        new Timer(3000, e -> {
            dispose();
            siguienteAccion.run(); // Abre el menú u otra escena
        }).start();
    }
}

