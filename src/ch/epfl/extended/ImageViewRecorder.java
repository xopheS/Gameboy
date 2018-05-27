package ch.epfl.extended;

import javafx.scene.image.ImageView;

public final class ImageViewRecorder {
	AVIWriter aviWriter;
	ImageView sourceImageView;
	private boolean isOn;
	
	public ImageViewRecorder(String fileName, ImageView sourceImageView) {
		aviWriter = new AVIWriter(fileName);
		this.sourceImageView = sourceImageView;
	}
	
	public void start() {
		isOn = true;
	}
	
	public void write() {
		if (isOn) {
			aviWriter.write(sourceImageView.getImage());
		}
	}
	
	public void stop() {
		isOn = false;
	}
}
