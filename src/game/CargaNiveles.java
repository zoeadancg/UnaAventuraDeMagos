package src.game;

import java.util.*;
import java.util.concurrent.*;
import javax.swing.SwingWorker;

import src.util.Resources;

import java.util.function.Consumer;

public class CargaNiveles {
    private final Map<String, LevelData> levels = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static class LevelData {
        public final String id;
        public final List<String> imagePaths;
        public final List<String> animationPaths;
        public final List<String> soundPaths;
        public final int difficulty;

        public LevelData(String id, List<String> imagePaths, List<String> animationPaths,
                List<String> soundPaths, int difficulty) {
            this.id = id;
            this.imagePaths = imagePaths == null ? Collections.emptyList() : new ArrayList<>(imagePaths);
            this.animationPaths = animationPaths == null ? Collections.emptyList() : new ArrayList<>(animationPaths);
            this.soundPaths = soundPaths == null ? Collections.emptyList() : new ArrayList<>(soundPaths);
            this.difficulty = difficulty;
        }

        public int getDifficulty() {
            return difficulty;
        }

        public src.model.Combatant createInitialEnemy() {
            // Placeholder implementation
            return new src.model.Combatant("Enemy", src.model.Elemento.FUEGO, 100, 10);
        }
    }

    public void registerLevel(LevelData level) {
        levels.put(level.id, level);
    }

    public LevelData getLevelData(String levelId) {
        return levels.get(levelId);
    }

    public void preloadLevel(String levelId,
            Consumer<Integer> onProgress,
            java.util.function.Consumer<LevelData> onDone,
            java.util.function.Consumer<Exception> onError) {
        LevelData meta = levels.get(levelId);
        if (meta == null) {
            if (onError != null)
                onError.accept(new IllegalArgumentException("Nivel no registrado: " + levelId));
            return;
        }

        // Ejecutar en SwingWorker para reportar progreso en EDT
        SwingWorker<LevelData, Integer> worker = new SwingWorker<>() {
            @Override
            protected LevelData doInBackground() throws Exception {
                List<String> all = new ArrayList<>();
                all.addAll(meta.imagePaths);
                all.addAll(meta.animationPaths);
                all.addAll(meta.soundPaths);

                int total = Math.max(1, all.size());
                int loaded = 0;

                for (String path : all) {
                    try {
                        // Delegar carga a Resources (puede ser imagen o sonido)
                        if (path.endsWith(".png") || path.endsWith(".jpg")) {
                            Resources.loadAndCacheImage(path);
                        } else {
                            Resources.loadAndCacheSound(path); // implementa en Resources
                        }
                    } catch (Exception ex) {
                        // registrar y continuar; opcional: lanzar si es crítico
                        System.err.println("Error cargando asset " + path + ": " + ex.getMessage());
                    }
                    loaded++;
                    int prog = (int) ((loaded / (double) total) * 100);
                    publish(prog);
                }
                publish(100);
                return meta;
            }

            @Override
            protected void process(List<Integer> chunks) {
                int last = chunks.get(chunks.size() - 1);
                if (onProgress != null)
                    onProgress.accept(last);
            }

            @Override
            protected void done() {
                try {
                    LevelData res = get();
                    if (onDone != null)
                        onDone.accept(res);
                } catch (Exception ex) {
                    if (onError != null)
                        onError.accept(ex);
                }
            }
        };

        worker.execute();
    }

    public void unloadLevel(String levelId) {
        LevelData meta = levels.get(levelId);
        if (meta == null)
            return;
        // Delegar a Resources para liberar cada asset
        for (String p : meta.imagePaths)
            Resources.unload(p);
        for (String p : meta.animationPaths)
            Resources.unload(p);
        for (String p : meta.soundPaths)
            Resources.unload(p);
    }

    public void shutdown() {
        executor.shutdownNow();
    }
}
