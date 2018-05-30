package ch.epfl.gameboj.mvc;

public final class View {
	private static final View view = new View();
	
	private View() {
		
	}
	
	public static View getView() {
		return view;
	}
}
