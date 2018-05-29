package ch.epfl.gameboj.mvc;

import ch.epfl.gameboj.GameBoy;

public final class Model {
	private static final Model model = new Model();
	private View view;
	
	private GameBoy gameboj;
	
	private Model() {
		
	}
	
	public static Model getModel() {
		return model;
	}
	
	public void setView(View view) {
		this.view = view;
	}
	
	public GameBoy getGameboj() {
		return gameboj;
	}
	
	public void setGameboj(GameBoy gameboj) {
		this.gameboj = gameboj;
	}
}
