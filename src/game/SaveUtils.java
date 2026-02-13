package game;

import model.Combatant;
import model.Elemento;
import model.StatusEffect;
import model.StatusType;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;

/**
 * Utilidades para reconstruir objetos desde Map (save) usando la API pública de
 * Combatant.
 */
final class SaveUtils {

    private SaveUtils() {
    }

    @SuppressWarnings("unchecked")
    static Combatant reconstructCombatant(Map<String, Object> data) throws SaveLoadManager.InvalidSaveException {
        if (data == null)
            throw new SaveLoadManager.InvalidSaveException("playerMap nulo");

        try {
            String id = safeString(data.get("id"));
            String name = safeString(data.getOrDefault("name", "Unknown"));
            String elem = safeString(data.get("elemento"));
            Elemento elemento = elem == null ? null : Elemento.valueOf(elem);

            Integer maxHp = safeInt(data.get("maxHp"));
            Integer hp = safeInt(data.get("hp"));
            Integer baseDamage = safeInt(data.get("baseDamage"));
            Integer shield = safeInt(data.getOrDefault("shield", 0));

            if (id == null || maxHp == null || hp == null || baseDamage == null) {
                throw new SaveLoadManager.InvalidSaveException(
                        "Campos obligatorios faltan en playerMap (id, maxHp, hp, baseDamage)");
            }

            Combatant c = new Combatant(id, name, elemento, maxHp, baseDamage);

            synchronized (c) {
                int currentHp = c.getHp();
                if (currentHp < hp)
                    c.heal(hp - currentHp);
                else if (currentHp > hp)
                    c.takeDamage(currentHp - hp);

                int currentShield = c.getShield();
                if (currentShield < shield)
                    c.addShield(shield - currentShield);
                else if (currentShield > shield)
                    c.subtractShield(currentShield - shield);
            }

            Object rawCooldowns = data.get("comboCooldowns");
            if (rawCooldowns instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> raw = (Map<String, Object>) rawCooldowns;
                for (Map.Entry<String, Object> e : raw.entrySet()) {
                    String comboName = e.getKey();
                    Integer val = safeInt(e.getValue());
                    if (comboName != null && val != null && val > 0) {
                        c.setComboOnCooldown(comboName, val);
                    }
                }
            }

            Object rawStatuses = data.get("statusEffects");
            if (rawStatuses instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> list = (List<Object>) rawStatuses;
                for (Object it : list) {
                    StatusEffect se = parseStatusEffect(it);
                    if (se != null)
                        c.applyStatus(se);
                }
            }

            Object sprite = data.get("spritePath");
            if (sprite != null) {
                String sp = safeString(sprite);
                if (sp != null)
                    c.setSpritePath(sp);
            }

            return c;
        } catch (SaveLoadManager.InvalidSaveException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SaveLoadManager.InvalidSaveException("Error reconstruyendo Combatant: " + ex.getMessage(), ex);
        }
    }

    private static String safeString(Object o) {
        if (o == null)
            return null;
        String s = String.valueOf(o).trim();
        return s.isEmpty() ? null : s;
    }

    private static Integer safeInt(Object o) {
        if (o == null)
            return null;
        if (o instanceof Number)
            return ((Number) o).intValue();
        try {
            String s = String.valueOf(o).trim();
            if (s.isEmpty())
                return null;
            return Integer.parseInt(s);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static StatusEffect parseStatusEffect(Object it) {
        if (it == null)
            return null;
        try {
            if (it instanceof String) {
                String s = ((String) it).trim();
                StatusType t = StatusType.valueOf(s);
                return new StatusEffect(t, 1, 0, null, true);
            } else if (it instanceof Map) {
                Map<String, Object> m = (Map<String, Object>) it;
                String typeS = safeString(m.get("type"));
                StatusType type = typeS == null ? null : StatusType.valueOf(typeS);
                int turns = safeInt(m.getOrDefault("duration", m.getOrDefault("turns", 1))) == null ? 1
                        : safeInt(m.getOrDefault("duration", m.getOrDefault("turns", 1)));
                int power = safeInt(m.getOrDefault("power", 0)) == null ? 0 : safeInt(m.getOrDefault("power", 0));
                Boolean persistent = m.get("persistent") == null ? Boolean.TRUE
                        : Boolean.valueOf(String.valueOf(m.get("persistent")));
                return new StatusEffect(type, turns, power, null, persistent);
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
