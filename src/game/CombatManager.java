package game;

import model.*;
import util.Resources;

import javax.swing.SwingUtilities;
import java.util.*;

/**
 * CombatManager
 * Orquesta encuentros, genera secuencias enemigas y delega la resolución de
 * turnos.
 * Notifica eventos a listeners registrados (UI / GameController).
 */
public class CombatManager {

    private final TurnResolver turnResolver;
    private final ComboRegistry comboRegistry;
    private final EnemyFactory enemyFactory;

    private Combatant currentPlayer;
    private Combatant currentEnemy;

    private final int defaultEnemyDifficulty;

    private final List<GameEventListener> listeners = new ArrayList<>();

    public CombatManager(List<Combos> combos, int enemyDifficulty) {
        this.comboRegistry = new ComboRegistry(combos);
        this.turnResolver = new TurnResolver(comboRegistry.getCombos());
        this.enemyFactory = new EnemyFactory();
        this.defaultEnemyDifficulty = Math.max(0, enemyDifficulty);
    }

    // -----------------------
    // Listeners para UI
    // -----------------------
    public interface GameEventListener {
        void onTurnResolved(Combatant player, Combatant enemy);

        void onEnemyDefeated(Combatant enemy);

        void onPlayerDefeated(Combatant player);

        void onError(Exception ex);
    }

    public void addListener(GameEventListener l) {
        if (l != null && !listeners.contains(l))
            listeners.add(l);
    }

    public void removeListener(GameEventListener l) {
        listeners.remove(l);
    }

    private void notifyTurnResolved() {
        SwingUtilities.invokeLater(() -> {
            for (GameEventListener l : listeners) {
                try {
                    l.onTurnResolved(currentPlayer, currentEnemy);
                } catch (Exception ignored) {
                }
            }
        });
    }

    private void notifyEnemyDefeated(Combatant enemy) {
        SwingUtilities.invokeLater(() -> {
            for (GameEventListener l : listeners) {
                try {
                    l.onEnemyDefeated(enemy);
                } catch (Exception ignored) {
                }
            }
        });
    }

    private void notifyPlayerDefeated(Combatant player) {
        SwingUtilities.invokeLater(() -> {
            for (GameEventListener l : listeners) {
                try {
                    l.onPlayerDefeated(player);
                } catch (Exception ignored) {
                }
            }
        });
    }

    private void notifyError(Exception ex) {
        SwingUtilities.invokeLater(() -> {
            for (GameEventListener l : listeners) {
                try {
                    l.onError(ex);
                } catch (Exception ignored) {
                }
            }
        });
    }

    // -----------------------
    // Encuentros y secuencias
    // -----------------------

    /**
     * Inicializa un encuentro para el jugador y devuelve el enemigo creado.
     */
    public Combatant createEnemyForPlayer(Combatant player) {
        this.currentPlayer = player;
        try {
            this.currentEnemy = enemyFactory.createEnemyFor(player, defaultEnemyDifficulty);
            // precarga sprite del enemigo si existe ruta
            if (currentEnemy.getSpritePath() != null) {
                Resources.loadAndCacheImage(currentEnemy.getSpritePath());
            }
            return currentEnemy;
        } catch (Exception ex) {
            notifyError(ex);
            throw ex;
        }
    }

    /**
     * Genera la secuencia enemiga para el turno actual delegando en EnemyFactory.
     */
    public List<Direccion> generateEnemySequence() {
        try {
            if (currentEnemy == null)
                currentEnemy = enemyFactory.createBasicEnemy();
            List<Direccion> seq = enemyFactory.generateSequenceFor(currentEnemy, currentPlayer);
            return EnemyFactory.sanitizeSequence(seq, 4);
        } catch (Exception ex) {
            notifyError(ex);
            return Collections.emptyList();
        }
    }

    // -----------------------
    // Resolución de turno
    // -----------------------

    /**
     * Resuelve un turno completo. Este método puede ser llamado desde un
     * SwingWorker
     * para no bloquear el EDT. Notifica a listeners al finalizar.
     *
     * @param playerSeq secuencia del jugador (puede tener longitud 0..4)
     * @param enemySeq  secuencia del enemigo (puede tener longitud 0..4)
     */
    public void resolveTurn(Combatant player, List<Direccion> playerSeq) {
        this.currentPlayer = player;
        List<Direccion> enemySeq = generateEnemySequence();
        resolveTurn(playerSeq, enemySeq);
    }

    /**
     * Resuelve un turno completo. Este método puede ser llamado desde un
     * SwingWorker
     * para no bloquear el EDT. Notifica a listeners al finalizar.
     *
     * @param playerSeq secuencia del jugador (puede tener longitud 0..4)
     * @param enemySeq  secuencia del enemigo (puede tener longitud 0..4)
     */
    public void resolveTurn(List<Direccion> playerSeq, List<Direccion> enemySeq) {
        try {
            if (currentPlayer == null || currentEnemy == null) {
                throw new IllegalStateException("Jugador o enemigo no inicializados");
            }

            // Sanitizar secuencias
            List<Direccion> pSeq = EnemyFactory.sanitizeSequence(playerSeq, 4);
            List<Direccion> eSeq = EnemyFactory.sanitizeSequence(enemySeq, 4);

            // Delegar la resolución paso a paso
            turnResolver.resolveTurn(currentPlayer, pSeq, currentEnemy, eSeq);

            // Aplicar combos exactos si coinciden
            applyCombosIfMatch(currentPlayer, currentEnemy, pSeq);
            applyCombosIfMatch(currentEnemy, currentPlayer, eSeq);

            // Limpieza post turno
            postTurnHousekeeping(currentPlayer);
            postTurnHousekeeping(currentEnemy);

            // Notificar UI
            notifyTurnResolved();

            // Comprobar muertes y notificar
            if (!currentEnemy.isAlive()) {
                notifyEnemyDefeated(currentEnemy);
                // liberar recursos del enemigo
                try {
                    if (currentEnemy.getSpritePath() != null)
                        Resources.unload(currentEnemy.getSpritePath());
                } catch (Exception ignored) {
                }
            }
            if (!currentPlayer.isAlive()) {
                notifyPlayerDefeated(currentPlayer);
            }

        } catch (Exception ex) {
            notifyError(ex);
            throw ex;
        }
    }

    private void applyCombosIfMatch(Combatant source, Combatant target, List<Direccion> seq) {
        try {
            Optional<Combos> match = comboRegistry.matchExact(seq, source);
            match.ifPresent(c -> c.applyEffect(source, target));
        } catch (Exception ex) {
            // no detener el flujo por errores en combos; notificar
            notifyError(ex);
        }
    }

    // -----------------------
    // Post turno y utilidades
    // -----------------------

    private void postTurnHousekeeping(Combatant c) {
        if (c == null)
            return;
        // Tick de efectos por turno
        c.tickStatusEffects(); // debe aplicar daño/curación y reducir duraciones

        // Reducir cooldowns de habilidades si aplica
        c.tickCooldowns();

        // Limpiar efectos expirados
        List<StatusEffect> toRemove = new ArrayList<>();
        for (StatusEffect s : c.getStatusEffects()) {
            if (s.isExpired())
                toRemove.add(s);
        }
        toRemove.forEach(s -> c.removeStatus(s.getType()));
    }

    public void resetEncounter() {
        if (currentEnemy != null) {
            try {
                if (currentEnemy.getSpritePath() != null)
                    Resources.unload(currentEnemy.getSpritePath());
            } catch (Exception ignored) {
            }
        }
        currentEnemy = null;
    }

    public Combatant getCurrentEnemy() {
        return currentEnemy;
    }

    public Combatant getCurrentPlayer() {
        return currentPlayer;
    }

    public List<Combos> getAvailableCombos() {
        return comboRegistry.getCombos();
    }

    // en CombatManager.java
    public TurnResolver getTurnResolver() {
        return this.turnResolver;
    }

    public void setCurrentEnemy(Combatant enemy) {
        this.currentEnemy = enemy;
    }

    // en CombatManager
    private volatile boolean paused = false;

    public synchronized void pause() {
        paused = true;
        // si hay timers o threads internos, pausarlos aquí
    }

    public synchronized void resume() {
        paused = false;
        // reanudar timers o threads si aplica
    }

    public boolean isPaused() {
        return paused;
    }

    // en CombatManager
    public void resolveTurnFor(Combatant actor, List<Direccion> actorSeq, Combatant target, List<Direccion> targetSeq) {
        this.turnResolver.resolveTurn(actor, actorSeq, target, targetSeq);
    }
}