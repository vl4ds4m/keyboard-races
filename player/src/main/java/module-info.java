module org.vl4ds4m.keyboardraces.player {
    requires javafx.controls;

    exports org.vl4ds4m.keyboardraces.player to
            org.vl4ds4m.keyboardraces.client,
            org.vl4ds4m.keyboardraces.server;
}