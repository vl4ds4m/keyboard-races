package org.vl4ds4m.keyboardraces.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class Main extends Application {
    Pane loginPane;
    Pane gamePane;

    static void setStageParams(Stage stage, Pane pane, String title) {
        stage.setTitle(title);

        if (pane.getScene() == null) {
            stage.setScene(new Scene(pane));
        } else {
            stage.setScene(pane.getScene());
        }

        final double windowPadding = 30;

        stage.setMinHeight(pane.getMinHeight() + windowPadding);
        stage.setMinWidth(pane.getMinWidth());

        stage.setMaxHeight(pane.getMaxHeight() + windowPadding);
        stage.setMaxWidth(pane.getMaxWidth());
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        loginPane = loadPane("/login-pane.fxml");
        gamePane = loadPane("/game-pane.fxml");

        setStageParams(primaryStage, loginPane, "Keyboard Races - Login");

        primaryStage.show();
    }

    static Pane loadPane(String resourceName) throws IOException {
        URL url = Main.class.getResource(resourceName);
        if (url == null) {
            throw new RuntimeException("The resource '" + resourceName + "' hasn't been found!");
        }
        return FXMLLoader.load(url);
    }
}
