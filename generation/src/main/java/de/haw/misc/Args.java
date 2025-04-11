package de.haw.misc;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class Args {

    private final static String TRUE_VALUE = "true";

    private final Map<String, String> args = new HashMap<>();

    private Args( final String[] args ) {
        this.parse( args );
    }

    public static Args of( final String[] args ) {
        return new Args( args );
    }

    public static Args empty() {
        return new Args( new String[]{} );
    }

    private void parse( final String[] args ) {
        this.args.clear();
        if ( args == null ) {
            return;
        }
        for ( final String arg : args ) {
            // Check if the argument is an option/flag (starts with '-' or '--')
            if ( arg.startsWith( "-" ) ) {
                String key;
                String value = TRUE_VALUE;  // Default value for flags (no explicit value provided)

                // Remove leading dashes. This handles both "-" and "--"
                if ( arg.startsWith( "--" ) ) {
                    key = arg.substring( 2 );
                } else {
                    key = arg.substring( 1 );
                }

                // Check if the argument contains an '=' sign (e.g., --port=8080)
                if ( key.contains( "=" ) ) {
                    // Split into key and value
                    final String[] parts = key.split( "=", 2 );
                    key = parts[0];
                    value = parts[1];
                }
                this.args.put( key, value );
            } else {
                throw new IllegalArgumentException( "Argument must start with \"-\" or \"--\"" );
            }
        }
    }

    public boolean has( final String key ) {
        return this.args.containsKey( key ) && this.args.get( key ) != null;
    }

    public String get( final String key ) {
        if ( !this.has( key ) ) {
            throw new IllegalArgumentException( "No argument found for key: " + key );
        }
        return this.args.get( key );
    }

    public String getOrElse( final String key, final String defaultValue ) {
        if ( !this.has( key ) ) {
            return defaultValue;
        }
        return this.args.get( key );
    }

    public boolean isTrue( final String key ) {
        return has( key ) && this.get( key ).equals( TRUE_VALUE );
    }

}
