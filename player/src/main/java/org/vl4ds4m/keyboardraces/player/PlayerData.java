package org.vl4ds4m.keyboardraces.player;

import java.io.Serializable;

public class PlayerData implements Serializable {
    private int inputCharsCount = 0;
    private int errorsCount = 0;
    private boolean connected = true;
    private final String name;

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


    public void updateInputValues(PlayerData updatedData) {
        this.inputCharsCount = updatedData.inputCharsCount;
        this.errorsCount = updatedData.errorsCount;
        this.connected = updatedData.connected;
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
