package ch.epfl.gameboj.gui.screens;

import javafx.animation.FadeTransition;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.util.Duration;

public final class SplashScreen {
	public static Scene getSplashScreen(Stage owner, Scene nextScene) {
		ImageView splashView = new ImageView("file:EPFL-Logo.jpg");
        BorderPane splashPane = new BorderPane();
        splashPane.setCenter(splashView);
        splashView.fitWidthProperty().bind(splashPane.widthProperty());
        splashView.fitHeightProperty().bind(splashPane.heightProperty());
        
        FadeTransition splashFade = new FadeTransition();
        splashFade.setNode(splashPane);
        splashFade.setDelay(new Duration(1000));
        splashFade.setDuration(new Duration(3000));
        splashFade.setFromValue(1.0);
        splashFade.setToValue(0.0);
        splashFade.setOnFinished(e -> {
        	owner.setScene(nextScene);
        });
        splashFade.play();
        
		return new Scene(splashPane, 433, 300);
	}
}
