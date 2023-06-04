package org.vl4ds4m.keyboardraces.player;

import java.io.Serializable;

public class PlayerData implements Serializable {
    private final String name;
    private int inputCharsCount = 0;
    private int errorsCount = 0;
    private boolean connected = true;

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
}
