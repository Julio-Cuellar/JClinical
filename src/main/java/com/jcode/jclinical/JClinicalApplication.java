package com.jcode.jclinical;

import com.jcode.jclinical.infrastructure.ui.JavaFxApplication;
import javafx.application.Application;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class JClinicalApplication {

    public static void main(String[] args) {

        Application.launch(JavaFxApplication.class, args);
    }
}