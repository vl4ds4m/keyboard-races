package org.vl4ds4m.keyboardraces.player;

import java.util.Comparator;

public class PlayerResult {
    private final PlayerData data;
    private boolean currentPlayer = false;

    public PlayerResult(PlayerData data) {
        this.data = data;
    }

    public PlayerData getData() {
        return data;
    }

    public void setCurrentPlayer(boolean currentPlayer) {
        this.currentPlayer = currentPlayer;
    }

    public int getSpeed() {
        return data.getInputCharsCount();
    }

    @Override
    public String toString() {
        return data.getName() +
                (currentPlayer ? " (You)" : "") +
                ", Speed: " + getSpeed() +
                ", Errors: " + data.getErrorsCount() +
                ", Connection: " + (data.connected() ? "Ok" : "Break");
    }

    public static final Comparator<PlayerResult> COMPARATOR = (r1, r2) -> {
        int speedDif = r2.getSpeed() - r1.getSpeed();
        int errorsDif = r1.getData().getErrorsCount() - r2.getData().getErrorsCount();
        return speedDif != 0 ? speedDif : errorsDif;
    };
}
