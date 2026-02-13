package model;

import java.util.*;
import java.util.stream.Collectors;

public class ComboRegistry {
    private final List<Combos> combos;

    public ComboRegistry() {
        this.combos = new ArrayList<>();
    }

    public ComboRegistry(Collection<Combos> initial) {
        this.combos = new ArrayList<>(initial);
    }

    public synchronized void addCombo(Combos c) {
        if (c != null)
            combos.add(c);
    }

    public synchronized List<Combos> getCombos() {
        return Collections.unmodifiableList(new ArrayList<>(combos));
    }

    /**
     * Devuelve el mejor combo que coincide con seq, teniendo en cuenta el elemento
     * del source
     * y la disponibilidad (cooldown). Si source == null, prioriza combos neutrales
     * primero.
     */
    public synchronized Optional<Combos> matchBest(List<Direccion> seq, Combatant source) {
        if (seq == null)
            return Optional.empty();

        // 1) filtrar por coincidencia exacta (si quieres forzar exact match aquí)
        List<Combos> matches = combos.stream()
                .filter(c -> c.matches(seq))
                .collect(Collectors.toList());
        if (matches.isEmpty())
            return Optional.empty();

        // 2) filtrar por disponibilidad (cooldown, recursos)
        List<Combos> available = matches.stream()
                .filter(c -> c.isAvailable(source))
                .collect(Collectors.toList());
        if (available.isEmpty()) {
            // si ninguno disponible, fallback a matches (quizá quieras devolver empty)
            available = matches;
        }

        // 3) priorizar por elemento: primero combos cuyo elemento == source.elemento
        Elemento srcElem = source != null ? source.getElemento() : null;
        List<Combos> byElement = available.stream()
                .filter(c -> c.getElemento() != null && c.getElemento() == srcElem)
                .collect(Collectors.toList());
        if (!byElement.isEmpty()) {
            // elegir el de mayor prioridad (y si empate, cualquiera)
            return byElement.stream().max(Comparator.comparingInt(Combos::getPriority));
        }

        // 4) luego combos neutrales (elemento == null)
        List<Combos> neutral = available.stream()
                .filter(c -> c.getElemento() == null)
                .collect(Collectors.toList());
        if (!neutral.isEmpty()) {
            return neutral.stream().max(Comparator.comparingInt(Combos::getPriority));
        }

        // 5) si no hay neutrales ni por elemento, elegir por prioridad entre los
        // disponibles
        return available.stream().max(Comparator.comparingInt(Combos::getPriority));
    }

    /**
     * matchExact: busca el mejor combo cuya secuencia coincide exactamente con seq.
     * - Coincidencia exacta = misma longitud y misma Direccion en cada índice.
     * - Respeta disponibilidad (isAvailable(source)) y prioridad.
     */
    public synchronized Optional<Combos> matchExact(List<Direccion> seq, Combatant source) {
        if (seq == null)
            return Optional.empty();

        // Filtrar combos que coinciden exactamente con la secuencia
        List<Combos> exactMatches = combos.stream()
                .filter(c -> matchesExactPattern(c, seq))
                .collect(Collectors.toList());

        if (exactMatches.isEmpty())
            return Optional.empty();

        // Filtrar por disponibilidad
        List<Combos> available = exactMatches.stream()
                .filter(c -> c.isAvailable(source))
                .collect(Collectors.toList());

        if (available.isEmpty()) {
            // Si ninguno disponible, devolvemos empty para indicar que no hay exact match
            // usable
            return Optional.empty();
        }

        // Priorizar por elemento del source
        Elemento srcElem = source != null ? source.getElemento() : null;
        List<Combos> byElement = available.stream()
                .filter(c -> c.getElemento() != null && c.getElemento() == srcElem)
                .collect(Collectors.toList());
        if (!byElement.isEmpty()) {
            return byElement.stream().max(Comparator.comparingInt(Combos::getPriority));
        }

        // Luego neutrales
        List<Combos> neutral = available.stream()
                .filter(c -> c.getElemento() == null)
                .collect(Collectors.toList());
        if (!neutral.isEmpty()) {
            return neutral.stream().max(Comparator.comparingInt(Combos::getPriority));
        }

        // Finalmente por prioridad general
        return available.stream().max(Comparator.comparingInt(Combos::getPriority));
    }

    /**
     * Helper: compara patrón del combo con la secuencia de forma exacta.
     * Asume que Combos expone getPattern() -> List<Direccion> o matches(seq) puede
     * ser no-exacta.
     * Si tu clase Combos ya tiene matchesExact, reemplaza la llamada por
     * c.matchesExact(seq).
     */
    private boolean matchesExactPattern(Combos c, List<Direccion> seq) {
        if (c == null || seq == null)
            return false;
        List<Direccion> pattern;
        try {
            pattern = c.getPattern(); // intenta obtener patrón; si no existe, usa c.matches(seq)
        } catch (NoSuchMethodError | AbstractMethodError | RuntimeException e) {
            // Fallback: si Combos no expone getPattern, usar matches (puede ser
            // equivalente)
            return c.matches(seq);
        }
        if (pattern == null)
            return false;
        if (pattern.size() != seq.size())
            return false;
        for (int i = 0; i < pattern.size(); i++) {
            if (pattern.get(i) != seq.get(i))
                return false;
        }
        return true;
    }

    /**
     * Conveniencia: intenta matchExact y si no hay, usa matchBest (comportamiento
     * actual).
     * Útil para TurnResolver cuando quieres preferir exact matches.
     */
    public synchronized Optional<Combos> matchExactOrBest(List<Direccion> seq, Combatant source) {
        Optional<Combos> exact = matchExact(seq, source);
        return exact.isPresent() ? exact : matchBest(seq, source);
    }
}
