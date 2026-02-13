package model;

public class StatusEffect {
    private final StatusType type;
    private int remainingTurns;
    private int power;
    private final boolean persistent;

    public StatusEffect(StatusType type, int duration, int power, Object meta, boolean persistent) {
        this.type = type;
        this.remainingTurns = duration;
        this.power = power;
        this.persistent = persistent;
    }

    public StatusType getType() {
        return type;
    }

    public int getRemainingTurns() {
        return remainingTurns;
    }

    public int getPower() {
        return power;
    }

    public void applyImmediate(Combatant target) {
        // ejemplo: si BURN no aplica inmediato
    }

    public StatusTickResult tick() {
        if (remainingTurns <= 0)
            return null;
        remainingTurns--;
        if (type == StatusType.BURN)
            return StatusTickResult.damage(power);
        return null;
    }

    public boolean isExpired() {
        return remainingTurns <= 0;
    }

    public void onExpire(Combatant target) {
        // revertir efectos si aplica
    }

    public void mergeWith(StatusEffect other) {
        // política simple: keep max power and max duration
        this.power = Math.max(this.power, other.power);
        this.remainingTurns = Math.max(this.remainingTurns, other.remainingTurns);
    }
}