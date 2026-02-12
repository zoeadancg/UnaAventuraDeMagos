package src.ui;

import javax.swing.*;

import src.util.Resources;

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
        BufferedImage botonJugar = Resources.getImage(Resources.IMG_BOTON_JUGAR);

        if (botonJugar != null) {
            int btnWidth = botonJugar.getWidth();
            int btnHeight = botonJugar.getHeight();

            // calcula posición tras setSize; aquí usamos getWidth() ya inicializado
            int x = (getWidth() - btnWidth) / 2;
            int y = getHeight() - btnHeight - 40;

            JButton jugarBtn = new JButton(new ImageIcon(botonJugar));
            jugarBtn.setBounds(x, y, btnWidth, btnHeight);
            jugarBtn.setBorderPainted(false);
            jugarBtn.setContentAreaFilled(false);
            jugarBtn.setFocusPainted(false);

            jugarBtn.addActionListener(e -> {
                dispose();
                new PantallaCarga(() -> new Menu());
            });

            add(jugarBtn);
        } else {
            // fallback: botón de texto si no hay imagen
            JButton jugarBtn = new JButton("Jugar");
            jugarBtn.setBounds(520, 600, 160, 40);
            jugarBtn.addActionListener(e -> {
                dispose();
                new PantallaCarga(() -> new Menu());
            });
            add(jugarBtn);
        }

        // Mostrar logo
        BufferedImage portadaImg = Resources.getImage(Resources.IMG_PORTADA);
        if (portadaImg != null) {
            Image portadaEscalada = portadaImg.getScaledInstance(getWidth(), getHeight(), Image.SCALE_SMOOTH);
            JLabel portadaLabel = new JLabel(new ImageIcon(portadaEscalada));
            portadaLabel.setBounds(0, 0, getWidth(), getHeight());
            add(portadaLabel);
        } else {
            // fallback visual simple
            JLabel label = new JLabel("La Aventura de Magos");
            label.setBounds(20, 20, 400, 40);
            add(label);
        }

        setVisible(true);
    }
}
