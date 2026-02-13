package ui;

import model.ActionSequence;
import model.Direccion;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.Toolkit;

public class InputHandler extends KeyAdapter {
    private final ActionSequence seq;

    public InputHandler(ActionSequence seq) {
        this.seq = seq;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        Direccion d = null;
        switch (e.getKeyCode()) {
            case KeyEvent.VK_W:
            case KeyEvent.VK_UP:
                d = Direccion.UP;
                break;
            case KeyEvent.VK_S:
            case KeyEvent.VK_DOWN:
                d = Direccion.DOWN;
                break;
            case KeyEvent.VK_A:
            case KeyEvent.VK_LEFT:
                d = Direccion.LEFT;
                break;
            case KeyEvent.VK_D:
            case KeyEvent.VK_RIGHT:
                d = Direccion.RIGHT;
                break;
        }
        if (d != null) {
            boolean ok = seq.add(d);
            if (!ok) {
                // feedback al jugador: sonido o shake
                Toolkit.getDefaultToolkit().beep();
            }
            // actualizar UI de secuencia
        }
    }
}
