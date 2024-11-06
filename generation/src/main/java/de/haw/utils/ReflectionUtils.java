package de.haw.utils;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;

@Slf4j
public class ReflectionUtils {

    public static void setInt( final Object obj, final String field, final int value ) {
        try {
            Field depth = obj.getClass().getDeclaredField( field );
            depth.setAccessible( true );
            depth.setInt( obj, value );
        } catch ( NoSuchFieldException | IllegalAccessException e ) {
            log.error( "Failed to set field [{}] of class {}", field, obj.getClass().getName() );
            throw new RuntimeException( e );
        }
    }

}
