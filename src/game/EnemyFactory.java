package game;

import model.*;
import java.util.*;

/**
 * EnemyFactory
 * - Crea enemigos escalados para el jugador.
 * - Genera secuencias enemigas (IA simple con variación).
 *
 * Requisitos:
 * - Combatant debe tener un constructor o factory compatible:
 * new Combatant(String name, Elemento elemento, int maxHp, int baseDamage)
 * o adapta las llamadas a tu constructor real.
 */
public class EnemyFactory {

    private final Random rnd = new Random();

    public EnemyFactory() {
    }

    /**
     * Crea un enemigo básico para pruebas.
     */
    public Combatant createBasicEnemy() {
        // Ajusta nombre, elemento, HP y daño base según tu modelo
        return createEnemy("Goblin", Elemento.FUEGO, 60, 8);
    }

    /**
     * Crea un enemigo escalado en función del jugador y la dificultad.
     * difficulty: 0 = fácil, 1 = medio, 2 = difícil, etc.
     */
    public Combatant createEnemyFor(Combatant player, int difficulty) {
        if (player == null)
            return createBasicEnemy();

        int baseHp = Math.max(40, player.getMaxHp() - 10 + difficulty * 20);
        int baseDmg = Math.max(5, player.getBaseDamage() - 2 + difficulty * 3);

        // Elegir elemento con algo de variación; puedes mejorar con tablas por nivel
        Elemento elem = pickElementForDifficulty(difficulty);

        String name = "Enemigo Niv " + Math.max(1, difficulty + 1);
        return createEnemy(name, elem, baseHp, baseDmg);
    }

    /**
     * Crea un enemigo con parámetros explícitos.
     * Adapta la llamada al constructor de tu Combatant si difiere.
     */
    public Combatant createEnemy(String name, Elemento elemento, int maxHp, int baseDamage) {
        // Si tu Combatant tiene otro constructor, cámbialo aquí.
        Combatant enemy = Combatant.create(name, elemento, maxHp, baseDamage);
        // Inicializa otros campos si tu Combatant los requiere (por ejemplo,
        // spritePath)
        // enemy.setSpritePath("/images/enemies/" + elemento.name().toLowerCase() +
        // "_1.png");
        return enemy;
    }

    /**
     * Genera una secuencia de direcciones para el enemigo.
     * - No repite la misma dirección consecutiva.
     * - Longitud por defecto 4 (puedes parametrizar).
     * - Añade sesgos según elemento o comportamiento del enemigo.
     */
    public List<Direccion> generateSequenceFor(Combatant enemy, Combatant player) {
        int length = 4;
        List<Direccion> seq = new ArrayList<>(length);
        Direccion[] values = Direccion.values();
        Direccion last = null;

        // Sesgo por elemento: por ejemplo, eléctrico prefiere horizontales
        double horizontalBias = elementHorizontalBias(enemy != null ? enemy.getElemento() : null);

        for (int i = 0; i < length; i++) {
            Direccion pick;
            int attempts = 0;
            do {
                pick = biasedPick(values, horizontalBias);
                attempts++;
                // seguridad: si no encontramos distinto tras varios intentos, aceptamos
                if (attempts > 8)
                    break;
            } while (pick == last);
            seq.add(pick);
            last = pick;
        }

        // Pequeña variación aleatoria: con baja probabilidad, acorta la secuencia
        if (rnd.nextDouble() < 0.08) {
            seq.remove(seq.size() - 1);
        }

        return seq;
    }

    // -----------------------
    // Helpers y heurísticas
    // -----------------------

    private Elemento pickElementForDifficulty(int difficulty) {
        // Simple rotación; mejora con tablas por enemigo/bioma
        Elemento[] elems = Elemento.values();
        return elems[Math.abs(difficulty) % elems.length];
    }

    private double elementHorizontalBias(Elemento e) {
        if (e == null)
            return 0.5;
        switch (e) {
            case RAYO:
                return 0.75; // más horizontales
            case AGUA:
                return 0.45; // más verticales
            case FUEGO:
                return 0.55;
            case HIELO:
                return 0.5;
            default:
                return 0.5;
        }
    }

    /**
     * Escoge una dirección con sesgo hacia horizontales si bias > 0.5.
     * bias = 0.5 -> uniforme; bias -> probabilidad de LEFT/RIGHT combinadas.
     */
    private Direccion biasedPick(Direccion[] values, double bias) {
        // Mapear índices: 0=UP,1=DOWN,2=LEFT,3=RIGHT (según tu enum)
        double r = rnd.nextDouble();
        double horizontalProb = Math.max(0.0, Math.min(1.0, bias));
        if (r < horizontalProb) {
            // elegir LEFT o RIGHT
            return rnd.nextBoolean() ? Direccion.LEFT : Direccion.RIGHT;
        } else {
            // elegir UP o DOWN
            return rnd.nextBoolean() ? Direccion.UP : Direccion.DOWN;
        }
    }

    /**
     * Sanitiza una secuencia: elimina repeticiones consecutivas y limita longitud.
     * Útil si la IA o el jugador generan secuencias inválidas.
     */
    public static List<Direccion> sanitizeSequence(List<Direccion> seq, int maxLen) {
        List<Direccion> out = new ArrayList<>();
        Direccion last = null;
        if (seq == null)
            return out;
        for (Direccion d : seq) {
            if (out.size() >= maxLen)
                break;
            if (d == null)
                continue;
            if (last != null && last == d)
                continue;
            out.add(d);
            last = d;
        }
        return out;
    }
}