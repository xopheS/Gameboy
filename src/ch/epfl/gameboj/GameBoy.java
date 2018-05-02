package ch.epfl.gameboj;

import java.util.Objects;

import ch.epfl.gameboj.component.Joypad;
import ch.epfl.gameboj.component.Timer;
import ch.epfl.gameboj.component.cartridge.Cartridge;
import ch.epfl.gameboj.component.cpu.Cpu;
import ch.epfl.gameboj.component.lcd.LcdController;
import ch.epfl.gameboj.component.memory.BootRomController;
import ch.epfl.gameboj.component.memory.Ram;
import ch.epfl.gameboj.component.memory.RamController;

/**
 * Cette classe modélise la Gameboy en entier, avec tous ses composants
 * fonctionnant ensemble.
 *
 * @author Christophe Saad (282557)
 * @author David Cian (287967)
 *
 */
public class GameBoy {

    private final Bus bus = new Bus();
    private final Ram workRam;
    private final RamController workRamController, workRamEchoController;
    private final BootRomController bootRomController;
    private final Timer timer;
    private final Cpu cpu;
    private final LcdController lcdController;
    private final Joypad joypad;

    public static final long CYCLES_PER_SECOND = (long) Math.pow(2, 20);
    public static final double CYCLES_PER_NANOSECOND = CYCLES_PER_SECOND / Math.pow(10, 9);

    private long cycles;

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
        workRam = new Ram(AddressMap.WORK_RAM_SIZE);

        bootRomController = new BootRomController(Objects.requireNonNull(cartridge));
        bootRomController.attachTo(bus);

        workRamController = new RamController(workRam, AddressMap.WORK_RAM_START, AddressMap.WORK_RAM_END);
        workRamEchoController = new RamController(workRam, AddressMap.ECHO_RAM_START, AddressMap.ECHO_RAM_END);

        workRamController.attachTo(bus);
        workRamEchoController.attachTo(bus);

        cpu = new Cpu();
        cpu.attachTo(bus);

        timer = new Timer(cpu);
        timer.attachTo(bus);

        lcdController = new LcdController(cpu);
        lcdController.attachTo(bus);

        joypad = new Joypad(cpu);
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
        Preconditions.checkArgument(cycles <= cycle);
        while (cycles < cycle) {
            timer.cycle(cycles);
            lcdController.cycle(cycles);
            cpu.cycle(cycles);
            //System.out.println(joypad.P1);
            cycles++;
        }
    }

    /**
     * Getter du cycle actuel.
     *
     * @return le cycle actuel
     */
    public long cycles() {
        return cycles;
    }

    /**
     * Getter du timer.
     *
     * @return le timer de la Gameboy
     */
    public Timer timer() {
        return timer;
    }

    /**
     * Getter du bus.
     *
     * @return le bus de la Gameboy
     */
    public Bus bus() {
        return bus;
    }

    /**
     * Getter du cpu.
     *
     * @return le processeur de la Gameboy
     */
    public Cpu cpu() {
        return cpu;
    }

    /**
     * Getter de LcdController.
     *
     * @return le LcdController de la Gameboy
     */
    public LcdController lcdController() {
        return lcdController;
    }

    /**
     * Getter du joypad.
     *
     * @return le joypad de la Gameboy
     */
    public Joypad joypad() {
        return joypad;
    }

}
