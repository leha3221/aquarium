package ru.mirea.aquarium.fx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.InputStream;

public class FxMain extends Application {
    @Override public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(FxMain.class.getResource("/ru/mirea/aquarium/fx/main.fxml"));
        stage.setTitle("Обслуживание аквариумов — заявки");
        Scene scene;
        try (InputStream fxml = FxMain.class.getResourceAsStream("/ru/mirea/aquarium/fx/main.fxml")) {
            scene = new Scene(loader.load(fxml), 1180, 700);
        }
        scene.getStylesheets().add(FxMain.class.getResource("/ru/mirea/aquarium/fx/style.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
    }
    public static void main(String[] args) { launch(args); }
}
