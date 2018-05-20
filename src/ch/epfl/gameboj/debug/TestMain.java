package ch.epfl.gameboj.debug;

public class TestMain {

    public static void main(String[] args) {
        System.out.println(Integer.numberOfTrailingZeros(0b00010000));
        System.out.println(31 - Integer.numberOfLeadingZeros(Integer.lowestOneBit(0b00010000)));

    }
}
