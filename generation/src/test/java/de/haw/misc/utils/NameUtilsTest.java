package de.haw.misc.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NameUtilsTest {

    @ParameterizedTest
    @CsvSource( {
            "org.example.MyClass, MyClass", "java.lang.String, String", "MyClass, MyClass"
    } )
    void testExtractClassNameNonEmpty( String scopedClassname, String expected ) {
        assertEquals( expected, NameUtils.extractClassName( scopedClassname ) );
    }

    @Test
    void testExtractClassNameEmptyString() {
        assertEquals( "", NameUtils.extractClassName( "" ) );
    }

    @Test
    void testExtractClassNameNull() {
        assertEquals( "", NameUtils.extractClassName( null ) );
    }

    @ParameterizedTest
    @CsvSource( {
            "org.example.MyClass, '.', true",
            "MyClass, '.', false",
            "example-test, '-', true",
            "exampletest, '-', false"
    } )
    void testIsScopedClassName( String scopedClassname, String delimiter, boolean expected ) {
        assertEquals( expected, NameUtils.isScopedClassName( scopedClassname, delimiter ) );
    }

    @Test
    void testIsScopedClassNameNullValues() {
        assertThrows( NullPointerException.class, () -> NameUtils.isScopedClassName( null, "." ) );
        assertThrows( NullPointerException.class, () -> NameUtils.isScopedClassName( "org.example.MyClass", null ) );
    }
}
