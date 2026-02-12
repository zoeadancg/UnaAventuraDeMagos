package src.model;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Combatant
 * Clase que representa a un combatiente (jugador o enemigo).
 * Incluye constructores, factory, serialización a Map y lógica básica de
 * combate.
 */
public class Combatant {

    private final String id;
    private String name;
    private Elemento elemento;

    private int maxHp;
    private int hp;
    private int baseDamage;
    private int shield;

    private final List<StatusEffect> statusEffects = new ArrayList<>();
    private final Map<String, Integer> comboCooldowns = new ConcurrentHashMap<>();
    private final Map<String, Integer> abilityCooldowns = new ConcurrentHashMap<>();
    private final Set<String> flags = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private String spritePath;

    // ---------------- Constructors ----------------

    /**
     * Constructor principal (ya existente).
     */
    public Combatant(String id, String name, Elemento elemento, int maxHp, int baseDamage) {
        this.id = Objects.requireNonNull(id);
        this.name = name == null ? "Unknown" : name;
        this.elemento = elemento;
        this.maxHp = Math.max(1, maxHp);
        this.hp = this.maxHp;
        this.baseDamage = Math.max(0, baseDamage);
        this.shield = 0;
    }

    /**
     * Constructor sobrecargado que genera un id automáticamente.
     */
    public Combatant(String name, Elemento elemento, int maxHp, int baseDamage) {
        this(UUID.randomUUID().toString(), name, elemento, maxHp, baseDamage);
    }

    /**
     * Factory estático para crear Combatant de forma explícita.
     */
    public static Combatant create(String name, Elemento elemento, int maxHp, int baseDamage) {
        return new Combatant(UUID.randomUUID().toString(), name, elemento, maxHp, baseDamage);
    }

    // ---------------- Getters / Setters ----------------

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Elemento getElemento() {
        return elemento;
    }

    public void setElemento(Elemento e) {
        this.elemento = e;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public int getHp() {
        return hp;
    }

    public int getBaseDamage() {
        return baseDamage;
    }

    public String getSpritePath() {
        return spritePath;
    }

    public void setSpritePath(String p) {
        this.spritePath = p;
    }

    public synchronized int getShield() {
        return shield;
    }

    // ---------------- Shield / Damage / Heal ----------------

    public synchronized void addShield(int amount) {
        if (amount == 0)
            return;
        shield = Math.max(0, shield + amount);
    }

    public synchronized void subtractShield(int amount) {
        if (amount <= 0)
            return;
        shield = Math.max(0, shield - amount);
    }

    public synchronized void takeDamage(int amount) {
        if (amount <= 0 || !isAlive())
            return;
        int remaining = amount;
        if (shield > 0) {
            int used = Math.min(shield, remaining);
            shield -= used;
            remaining -= used;
        }
        if (remaining > 0) {
            hp = Math.max(0, hp - remaining);
        }
    }

    public synchronized void heal(int amount) {
        if (amount <= 0 || !isAlive())
            return;
        hp = Math.min(maxHp, hp + amount);
    }

    public boolean isAlive() {
        return hp > 0;
    }

    // ---------------- Status effects ----------------

    public synchronized void applyStatus(StatusEffect s) {
        if (s == null)
            return;
        for (StatusEffect existing : statusEffects) {
            if (existing.getType() == s.getType()) {
                int oldPower = existing.getPower();
                int oldTurns = existing.getRemainingTurns();
                existing.mergeWith(s);
                if (existing.getPower() > oldPower || existing.getRemainingTurns() > oldTurns) {
                    existing.applyImmediate(this);
                }
                return;
            }
        }
        statusEffects.add(s);
        s.applyImmediate(this);
    }

    public synchronized void removeStatus(StatusType t) {
        statusEffects.removeIf(s -> s.getType() == t);
    }

    public synchronized void removeStatusInstance(StatusEffect s) {
        statusEffects.remove(s);
    }

    public synchronized List<StatusEffect> getStatusEffects() {
        return Collections.unmodifiableList(new ArrayList<>(statusEffects));
    }

    /**
     * Ejecuta un tick sobre los status effects: aplica daño/curación/flags y expira
     * efectos.
     */
    public synchronized void tickStatusEffects() {
        List<StatusEffect> copy = new ArrayList<>(statusEffects);
        for (StatusEffect s : copy) {
            StatusTickResult r = s.tick();
            if (r != null) {
                switch (r.getKind()) {
                    case DAMAGE:
                        takeDamage(r.getAmount());
                        break;
                    case HEAL:
                        heal(r.getAmount());
                        break;
                    case STUN:
                        addFlag("STUNNED");
                        break;
                    case SLOW:
                        addFlag("SLOWED");
                        break;
                    default:
                        break;
                }
            }
            if (s.isExpired()) {
                s.onExpire(this);
                statusEffects.remove(s);
            }
        }
    }

    // ---------------- Cooldowns ----------------

    public boolean hasComboOnCooldown(String comboName) {
        Integer rem = comboCooldowns.get(comboName);
        return rem != null && rem > 0;
    }

    public void setComboOnCooldown(String comboName, int turns) {
        if (comboName == null || turns <= 0)
            return;
        comboCooldowns.put(comboName, turns);
    }

    public void tickComboCooldowns() {
        comboCooldowns.replaceAll((k, v) -> Math.max(0, v - 1));
    }

    public void tickCooldowns() {
        tickComboCooldowns();
        abilityCooldowns.replaceAll((k, v) -> Math.max(0, v - 1));
    }

    // ---------------- Misc / Hooks ----------------

    public void onPerfectDodge() {
        // hook vacío por defecto; override o implementar lógica si hace falta
    }

    public int modifyDamageByElementAndDirection(int baseDamage, Elemento attackElement, Direccion dir) {
        // placeholder: aplicar resistencias/ventajas elementales y modificadores por
        // dirección
        // por defecto devuelve baseDamage sin cambios
        return baseDamage;
    }

    // ---------------- Flags ----------------

    public boolean hasFlag(String f) {
        return flags.contains(f);
    }

    public void addFlag(String f) {
        if (f != null)
            flags.add(f);
    }

    public void removeFlag(String f) {
        if (f != null)
            flags.remove(f);
    }

    // ---------------- Serialization / Debug ----------------

    /**
     * Serializa el estado esencial a un Map para guardado/depuración.
     */
    public Map<String, Object> toMap() {
        Map<String, Object> m = new HashMap<>();
        m.put("id", id);
        m.put("name", name);
        m.put("elemento", elemento == null ? null : elemento.name());
        m.put("hp", hp);
        m.put("maxHp", maxHp);
        m.put("baseDamage", baseDamage);
        m.put("shield", shield);
        m.put("spritePath", spritePath);

        // comboCooldowns snapshot
        Map<String, Integer> ccCopy = new HashMap<>(comboCooldowns);
        m.put("comboCooldowns", ccCopy);

        // statusEffects simple serialization
        List<Map<String, Object>> seList = new ArrayList<>();
        synchronized (this) {
            for (StatusEffect s : statusEffects) {
                Map<String, Object> sm = new HashMap<>();
                sm.put("type", s.getType().name());
                sm.put("duration", s.getRemainingTurns());
                sm.put("power", s.getPower());
                seList.add(sm);
            }
        }
        m.put("statusEffects", seList);

        return m;
    }

    @SuppressWarnings("unchecked")
    public static Combatant fromMap(Map<String, Object> map) {
        if (map == null)
            return null;
        try {
            String name = (String) map.getOrDefault("name", "Unknown");
            String elemStr = (String) map.get("elemento");
            Elemento el = Elemento.valueOf(elemStr); // O safe parse
            int maxHp = ((Number) map.getOrDefault("maxHp", 100)).intValue();
            int baseDamage = ((Number) map.getOrDefault("baseDamage", 10)).intValue();

            // Reconstruct basic
            Combatant c = new Combatant((String) map.get("id"), name, el, maxHp, baseDamage);

            // Restore dynamic
            c.hp = ((Number) map.getOrDefault("hp", maxHp)).intValue();
            c.shield = ((Number) map.getOrDefault("shield", 0)).intValue();
            c.spritePath = (String) map.get("spritePath");

            // Restore status effects (simplified)
            // Note: This requires a way to reconstruct StatusEffects easily or ignoring
            // them
            // For now, we ignore complex reconstruction of status effects to keep it
            // simple,
            // or we could implement a basic parser if needed.
            // Given the error was just about loadGameDialog, basic restoration is enough to
            // start.

            return c;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public String toString() {
        return "Combatant{" + name + " hp=" + hp + "/" + maxHp + " dmg=" + baseDamage + " elem=" + elemento + " shield="
                + shield + "}";
    }
}