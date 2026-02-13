package ui;

import javax.swing.*;
import java.awt.*;
import util.Resources;

public class PantallaCarga extends JFrame {
    public PantallaCarga(Runnable siguienteAccion) {
        setTitle("Cargando...");
        setSize(1200, 700);
        setLocationRelativeTo(null);
        setUndecorated(true);
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(10, 18, 40));

        ImageIcon gif = Resources.getIcon(Resources.GIF_CARGANDO);
        JLabel animacion;
        if (gif != null) {
            animacion = new JLabel(gif);
        } else {
            animacion = new JLabel("Cargando...");
            animacion.setForeground(Color.WHITE);
        }
        animacion.setHorizontalAlignment(SwingConstants.CENTER);
        add(animacion, BorderLayout.CENTER);

        setVisible(true);

        new Timer(3000, e -> {
            dispose();
            siguienteAccion.run();
        }).start();
    }

    // constructor auxiliar que acepta JFrame
    public PantallaCarga(JFrame siguienteVentana) {
        this(() -> {
            siguienteVentana.setVisible(true);
            siguienteVentana.setLocationRelativeTo(null);
        });
    }
}