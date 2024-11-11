package de.haw.misc.utils;

public class FormatUtils {

    public static String format( final double value, final int precision ) {
        return String.format( "%." + precision + "f", value );
    }

}
