package src.game;

import src.model.*;
import java.util.*;
import java.util.function.BiConsumer;

/**
 * TurnResolver
 * Resuelve un turno completo entre dos combatants, paso a paso.
 */
public class TurnResolver {

    private final List<Combos> combos;
    private final Random rnd;

    public TurnResolver(Collection<Combos> combos) {
        this(combos, new Random());
    }

    public TurnResolver(Collection<Combos> combos, Random rnd) {
        this.combos = combos == null ? Collections.emptyList() : new ArrayList<>(combos);
        this.rnd = rnd == null ? new Random() : rnd;
    }

    /**
     * Resuelve un turno: pSeq = secuencia del jugador, eSeq = secuencia del
     * enemigo.
     * notifyStep recibe (stepIndex, description) para logging/UI opcional.
     */
    public void resolveTurn(Combatant player, List<Direccion> pSeq, Combatant enemy, List<Direccion> eSeq) {
        if (player == null || enemy == null)
            throw new IllegalArgumentException("Combatants no nulos");
        List<Direccion> p = pSeq == null ? Collections.emptyList() : new ArrayList<>(pSeq);
        List<Direccion> e = eSeq == null ? Collections.emptyList() : new ArrayList<>(eSeq);

        int maxSteps = Math.max(p.size(), e.size());
        // Paso por paso
        for (int i = 0; i < maxSteps; i++) {
            if (!player.isAlive() || !enemy.isAlive())
                break;

            Direccion pd = i < p.size() ? p.get(i) : null;
            Direccion ed = i < e.size() ? e.get(i) : null;

            // 1) Calcular daño base (puedes usar modifyDamageByElementAndDirection)
            int playerDamage = pd == null ? 0
                    : player.modifyDamageByElementAndDirection(player.getBaseDamage(), player.getElemento(), pd);
            int enemyDamage = ed == null ? 0
                    : enemy.modifyDamageByElementAndDirection(enemy.getBaseDamage(), enemy.getElemento(), ed);

            // 2) Aplicar daño simultáneo: aplicar escudos y HP
            if (playerDamage > 0 && enemy.isAlive()) {
                enemy.takeDamage(playerDamage);
            }
            if (enemyDamage > 0 && player.isAlive()) {
                player.takeDamage(enemyDamage);
            }

            // 3) Efectos por impacto (on-hit) según elemento y dirección
            if (pd != null && player.isAlive() && enemy.isAlive()) {
                applyOnHitEffects(player, enemy, pd);
            }
            if (ed != null && enemy.isAlive() && player.isAlive()) {
                applyOnHitEffects(enemy, player, ed);
            }

            // 4) Comprobar muertes inmediatas
            if (!player.isAlive() || !enemy.isAlive())
                break;
        }

        // 5) Aplicar combos que dependen de la secuencia completa
        applyCombosIfMatch(player, enemy, p);
        applyCombosIfMatch(enemy, player, e);

        // 6) Post-turn: ticks de status y cooldowns
        tickStatusEffects(player);
        tickStatusEffects(enemy);

        player.tickCooldowns();
        enemy.tickCooldowns();

        // 7) Limpiar efectos expirados (tickStatusEffects ya los remueve)
    }

    // -----------------------
    // Helpers (personaliza según tu StatusEffect API)
    // -----------------------

    private void applyOnHitEffects(Combatant source, Combatant target, Direccion dir) {
        if (source == null || target == null || !target.isAlive())
            return;

        // Ejemplo: eléctrico tiene probabilidad de stun en horizontales
        if (source.getElemento() == Elemento.RAYO && (dir == Direccion.LEFT || dir == Direccion.RIGHT)) {
            if (rnd.nextDouble() < 0.20) {
                target.applyStatus(new StatusEffect(StatusType.STUN, 1, 0, source, true));
            }
        }

        // Water: curación leve en verticales
        if (source.getElemento() == Elemento.AGUA && (dir == Direccion.UP || dir == Direccion.DOWN)) {
            source.heal(Math.max(1, (int) Math.round(source.getMaxHp() * 0.05)));
        }

        // Fire: pequeña probabilidad de aplicar burn en zigzag (ejemplo)
        // Puedes detectar patrones locales si lo deseas.
    }

    private void applyCombosIfMatch(Combatant source, Combatant target, List<Direccion> seq) {
        if (seq == null || seq.isEmpty())
            return;
        // Si tienes ComboRegistry, úsalo; aquí hacemos búsqueda simple
        ComboRegistry registry = new ComboRegistry(this.combos);
        Optional<Combos> opt = registry.matchBest(seq, source);
        if (opt.isPresent()) {
            Combos c = opt.get();
            if (c.isAvailable(source)) {
                c.applyEffect(source, target);
                // aplicar cooldown en el source
                if (c.getCooldown() > 0)
                    source.setComboOnCooldown(c.getName(), c.getCooldown());
            }
        }
    }

    private void tickStatusEffects(Combatant c) {
        if (c == null)
            return;
        // Asumimos que StatusEffect.tick() devuelve StatusTickResult o modifica
        // internamente
        List<StatusEffect> copy = new ArrayList<>(c.getStatusEffects());
        for (StatusEffect s : copy) {
            StatusTickResult r = s.tick();
            if (r != null) {
                switch (r.getKind()) {
                    case DAMAGE:
                        c.takeDamage(r.getAmount());
                        break;
                    case HEAL:
                        c.heal(r.getAmount());
                        break;
                    case STUN:
                        c.addFlag("STUNNED");
                        break;
                    case SLOW:
                        c.addFlag("SLOWED");
                        break;
                    default:
                        break;
                }
            }
            if (s.isExpired()) {
                c.removeStatus(s.getType());
                s.onExpire(c);
            }
        }
    }
}