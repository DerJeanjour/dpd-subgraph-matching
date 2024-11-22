package de.haw.misc.utils;

import org.apache.commons.lang3.StringUtils;

public class NameUtils {

    public static String extractClassName( final String scopedClassname ) {
        if ( StringUtils.isEmpty( scopedClassname ) ) {
            return "";
        }
        final String[] parts = scopedClassname.split( "\\." );
        return parts[parts.length - 1];
    }

    public static boolean isScopedClassName( final String scopedClassname, final String delimiter ) {
        return scopedClassname.contains( delimiter );
    }

}
