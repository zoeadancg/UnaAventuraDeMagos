package game;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.*;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * SaveLoadManager: manejo de guardado/lectura de SaveData.
 * Añade saveAsync para uso no bloqueante desde GameController.
 */
public class SaveLoadManager {

    private final Path savesDir;
    private final Gson gson;

    public SaveLoadManager(Path savesDir) {
        this.savesDir = Objects.requireNonNull(savesDir);
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    // ---------------- Síncrono (existente) ----------------
    public void save(SaveData sd) throws IOException {
        Objects.requireNonNull(sd);
        ensureDir();
        String json = gson.toJson(sd);
        Path target = savesDir.resolve(sd.id + ".json");
        atomicWrite(target, json);
    }

    public java.util.List<SaveData> listSaves() {
        java.util.List<SaveData> result = new java.util.ArrayList<>();
        if (!Files.exists(savesDir))
            return result;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(savesDir, "*.json")) {
            for (Path entry : stream) {
                try {
                    String json = Files.readString(entry);
                    SaveData sd = gson.fromJson(json, SaveData.class);
                    if (sd != null)
                        result.add(sd);
                } catch (Exception ignored) {
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        // sort by timestamp desc
        result.sort((a, b) -> Long.compare(b.timestamp, a.timestamp));
        return result;
    }

    public SaveData load(String id) throws IOException {
        Path target = savesDir.resolve(id + ".json");
        if (!Files.exists(target))
            return null;
        String json = Files.readString(target);
        return gson.fromJson(json, SaveData.class);
    }

    // ---------------- Asíncrono recomendado ----------------
    public void saveAsync(SaveData sd, Consumer<Boolean> onComplete, Consumer<Exception> onError) {
        Objects.requireNonNull(sd);
        // Ejecuta en un hilo del ForkJoinPool.commonPool (o usa tu propio executor)
        CompletableFuture.runAsync(() -> {
            try {
                ensureDir();
                String json = gson.toJson(sd);
                Path target = savesDir.resolve(sd.id + ".json");
                atomicWrite(target, json);
                if (onComplete != null)
                    onComplete.accept(true);
            } catch (Exception ex) {
                if (onError != null)
                    onError.accept(ex);
                else
                    throw new RuntimeException(ex);
            }
        });
    }

    // ---------------- Helpers ----------------
    private void ensureDir() throws IOException {
        if (!Files.exists(savesDir)) {
            Files.createDirectories(savesDir);
        }
    }

    private void atomicWrite(Path target, String content) throws IOException {
        Path parent = target.getParent();
        if (parent == null)
            parent = Paths.get(".");
        // Crear archivo temporal en el mismo directorio para permitir ATOMIC_MOVE
        Path tmp = Files.createTempFile(parent, target.getFileName().toString(), ".tmp");
        try {
            Files.writeString(tmp, content, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
            // Intentar mover de forma atómica; si no es soportado, REPLACE_EXISTING como
            // fallback
            try {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            // Asegurar limpieza si algo falla
            try {
                Files.deleteIfExists(tmp);
            } catch (Exception ignored) {
            }
        }
    }

    // Inner exception class si la usas en otros lugares
    public static class InvalidSaveException extends Exception {
        public InvalidSaveException() {
            super();
        }

        public InvalidSaveException(String msg) {
            super(msg);
        }

        public InvalidSaveException(String msg, Throwable cause) {
            super(msg, cause);
        }
    }
}