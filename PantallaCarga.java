import javax.swing.*;
import java.awt.*;

public class PantallaCarga extends JFrame {
    public PantallaCarga(String mensaje, Runnable siguienteAccion) {
        setTitle("Cargando...");
        setSize(1200, 700);
        setLocationRelativeTo(null);
        setUndecorated(true); // Sin bordes
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(10, 18, 40)); // Fondo azul oscuro

        JLabel texto = new JLabel(mensaje, SwingConstants.CENTER);
        texto.setFont(new Font("Courier New", Font.BOLD, 36));
        texto.setForeground(Color.YELLOW);
        add(texto, BorderLayout.CENTER);

        setVisible(true);

        // Simula carga con animación o espera
        new Timer(2000, e -> {
            dispose(); // Cierra pantalla de carga
            siguienteAccion.run(); // Ejecuta lo que sigue (como abrir el menú)
        }).start();
    }
}
