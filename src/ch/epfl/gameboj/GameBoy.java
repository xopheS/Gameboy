package ch.epfl.gameboj;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Objects;

import javax.sound.sampled.LineUnavailableException;

import ch.epfl.gameboj.component.Joypad;
import ch.epfl.gameboj.component.cartridge.Cartridge;
import ch.epfl.gameboj.component.cpu.Cpu;
import ch.epfl.gameboj.component.lcd.LcdController;
import ch.epfl.gameboj.component.memory.BootRomController;
import ch.epfl.gameboj.component.memory.Ram;
import ch.epfl.gameboj.component.memory.RamController;
import ch.epfl.gameboj.component.serial.SerialIO;
import ch.epfl.gameboj.component.sound.SoundController;
import ch.epfl.gameboj.component.time.Timer;

/**
 * Cette classe modélise la Gameboy en entier, avec tous ses composants
 * fonctionnant ensemble.
 *
 * @author Christophe Saad (282557)
 * @author David Cian (287967)
 *
 */
public final class GameBoy {

    private final Bus bus = new Bus();
    private final Ram workRam = new Ram(AddressMap.WRAM_BANK_0_SIZE + AddressMap.WRAM_BANK_1_SIZE);
    private final RamController workRamController = new RamController(workRam, AddressMap.WRAM_BANK_0_START, AddressMap.WRAM_BANK_1_END);
    private final RamController workRamEchoController = new RamController(workRam, AddressMap.ECHO_RAM_START, AddressMap.ECHO_RAM_END);
    private final BootRomController bootRomController;
    private final Cartridge loadedCartridge;
    private final Cpu cpu = new Cpu();
    private final Timer timer = new Timer(cpu);
    private final LcdController lcdController = new LcdController(cpu);
    private final SoundController soundController;
    private final Joypad joypad = new Joypad(cpu);
    private final SerialIO serialIO = new SerialIO(cpu);

    public static final long CYCLES_PER_SECOND = (long) Math.pow(2, 20);
    public static final double CYCLES_PER_NANOSECOND = CYCLES_PER_SECOND / 1e9;

    private long currentCycle;
    
    public GameBoy(Cartridge cartridge) throws LineUnavailableException {
    	loadedCartridge = cartridge;
    	
        bootRomController = new BootRomController(Objects.requireNonNull(cartridge));
        bootRomController.attachTo(bus);

        workRamController.attachTo(bus);
        workRamEchoController.attachTo(bus);
        
        soundController = new SoundController(cpu);
        soundController.attachTo(bus);

        cpu.attachTo(bus);

        timer.attachTo(bus);

        lcdController.attachTo(bus);

        joypad.attachTo(bus);
        
        serialIO.attachTo(bus);
    }

    /**
     * Constructeur qui initialise une Gameboy avec une cartouche donnée.
     *
     * @param cartridge
     *            La cartouche que la Gameboy va exécuter
     * @throws LineUnavailableException 
     *
     * @throws NullPointerException
     *             si la cartouche est null
     */
    public GameBoy(Cartridge cartridge, File saveFile) throws LineUnavailableException {
    	loadedCartridge = cartridge;
    	
        bootRomController = new BootRomController(Objects.requireNonNull(cartridge));
        try (FileInputStream fis = new FileInputStream(saveFile)) {
			bootRomController.setCartridgeRam(fis.readAllBytes());
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        bootRomController.attachTo(bus);

        workRamController.attachTo(bus);
        workRamEchoController.attachTo(bus);
        
        soundController = new SoundController(cpu);
        soundController.attachTo(bus);

        cpu.attachTo(bus);

        timer.attachTo(bus);

        lcdController.attachTo(bus);

        joypad.attachTo(bus);
    }

    /**
     * Cette méthode dit à la Gameboy de continuer à fonctionner jusqu'à atteindre
     * un certain cycle.
     *
     * @param cycle
     *            Le cycle maximal à atteindre
     *
     * @throws IllegalArgumentException
     *             si le cycle à atteindre est strictement inférieur au cycle actuel
     */
    public void runUntil(long cycle) {
        Preconditions.checkArgument(currentCycle <= cycle);
        while (currentCycle < cycle) {
            timer.cycle(currentCycle);
            serialIO.cycle(currentCycle);
            soundController.cycle(currentCycle);
            lcdController.cycle(currentCycle);
            cpu.cycle(currentCycle);
            currentCycle++;
        }
    }

    public long getCycles() {
        return currentCycle;
    }

    public Timer getTimer() {
        return timer;
    }

    public Bus getBus() {
        return bus;
    }

    public Cpu getCpu() {
        return cpu;
    }

    public LcdController getLcdController() {
        return lcdController;
    }
    
    public SoundController getSoundController() {
    	return soundController;
    }

    public Joypad getJoypad() {
        return joypad;
    }
    
    public Cartridge getCartridge() {
    	return loadedCartridge;
    }
    
    public SerialIO getSerialIO() {
    	return serialIO;
    }
}
