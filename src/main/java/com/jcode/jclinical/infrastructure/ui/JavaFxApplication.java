package com.jcode.jclinical.infrastructure.ui;

import com.jcode.jclinical.JClinicalApplication;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.net.URL;

public class JavaFxApplication extends Application {

    private ConfigurableApplicationContext springContext;

    @Override
    public void init() {
        // Arrancamos el motor de Spring Boot en segundo plano
        String[] args = getParameters().getRaw().toArray(new String[0]);
        this.springContext = new SpringApplicationBuilder()
                .sources(JClinicalApplication.class)
                .run(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        // AHORA LA APLICACIÓN ARRANCA DIRECTAMENTE EN LA LISTA (DASHBOARD)
        URL fxmlLocation = getClass().getResource("/fxml/lista_expedientes.fxml");
        FXMLLoader fxmlLoader = new FXMLLoader(fxmlLocation);

        // Inyección de dependencias para los controladores visuales
        fxmlLoader.setControllerFactory(springContext::getBean);

        Parent root = fxmlLoader.load();

        Scene scene = new Scene(root, 900, 600);
        primaryStage.setScene(scene);
        primaryStage.setTitle("JClinical - Expedientes Clínicos");
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    @Override
    public void stop() {
        // Cerramos el contexto de Spring de forma segura al salir
        springContext.close();
        Platform.exit();
    }
}