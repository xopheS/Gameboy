package ch.epfl.gameboj;

import ch.epfl.gameboj.component.Joypad;
import ch.epfl.gameboj.component.Joypad.Key;
import ch.epfl.gameboj.component.cartridge.Cartridge;
import ch.epfl.gameboj.component.lcd.LcdController;
import ch.epfl.gameboj.component.lcd.LcdImage;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * 
 * @author Ulysse Ramage
 * @author David Cian (added input module)
 *
 */
public final class DebugMainLive extends Application {

    private static final float EMULATION_SPEED = 1f;
    private static final int CYCLES_PER_ITERATION = (int) (17_556 * EMULATION_SPEED);
    private static final int[] COLOR_MAP = new int[] {
        0xFF_FF_FF, 0xD3_D3_D3, 0xA9_A9_A9, 0x00_00_00
    };

    public static void main(String[] args) {
        Application.launch(args);
    }
    
    @Override 
    public void start(Stage stage) throws IOException, InterruptedException {
        // Create GameBoy
        File romFile = new File(getParameters().getRaw().get(0));
        GameBoy gb = new GameBoy(Cartridge.ofFile(romFile));
        Joypad jp = gb.joypad();

        // Create Scene
        ImageView imageView = new ImageView();
        imageView.setImage(getImage(gb));
        imageView.setSmooth(false);
        Group root = new Group();
        Scene scene = new Scene(root);
        imageView.fitWidthProperty().bind(scene.widthProperty());
        imageView.fitHeightProperty().bind(scene.heightProperty());
        scene.setFill(Color.BLACK);
        setInput(scene, jp);
        HBox box = new HBox();
        box.getChildren().add(imageView);
        root.getChildren().add(box);
        stage.setWidth(LcdController.LCD_WIDTH);
        stage.setHeight(LcdController.LCD_HEIGHT);
        stage.setScene(scene);
        stage.sizeToScene();
        stage.minWidthProperty().bind(scene.heightProperty());
        stage.minHeightProperty().bind(scene.widthProperty());
        stage.setTitle("gameboj");
        stage.show();

        // Update GameBoy
        new AnimationTimer() {
            public void handle(long currentNanoTime) {
                gb.runUntil(gb.cycles() + CYCLES_PER_ITERATION);
                imageView.setImage(null);
                imageView.setImage(getImage(gb));
            }
        }.start();
    }
    
    private static Image getImage(GameBoy gb) {
        LcdImage lcdImage = gb.lcdController().currentImage();
        BufferedImage bufferedImage = new BufferedImage(lcdImage.getWidth(),
                lcdImage.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < lcdImage.getHeight(); ++y) {
            for (int x = 0; x < lcdImage.getWidth(); ++x) {
                bufferedImage.setRGB(x, y, COLOR_MAP[lcdImage.get(x, y)]);
            }
        }
        return SwingFXUtils.toFXImage(bufferedImage, null);
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
               case A:
                   jp.keyPressed(Key.A);
                   break;
               case B:
                   jp.keyPressed(Key.B);
                   break;
               case S:
                   jp.keyPressed(Key.SELECT);
                   break;
               case X:
                   jp.keyPressed(Key.START);
                   break;
               case Q:
                   System.exit(0);
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
               case A:
                   jp.keyReleased(Key.A);
                   break;
               case B:
                   jp.keyReleased(Key.B);
                   break;
               case S:
                   jp.keyReleased(Key.SELECT);
                   break;
               case X:
                   jp.keyReleased(Key.START);
                   break;
               }
           });
       }
}