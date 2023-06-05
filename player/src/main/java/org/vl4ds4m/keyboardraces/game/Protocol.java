package org.vl4ds4m.keyboardraces.game;

import java.io.Serializable;

public enum Protocol implements Serializable {
    TEXT,
    PLAYER_NUM,
    TIME,
    DATA,
    DATA_LIST,
    READY,
    START,
    STOP
}
