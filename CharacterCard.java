
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Objects;

public class CharacterCard extends JPanel {
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

        // Escalar la imagen si existe
        if (baseImage != null) {
            this.displayImage = ImageUtils.scalePixelArt(baseImage, 2);
        }

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