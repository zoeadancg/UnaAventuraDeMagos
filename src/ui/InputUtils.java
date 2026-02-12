package src.ui;

import src.model.Direccion;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.function.Consumer;

/**
 * InputUtils
 * Helpers para mapear teclas a Direccion y para instalar KeyBindings
 * reutilizables en componentes Swing.
 */
public final class InputUtils {

    private InputUtils() {
        /* utilitario */ }

    /**
     * Convierte un KeyEvent a Direccion (WASD, flechas, numpad).
     * Devuelve null si la tecla no corresponde a una dirección.
     */
    public static Direccion keyToDirection(KeyEvent e) {
        if (e == null)
            return null;
        return keyCodeToDirection(e.getKeyCode());
    }

    /**
     * Convierte un keyCode a Direccion (sin KeyEvent).
     */
    public static Direccion keyCodeToDirection(int keyCode) {
        switch (keyCode) {
            // Arriba
            case KeyEvent.VK_W:
            case KeyEvent.VK_UP:
            case KeyEvent.VK_NUMPAD8:
                return Direccion.UP;
            // Abajo
            case KeyEvent.VK_S:
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_NUMPAD2:
                return Direccion.DOWN;
            // Izquierda
            case KeyEvent.VK_A:
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_NUMPAD4:
                return Direccion.LEFT;
            // Derecha
            case KeyEvent.VK_D:
            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_NUMPAD6:
                return Direccion.RIGHT;
            default:
                return null;
        }
    }

    /**
     * Instala key bindings direccionales en el componente dado.
     * - onDirection.accept(dir) se ejecuta en el EDT cuando se pulsa la tecla.
     * - Usa WHEN_IN_FOCUSED_WINDOW para que funcione aunque el componente no tenga
     * foco exacto.
     */
    public static void bindDirectionalKeys(JComponent comp, Consumer<Direccion> onDirection) {
        if (comp == null || onDirection == null)
            return;

        InputMap im = comp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = comp.getActionMap();

        // Mapeos: tecla -> action name
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, 0), "dirUp");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "dirUp");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD8, 0), "dirUp");

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, 0), "dirDown");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "dirDown");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD2, 0), "dirDown");

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, 0), "dirLeft");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "dirLeft");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD4, 0), "dirLeft");

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0), "dirRight");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "dirRight");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD6, 0), "dirRight");

        // Actions
        am.put("dirUp", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onDirection.accept(Direccion.UP);
            }
        });
        am.put("dirDown", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onDirection.accept(Direccion.DOWN);
            }
        });
        am.put("dirLeft", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onDirection.accept(Direccion.LEFT);
            }
        });
        am.put("dirRight", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onDirection.accept(Direccion.RIGHT);
            }
        });
    }

    /**
     * Instala bindings para confirmar/limpiar:
     * - Enter / Space -> onConfirm.run()
     * - Backspace / Delete -> onClear.run()
     */
    public static void bindConfirmClear(JComponent comp, Runnable onConfirm, Runnable onClear) {
        if (comp == null)
            return;

        InputMap im = comp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = comp.getActionMap();

        if (onConfirm != null) {
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "confirm");
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "confirm");
            am.put("confirm", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    onConfirm.run();
                }
            });
        }

        if (onClear != null) {
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "clear");
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "clear");
            am.put("clear", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    onClear.run();
                }
            });
        }
    }

    /**
     * Conveniencia: instala direccionales + confirm + clear en un solo llamado.
     */
    public static void installDefaultBindings(JComponent comp,
            Consumer<Direccion> onDirection,
            Runnable onConfirm,
            Runnable onClear) {
        bindDirectionalKeys(comp, onDirection);
        bindConfirmClear(comp, onConfirm, onClear);
        // Asegurar que el componente puede recibir foco para otras interacciones
        comp.setFocusable(true);
    }

    /**
     * Convierte una representación textual a Direccion (ej: "UP","arriba","W").
     * Útil para tests o serialización simple.
     */
    public static Direccion parseDirection(String s) {
        if (s == null)
            return null;
        String t = s.trim().toUpperCase();
        switch (t) {
            case "UP":
            case "ARRIBA":
            case "W":
                return Direccion.UP;
            case "DOWN":
            case "ABAJO":
            case "S":
                return Direccion.DOWN;
            case "LEFT":
            case "IZQUIERDA":
            case "A":
                return Direccion.LEFT;
            case "RIGHT":
            case "DERECHA":
            case "D":
                return Direccion.RIGHT;
            default:
                return null;
        }
    }

    /**
     * Convierte Direccion a string legible (ej: "Arriba").
     */
    public static String toReadable(Direccion d) {
        if (d == null)
            return "";
        switch (d) {
            case UP:
                return "Arriba";
            case DOWN:
                return "Abajo";
            case LEFT:
                return "Izquierda";
            case RIGHT:
                return "Derecha";
            default:
                return d.name();
        }
    }
}