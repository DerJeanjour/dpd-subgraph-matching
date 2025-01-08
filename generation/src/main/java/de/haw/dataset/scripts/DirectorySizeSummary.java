package de.haw.dataset.scripts;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvBindByPosition;
import de.haw.misc.utils.CsvUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class DirectorySizeSummary {

    @Data
    public static class CsvData {

        @CsvBindByPosition( position = 0 )
        @CsvBindByName( column = "project" )
        private String project;

        @CsvBindByPosition( position = 1 )
        @CsvBindByName( column = "bytes" )
        private long bytes;

    }

    public static void main( String[] args ) {
        final String sourceDir = "datasets/java/dpdf/java_projects";
        final String summaryDir = "datasets/java/dpdf/size_summary.csv";
        generateDirectorySizes( sourceDir, summaryDir );
        log.info( "Directory sizes: {}", getDirectorySizes( summaryDir ) );
    }

    public static void generateDirectorySizes( final String sourceDir, final String summaryDir ) {
        final File directory = new File( sourceDir );

        if ( !directory.exists() || !directory.isDirectory() ) {
            log.warn( "The specified path is not a directory." );
            return;
        }

        final Map<String, Long> directorySizes = new HashMap<>();
        final File[] subDirs = Objects.requireNonNull( directory.listFiles( File::isDirectory ) );
        int processed = 1;
        for ( final File subDir : subDirs ) {
            log.info( "Processing subdir {}/{}", processed, subDirs.length );
            directorySizes.put( subDir.getName(), calculateDirectorySize( subDir.toPath() ) );
            processed++;
        }

        final List<Map.Entry<String, Long>> sortedDirectories = new ArrayList<>( directorySizes.entrySet() );
        sortedDirectories.sort( Map.Entry.comparingByValue() );

        final List<CsvData> csvRows = new ArrayList<>();
        for ( Map.Entry<String, Long> entry : sortedDirectories ) {
            final CsvData csvRow = new CsvData();
            csvRow.setProject( entry.getKey() );
            csvRow.setBytes( entry.getValue() );
            csvRows.add( csvRow );
        }
        final byte[] csv = CsvUtils.write( csvRows, CsvData.class );
        try ( FileOutputStream fileOutputStream = new FileOutputStream( summaryDir ) ) {
            fileOutputStream.write( csv );
        } catch ( IOException e ) {
            log.info( "Failed to write csv: {}", e.getMessage() );
        }
    }

    private static long calculateDirectorySize( final Path path ) {
        log.info( "Calculate dir size of {} ...", path.getFileName() );
        final AtomicLong size = new AtomicLong();
        try {
            Files.walkFileTree( path, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile( final Path file, final BasicFileAttributes attrs ) {
                    size.addAndGet( attrs.size() );
                    return FileVisitResult.CONTINUE;
                }
            } );
        } catch ( IOException e ) {
            log.error( "Failed to calculate dir size for path {}: {}", path.getFileName(), e.getMessage() );
        }
        return size.get();
    }

    public static Map<String, Long> getDirectorySizes( final String summaryDir ) {
        final Map<String, Long> dirSizes = new HashMap<>();
        try ( FileInputStream fileInputStream = new FileInputStream( summaryDir ) ) {
            // Allocate byte array to hold file data
            byte[] data = new byte[fileInputStream.available()];
            fileInputStream.read( data );
            final List<CsvData> csv = CsvUtils.read( data, CsvData.class, ',', true );

            csv.forEach( csvData -> dirSizes.put( csvData.getProject(), csvData.getBytes() ) );

        } catch ( IOException e ) {
            log.error( "Failed to read csv file {}: {}", summaryDir, e.getMessage() );
        }
        return dirSizes;
    }

}
