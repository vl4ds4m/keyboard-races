module org.vl4ds4m.keyboardraces.game {
    requires javafx.controls;

    exports org.vl4ds4m.keyboardraces.game to
            org.vl4ds4m.keyboardraces.client,
            org.vl4ds4m.keyboardraces.server;
}