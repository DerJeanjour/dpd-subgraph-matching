package de.haw.misc.utils;

import java.util.Locale;

public class FormatUtils {

    public static String format( final double value, final int precision ) {
        return String.format( Locale.ENGLISH, "%." + precision + "f", value );
    }

}
