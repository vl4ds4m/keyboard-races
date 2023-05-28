package org.vl4ds4m.keyboardraces.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class Main extends Application {
   private Pane loadPane(String resourceName) throws IOException {
        URL url = getClass().getResource(resourceName);
        if (url == null) {
            throw new RuntimeException("The resource '" + resourceName + "' hasn't been found!");
        }
        return FXMLLoader.load(url);
    }

    private void setStageParams(Stage stage, Pane pane, String title) {
        stage.setTitle(title);

        if (pane.getScene() == null) {
            stage.setScene(new Scene(pane));
        } else {
            stage.setScene(pane.getScene());
        }

        stage.setMinHeight(pane.getPrefHeight());
        stage.setMaxHeight(pane.getPrefHeight());

        stage.setMinWidth(pane.getPrefWidth());
        stage.setMaxWidth(pane.getPrefWidth());
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Pane loginPane = loadPane("/login-pane.fxml");
        Pane gamePane = loadPane("/game-pane.fxml");

        Button startGameButton = new Button("START");
        loginPane.getChildren().add(startGameButton);
        startGameButton.setOnAction(actionEvent ->
                setStageParams(primaryStage, gamePane, "Keyboard Races - Game"));

        Button exitButton = new Button("Exit");
        gamePane.getChildren().add(exitButton);
        exitButton.setOnAction(actionEvent ->
                setStageParams(primaryStage, loginPane, "Keyboard Races - Login"));

        setStageParams(primaryStage, loginPane, "Keyboard Races - Login");

        primaryStage.show();
    }
}
