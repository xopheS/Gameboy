package ch.epfl.gameboj.gui;

import static ch.epfl.gameboj.GameBoy.CYCLES_PER_NANOSECOND;
import static ch.epfl.gameboj.component.lcd.LcdController.LCD_HEIGHT;
import static ch.epfl.gameboj.component.lcd.LcdController.LCD_WIDTH;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import ch.epfl.gameboj.GameBoy;
import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.component.Joypad;
import ch.epfl.gameboj.component.Joypad.Key;
import ch.epfl.gameboj.component.cartridge.Cartridge;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ToolBar;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Main extends Application {
    private static GameBoy gameboj = null;

    private static final Map<Key, String> keyToString = Map.of(Key.A, "A", Key.B, "B", Key.START, "S", Key.SELECT,
            "Space"); // TODO use this, invert mapping

    public static void main(String[] args) {
        Application.launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        List<String> cmdArgs = getParameters().getRaw();
        Preconditions.checkArgument(cmdArgs.size() == 1, () -> System.exit(1));

        String fileName = cmdArgs.get(0);

        try {
            gameboj = new GameBoy(Cartridge.ofFile(new File(fileName)));
        } catch (FileNotFoundException e) {
            System.exit(1); // TODO can improve this with eg different exit codes?
        } catch (IOException e) {
            System.exit(1);
        }

        // TODO replace gameboj with clever name
        primaryStage.setTitle("gameboj: the GameBoy emulator");

        // Splash screen
        Parent splashScreenRoot = FXMLLoader.load(getClass().getResource("Splash_Screen.fxml"));
        Scene splashScreen = new Scene(splashScreenRoot, 200, 200);

        // Simple mode screen TODO rename to simple/development/other???
        ImageView emulationView = new ImageView();
        emulationView.setFitWidth(2 * LCD_WIDTH);
        emulationView.setFitHeight(2 * LCD_HEIGHT);

        BorderPane simpleBorderPane = new BorderPane(emulationView);

        Scene simpleModeScreen = new Scene(simpleBorderPane);
        setInput(simpleModeScreen, gameboj.joypad());

        // Extended mode screen
        ToolBar toolBar = new ToolBar(new Button("Reset"), new Button("Speed"), new Button("Screen"),
                new Button("Dump"), new Button("Debug"), new Button("Save"), new Button("Help"));

        BorderPane extendedBorderPane = new BorderPane();

        extendedBorderPane.setTop(toolBar);
        extendedBorderPane.setCenter(emulationView); // Copy the emulation view actually or it doesn't work TODO

        Scene extendedModeScreen = new Scene(extendedBorderPane);
        setInput(extendedModeScreen, gameboj.joypad());

        // Mode choice screen
        BorderPane modeChoicePane = new BorderPane();

        Button simpleModeButton = new Button("Simple Mode");
        simpleModeButton.setOnAction(e -> primaryStage.setScene(simpleModeScreen));
        Button extendedModeButton = new Button("Extended Mode");
        extendedModeButton.setOnAction(e -> primaryStage.setScene(extendedModeScreen));
        VBox modeButtonsBox = new VBox(10);

        modeButtonsBox.getChildren().add(simpleModeButton);
        modeButtonsBox.getChildren().add(extendedModeButton);

        modeChoicePane.getChildren().add(modeButtonsBox);

        Scene modeChoiceScreen = new Scene(modeChoicePane);

        primaryStage.setScene(modeChoiceScreen); // TODO set to splash screen
        primaryStage.show();

        long start = System.nanoTime();

        new AnimationTimer() {
            @Override
            public void handle(long now) {
                long elapsed = now - start;
                long elapsedCycles = (long) (elapsed * CYCLES_PER_NANOSECOND);

                gameboj.runUntil(elapsedCycles);

                emulationView.setImage(ImageConverter.convert(gameboj.lcdController().currentImage()));
            }
        }.start();
    }

    private static void setInput(Scene scene, Joypad jp) {
        // Set up keyboard input
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
                jp.keyPressed(Key.START);
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

            /*
             * switch (e.getText()) { //TODO this doesn't work, why? case "A":
             * System.out.println("yiss"); jp.keyPressed(Key.A); break; case "B":
             * jp.keyPressed(Key.B); break; case "S": jp.keyPressed(Key.START); break; case
             * "Space": jp.keyPressed(Key.START); break; }
             */
        });

        scene.setOnKeyReleased(e -> {
            switch (e.getCode()) {
            case A:
                jp.keyReleased(Key.A);
                break;
            case B:
                jp.keyReleased(Key.B);
                break;
            case C:
                jp.keyReleased(Key.START);
                break;
            case SPACE:
                jp.keyReleased(Key.START);
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

            /*
             * switch (e.getText()) { case "A": jp.keyReleased(Key.A); break; case "B":
             * jp.keyReleased(Key.B); break; case "S": jp.keyReleased(Key.START); break;
             * case "Space": jp.keyReleased(Key.START); break; }
             */
        });
    }
}
