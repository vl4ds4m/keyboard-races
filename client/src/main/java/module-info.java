module org.vl4ds4m.keyboardraces.client {
    requires javafx.graphics;
    requires javafx.controls;
    requires javafx.fxml;

    requires org.vl4ds4m.keyboardraces.game;

    opens org.vl4ds4m.keyboardraces.client to javafx.graphics, javafx.fxml;
}