package de.haw.misc.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReflectionUtilsTest {

    private static class Dummy {
        private int x;
        private int y;
    }

    @ParameterizedTest
    @CsvSource( {
            "x, 10", "y, -5", "x, 42"
    } )
    void testSetIntValidField( String fieldName, int value ) throws Exception {
        Dummy dummy = new Dummy();
        ReflectionUtils.setInt( dummy, fieldName, value );
        Field field = Dummy.class.getDeclaredField( fieldName );
        field.setAccessible( true );
        assertEquals( value, field.getInt( dummy ) );
    }

    @Test
    void testSetIntWithNonExistentFieldThrowsException() {
        Dummy dummy = new Dummy();
        assertThrows( RuntimeException.class, () -> ReflectionUtils.setInt( dummy, "nonExistent", 100 ) );
    }

    @Test
    void testSetIntWithNullObjectThrowsException() {
        assertThrows( NullPointerException.class, () -> ReflectionUtils.setInt( null, "x", 5 ) );
    }

    @Test
    void testSetIntWithNullFieldNameThrowsException() {
        Dummy dummy = new Dummy();
        assertThrows( NullPointerException.class, () -> ReflectionUtils.setInt( dummy, null, 5 ) );
    }
}
