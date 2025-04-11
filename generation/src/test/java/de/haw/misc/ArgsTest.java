package de.haw.misc;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ArgsTest {

    @Test
    @DisplayName( "Test flag without explicit value using a single dash" )
    public void testSingleDashFlag() {
        String[] argsArray = { "-verbose" };
        Args args = Args.of( argsArray );

        // The flag 'verbose' should be present and default to "true"
        assertTrue( args.has( "verbose" ) );
        assertEquals( "true", args.get( "verbose" ) );
        assertTrue( args.isTrue( "verbose" ) );
    }

    @Test
    @DisplayName( "Test flag without explicit value using double dashes" )
    public void testDoubleDashFlag() {
        String[] argsArray = { "--debug" };
        Args args = Args.of( argsArray );

        // The flag 'debug' should be present and default to "true"
        assertTrue( args.has( "debug" ) );
        assertEquals( "true", args.get( "debug" ) );
        assertTrue( args.isTrue( "debug" ) );
    }

    @Test
    @DisplayName( "Test flag with an explicit value" )
    public void testFlagWithExplicitValue() {
        String[] argsArray = { "--port=8080" };
        Args args = Args.of( argsArray );

        // The flag 'port' should be present with value "8080"
        assertTrue( args.has( "port" ) );
        assertEquals( "8080", args.get( "port" ) );
        // Since the explicit value is not "true", isTrue() should return false.
        assertFalse( args.isTrue( "port" ) );
    }

    @Test
    @DisplayName( "Test behavior when retrieving a non-existent key" )
    public void testGetNonExistingKey() {
        String[] argsArray = { "--verbose" };
        Args args = Args.of( argsArray );

        // Attempting to get a non-existing key should throw an exception.
        assertThrows( IllegalArgumentException.class, () -> args.get( "nonexistent" ) );
    }

    @Test
    @DisplayName( "Test invalid argument format (argument not starting with '-' or '--')" )
    public void testInvalidArgumentFormat() {
        String[] argsArray = { "filename.txt" };

        // Constructor should throw an IllegalArgumentException because the argument does not start with '-' or '--'
        assertThrows( IllegalArgumentException.class, () -> Args.of( argsArray ) );
    }

    @Test
    @DisplayName( "Test behavior with a null input array" )
    public void testNullArguments() {
        // Passing null as an argument array should not cause a crash
        Args args = Args.of( null );

        // There should be no arguments present
        assertFalse( args.has( "any" ) );

        // Attempting to get any key should throw an exception.
        assertThrows( IllegalArgumentException.class, () -> args.get( "any" ) );
    }
}
