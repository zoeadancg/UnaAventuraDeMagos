package src.game;

import src.model.Combatant;
import src.model.Elemento;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * LevelData: datos del nivel y lista de assets.
 * Diseñada para ser fácilmente deserializable por Gson.
 */
public class LevelData {
    public String id;
    public String name;
    public int difficulty = 1;

    public List<String> imagePaths = new ArrayList<>();
    public List<String> animationPaths = new ArrayList<>();
    public List<String> soundPaths = new ArrayList<>();

    /**
     * Metadatos opcionales para configurar el enemigo inicial u otros parámetros.
     * Ejemplo:
     * {"enemyName":"Goblin","enemyHp":80,"enemyDamage":8,"enemyElement":"FIRE"}
     */
    public Map<String, Object> metadata;

    // Constructor vacío para Gson
    public LevelData() {
    }

    // Constructor completo (útil para tests o creación programática)
    public LevelData(String id, String name, int difficulty,
            List<String> imagePaths, List<String> animationPaths,
            List<String> soundPaths, Map<String, Object> metadata) {
        this.id = id;
        this.name = name == null ? "Nivel" : name;
        this.difficulty = Math.max(1, difficulty);
        this.imagePaths = imagePaths == null ? new ArrayList<>() : new ArrayList<>(imagePaths);
        this.animationPaths = animationPaths == null ? new ArrayList<>() : new ArrayList<>(animationPaths);
        this.soundPaths = soundPaths == null ? new ArrayList<>() : new ArrayList<>(soundPaths);
        this.metadata = metadata;
    }

    public int getDifficulty() {
        return difficulty;
    }

    public List<String> getImagePaths() {
        return Collections.unmodifiableList(imagePaths);
    }

    public List<String> getAnimationPaths() {
        return Collections.unmodifiableList(animationPaths);
    }

    public List<String> getSoundPaths() {
        return Collections.unmodifiableList(soundPaths);
    }

    public List<String> allAssetPaths() {
        List<String> all = new ArrayList<>();
        if (imagePaths != null)
            all.addAll(imagePaths);
        if (animationPaths != null)
            all.addAll(animationPaths);
        if (soundPaths != null)
            all.addAll(soundPaths);
        return all;
    }

    /**
     * Crea el enemigo inicial usando los metadatos si existen.
     * Usa los constructores públicos de Combatant que ya tienes.
     */
    public Combatant createInitialEnemy() {
        String enemyName = "Enemigo";
        Elemento elem = null;
        int hp = 50;
        int dmg = 5;

        if (metadata != null) {
            Object n = metadata.get("enemyName");
            if (n != null)
                enemyName = String.valueOf(n);

            Object e = metadata.get("enemyElement");
            if (e == null)
                e = metadata.get("enemyElemento"); // alias
            if (e != null) {
                try {
                    elem = Elemento.valueOf(String.valueOf(e));
                } catch (Exception ignored) {
                }
            }

            Object hpObj = metadata.get("enemyHp");
            if (hpObj instanceof Number)
                hp = ((Number) hpObj).intValue();
            else if (hpObj != null) {
                try {
                    hp = Integer.parseInt(String.valueOf(hpObj));
                } catch (Exception ignored) {
                }
            }

            Object dmgObj = metadata.get("enemyDamage");
            if (dmgObj instanceof Number)
                dmg = ((Number) dmgObj).intValue();
            else if (dmgObj != null) {
                try {
                    dmg = Integer.parseInt(String.valueOf(dmgObj));
                } catch (Exception ignored) {
                }
            }
        }

        // Usa el constructor que genera id automáticamente
        return new Combatant(enemyName, elem, Math.max(1, hp), Math.max(0, dmg));
    }

    @Override
    public String toString() {
        return "LevelData{" + id + " name=" + name + " difficulty=" + difficulty + "}";
    }
}