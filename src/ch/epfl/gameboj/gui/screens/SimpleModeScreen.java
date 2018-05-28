package ch.epfl.gameboj.gui.screens;

import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;

public final class SimpleModeScreen {
	public static Scene getSimpleModeScreen(ImageView emulationView) {
        BorderPane simpleBorderPane = new BorderPane(emulationView);

        return new Scene(simpleBorderPane);
	}
}
