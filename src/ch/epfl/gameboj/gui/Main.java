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
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
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
        /*
         * Menu fileMenu = new Menu("File"); Menu helpMenu = new Menu("Help");
         * 
         * MenuBar mainMenuBar = new MenuBar(); mainMenuBar.getMenus().addAll(fileMenu,
         * helpMenu);
         * 
         * ToolBar toolBar = new ToolBar(new Button("Reset"), new Button("Speed"), new
         * Button("Screen"), new Button("Dump"), new Button("Debug"), new
         * Button("Save"));
         */

        BorderPane extendedBorderPane = new BorderPane();

        // extendedBorderPane.setTop(toolBar); TODO fix code duplication
        extendedBorderPane.setCenter(emulationView); // Copy the emulation view actually or it doesn't work TODO

        // TODO add gameboy overlay

        Scene extendedModeScreen = new Scene(extendedBorderPane);
        setInput(extendedModeScreen, gameboj.joypad());

        // Development mode screen
        BorderPane developmentBorderPane = new BorderPane();

        Menu fileMenu = new Menu("File"); // file related functionality
        MenuItem exitMenuItem = new MenuItem("Exit");
        exitMenuItem.setOnAction(e -> System.exit(0));
        fileMenu.getItems().addAll(exitMenuItem);

        Menu dumpMenu = new Menu("Dump"); // dumping related functionality
        MenuItem dumpTileSourceMenuItem = new MenuItem("Dump tile source");
        MenuItem dumpBackgroundMenuItem = new MenuItem("Dump background");
        MenuItem dumpWindowMenuItem = new MenuItem("Dump window");
        MenuItem dumpSpritesMenuItem = new MenuItem("Dump sprites");
        dumpMenu.getItems().addAll(dumpTileSourceMenuItem, dumpBackgroundMenuItem, dumpWindowMenuItem,
                dumpSpritesMenuItem);

        Menu debugMenu = new Menu("Debug"); // debugging related functionality
        MenuItem stepByStepMenuItem = new MenuItem("Step by step execution");
        MenuItem decompileMenuItem = new MenuItem("Decompile");
        MenuItem showStateMenuItem = new MenuItem("Show Gameboy state");
        debugMenu.getItems().addAll(stepByStepMenuItem, decompileMenuItem, showStateMenuItem);

        Menu optionsMenu = new Menu("Options"); // gameboy options
        MenuItem changeSpeedMenuItem = new MenuItem("Change speed");
        MenuItem gameboyConfigurationMenuItem = new MenuItem("Configuration");
        optionsMenu.getItems().addAll(changeSpeedMenuItem, gameboyConfigurationMenuItem);

        Menu windowMenu = new Menu("Window"); // workspace visual control
        Menu perspectiveMenu = new Menu("Perspectives"); // switch to view layout presets
        Menu showViewMenu = new Menu("Show view"); // show view in workspace
        MenuItem backgroundViewMenuItem = new MenuItem("Background");

        ImageView backgroundView = new ImageView();

        backgroundViewMenuItem.setOnAction(e -> developmentBorderPane.setLeft(backgroundView));
        MenuItem windowViewMenuItem = new MenuItem("Window");
        MenuItem spritesViewMenuItem = new MenuItem("Sprites");
        showViewMenu.getItems().addAll(backgroundViewMenuItem, windowViewMenuItem, spritesViewMenuItem);
        windowMenu.getItems().addAll(perspectiveMenu, showViewMenu);

        Menu preferencesMenu = new Menu("Preferences"); // program preferences

        Menu helpMenu = new Menu("Help"); // help functionality
        MenuItem programmingManualMenuItem = new MenuItem("Nintendo programming manual");
        helpMenu.getItems().addAll(programmingManualMenuItem);

        MenuBar mainMenuBar = new MenuBar();
        mainMenuBar.getMenus().addAll(fileMenu, dumpMenu, debugMenu, optionsMenu, windowMenu, preferencesMenu,
                helpMenu);

        ToolBar toolBar = new ToolBar(new Button("Reset"), new Button("Screen"), new Button("Save")); // TODO add to top
                                                                                                      // pane under menu

        developmentBorderPane.setTop(mainMenuBar);
        developmentBorderPane.setCenter(emulationView);

        Scene developmentModeScreen = new Scene(developmentBorderPane); // TODO
        setInput(developmentModeScreen, gameboj.joypad());

        // Mode choice screen
        BorderPane modeChoicePane = new BorderPane();

        Button simpleModeButton = new Button("Simple Mode");
        simpleModeButton.setOnAction(e -> primaryStage.setScene(simpleModeScreen));
        Button extendedModeButton = new Button("Extended Mode");
        extendedModeButton.setOnAction(e -> primaryStage.setScene(extendedModeScreen));
        Button developmentModeButton = new Button("Development Mode");
        developmentModeButton.setOnAction(e -> primaryStage.setScene(developmentModeScreen));
        VBox modeButtonsBox = new VBox(10);

        modeButtonsBox.getChildren().add(simpleModeButton);
        modeButtonsBox.getChildren().add(extendedModeButton);
        modeButtonsBox.getChildren().add(developmentModeButton); // TODO replace with addAll

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
                backgroundView.setImage(ImageConverter.convert(gameboj.lcdController().getBackground()));
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
