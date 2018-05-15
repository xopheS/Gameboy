package ch.epfl.gameboj.component.sound;

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

    public SoundController() throws InterruptedException, LineUnavailableException {
        //////////////////////////////// TEST
        // int SAMPLE_RATE = 44100;
        //
        // AudioFormat gameboySoundFormat = new
        // AudioFormat(AudioFormat.Encoding.PCM_SIGNED, SAMPLE_RATE, 16, 2, 4,
        // SAMPLE_RATE, false);
        //
        // DataLine.Info info = new DataLine.Info(SourceDataLine.class,
        // gameboySoundFormat);
        // final SourceDataLine soundLine = (SourceDataLine) AudioSystem.getLine(info);
        // soundLine.open(gameboySoundFormat);
        //
        // info = new DataLine.Info(TargetDataLine.class, gameboySoundFormat);
        // final TargetDataLine targetLine = (TargetDataLine) AudioSystem.getLine(info);
        // targetLine.open(gameboySoundFormat);
        //
        // final ByteArrayOutputStream out = new ByteArrayOutputStream();
        //
        // Thread audioThread = new Thread() {
        // @Override
        // public void run() {
        // soundLine.start();
        // while (true) {
        // soundLine.write(out.toByteArray(), 0, out.size());
        // }
        // }
        // };
        //
        // Thread targetThread = new Thread() {
        // @Override
        // public void run() {
        // targetLine.start();
        // byte[] data = new byte[targetLine.getBufferSize() / 5];
        // int readBytes;
        // while (true) {
        // readBytes = targetLine.read(data, 0, data.length);
        // out.write(data, 0, readBytes);
        // }
        // }
        // };
        //
        // FloatControl audioVolume = (FloatControl)
        // soundLine.getControl(FloatControl.Type.MASTER_GAIN);
        // audioVolume.setValue(6f);
        //
        // targetThread.start();
        // System.out.println("Started recording");
        // Thread.sleep(5000);
        // targetLine.stop();
        // targetLine.close();
        //
        // System.out.println("Ended recording");
        // System.out.println("Starting playback");
        //
        // audioThread.start();
        // Thread.sleep(5000);
        // soundLine.stop();
        // soundLine.close();
        //
        // System.out.println("Ended playback");

        /////////////////////////////////////
    }

    public void start() {

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
