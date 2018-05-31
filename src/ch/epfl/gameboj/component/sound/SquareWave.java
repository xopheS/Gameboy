package ch.epfl.gameboj.component.sound;

public abstract class SquareWave extends SoundCircuit {
	protected int[] getSquareWave(int waveDuty) {
		int[] squareWave = new int[32];
		int dutyLength = 0;
		
		switch (waveDuty) {
		case 0:
			dutyLength = 4; //12.5%
			break;
		case 1:
			dutyLength = 8; //25%
			break;
		case 2:
			dutyLength = 16; //50%
			break;
		case 3:
			dutyLength = 24; //75%
			break;
		}
		
		for (int i = 0; i < squareWave.length; ++i) {
			squareWave[i] = (i < dutyLength) ? 1 : -1;
		}
		
		return squareWave;
	}
}
