package com.jcode.jclinical.infrastructure.ui.support;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.context.ApplicationContext;

public class SceneNavigator {
    private final ApplicationContext applicationContext;

    public SceneNavigator(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public <T> T navigate(Stage stage, String fxmlPath, double width, double height) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        loader.setControllerFactory(applicationContext::getBean);
        Parent root = loader.load();
        stage.setScene(new Scene(root, width, height));
        stage.centerOnScreen();
        return loader.getController();
    }
}
