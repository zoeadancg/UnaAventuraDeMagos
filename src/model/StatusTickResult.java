package model;

/**
 * StatusTickResult
 * Resultado de un tick de StatusEffect.
 */
public final class StatusTickResult {

    public enum Kind {
        NONE, DAMAGE, HEAL, STUN, SLOW, CUSTOM
    }

    private final Kind kind;
    private final int amount;
    private final String note;

    private StatusTickResult(Kind kind, int amount, String note) {
        this.kind = kind;
        this.amount = amount;
        this.note = note;
    }

    public Kind getKind() {
        return kind;
    }

    public int getAmount() {
        return amount;
    }

    public String getNote() {
        return note;
    }

    public static StatusTickResult none() {
        return new StatusTickResult(Kind.NONE, 0, null);
    }

    public static StatusTickResult damage(int amount) {
        return new StatusTickResult(Kind.DAMAGE, amount, null);
    }

    public static StatusTickResult heal(int amount) {
        return new StatusTickResult(Kind.HEAL, amount, null);
    }

    public static StatusTickResult stun() {
        return new StatusTickResult(Kind.STUN, 0, null);
    }

    public static StatusTickResult slow(int magnitude) {
        return new StatusTickResult(Kind.SLOW, magnitude, null);
    }

    public static StatusTickResult custom(String note) {
        return new StatusTickResult(Kind.CUSTOM, 0, note);
    }
}
