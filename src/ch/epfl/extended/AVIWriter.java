package ch.epfl.extended;

import java.io.File;

import javafx.scene.image.Image;

public final class AVIWriter {
	File targetFile;
	
	public AVIWriter(String fileName) {
		targetFile = new File(fileName);
	}
	
	public void write(Image sourceImage) {
		
	}
}
