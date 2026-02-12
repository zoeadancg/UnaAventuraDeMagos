package src.ui;

import src.game.GameController;
import src.model.Combatant;
import src.model.Direccion;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.ArrayList;

public class MainFrame extends JFrame implements GameController.GameControllerListener {
    private final GameController controller;

    private JLabel lblInfo = new JLabel("Listo");
    private JProgressBar progress = new JProgressBar(0, 100);
    private JPanel combatPanel = new JPanel();
    private JPanel sequencePanel = new JPanel();
    private JButton btnSend = new JButton("Enviar");
    private JButton btnPause = new JButton("Pausar");
    private List<Direccion> currentSequence = new ArrayList<>();

    public MainFrame(GameController controller) {
        super("Mi Juego");
        this.controller = controller;
        this.controller.addListener(this);
        initUI();
    }

    private void initUI() {
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onExitRequested();
            }
        });

        // Layout básico
        setLayout(new BorderLayout());
        add(buildTopPanel(), BorderLayout.NORTH);
        add(combatPanel, BorderLayout.CENTER);
        add(buildBottomPanel(), BorderLayout.SOUTH);
        add(buildRightPanel(), BorderLayout.EAST);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private JPanel buildTopPanel() {
        JPanel p = new JPanel(new BorderLayout());
        JMenuBar mb = new JMenuBar();
        JMenu mFile = new JMenu("Archivo");
        JMenuItem miLoad = new JMenuItem("Cargar");
        miLoad.addActionListener(e -> onLoadRequested());
        JMenuItem miSave = new JMenuItem("Guardar");
        miSave.addActionListener(e -> controller.saveGame());
        JMenuItem miExit = new JMenuItem("Salir");
        miExit.addActionListener(e -> onExitRequested());
        mFile.add(miLoad);
        mFile.add(miSave);
        mFile.addSeparator();
        mFile.add(miExit);
        mb.add(mFile);
        p.add(mb, BorderLayout.NORTH);

        JPanel status = new JPanel(new FlowLayout(FlowLayout.LEFT));
        status.add(lblInfo);
        status.add(progress);
        p.add(status, BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildBottomPanel() {
        sequencePanel.setLayout(new FlowLayout());
        // ejemplo de botones direccionales
        JButton up = new JButton("↑");
        up.addActionListener(e -> addToSequence(Direccion.UP));
        JButton left = new JButton("←");
        left.addActionListener(e -> addToSequence(Direccion.LEFT));
        JButton right = new JButton("→");
        right.addActionListener(e -> addToSequence(Direccion.RIGHT));
        JButton down = new JButton("↓");
        down.addActionListener(e -> addToSequence(Direccion.DOWN));
        sequencePanel.add(left);
        sequencePanel.add(up);
        sequencePanel.add(down);
        sequencePanel.add(right);

        btnSend.addActionListener(e -> submitSequence());
        btnPause.addActionListener(e -> togglePause());

        JPanel p = new JPanel(new BorderLayout());
        p.add(sequencePanel, BorderLayout.CENTER);

        JPanel actions = new JPanel();
        actions.add(btnSend);
        actions.add(btnPause);
        p.add(actions, BorderLayout.EAST);
        return p;
    }

    private JPanel buildRightPanel() {
        JPanel p = new JPanel();
        p.setPreferredSize(new Dimension(200, 0));
        p.add(new JLabel("Combos"));
        // lista de combos, cooldowns, etc.
        return p;
    }

    private void addToSequence(Direccion d) {
        currentSequence.add(d);
        lblInfo.setText("Secuencia: " + currentSequence.size());
    }

    private void submitSequence() {
        if (currentSequence.isEmpty()) {
            lblInfo.setText("Secuencia vacía");
            return;
        }
        setControlsEnabled(false);
        lblInfo.setText("Enviando secuencia...");
        // delegar al controller (asegúrate que el controller maneje en background)
        controller.onPlayerSequenceSubmitted(new ArrayList<>(currentSequence));
    }

    private void togglePause() {
        if (controller == null)
            return;
        if (controller.isPaused()) {
            controller.resumeGame();
            btnPause.setText("Pausar");
        } else {
            controller.pauseGame();
            btnPause.setText("Reanudar");
        }
    }

    private void setControlsEnabled(boolean enabled) {
        btnSend.setEnabled(enabled);
        sequencePanel.setEnabled(enabled);
        // deshabilitar botones hijos
        for (Component c : sequencePanel.getComponents())
            c.setEnabled(enabled);
    }

    private void onLoadRequested() {
        if (controller == null) {
            JOptionPane.showMessageDialog(this, "Controlador no disponible", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        controller.loadGameDialog();
    }

    private void onExitRequested() {
        int opt = JOptionPane.showConfirmDialog(this, "¿Salir y cerrar el juego?", "Confirmar",
                JOptionPane.YES_NO_OPTION);
        if (opt == JOptionPane.YES_OPTION) {
            if (controller != null)
                controller.shutdown();
            dispose();
        }
    }

    // ---------------- Callbacks del GameController ----------------
    @Override
    public void onLoadingProgress(int percent) {
        SwingUtilities.invokeLater(() -> {
            progress.setValue(percent);
            lblInfo.setText("Cargando " + percent + "%");
        });
    }

    @Override
    public void onGameStarted() {
        SwingUtilities.invokeLater(() -> {
            lblInfo.setText("Juego iniciado");
            setControlsEnabled(true);
            currentSequence.clear();
        });
    }

    @Override
    public void onReturnedToMenu() {
        SwingUtilities.invokeLater(() -> {
            lblInfo.setText("Volviendo al menú");
            setControlsEnabled(false);
        });
    }

    @Override
    public void onSaveCompleted(boolean ok) {
        SwingUtilities.invokeLater(() -> {
            lblInfo.setText(ok ? "Guardado correcto" : "Error guardando");
        });
    }

    @Override
    public void onError(Exception ex) {
        SwingUtilities.invokeLater(() -> {
            lblInfo.setText("Error: " + ex.getMessage());
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            setControlsEnabled(true);
        });
    }
}
