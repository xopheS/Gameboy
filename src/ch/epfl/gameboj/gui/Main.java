package ch.epfl.gameboj.gui;

import static ch.epfl.gameboj.GameBoy.CYCLES_PER_NANOSECOND;
import static ch.epfl.gameboj.component.lcd.LcdController.LCD_HEIGHT;
import static ch.epfl.gameboj.component.lcd.LcdController.LCD_WIDTH;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.sound.sampled.LineUnavailableException;

import ch.epfl.gameboj.GameBoy;
import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.component.Joypad;
import ch.epfl.gameboj.component.Joypad.Key;
import ch.epfl.gameboj.component.cartridge.Cartridge;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/**
 * Cette classe contient le point d'entrée de l'application Java, elle gère l'interface de l'utilisateur.
 * 
 * @author Christophe Saad (282557)
 * @author David Cian (287967)
 *
 */
public class Main extends Application {
	private static GameBoy gameboj;

	/**
	 * Point d'entrée du programme
	 * 
	 * @param args
	 *            les arguments de lancement
	 */
	public static void main(String[] args) {
		Application.launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws IOException, LineUnavailableException, InterruptedException {
		List<String> cmdArgs = getParameters().getRaw();
		Preconditions.checkArgument(cmdArgs.size() == 1, () -> System.exit(1));

		// Le nom du fichier de la cartouche .gb est le premier et dernier argument
		String fileName = cmdArgs.get(0);

		gameboj = new GameBoy(Cartridge.ofFile(new File(fileName)));

		// La vue de l'émulation
		ImageView emulationView = new ImageView();
		emulationView.setFitWidth(2 * LCD_WIDTH);
		emulationView.setFitHeight(2 * LCD_HEIGHT);

		// L'unité de mise en scène JavaFX
		BorderPane mainPane = new BorderPane(emulationView);

		// La scène (état) principal de la fenêtre
		Scene mainScene = new Scene(mainPane);
		setInput(mainScene, gameboj.getJoypad());

		primaryStage.setScene(mainScene);
		primaryStage.show();

		long start = System.nanoTime();

		// L'animation de l'écran de l'émulation débute
		new AnimationTimer() {
			@Override
			public void handle(long now) {
				long elapsed = now - start;
				long elapsedCycles = (long) (elapsed * CYCLES_PER_NANOSECOND);

				gameboj.runUntil(elapsedCycles);

				emulationView.setImage(ImageConverter.convert(gameboj.getLcdController().currentImage()));
			}
		}.start();
	}

	/**
	 * Cette méthode permet d'initialiser les actions qui répondent aux entrées de
	 * l'utilisateur (interaction avec les touches).
	 * 
	 * @param scene
	 *            la scène à laquelle donner ces interactions
	 * @param jp
	 *            le joypad à utiliser
	 */
	private static void setInput(Scene scene, Joypad jp) {
		scene.setOnKeyPressed(e -> {
			switch (e.getCode()) {
			case A:
				jp.keyPressed(Key.A);
				break;
			case B:
				jp.keyPressed(Key.B);
				break;
			case S:
				jp.keyPressed(Key.START);
				break;
			case SPACE:
				jp.keyPressed(Key.SELECT);
				break;
			case UP:
				jp.keyPressed(Key.UP);
				break;
			case RIGHT:
				jp.keyPressed(Key.RIGHT);
				break;
			case DOWN:
				jp.keyPressed(Key.DOWN);
				break;
			case LEFT:
				jp.keyPressed(Key.LEFT);
				break;
			}
		});

		scene.setOnKeyReleased(e -> {
			switch (e.getCode()) {
			case A:
				jp.keyReleased(Key.A);
				break;
			case B:
				jp.keyReleased(Key.B);
				break;
			case S:
				jp.keyReleased(Key.START);
				break;
			case SPACE:
				jp.keyReleased(Key.SELECT);
				break;
			case UP:
				jp.keyReleased(Key.UP);
				break;
			case RIGHT:
				jp.keyReleased(Key.RIGHT);
				break;
			case DOWN:
				jp.keyReleased(Key.DOWN);
				break;
			case LEFT:
				jp.keyReleased(Key.LEFT);
				break;
			}
		});
	}
}
