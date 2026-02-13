package game;

import model.*;
import ui.GameFrame;
import util.Resources;

import javax.swing.SwingUtilities;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * GameController
 * Orquestador principal entre UI y lógica del juego.
 */
public class GameController {

    private static final Logger LOG = Logger.getLogger(GameController.class.getName());

    public interface GameControllerListener {
        void onLoadingProgress(int percent);

        void onGameStarted();

        void onReturnedToMenu();

        void onSaveCompleted(boolean ok);

        void onError(Exception ex);
    }

    private final CargaNiveles levelLoader;
    private final SaveLoadManager saveManager;
    private final ExecutorService background;
    private final List<GameControllerListener> listeners = Collections.synchronizedList(new ArrayList<>());

    private CombatManager combatManager;
    private TurnResolver turnResolver; // opcional: puede obtenerse desde CombatManager si añades getter
    private GameFrame gameFrame;
    private Combatant currentPlayer;
    private String currentLevelId;
    private volatile boolean paused = false;

    public GameController(CargaNiveles levelLoader, SaveLoadManager saveManager) {
        this.levelLoader = Objects.requireNonNull(levelLoader);
        this.saveManager = Objects.requireNonNull(saveManager);
        this.background = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "GameController-BG");
            t.setDaemon(true);
            return t;
        });
    }

    // ---------------- Listeners ----------------
    public void addListener(GameControllerListener l) {
        if (l == null)
            return;
        listeners.add(l);
    }

    public void removeListener(GameControllerListener l) {
        listeners.remove(l);
    }

    private void notifyLoadingProgress(int p) {
        synchronized (listeners) {
            for (GameControllerListener l : listeners) {
                try {
                    l.onLoadingProgress(p);
                } catch (Exception ignored) {
                }
            }
        }
    }

    private void notifyGameStarted() {
        synchronized (listeners) {
            for (GameControllerListener l : listeners) {
                try {
                    l.onGameStarted();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private void notifyReturnedToMenu() {
        synchronized (listeners) {
            for (GameControllerListener l : listeners) {
                try {
                    l.onReturnedToMenu();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private void notifySaveCompleted(boolean ok) {
        synchronized (listeners) {
            for (GameControllerListener l : listeners) {
                try {
                    l.onSaveCompleted(ok);
                } catch (Exception ignored) {
                }
            }
        }
    }

    private void notifyError(Exception ex) {
        synchronized (listeners) {
            for (GameControllerListener l : listeners) {
                try {
                    l.onError(ex);
                } catch (Exception ignored) {
                }
            }
        }
    }

    // ---------------- Start / Load level ----------------
    public void startGameWithPlayer(Combatant player, String levelId) {
        if (player == null || levelId == null)
            throw new IllegalArgumentException("player y levelId no nulos");
        this.currentPlayer = player;
        this.currentLevelId = levelId;

        background.submit(() -> {
            try {
                levelLoader.preloadLevel(levelId,
                        // onProgress
                        prog -> SwingUtilities.invokeLater(() -> notifyLoadingProgress(prog)),
                        // onDone
                        levelData -> {
                            // --- Extraer difficulty de forma segura (Opción 2) ---
                            int difficulty = 1; // valor por defecto
                            if (levelData != null) {
                                // 1) intentar getter
                                try {
                                    difficulty = levelData.getDifficulty();
                                } catch (NoSuchMethodError | NoSuchFieldError | AbstractMethodError
                                        | RuntimeException ignored) {
                                    // 2) intentar campo público por reflexión
                                    try {
                                        Object fld = levelData.getClass().getField("difficulty").get(levelData);
                                        if (fld instanceof Number)
                                            difficulty = ((Number) fld).intValue();
                                        else if (fld != null)
                                            difficulty = Integer.parseInt(String.valueOf(fld));
                                    } catch (Exception ignored2) {
                                        // 3) fallback: buscar en metadata
                                        try {
                                            Object meta = levelData.getClass().getField("metadata").get(levelData);
                                            if (meta instanceof Map) {
                                                @SuppressWarnings("unchecked")
                                                Map<String, Object> m = (Map<String, Object>) meta;
                                                Object d = m.get("difficulty");
                                                if (d instanceof Number)
                                                    difficulty = ((Number) d).intValue();
                                                else if (d != null) {
                                                    try {
                                                        difficulty = Integer.parseInt(String.valueOf(d));
                                                    } catch (Exception ignored3) {
                                                    }
                                                }
                                            }
                                        } catch (Exception ignored3) {
                                            // se queda el valor por defecto
                                        }
                                    }
                                }
                            }

                            // Preparación en background: combos y manager
                            List<Combos> combos = ComboFactory.defaultCombos();
                            CombatManager newCombatManager = new CombatManager(combos, difficulty);

                            // Encolar en EDT solo lo necesario para la UI
                            SwingUtilities.invokeLater(() -> {
                                try {
                                    this.combatManager = newCombatManager;
                                    // Si necesitas el TurnResolver en el controller, añade getter en CombatManager
                                    // this.turnResolver = combatManager.getTurnResolver();

                                    if (this.gameFrame == null)
                                        this.gameFrame = new GameFrame(this);

                                    // Delegar creación de enemigo al CombatManager
                                    Combatant enemy = this.combatManager.createEnemyForPlayer(player);

                                    this.gameFrame.startGame(player, enemy);

                                    notifyGameStarted();
                                } catch (Exception ex) {
                                    notifyError(ex);
                                }
                            });
                        },
                        // onError
                        ex -> SwingUtilities.invokeLater(() -> notifyError(ex)));
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> notifyError(ex));
            }
        });
    }

    // ---------------- Pause / Resume ----------------
    public void pauseGame() {
        paused = true;
        try {
            if (combatManager != null) {
                try {
                    combatManager.pause();
                } catch (NoSuchMethodError | AbstractMethodError ignored) {
                }
            }
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Error al pausar juego", ex);
            SwingUtilities.invokeLater(() -> notifyError(ex));
        }
    }

    public void resumeGame() {
        paused = false;
        try {
            if (combatManager != null) {
                try {
                    combatManager.resume();
                } catch (NoSuchMethodError | AbstractMethodError ignored) {
                }
            }
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Error al reanudar juego", ex);
            SwingUtilities.invokeLater(() -> notifyError(ex));
        }
    }

    // --------- Separado del Resume y Pause ---------
    public void onPlayerSequenceSubmitted(List<Direccion> sequence) {
        if (combatManager == null || sequence == null || sequence.isEmpty())
            return;
        background.submit(() -> {
            try {
                combatManager.resolveTurn(currentPlayer, sequence);
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> notifyError(ex));
            }
        });
    }

    // ---------------- Save / Build SaveData ----------------

    public void loadGameDialog() {
        if (gameFrame == null)
            return;

        background.submit(() -> {
            try {
                java.util.List<SaveData> saves = saveManager.listSaves();
                SwingUtilities.invokeLater(() -> {
                    if (saves.isEmpty()) {
                        javax.swing.JOptionPane.showMessageDialog(gameFrame, "No hay partidas guardadas.");
                        return;
                    }

                    // Prepare options using safe names
                    String[] options = new String[saves.size()];
                    for (int i = 0; i < saves.size(); i++) {
                        SaveData s = saves.get(i);
                        String d = new Date(s.timestamp).toString();
                        options[i] = (s.name != null ? s.name : "Sin nombre") + " (" + d + ")";
                    }

                    String selection = (String) javax.swing.JOptionPane.showInputDialog(
                            gameFrame,
                            "Selecciona una partida:",
                            "Cargar Partida",
                            javax.swing.JOptionPane.QUESTION_MESSAGE,
                            null,
                            options,
                            options[0]);

                    if (selection != null) {
                        for (int i = 0; i < options.length; i++) {
                            if (options[i].equals(selection)) {
                                loadGame(saves.get(i));
                                return;
                            }
                        }
                    }
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> notifyError(ex));
            }
        });
    }

    private void loadGame(SaveData sd) {
        if (sd == null)
            return;

        Combatant restoredPlayer = Combatant.fromMap(sd.playerMap);
        if (restoredPlayer == null) {
            SwingUtilities.invokeLater(
                    () -> javax.swing.JOptionPane.showMessageDialog(gameFrame, "Error al restaurar jugador."));
            return;
        }

        String lvl = sd.levelId != null ? sd.levelId : "level1";
        startGameWithPlayer(restoredPlayer, lvl);
    }

    public void saveGame() {
        SaveData sd = buildSaveData();
        if (sd == null) {
            SwingUtilities.invokeLater(() -> notifySaveCompleted(false));
            return;
        }

        // Usar saveAsync del SaveLoadManager (implementación recomendada)
        try {
            saveManager.saveAsync(sd,
                    ok -> SwingUtilities.invokeLater(() -> notifySaveCompleted(ok)),
                    ex -> SwingUtilities.invokeLater(() -> notifyError(new Exception("Error guardando partida", ex))));
        } catch (NoSuchMethodError | AbstractMethodError ex) {
            // Fallback: si no existe saveAsync, ejecutar save síncrono en background
            background.submit(() -> {
                try {
                    saveManager.save(sd);
                    SwingUtilities.invokeLater(() -> notifySaveCompleted(true));
                } catch (Exception e) {
                    LOG.log(Level.SEVERE, "Error guardando partida (fallback)", e);
                    SwingUtilities.invokeLater(() -> notifyError(new Exception("Error guardando partida", e)));
                }
            });
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Error iniciando guardado", ex);
            SwingUtilities.invokeLater(() -> notifyError(new Exception("Error guardando partida", ex)));
        }
    }

    private SaveData buildSaveData() {
        try {
            SaveData sd = new SaveData();
            sd.id = UUID.randomUUID().toString();
            sd.name = "Save " + new Date().toString();
            sd.timestamp = System.currentTimeMillis();
            sd.version = 1;
            sd.levelId = currentLevelId;
            if (currentPlayer != null) {
                Map<String, Object> map = currentPlayer.toMap();
                sd.playerMap = map == null ? Collections.emptyMap() : map;
            } else {
                sd.playerMap = Collections.emptyMap();
            }
            return sd;
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Error construyendo SaveData", ex);
            return null;
        }
    }

    // ---------------- Shutdown / Utilities ----------------
    public void shutdown() {
        try {
            if (background != null) {
                background.shutdown();
            }
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Error al cerrar executor", ex);
        }
    }

    public CombatManager getCombatManager() {
        return combatManager;
    }

    public TurnResolver getTurnResolver() {
        return turnResolver;
    } // puede ser null si no lo expones

    public GameFrame getGameFrame() {
        return gameFrame;
    }

    public Combatant getCurrentPlayer() {
        return currentPlayer;
    }

    public String getCurrentLevelId() {
        return currentLevelId;
    }

    public boolean isPaused() {
        return paused;
    }
}