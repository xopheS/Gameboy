package ch.epfl.gameboj.gui;

import static ch.epfl.gameboj.component.lcd.LcdController.LCD_WIDTH;
import static ch.epfl.gameboj.component.lcd.LcdController.LCD_HEIGHT;

import static ch.epfl.gameboj.GameBoy.CYCLES_PER_NANOSECOND;

import java.awt.image.BufferedImage;
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
import ch.epfl.gameboj.component.lcd.LcdImage;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class Main extends Application {
    GameBoy gameboj = null;
    
    private static final Map<Key, String> keyToString = Map.of(
            Key.A, "A",
            Key.B, "B",
            Key.START, "S",
            Key.SELECT, "Space"); //TODO use this, invert mapping
    
    public static void main(String[] args) {
        Application.launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        List<String> cmdArgs = getParameters().getRaw();
        
        Preconditions.checkArgument(cmdArgs.size() == 1, () -> System.exit(1));
        
        String fileName = cmdArgs.get(0);
        try {
            gameboj = new GameBoy(Cartridge.ofFile(new File(fileName)));
        } catch (FileNotFoundException e) {
            System.exit(1); //TODO can improve this with eg different exit codes?
        } catch (IOException e) {
            System.exit(1);
        }
        
        ImageView imgView = new ImageView();
        imgView.setFitWidth(2 * LCD_WIDTH);
        imgView.setFitHeight(2 * LCD_HEIGHT); //TODO fit stage/scene/imgview
        
        BorderPane borderPane = new BorderPane();
        borderPane.getChildren().add(imgView);
        
        Scene mainScene = new Scene(borderPane);
        setInput(mainScene, gameboj.joypad());
        
        primaryStage.setWidth(2 * LCD_WIDTH);
        primaryStage.setHeight(2 * LCD_HEIGHT);
        primaryStage.setScene(mainScene);       
        primaryStage.show();
        
        long start = System.nanoTime();
        
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                long elapsed = now - start;
                long elapsedCycles = (long) (elapsed * CYCLES_PER_NANOSECOND);
                
                gameboj.runUntil(elapsedCycles);
                
                imgView.setImage(ImageConverter.convert(gameboj.lcdController().currentImage()));
            }
        }.start();
    }

    private static void setInput(Scene scene, Joypad jp) {
        // Set up keyboard input
        scene.setOnKeyPressed(e -> {
            switch (e.getCode()) {
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

            switch (e.getText()) { //TODO this doesn't work, why?
            case "A":
                jp.keyPressed(Key.A);
                break;
            case "B":
                jp.keyPressed(Key.B);
                break;
            case "S":
                jp.keyPressed(Key.START);
                break;
            case "Space":
                jp.keyPressed(Key.START);
                break;
            }
        });

        scene.setOnKeyReleased(e -> {
            switch (e.getCode()) {
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

            switch (e.getText()) {
            case "A":
                jp.keyReleased(Key.A);
                break;
            case "B":
                jp.keyReleased(Key.B);
                break;
            case "S":
                jp.keyReleased(Key.START);
                break;
            case "Space":
                jp.keyReleased(Key.START);
                break;
            }
        });
    }
}
