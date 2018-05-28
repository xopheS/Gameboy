package ch.epfl.gameboj.gui;

import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;

public final class SplashScreen extends Scene {
	public SplashScreen() {
		ImageView splashView = new ImageView("file:EPFL-Logo.jpg");
        BorderPane splashPane = new BorderPane();
        splashPane.setCenter(splashView);
        splashView.fitWidthProperty().bind(splashPane.widthProperty());
        splashView.fitHeightProperty().bind(splashPane.heightProperty());
		super(splashPane);
	}
}
