
import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.image.BufferedImage;

public class Menu extends JFrame {

    private CharacterCard selectedCard;

    public Menu() {
        super("Selección de Personaje");
        setupFrame();

        JPanel root = buildRootPanel();
        setContentPane(root);

        JPanel cardsPanel = createCardsPanel();
        root.add(cardsPanel, BorderLayout.CENTER);

        JPanel bottom = new JPanel();
        bottom.setOpaque(false);
        bottom.setBorder(BorderFactory.createEmptyBorder(10, 0, 20, 0));
        bottom.add(createPlayButton());
        root.add(bottom, BorderLayout.SOUTH);

        setVisible(true);
    }

    private void setupFrame() {
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setSize(1200, 700);
        this.setLocationRelativeTo(null);
        this.setResizable(false);
    }

    private JPanel buildRootPanel() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(new Color(10, 18, 40));
        return root;
    }

    private JPanel createCardsPanel() {
        JPanel cardsPanel = new JPanel(new GridBagLayout());
        cardsPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(16, 16, 16, 16);

        // Cargar imágenes desde carpeta raíz
        BufferedImage imgHielo = Resources.loadImage(Resources.IMG_HIELO);
        BufferedImage imgFuego = Resources.loadImage(Resources.IMG_FUEGO);
        BufferedImage imgAgua = Resources.loadImage(Resources.IMG_AGUA);
        BufferedImage imgElectricidad = Resources.loadImage(Resources.IMG_ELECTRICIDAD);

        CharacterCard cardHielo = new CharacterCard("Mago de Hielo", imgHielo, new Color(160, 220, 255),
                "Daño: 50\nHP: 200\nVelocidad: 80");
        CharacterCard cardFuego = new CharacterCard("Mago de Fuego", imgFuego, new Color(255, 120, 70),
                "Daño: 120\nHP: 80\nVelocidad: 120");
        CharacterCard cardAgua = new CharacterCard("Mago de Agua", imgAgua, new Color(80, 160, 255),
                "Daño: 80\nHP: 90\nVelocidad: 90");
        CharacterCard cardElectricidad = new CharacterCard("Mago de Electricidad", imgElectricidad,
                new Color(255, 230, 60),
                "Daño: 100\nHP: 70\nVelocidad: 150");

        gbc.gridx = 0;
        cardsPanel.add(cardHielo, gbc);
        gbc.gridx = 1;
        cardsPanel.add(cardFuego, gbc);
        gbc.gridx = 2;
        cardsPanel.add(cardAgua, gbc);
        gbc.gridx = 3;
        cardsPanel.add(cardElectricidad, gbc);

        cardHielo.addSelectionListener(() -> selectCard(cardHielo));
        cardFuego.addSelectionListener(() -> selectCard(cardFuego));
        cardAgua.addSelectionListener(() -> selectCard(cardAgua));
        cardElectricidad.addSelectionListener(() -> selectCard(cardElectricidad));

        return cardsPanel;
    }

    private JButton createPlayButton() {
        JButton playButton = new JButton("");

        BufferedImage btnImg = Resources.loadImage(Resources.IMG_BOTON_CONTINUAR);
        if (btnImg != null) {
            playButton.setIcon(new ImageIcon(ImageUtils.scalePixelArt(btnImg, 2)));
            playButton.setHorizontalTextPosition(SwingConstants.CENTER);
            playButton.setVerticalTextPosition(SwingConstants.CENTER);
            playButton.setBorderPainted(false);
            playButton.setContentAreaFilled(false);
        } else {
            playButton.setFont(pixelFont(22));
            playButton.setForeground(new Color(245, 203, 66));
            playButton.setBackground(new Color(60, 40, 20));
            playButton.setFocusPainted(false);
            playButton.setBorder(pixelWoodBorder());
        }

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
        return playButton;
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Menu m = new Menu();
        });
    }
}