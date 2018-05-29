package ch.epfl.gameboj.mvc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.sound.sampled.LineUnavailableException;

import ch.epfl.gameboj.GameBoy;
import ch.epfl.gameboj.component.cartridge.Cartridge;

public final class Controller {
	private static final Controller controller = new Controller();
	private Model model;
	
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
			model.setGameboj(new GameBoy(Cartridge.ofFile(new File(fileName)), saveName));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (LineUnavailableException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
