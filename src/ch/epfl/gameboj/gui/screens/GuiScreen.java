package ch.epfl.gameboj.gui.screens;

import java.util.List;
import java.util.ResourceBundle;

import javafx.scene.Scene;
import javafx.stage.Stage;

public abstract class GuiScreen {
	protected Scene screen;
	protected final Stage owner;
	protected final List<Scene> linkedScreens;
	protected final ResourceBundle guiBundle;
	
	public GuiScreen(Stage owner, List<Scene> linkedScreens, ResourceBundle guiBundle) {
		this.owner = owner;
		this.linkedScreens = linkedScreens;
		this.guiBundle = guiBundle;
	}
	
	public Scene getScreen() {
		return screen;
	}
}
