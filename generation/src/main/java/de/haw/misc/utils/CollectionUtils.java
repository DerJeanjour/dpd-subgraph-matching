package de.haw.misc.utils;

import java.util.ArrayList;
import java.util.List;

public class CollectionUtils {

    public static <T> List<T> intersection( final List<T> a, final List<T> b ) {
        final List<T> intersection = new ArrayList<>();
        if ( a == null || b == null ) {
            return intersection;
        }
        for ( final T value : a ) {
            if ( b.contains( value ) ) {
                intersection.add( value );
            }
        }
        return intersection;
    }

}
