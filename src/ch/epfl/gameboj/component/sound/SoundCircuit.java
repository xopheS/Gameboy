package ch.epfl.gameboj.component.sound;

public abstract class SoundCircuit {
	private float freq;
	private int[] wave;
	
	public int[] getWave() {
		return wave;
	}
	
	public float getFreq() {
		return freq;
	}
	
	public void setFreq(float freq) {
		this.freq = freq;
	}
}
