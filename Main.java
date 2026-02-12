import src.ui.Portada;

public class Main {
    public static void main(String[] args) {
        // Inicia mostrando la portada
        javax.swing.SwingUtilities.invokeLater(() -> {
            new Portada();
        });
    }
}
