package ch.epfl.gameboj.gui;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {
    
    public static void main(String[] args) {
        Application.launch(args);
      }

    @Override
    public void start(Stage primaryStage) {
        long start = System.nanoTime();
        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                long elapsed = now - start;
                System.out.printf("Temps écoulé : %.2f s%n", elapsed / 1e9);
            }
        };
        timer.start();
    }

}
