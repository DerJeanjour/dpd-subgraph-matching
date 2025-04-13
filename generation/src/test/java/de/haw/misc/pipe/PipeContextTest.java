package de.haw.misc.pipe;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class PipeContextTest {

    @Test
    void testSetAndGetStringValue() {
        PipeContext context = PipeContext.empty();
        context.set( "key", "value" );

        // Using get(String key)
        assertEquals( "value", context.get( "key" ) );

        // Using get(String key, Class<String>)
        Optional<String> optValue = context.get( "key", String.class );
        assertTrue( optValue.isPresent() );
        assertEquals( "value", optValue.get() );

        // Using get(String key, default, Class<String>)
        assertEquals( "value", context.get( "key", "default", String.class ) );
    }

    @Test
    void testGetNonExistingKeyReturnsNullOrDefault() {
        PipeContext context = PipeContext.empty();

        // get(String key) returns null for non-existing key
        assertNull( context.get( "nonExisting" ) );

        // get(String key, default, Class<String>) returns default value
        assertEquals( "default", context.get( "nonExisting", "default", String.class ) );

        // get(String key, Class<String>) returns Optional.empty
        Optional<String> result = context.get( "nonExisting", String.class );
        assertFalse( result.isPresent() );
    }

    @Test
    void testGetWithWrongTypeReturnsOptionalEmptyAndDefault() {
        PipeContext context = PipeContext.empty();
        context.set( "number", 123 );

        // Attempting to retrieve an Integer as a String yields Optional.empty
        Optional<String> wrongTypeOpt = context.get( "number", String.class );
        assertFalse( wrongTypeOpt.isPresent() );

        // And using the default value variant returns the provided default
        String defaultValue = "default";
        assertEquals( defaultValue, context.get( "number", defaultValue, String.class ) );

        // Correctly retrieving as Integer works
        Optional<Integer> intOpt = context.get( "number", Integer.class );
        assertTrue( intOpt.isPresent() );
        assertEquals( 123, intOpt.get() );
    }

    @Test
    void testSetOverwritesValue() {
        PipeContext context = PipeContext.empty();
        context.set( "key", "firstValue" );
        assertEquals( "firstValue", context.get( "key" ) );

        context.set( "key", "secondValue" );
        assertEquals( "secondValue", context.get( "key" ) );
    }
}
