package ch.epfl.gameboj.mvc.controller;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.sound.sampled.LineUnavailableException;

import ch.epfl.gameboj.GameBoy;
import ch.epfl.gameboj.component.Joypad;
import ch.epfl.gameboj.component.Joypad.Key;
import ch.epfl.gameboj.component.cartridge.Cartridge;
import ch.epfl.gameboj.mvc.Model;
import javafx.scene.Node;

public final class Controller {
	private static final Controller controller = new Controller();
	private Model model;
	
    public static boolean printCPU;
    public static boolean mustStart;
    
    public static String fileName;
	
	private Controller() {
		
	}
	
	public static Controller getController() {
		return controller;
	}
	
	public void setModel(Model model) {
		this.model = model;
	}
	
	public void startModelGameboy(String fileName) {
		try {
			Controller.fileName = fileName;
			model.setGameboj(new GameBoy(Cartridge.ofFile(new File(fileName))));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (LineUnavailableException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void startModelGameboy(File file) {
		try {
			Controller.fileName = file.getAbsolutePath();
			model.setGameboj(new GameBoy(Cartridge.ofFile(file)));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (LineUnavailableException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void startModelGameboy(String fileName, String saveName) {
		try {
			Controller.fileName = fileName;
			model.setGameboj(new GameBoy(Cartridge.ofFile(new File(fileName)), saveName));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (LineUnavailableException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
    public void startEmulation(String fileName) {
		Controller.fileName = fileName;
    	startModelGameboy(fileName);
		mustStart = true;
    }
	
	public void setInput(Node node, Joypad jp) {
        // Set up keyboard input
        node.setOnKeyPressed(e -> {
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
            case H:
            	printCPU = !printCPU;
            	break;
            }
        });

        node.setOnKeyReleased(e -> {
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
	
	public boolean getMustStart() {
		return mustStart;
	}
}
