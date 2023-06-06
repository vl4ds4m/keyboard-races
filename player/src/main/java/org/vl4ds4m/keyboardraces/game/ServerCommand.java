package org.vl4ds4m.keyboardraces.game;

import java.io.Serializable;

public enum ServerCommand implements Serializable {
    TEXT,
    NEED_NAME,
    PLAYER_NUM,
    TIME,
    NEED_COUNTS,
    DATA_LIST,
    READY,
    START,
    STOP
}
