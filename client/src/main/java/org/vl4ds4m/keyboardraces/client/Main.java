package org.vl4ds4m.keyboardraces.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        Pane loginPane = FXMLLoader.load(getURL("/login-pane.fxml"));
        primaryStage.setTitle("Keyboard Races");
        setStageParams(primaryStage, loginPane);
        primaryStage.show();
    }


    static URL getURL(String resourceName) throws IOException {
        URL url = Main.class.getResource(resourceName);
        if (url == null) {
            throw new RuntimeException("The resource '" + resourceName + "' hasn't been found!");
        }
        return url;
    }

    static void setStageParams(Stage stage, Pane pane) {
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
}
