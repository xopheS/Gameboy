package ch.epfl.gameboj.component.sound;

import ch.epfl.gameboj.component.Clocked;
import ch.epfl.gameboj.component.Component;

public abstract class SoundCircuit implements Component, Clocked {
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
