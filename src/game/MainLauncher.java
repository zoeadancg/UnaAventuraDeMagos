package game;

import model.Combatant;
import model.Elemento;

import javax.swing.*;
import java.nio.file.Paths;

public class MainLauncher {
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeLater(() -> {
            try {
                SaveLoadManager saveManager = new SaveLoadManager(
                        Paths.get(System.getProperty("user.home"), ".miJuego", "saves"));
                CargaNiveles levelLoader = new CargaNiveles();
                GameController controller = new GameController(levelLoader, saveManager);
                // Crear player de prueba
                Combatant player = new Combatant("player-1", "Zoe", Elemento.FUEGO, 120, 12);
                controller.startGameWithPlayer(player, "level-1");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}