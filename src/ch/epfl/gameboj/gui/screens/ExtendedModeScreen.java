package ch.epfl.gameboj.gui.screens;

import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;

public final class ExtendedModeScreen {
	public static Scene getExtendedModeScreen(ImageView emulationView) {
		BorderPane extendedBorderPane = new BorderPane();

        extendedBorderPane.setCenter(emulationView);

        return new Scene(extendedBorderPane);
	}
}
