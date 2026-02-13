package model;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Combos: patrón + metadatos + efecto.
 *
 * Contiene métodos estáticos fromMapSafe / toMapSafe y helpers privados.
 * Asegúrate de mover ComboEffect y ComboEffectRegistry a sus propios archivos
 * si ya existen.
 */
public class Combos {
    private final String name;
    private final String description;
    private final List<Direccion> pattern;
    private final Elemento elemento; // puede ser null = neutral
    private final int cooldown; // turnos de recarga
    private final int cost;
    private final int priority; // mayor = más prioridad en matching
    private final ComboEffect effect;

    public Combos(String name,
            String description,
            List<Direccion> pattern,
            Elemento elemento,
            int cooldown,
            int cost,
            int priority,
            ComboEffect effect) {
        this.name = Objects.requireNonNull(name);
        this.description = description == null ? "" : description;
        this.pattern = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(pattern)));
        this.elemento = elemento;
        this.cooldown = Math.max(0, cooldown);
        this.cost = Math.max(0, cost);
        this.priority = Math.max(0, priority);
        this.effect = Objects.requireNonNull(effect);
    }

    // Constructor simple
    public Combos(String name, List<Direccion> pattern, Elemento elemento, ComboEffect effect) {
        this(name, "", pattern, elemento, 0, 0, 0, effect);
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public List<Direccion> getPattern() {
        return pattern;
    }

    public Elemento getElemento() {
        return elemento;
    }

    public int getCooldown() {
        return cooldown;
    }

    public int getCost() {
        return cost;
    }

    public int getPriority() {
        return priority;
    }

    public ComboEffect getEffect() {
        return effect;
    }

    public boolean matches(List<Direccion> seq) {
        if (seq == null)
            return false;
        if (seq.size() != pattern.size())
            return false;
        for (int i = 0; i < pattern.size(); i++) {
            if (pattern.get(i) != seq.get(i))
                return false;
        }
        return true;
    }

    public void applyEffect(Combatant source, Combatant target) {
        if (target == null || !target.isAlive())
            return;
        effect.apply(source, target);
    }

    public boolean isAvailable(Combatant source) {
        if (source == null)
            return false;
        return !source.hasComboOnCooldown(name);
    }

    @Override
    public String toString() {
        return "Combos{" + name + ", elem=" + elemento + ", pattern=" + pattern + ", prio=" + priority + "}";
    }

    // ------------------ Serialización / Deserialización segura ------------------

    /**
     * Intenta construir un Combos desde un Map. Devuelve Optional.empty() si el map
     * es inválido.
     * effectRegistry puede ser null si tus efectos no requieren resolución por id.
     */
    public static Optional<Combos> fromMapSafe(Map<String, Object> m, ComboEffectRegistry effectRegistry) {
        if (m == null)
            return Optional.empty();
        try {
            Object nameObj = m.get("name");
            if (!(nameObj instanceof String))
                return Optional.empty();
            String name = ((String) nameObj).trim();
            if (name.isEmpty())
                return Optional.empty();

            String description = Optional.ofNullable(m.get("description")).map(Object::toString).orElse("");

            Elemento elemento = null;
            Object elObj = m.get("elemento");
            if (elObj != null) {
                try {
                    elemento = Elemento.valueOf(String.valueOf(elObj).trim());
                } catch (IllegalArgumentException ignored) {
                    elemento = null;
                }
            }

            int cooldown = parseIntSafe(m.get("cooldown"), 0);
            int cost = parseIntSafe(m.get("cost"), 0);
            int priority = parseIntSafe(m.get("priority"), 0);

            List<Direccion> pattern = parsePatternSafe(m.get("pattern"));
            if (pattern == null || pattern.isEmpty())
                return Optional.empty();

            // Resolver efecto: puede venir como id String o como Map con "id"
            ComboEffect effect = null;
            Object effObj = m.get("effect");
            if (effObj instanceof String && effectRegistry != null) {
                effect = effectRegistry.getById((String) effObj);
            } else if (effObj instanceof Map && effectRegistry != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> effMap = (Map<String, Object>) effObj;
                Object idObj = effMap.get("id");
                if (idObj instanceof String)
                    effect = effectRegistry.getById((String) idObj);
            }

            // Si no tienes registry, podrías construir un efecto por defecto o rechazar
            if (effect == null) {
                // opción: intentar construir un efecto simple desde el map (no implementado
                // aquí)
                return Optional.empty();
            }

            Combos c = new Combos(name, description, pattern, elemento, cooldown, cost, priority, effect);
            return Optional.of(c);
        } catch (Exception ex) {
            ex.printStackTrace();
            return Optional.empty();
        }
    }

    /**
     * Serializa Combos a Map de forma segura (lista de strings para pattern, effect
     * por id).
     */
    // Cambia la firma para recibir el registry o accede a una instancia global
    public static Map<String, Object> toMapSafe(Combos c, ComboEffectRegistry registry) {
        Map<String, Object> m = new HashMap<>();
        if (c == null)
            return m;
        m.put("name", c.getName());
        m.put("description", c.getDescription());
        m.put("elemento", c.getElemento() == null ? null : c.getElemento().name());
        m.put("cooldown", c.getCooldown());
        m.put("cost", c.getCost());
        m.put("priority", c.getPriority());
        m.put("pattern", c.getPattern().stream().map(Direccion::name).collect(Collectors.toList()));
        // obtener id del efecto desde el registry
        String effectId = registry == null ? null : registry.getIdFor(c.getEffect());
        m.put("effect", effectId);
        return m;
    }

    // ------------------ Helpers privados ------------------

    private static int parseIntSafe(Object o, int def) {
        if (o == null)
            return def;
        if (o instanceof Number)
            return ((Number) o).intValue();
        try {
            return Integer.parseInt(String.valueOf(o).trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Direccion> parsePatternSafe(Object o) {
        if (o == null)
            return Collections.emptyList();
        List<Direccion> out = new ArrayList<>();
        if (o instanceof List) {
            for (Object it : (List<Object>) o) {
                if (it == null)
                    continue;
                try {
                    out.add(Direccion.valueOf(String.valueOf(it).trim()));
                } catch (IllegalArgumentException ignored) {
                }
            }
        } else {
            String s = String.valueOf(o);
            for (String part : s.split("[,;\\s]+")) {
                try {
                    out.add(Direccion.valueOf(part.trim()));
                } catch (Exception ignored) {
                }
            }
        }
        return out;
    }
}