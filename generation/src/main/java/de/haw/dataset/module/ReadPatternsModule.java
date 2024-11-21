package de.haw.dataset.module;

import de.haw.dataset.model.Dataset;
import de.haw.dataset.model.DatasetDesignPatterns;
import de.haw.dataset.reader.PatternReader;
import de.haw.dataset.reader.PatternReaderCsv;
import de.haw.dataset.reader.PatternReaderXml;
import de.haw.misc.pipe.PipeContext;
import de.haw.misc.pipe.PipeModule;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

@Slf4j
@NoArgsConstructor( staticName = "instance" )
public class ReadPatternsModule<Target> extends PipeModule<File, DatasetDesignPatterns, Target> {

    @Override
    protected DatasetDesignPatterns processImpl( final File file, final PipeContext ctx ) {

        final boolean fileExists = ctx.get( PipeContext.CPG_DESIGN_PATTERNS_EXISTS, false, Boolean.class );
        if ( !fileExists || file == null ) {
            return null;
        }

        log.info( "Reading design pattern file {}", file.getName() );

        final Dataset dataset = ctx.get( PipeContext.CPG_DATASET_KEY, Dataset.class )
                .orElseThrow( IllegalStateException::new );

        return this.getReader( file ).read( dataset, file );
    }

    private PatternReader getReader( final File file ) {
        final String extension = getFileExtension( file );
        return switch ( extension ) {
            case "xml" -> PatternReaderXml.instance();
            case "csv" -> PatternReaderCsv.instance();
            default -> throw new IllegalArgumentException( "extension not supported: " + extension );
        };
    }

    public static String getFileExtension( File file ) {
        if ( file == null || !file.exists() ) {
            throw new IllegalArgumentException( "File must not be null and should exist" );
        }
        String fileName = file.getName();
        int lastDotIndex = fileName.lastIndexOf( '.' );

        // Check if there is a dot and it is not the first character
        if ( lastDotIndex > 0 && lastDotIndex < fileName.length() - 1 ) {
            return fileName.substring( lastDotIndex + 1 );
        }
        // No extension found
        return "";
    }

}
