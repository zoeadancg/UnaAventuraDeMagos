package src.model;

import java.util.*;
import java.util.stream.Collectors;

/**
 * ComboFactory: parseo seguro entre Map <-> Combos
 * Usa ComboEffectRegistry para resolver efectos por id.
 */
public final class ComboFactory {

    private ComboFactory() {
    }

    /**
     * Construye un Combos desde un Map de forma tolerante.
     * Devuelve Optional.empty() si el map no contiene datos mínimos válidos.
     */
    public static Optional<Combos> fromMap(Map<String, Object> m, ComboEffectRegistry effectRegistry) {
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

            // elemento tolerante
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

            // resolver efecto por id usando registry
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
            if (effect == null)
                return Optional.empty();

            Combos c = new Combos(name, description, pattern, elemento, cooldown, cost, priority, effect);
            return Optional.of(c);
        } catch (Exception ex) {
            ex.printStackTrace();
            return Optional.empty();
        }
    }

    /**
     * Serializa un Combos a Map usando el registry para obtener el effectId.
     */
    public static Map<String, Object> toMap(Combos c, ComboEffectRegistry registry) {
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
        String effectId = registry == null ? null : registry.getIdFor(c.getEffect());
        m.put("effect", effectId);
        return m;
    }

    // ---------------- Helpers ----------------

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

    /**
     * Devuelve una lista de combos por defecto para pruebas o inicio rápido.
     */
    public static List<Combos> defaultCombos() {
        List<Combos> list = new ArrayList<>();

        // Lightning: [RIGHT, RIGHT, RIGHT] -> 20 dmg
        list.add(new Combos(
                "Lightning",
                "Golpe de rayo concentrado",
                Arrays.asList(Direccion.RIGHT, Direccion.RIGHT, Direccion.RIGHT),
                Elemento.RAYO,
                0, 0, 10,
                (src, tgt) -> {
                    tgt.takeDamage(20);
                }));

        // FireBall: [RIGHT, UP, RIGHT] -> 15 dmg + BURN
        list.add(new Combos(
                "FireBall",
                "Bola de fuego que quema al enemigo",
                Arrays.asList(Direccion.RIGHT, Direccion.UP, Direccion.RIGHT),
                Elemento.FUEGO,
                0, 0, 10,
                (src, tgt) -> {
                    tgt.takeDamage(15);
                    tgt.applyStatus(new StatusEffect(StatusType.BURN, 3, 5, null, true));
                }));

        // Blizzard: [LEFT, DOWN, LEFT] -> 10 dmg + FREEZE
        list.add(new Combos(
                "Blizzard",
                "Tormenta de hielo que congela",
                Arrays.asList(Direccion.LEFT, Direccion.DOWN, Direccion.LEFT),
                Elemento.HIELO,
                0, 0, 10,
                (src, tgt) -> {
                    tgt.takeDamage(10);
                    tgt.applyStatus(new StatusEffect(StatusType.FREEZE, 2, 0, null, true));
                }));

        // Flood: [DOWN, DOWN, DOWN] -> 10 dmg + SLOW (simulando Wet/Pushback)
        list.add(new Combos(
                "Flood",
                "Inundación que ralentiza",
                Arrays.asList(Direccion.DOWN, Direccion.DOWN, Direccion.DOWN),
                Elemento.AGUA,
                0, 0, 10,
                (src, tgt) -> {
                    tgt.takeDamage(10);
                    tgt.applyStatus(new StatusEffect(StatusType.SLOW, 3, 0, null, true));
                }));

        return list;
    }
}
