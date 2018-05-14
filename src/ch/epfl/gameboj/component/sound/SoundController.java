package ch.epfl.gameboj.component.sound;

import java.util.Arrays;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.Register;
import ch.epfl.gameboj.RegisterFile;
import ch.epfl.gameboj.component.Clocked;
import ch.epfl.gameboj.component.Component;

public final class SoundController implements Component, Clocked {
    private static final int SAMPLE_RATE = 44100;
    private static final int SSAMPLE_RATE = 11000;

    private SourceDataLine soundLine;

    private Sound sound1 = new Sound1();
    private Sound sound2 = new Sound2();
    private Sound sound3 = new Sound3();
    private Sound sound4 = new Sound4();

    private enum Reg implements Register {
        CHANNEL_CONTROL, SELECT, SOUND_TOGGLE
    }

    RegisterFile<Register> scRegs = new RegisterFile<>(Reg.values());

    public SoundController() {
        AudioFormat gameboySoundFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, SAMPLE_RATE, Byte.SIZE, 2, 2,
                SAMPLE_RATE, true);

        DataLine.Info info = new DataLine.Info(SourceDataLine.class, gameboySoundFormat);

        try {
            soundLine = (SourceDataLine) AudioSystem.getLine(info);
        } catch (LineUnavailableException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        try {
            soundLine.open(gameboySoundFormat);
            System.out.println("open soundline");
        } catch (LineUnavailableException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void start() {
        byte[] test = new byte[10000];
        Arrays.fill(test, (byte) 100);

        Thread audioThread = new Thread() {
            @Override
            public void run() {
                System.out.println("start sound");
                this.start(); // TODO move this to powerup action
                while (true) {
                    soundLine.write(test, 0, 10000);
                }
            }
        };

        audioThread.start();
    }

    @Override
    public void cycle(long cycle) {

    }

    @Override
    public int read(int address) {
        switch (Preconditions.checkBits16(address)) {
        case AddressMap.REG_NR50:
            return scRegs.get(Reg.CHANNEL_CONTROL);
        case AddressMap.REG_NR51:
            return scRegs.get(Reg.SELECT);
        case AddressMap.REG_NR52:
            return scRegs.get(Reg.SOUND_TOGGLE);
        default:
            return NO_DATA;
        }
    }

    @Override
    public void write(int address, int data) {
        Preconditions.checkBits8(data);

        switch (Preconditions.checkBits16(address)) {
        case AddressMap.REG_NR50:
            scRegs.set(Reg.CHANNEL_CONTROL, data);
            break;
        case AddressMap.REG_NR51:
            scRegs.set(Reg.SELECT, data);
            break;
        case AddressMap.REG_NR52:
            scRegs.set(Reg.SOUND_TOGGLE, data);
            break;
        }
    }
}
