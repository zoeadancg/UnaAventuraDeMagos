package ui;

import game.GameController;
import model.Combatant;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * GameFrame
 * Ventana principal que contiene GamePanel y controla la interacción con
 * GameController.
 */
public class GameFrame extends JFrame {

    private final GamePanel gamePanel;
    private GameController controller;

    private final JLabel statusLabel = new JLabel("Listo");
    private final JProgressBar progressBar = new JProgressBar(0, 100);

    private final JMenuItem miSave = new JMenuItem("Guardar");
    private final JMenuItem miLoad = new JMenuItem("Cargar");
    private final JMenuItem miPause = new JMenuItem("Pausar");
    private final JMenuItem miResume = new JMenuItem("Reanudar");
    private final JMenuItem miExit = new JMenuItem("Salir");

    public GameFrame(GameController controller) {
        super("Una Aventura de Magos");
        this.controller = controller;
        this.gamePanel = new GamePanel();
        if (controller != null)
            controller.addListener(new InternalControllerListener());

        initUI();
        installWindowHandlers();
        pack();
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
    }

    private void initUI() {
        setLayout(new BorderLayout());
        add(gamePanel, BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);
        setJMenuBar(buildMenuBar());
        setPreferredSize(new Dimension(1000, 700));
        // Shortcuts
        setupKeyBindings();
    }

    private JPanel buildStatusBar() {
        JPanel p = new JPanel(new BorderLayout(6, 6));
        p.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        p.add(statusLabel, BorderLayout.WEST);
        progressBar.setStringPainted(true);
        progressBar.setVisible(false);
        p.add(progressBar, BorderLayout.EAST);
        return p;
    }

    private JMenuBar buildMenuBar() {
        JMenuBar mb = new JMenuBar();
        JMenu mFile = new JMenu("Archivo");
        miSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
        miLoad.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK));
        miExit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK));
        mFile.add(miSave);
        mFile.add(miLoad);
        mFile.addSeparator();
        mFile.add(miExit);

        JMenu mGame = new JMenu("Juego");
        miPause.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, 0));
        miResume.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, 0));
        mGame.add(miPause);
        mGame.add(miResume);

        mb.add(mFile);
        mb.add(mGame);

        // Actions
        miSave.addActionListener(e -> onSaveRequested());
        miLoad.addActionListener(e -> onLoadRequested());
        miExit.addActionListener(e -> onExitRequested());
        miPause.addActionListener(e -> onPauseRequested());
        miResume.addActionListener(e -> onResumeRequested());

        return mb;
    }

    private void setupKeyBindings() {
        // Escape to pause
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "pause");
        getRootPane().getActionMap().put("pause", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onPauseRequested();
            }
        });
    }

    private void installWindowHandlers() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onExitRequested();
            }
        });
    }

    // ---------------- Public API ----------------

    public void setGameController(GameController controller) {
        if (this.controller != null)
            this.controller.removeListener(new InternalControllerListener()); // safe no-op if not registered
        this.controller = controller;
        if (controller != null)
            controller.addListener(new InternalControllerListener());
        gamePanel.setGameController(controller);
    }

    public void startGame(Combatant player, Combatant enemy) {
        SwingUtilities.invokeLater(() -> {
            setVisible(true);
            gamePanel.startGame(player, enemy);
            showInfo("Nivel cargado");
        });
    }

    public void stopGame() {
        SwingUtilities.invokeLater(() -> {
            gamePanel.stopGame();
            setVisible(false);
            dispose();
        });
    }

    public void showLoadingProgress(int percent) {
        SwingUtilities.invokeLater(() -> {
            progressBar.setVisible(true);
            progressBar.setValue(Math.max(0, Math.min(100, percent)));
            statusLabel.setText("Cargando... " + percent + "%");
            if (percent >= 100) {
                progressBar.setVisible(false);
                statusLabel.setText("Listo");
            }
        });
    }

    public void showInfo(String text) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(text));
    }

    public void showError(String text) {
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, text, "Error", JOptionPane.ERROR_MESSAGE));
    }

    // ---------------- Menu actions ----------------

    private void onSaveRequested() {
        if (controller == null) {
            showError("Controlador no disponible");
            return;
        }
        showInfo("Guardando partida...");
        controller.saveGame();
    }

    private void onLoadRequested() {
        if (controller == null) {
            showError("Controlador no disponible");
            return;
        }
        // Delegar a controller para mostrar diálogo de saves
        controller.loadGameDialog();
    }

    private void onExitRequested() {
        int opt = JOptionPane.showConfirmDialog(this, "¿Salir y cerrar el juego?", "Confirmar salida",
                JOptionPane.YES_NO_OPTION);
        if (opt == JOptionPane.YES_OPTION) {
            if (controller != null)
                controller.shutdown();
            stopGame();
        }
    }

    private void onPauseRequested() {
        if (controller != null)
            controller.pauseGame();
        gamePanel.setControlsEnabled(false);
        showInfo("Pausado");
    }

    private void onResumeRequested() {
        if (controller != null)
            controller.resumeGame();
        gamePanel.setControlsEnabled(true);
        showInfo("Reanudado");
    }

    // ---------------- Internal listener to receive controller events
    // ----------------

    private class InternalControllerListener implements GameController.GameControllerListener {
        @Override
        public void onLoadingProgress(int percent) {
            showLoadingProgress(percent);
        }

        @Override
        public void onGameStarted() {
            showInfo("Juego iniciado");
        }

        @Override
        public void onReturnedToMenu() {
            showInfo("Volviendo al menú");
        }

        @Override
        public void onSaveCompleted(boolean ok) {
            if (ok)
                showInfo("Partida guardada");
            else
                showError("Error guardando partida");
        }

        @Override
        public void onError(Exception ex) {
            showError("Error: " + ex.getMessage());
        }
    }
}