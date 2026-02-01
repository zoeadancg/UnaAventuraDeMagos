
import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Objects;
import javax.imageio.ImageIO;

public class Menu extends JFrame {

    private CharacterCard selectedCard;

    public Menu() {
        super("Selección de Personaje");

        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setSize(1200, 700);
        this.setLocationRelativeTo(null);
        this.setResizable(false);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(new Color(10, 18, 40));
        setContentPane(root);

        // Panel de tarjetas
        JPanel cardsPanel = new JPanel();
        cardsPanel.setOpaque(false);
        cardsPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(16, 16, 16, 16);

        // Carga de imágenes (REEMPLAZA LAS RUTAS)
        BufferedImage imgHielo = loadImage("Proyecto01/Images/mago_hielo.png");
        BufferedImage imgFuego = loadImage("Proyecto01/Images/mago_fuego.png");
        BufferedImage imgAgua = loadImage("Proyecto01/Images/mago_agua.png");
        BufferedImage imgElectricidad = loadImage("Proyecto01/Images/mago_electricidad.png");

        // Tarjetas con estadísticas
        CharacterCard cardHielo = new CharacterCard("Mago de Hielo", imgHielo, new Color(160, 220, 255),
                "Daño: 50\nHP: 200\nVelocidad: 80");
        CharacterCard cardFuego = new CharacterCard("Mago de Fuego", imgFuego, new Color(255, 120, 70),
                "Daño: 120\nHP: 80\nVelocidad: 120");
        CharacterCard cardAgua = new CharacterCard("Mago de Agua", imgAgua, new Color(80, 160, 255),
                "Daño: 80\nHP: 90\nVelocidad: 90");
        CharacterCard cardElectricidad = new CharacterCard("Mago de Electricidad", imgElectricidad,
                new Color(255, 230, 60),
                "Daño: 100\nHP: 70\nVelocidad: 150");

        // Distribución horizontal
        gbc.gridx = 0;
        cardsPanel.add(cardHielo, gbc);
        gbc.gridx = 1;
        cardsPanel.add(cardFuego, gbc);
        gbc.gridx = 2;
        cardsPanel.add(cardAgua, gbc);
        gbc.gridx = 3;
        cardsPanel.add(cardElectricidad, gbc);

        root.add(cardsPanel, BorderLayout.CENTER);

        // Botón JUGAR
        JButton playButton = new JButton("JUGAR");
        playButton.setFont(pixelFont(22));
        playButton.setForeground(new Color(245, 203, 66));
        playButton.setBackground(new Color(60, 40, 20));
        playButton.setFocusPainted(false);
        playButton.setBorder(pixelWoodBorder());
        playButton.setPreferredSize(new Dimension(220, 56));
        playButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        playButton.addActionListener(e -> {
            if (selectedCard == null) {
                JOptionPane.showMessageDialog(this, "Elige un personaje antes de jugar.", "Aviso",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                        "Has elegido: " + selectedCard.getTitle() + "\n\n" + selectedCard.getStats(),
                        "¡A la aventura!", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        JPanel bottom = new JPanel();
        bottom.setOpaque(false);
        bottom.setBorder(BorderFactory.createEmptyBorder(10, 0, 20, 0));
        bottom.add(playButton);
        root.add(bottom, BorderLayout.SOUTH);

        // Selección y resaltado
        cardHielo.addSelectionListener(() -> selectCard(cardHielo));
        cardFuego.addSelectionListener(() -> selectCard(cardFuego));
        cardAgua.addSelectionListener(() -> selectCard(cardAgua));
        cardElectricidad.addSelectionListener(() -> selectCard(cardElectricidad));
    }

    private void selectCard(CharacterCard card) {
        if (selectedCard != null)
            selectedCard.setSelected(false);
        selectedCard = card;
        selectedCard.setSelected(true);
        repaint();
    }

    private Font pixelFont(int size) {
        return new Font(Font.MONOSPACED, Font.BOLD, size);
    }

    private Border pixelWoodBorder() {
        Color outer = new Color(40, 26, 14);
        Color inner = new Color(94, 62, 28);
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(outer, 4),
                BorderFactory.createLineBorder(inner, 4));
    }

    private BufferedImage loadImage(String path) {
        try {
            return ImageIO.read(new File(path));
        } catch (Exception e) {
            BufferedImage placeholder = new BufferedImage(128, 192, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = placeholder.createGraphics();
            g.setColor(new Color(20, 30, 60));
            g.fillRect(0, 0, placeholder.getWidth(), placeholder.getHeight());
            g.setColor(Color.WHITE);
            g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 14));
            g.drawString("Imagen no encontrada", 6, placeholder.getHeight() / 2);
            g.dispose();
            return placeholder;
        }
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

    static class CharacterCard extends JPanel {
        private final String title;
        private final BufferedImage baseImage;
        private BufferedImage displayImage;
        private boolean selected = false;
        private Runnable onSelect;
        private final String stats;

        public CharacterCard(String title, BufferedImage image, Color accent, String stats) {
            this.title = Objects.requireNonNull(title);
            this.baseImage = image;
            this.stats = stats;
            setOpaque(false);
            setPreferredSize(new Dimension(260, 360));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            this.displayImage = scalePixelArt(baseImage, 2);

            JLabel name = new JLabel(title, SwingConstants.CENTER);
            name.setFont(new Font(Font.MONOSPACED, Font.BOLD, 16));
            name.setForeground(accent);

            JLabel statLabel = new JLabel("<html><center>" + stats.replaceAll("\n", "<br>") + "</center></html>",
                    SwingConstants.CENTER);
            statLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
            statLabel.setForeground(Color.LIGHT_GRAY);

            JPanel infoPanel = new JPanel(new GridLayout(2, 1));
            infoPanel.setOpaque(false);
            infoPanel.add(name);
            infoPanel.add(statLabel);

            setLayout(new BorderLayout());
            add(infoPanel, BorderLayout.SOUTH);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(new Color(245, 203, 66), 3),
                            BorderFactory.createLineBorder(new Color(120, 90, 40), 3)));
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    if (!selected) {
                        setBorder(null);
                        repaint();
                    }
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    if (onSelect != null)
                        onSelect.run();
                }
            });
        }

        public void addSelectionListener(Runnable r) {
            this.onSelect = r;
        }

        public String getTitle() {
            return title;
        }

        public String getStats() {
            return stats;
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
            if (selected) {
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(245, 203, 66), 4),
                        BorderFactory.createLineBorder(new Color(94, 62, 28), 4)));
            } else {
                setBorder(null);
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();

            g2.setColor(new Color(18, 28, 68));
            g2.fillRoundRect(10, 10, getWidth() - 20, getHeight() - 60, 12, 12);

            if (displayImage != null) {
                g2.drawImage(displayImage, 10, 10, getWidth() - 20, getHeight() - 60, this);
            }
        }
    }
}
