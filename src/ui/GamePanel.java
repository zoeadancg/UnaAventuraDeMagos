package ui;

import game.GameController;
import game.CombatManager;
import model.Combatant;
import model.Direccion;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * GamePanel
 * Panel principal de juego: entrada de secuencias, HUD y canvas de combate.
 * Integración esperada:
 * - GameController debe exponer onPlayerSequenceSubmitted(List<Direccion>)
 * - CombatManager puede usarse como fallback para resolveTurn(player, seq)
 */
public class GamePanel extends JPanel {

    private final DefaultListModel<Direccion> seqModel = new DefaultListModel<>();
    private final JList<Direccion> seqList = new JList<>(seqModel);

    private final JButton btnConfirm = new JButton("Confirmar");
    private final JButton btnClear = new JButton("Limpiar");
    private final JButton btnPause = new JButton("Pausa");

    private final JLabel lblPlayerHp = new JLabel("HP: -");
    private final JLabel lblEnemyHp = new JLabel("Enemy HP: -");
    private final JLabel lblInfo = new JLabel("Listo");
    private final JProgressBar loadingBar = new JProgressBar(0, 100);

    private final CombatCanvas canvas = new CombatCanvas();

    private GameController controller;
    private CombatManager combatManager;
    private Combatant player;
    private Combatant enemy;

    private final ExecutorService bg = Executors.newSingleThreadExecutor();

    public GamePanel() {
        setLayout(new BorderLayout());
        add(buildHudPanel(), BorderLayout.NORTH);
        add(buildCenterPanel(), BorderLayout.CENTER);
        add(buildControlsPanel(), BorderLayout.SOUTH);
        installListeners();
    }

    // ---------------- UI builders ----------------
    private JPanel buildHudPanel() {
        JPanel p = new JPanel(new BorderLayout());
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT));
        left.add(lblPlayerHp);
        left.add(lblEnemyHp);
        p.add(left, BorderLayout.WEST);
        p.add(loadingBar, BorderLayout.CENTER);
        p.add(lblInfo, BorderLayout.EAST);
        return p;
    }

    private JSplitPane buildCenterPanel() {
        seqList.setVisibleRowCount(6);
        seqList.setFixedCellWidth(140);
        JScrollPane seqScroll = new JScrollPane(seqList);

        JPanel right = new JPanel(new BorderLayout());
        right.add(canvas, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, seqScroll, right);
        split.setResizeWeight(0.25);
        return split;
    }

    private JPanel buildControlsPanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER));
        p.add(btnConfirm);
        p.add(btnClear);
        p.add(btnPause);
        return p;
    }

    // ---------------- Listeners and input ----------------
    private void installListeners() {
        btnConfirm.addActionListener(e -> submitSequence());
        btnClear.addActionListener(e -> clearSequence());
        btnPause.addActionListener(e -> {
            if (controller != null)
                controller.pauseGame();
            setControlsEnabled(false);
        });

        // Keyboard: arrow keys to add directions
        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                Direccion d = null;
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_LEFT:
                        d = Direccion.LEFT;
                        break;
                    case KeyEvent.VK_RIGHT:
                        d = Direccion.RIGHT;
                        break;
                    case KeyEvent.VK_UP:
                        d = Direccion.UP;
                        break;
                    case KeyEvent.VK_DOWN:
                        d = Direccion.DOWN;
                        break;
                }
                if (d != null)
                    addDirection(d);
            }
        });

        // Canvas click to focus
        canvas.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                requestFocusInWindow();
            }
        });
    }

    // ---------------- Public API ----------------
    public void setGameController(GameController controller) {
        this.controller = controller;
    }

    public void setCombatManager(CombatManager manager) {
        this.combatManager = manager;
    }

    /**
     * Inicializa panel con combatants. Llamar desde GameController cuando el nivel
     * esté listo.
     */
    public void startGame(Combatant player, Combatant enemy) {
        this.player = player;
        this.enemy = enemy;
        SwingUtilities.invokeLater(() -> {
            clearSequence();
            updateHUD();
            setControlsEnabled(true);
            lblInfo.setText("Comienza el combate");
            requestFocusInWindow();
        });
    }

    /**
     * Detiene el panel y libera recursos. Llamar al cerrar la ventana.
     */
    public void stopGame() {
        bg.submit(() -> {
            // detener animaciones/sonidos si aplica
            SwingUtilities.invokeLater(() -> {
                clearSequence();
                setControlsEnabled(false);
                lblInfo.setText("Juego detenido");
            });
        });
        bg.shutdownNow();
    }

    /**
     * Actualiza HUD desde los combatants actuales.
     */
    public void updateHUD() {
        SwingUtilities.invokeLater(() -> {
            if (player != null)
                lblPlayerHp.setText("HP: " + player.getHp() + "/" + player.getMaxHp());
            else
                lblPlayerHp.setText("HP: -");
            if (enemy != null)
                lblEnemyHp.setText("Enemy HP: " + enemy.getHp() + "/" + enemy.getMaxHp());
            else
                lblEnemyHp.setText("Enemy HP: -");
            loadingBar.setValue(0);
        });
    }

    public void setControlsEnabled(boolean enabled) {
        SwingUtilities.invokeLater(() -> {
            btnConfirm.setEnabled(enabled && !seqModel.isEmpty());
            btnClear.setEnabled(enabled && !seqModel.isEmpty());
            btnPause.setEnabled(enabled);
        });
    }

    public void clearSequence() {
        SwingUtilities.invokeLater(() -> {
            seqModel.clear();
            lblInfo.setText("Secuencia limpiada");
            setControlsEnabled(true);
        });
    }

    public void addDirection(Direccion d) {
        if (d == null)
            return;
        SwingUtilities.invokeLater(() -> {
            if (seqModel.size() >= 8) {
                lblInfo.setText("Máx 8 inputs");
                return;
            }
            seqModel.addElement(d);
            lblInfo.setText("Input: " + d);
            setControlsEnabled(true);
        });
    }

    /**
     * Envía la secuencia al GameController (preferido) o al CombatManager si no hay
     * controller.
     * Ejecuta la resolución en background y actualiza UI en EDT.
     */
    public void submitSequence() {
        final List<Direccion> seq = Collections.list(seqModel.elements());
        if (seq.isEmpty()) {
            lblInfo.setText("Secuencia vacía");
            return;
        }
        setControlsEnabled(false);
        lblInfo.setText("Enviando secuencia...");
        bg.submit(() -> {
            try {
                // Preferir GameController para orquestación
                if (controller != null) {
                    controller.onPlayerSequenceSubmitted(seq);
                } else if (combatManager != null && player != null && enemy != null) {
                    // Fallback directo: resolver turno y actualizar HUD
                    combatManager.resolveTurn(player, seq);
                }
                SwingUtilities.invokeLater(() -> {
                    lblInfo.setText("Secuencia procesada");
                    clearSequence();
                    updateHUD();
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    lblInfo.setText("Error procesando secuencia");
                    showError("Error: " + ex.getMessage());
                    setControlsEnabled(true);
                });
            }
        });
    }

    public void showError(String msg) {
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE));
    }

    public void showInfo(String msg) {
        SwingUtilities.invokeLater(() -> lblInfo.setText(msg));
    }

    public void showComboApplied(String comboName) {
        SwingUtilities.invokeLater(() -> {
            lblInfo.setText("Combo: " + comboName);
            // opcional: animación breve en canvas
        });
    }

    // ---------------- Canvas inner class ----------------
    private static class CombatCanvas extends JComponent {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(Color.DARK_GRAY);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(Color.WHITE);
            g2.drawString("Combat Canvas", 10, 20);
            g2.dispose();
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(640, 420);
        }
    }
}