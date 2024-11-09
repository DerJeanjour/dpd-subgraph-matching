package backend.game.bitbased;

/**
 * |    := Bitwise OR           -> 0101 | 0011 => 0111
 * &    := Bitwise AND          -> 0101 & 0011 => 0001
 * ^    := Bitwise XOR          -> 0101 ^ 0011 => 0110
 * ~    := Bitwise Complement   -> 0011 => 1100
 * <<   := Left Shift           -> 0110 => 1100
 * >>   := Right Shift          -> 0110 => 0011
 */
public class BitUtils {

    public static long and( final long a, final long b ) {
        return a & b;
    }

    public static long or( final long a, final long b ) {
        return a | b;
    }

    public static long xor( final long a, final long b ) {
        return a ^ b;
    }

    public static long invert( final long value ) {
        return ~value;
    }

    public static long shiftRight( final long value, final int steps ) {
        return value >> steps;
    }

    public static long shiftLeft( final long value, final int steps ) {
        return value << steps;
    }

    public static String toString( long value ) {
        StringBuilder bitString = new StringBuilder();

        for ( int i = 63; i >= 0; i-- ) {
            long mask = 1L << i;
            long bit = ( value & mask ) == 0 ? 0 : 1;
            bitString.append( bit );
        }

        return bitString.toString();
    }

}
