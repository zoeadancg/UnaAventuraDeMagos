package model;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry thread-safe para ComboEffect con mapeo bidireccional.
 */
public final class ComboEffectRegistry {

    private final Map<String, ComboEffect> idToEffect = new ConcurrentHashMap<>();
    private final Map<ComboEffect, String> effectToId = Collections.synchronizedMap(new IdentityHashMap<>());

    public ComboEffectRegistry() {
    }

    public void register(String id, ComboEffect effect) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(effect, "effect");
        if (id.trim().isEmpty())
            throw new IllegalArgumentException("id vacío");
        idToEffect.put(id, effect);
        effectToId.put(effect, id);
    }

    public void registerAll(Map<String, ComboEffect> effects) {
        if (effects == null)
            return;
        effects.forEach(this::register);
    }

    public ComboEffect getById(String id) {
        if (id == null)
            return null;
        return idToEffect.get(id);
    }

    /**
     * Devuelve el id asociado a la instancia de ComboEffect o null si no está
     * registrado.
     * Usa IdentityHashMap para distinguir instancias lambda distintas.
     */
    public String getIdFor(ComboEffect effect) {
        if (effect == null)
            return null;
        String id = effectToId.get(effect);
        if (id != null)
            return id;
        // Fallback: buscar por igualdad en idToEffect (por si las instancias no son las
        // mismas)
        for (Map.Entry<String, ComboEffect> e : idToEffect.entrySet()) {
            if (Objects.equals(e.getValue(), effect))
                return e.getKey();
        }
        return null;
    }

    public void unregister(String id) {
        if (id == null)
            return;
        ComboEffect removed = idToEffect.remove(id);
        if (removed != null)
            effectToId.remove(removed);
    }

    public List<String> listIds() {
        return new ArrayList<>(idToEffect.keySet());
    }

    public void registerDefaults() {
        register("damage_small", (src, tgt) -> {
            if (src == null || tgt == null)
                return;
            int dmg = Math.max(1, src.getBaseDamage() / 2);
            tgt.takeDamage(dmg);
        });
        register("fire_strike", (src, tgt) -> {
            if (src == null || tgt == null)
                return;
            int dmg = Math.max(1, src.getBaseDamage() + 3);
            tgt.takeDamage(dmg);
            tgt.applyStatus(new StatusEffect(StatusType.BURN, 2, 4, null, true));
        });
        // registra más efectos según necesites
    }
}