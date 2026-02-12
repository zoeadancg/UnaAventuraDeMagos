package src.model;

@FunctionalInterface
public interface ComboEffect {
    /**
     * Aplica el efecto del combo desde source hacia target.
     * Implementa aquí daño, estados (StatusEffect), curación, etc.
     */
    void apply(Combatant source, Combatant target);
}
