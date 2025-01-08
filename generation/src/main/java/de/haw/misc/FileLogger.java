package de.haw.misc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor( staticName = "of" )
public class FileLogger {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern( "yyyy-MM-dd HH:mm:ss" );

    private static final String TIMESTAMP_REGEX = "\\[\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\] ";

    private final String fileName;

    public void clear() {
        try {
            Files.write( Paths.get( fileName ), new ArrayList<String>(), StandardOpenOption.TRUNCATE_EXISTING );
        } catch ( IOException e ) {
            log.error( "Error clearing file: {}", e.getMessage() );
        }
    }

    public void write( final String line ) {
        this.write( line, true );
    }

    public void write( final String line, final boolean addTimestamp ) {
        try ( BufferedWriter writer = new BufferedWriter( new FileWriter( fileName, true ) ) ) {
            if( addTimestamp ) {
                writer.write(  String.format( "[%s] %s", LocalDateTime.now().format( TIMESTAMP_FORMAT ), line ) );
            } else {
                writer.write( line );
            }
            writer.newLine();
        } catch ( IOException e ) {
            log.error( "Error writing to file: {}", e.getMessage() );
        }
    }

    public List<String> read() {
        try {
            return Files.readAllLines( Paths.get( fileName ) ).stream().filter( StringUtils::isNotBlank ).toList();
        } catch ( IOException e ) {
            log.error( "Error reading file: {}", e.getMessage() );
            return new ArrayList<>();
        }
    }

    public List<String> readContent() {
        return this.read().stream().map( line -> line.replaceFirst( TIMESTAMP_REGEX, "" ).trim() ).toList();
    }

    public void clearLastLines( final int n ) {
        final List<String> lines = read();
        this.clear();
        for ( int i = 0; i < Math.max( 0, lines.size() - n ); i++ ) {
            this.write( lines.get( i ), false );
        }
    }

    public void clearLastLine() {
        this.clearLastLines( 1 );
    }

}