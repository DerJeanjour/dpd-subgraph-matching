package de.haw.misc.utils;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class FileUtils {

    public static File get( final String path ) {
        String absPath = path;
        if ( !isAbsolutePath( absPath ) ) {
            absPath = toAbsolutePath( absPath );
        }
        return new File( absPath );
    }

    public static String toAbsolutePath( final String relativePath ) {
        final Path relative = Paths.get( relativePath );
        return relative.toAbsolutePath().toString();
    }

    public static boolean isAbsolutePath( final String path ) {
        if ( path == null ) {
            throw new IllegalArgumentException( "Provided path is null!" );
        }
        return path.startsWith( currentWorkingDir() );
    }

    public static String currentWorkingDir() {
        return toAbsolutePath( "" );
    }

    public static List<String> getDirectoriesInDir( final String path ) {
        final File rootFile = get( path );
        if ( rootFile.listFiles() == null || !rootFile.isDirectory() ) {
            return Collections.emptyList();
        }
        return Stream.of( Objects.requireNonNull( rootFile.listFiles() ) )
                .filter( File::isDirectory )
                .map( File::getAbsolutePath )
                .toList();
    }

}
