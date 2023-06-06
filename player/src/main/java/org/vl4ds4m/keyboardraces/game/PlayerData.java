package org.vl4ds4m.keyboardraces.game;

import java.io.Serializable;
import java.util.Comparator;

public class PlayerData implements Serializable {
    private final String name;
    private int inputCharsCount = 0;
    private int errorsCount = 0;
    private boolean connected = true;
    private boolean currentPlayer = false;

    public PlayerData(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public int getInputCharsCount() {
        return inputCharsCount;
    }

    public int getErrorsCount() {
        return errorsCount;
    }

    public boolean currentPlayer() {
        return currentPlayer;
    }

    public boolean connected() {
        return connected;
    }

    public void setInputCharsCount(int inputCharsCount) {
        this.inputCharsCount = inputCharsCount;
    }

    public void setErrorsCount(int errorsCount) {
        this.errorsCount = errorsCount;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public void setCurrentPlayer(boolean currentPlayer) {
        this.currentPlayer = currentPlayer;
    }

    public int getSpeed() {
        return inputCharsCount;
    }

    public static final Comparator<PlayerData> RATE_COMP = (o1, o2) -> {
        if (!o1.connected || !o2.connected) {
            if (o1.connected) {
                return -1;
            }
            if (o2.connected) {
                return 1;
            }
            return 0;
        }

        int speedDif = o2.getSpeed() - o1.getSpeed();
        int errorsDif = o1.errorsCount - o2.errorsCount;

        return speedDif != 0 ? speedDif : errorsDif;
    };
}
