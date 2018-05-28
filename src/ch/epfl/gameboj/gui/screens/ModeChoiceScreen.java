package ch.epfl.gameboj.gui.screens;

import java.util.List;
import java.util.ResourceBundle;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public final class ModeChoiceScreen extends GuiScreen {

	public ModeChoiceScreen(Stage owner, List<Scene> linkedScreens, ResourceBundle guiBundle) {
		super(owner, linkedScreens, guiBundle);
		
		Button simpleModeButton = new Button("Simple Mode");
        simpleModeButton.setOnAction(e -> owner.setScene(linkedScreens.get(0)));
        Button extendedModeButton = new Button("Extended Mode");
        extendedModeButton.setOnAction(e -> owner.setScene(linkedScreens.get(1)));
        Button developmentModeButton = new Button("Development Mode");
        developmentModeButton.setOnAction(e -> {
        	owner.setScene(linkedScreens.get(2));
        	owner.setMaximized(true);
        });
        VBox modeButtonsBox = new VBox(10);

        modeButtonsBox.getChildren().addAll(simpleModeButton, extendedModeButton, developmentModeButton);
        modeButtonsBox.setAlignment(Pos.CENTER);
        modeButtonsBox.setPadding(new Insets(25, 25, 25, 25));

        BorderPane modeChoicePane = new BorderPane(modeButtonsBox);
        
        screen = new Scene(modeChoicePane);
	}
}
