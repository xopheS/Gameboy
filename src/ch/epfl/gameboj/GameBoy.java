package ch.epfl.gameboj;

import java.util.Objects;

import ch.epfl.gameboj.component.Joypad;
import ch.epfl.gameboj.component.cartridge.Cartridge;
import ch.epfl.gameboj.component.cpu.Cpu;
import ch.epfl.gameboj.component.lcd.LcdController;
import ch.epfl.gameboj.component.memory.BootRomController;
import ch.epfl.gameboj.component.memory.Ram;
import ch.epfl.gameboj.component.memory.RamController;
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
    private final Cpu cpu = new Cpu();
    private final Timer timer = new Timer(cpu);
    private final LcdController lcdController = new LcdController(cpu);
    private final Joypad joypad = new Joypad(cpu);

    public static final long CYCLES_PER_SECOND = (long) Math.pow(2, 20);
    public static final double CYCLES_PER_NANOSECOND = CYCLES_PER_SECOND / Math.pow(10, 9);

    private long currentCycle;

    /**
     * Constructeur qui initialise une Gameboy avec une cartouche donnée.
     *
     * @param cartridge
     *            La cartouche que la Gameboy va exécuter
     *
     * @throws NullPointerException
     *             si la cartouche est null
     */
    public GameBoy(Cartridge cartridge) {
        bootRomController = new BootRomController(Objects.requireNonNull(cartridge));
        bootRomController.attachTo(bus);

        workRamController.attachTo(bus);
        workRamEchoController.attachTo(bus);

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

    public Joypad getJoypad() {
        return joypad;
    }
}
