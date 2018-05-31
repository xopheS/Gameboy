package ch.epfl.gameboj.component.sound;

import ch.epfl.gameboj.component.Clocked;
import ch.epfl.gameboj.component.Component;

public abstract class SoundCircuit implements Component, Clocked {
	public static final int NUMBER_OF_STEPS = 32;
	
	protected int[] wave = new int[NUMBER_OF_STEPS];
	
	public int[] getWave() {
		return wave;
	}
	
	public abstract boolean isCounterActive();
	
	public abstract float getFreq();
	
	protected abstract int getDefaultInternalFreq();
	
	protected abstract void reset();
}
