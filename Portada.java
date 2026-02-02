import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

public class Portada extends JFrame {
    public Portada() {
        setTitle("Portada");
        setSize(1200, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(null);

        // Cargar imágenes desde Resources
        BufferedImage botonJugar = Resources.loadImage(Resources.IMG_BOTON_JUGAR);

        // Botón JUGAR con imagen
        if (botonJugar != null) {
            int btnWidth = botonJugar.getWidth();
            int btnHeight = botonJugar.getHeight();

            // Centrar el botón horizontalmente y colocarlo cerca del fondo
            int x = (getWidth() - btnWidth) / 2;
            int y = getHeight() - btnHeight - 40;

            JButton jugarBtn = new JButton(new ImageIcon(botonJugar));
            jugarBtn.setBounds(x, y, btnWidth, btnHeight);
            jugarBtn.setBorderPainted(false);
            jugarBtn.setContentAreaFilled(false);
            jugarBtn.setFocusPainted(false);

            jugarBtn.addActionListener(e -> {
                dispose(); // Cierra la portada
                new PantallaCarga("Cargando...", () -> new Menu()); // Muestra carga y luego el menú
            });

            add(jugarBtn);
        }

        // Mostrar logo
        BufferedImage portadaImg = Resources.loadImage(Resources.IMG_PORTADA);

        if (portadaImg != null) {
            // Escalar la imagen al tamaño de la ventana
            Image portadaEscalada = portadaImg.getScaledInstance(getWidth(), getHeight(), Image.SCALE_SMOOTH);
            JLabel portadaLabel = new JLabel(new ImageIcon(portadaEscalada));
            portadaLabel.setBounds(0, 0, getWidth(), getHeight());
            add(portadaLabel);
        }
        setVisible(true);
    }
}
