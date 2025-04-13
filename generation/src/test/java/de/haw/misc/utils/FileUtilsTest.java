package de.haw.misc.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class FileUtilsTest {

    @ParameterizedTest
    @ValueSource( strings = { ".", "src", "testDir" } )
    void testToAbsolutePath( String input ) {
        String expected = new File( input ).getAbsolutePath();
        String actual = FileUtils.toAbsolutePath( input );
        assertEquals( expected, actual );
    }

    @Test
    void testGetWithRelativePath() {
        String relPath = "someRelativePath";
        File file = FileUtils.get( relPath );
        assertEquals( FileUtils.toAbsolutePath( relPath ), file.getPath() );
    }

    @Test
    void testGetWithAbsolutePath() {
        String absPath = FileUtils.currentWorkingDir() + File.separator + "testAbsolute";
        File file = FileUtils.get( absPath );
        assertEquals( absPath, file.getPath() );
    }

    @Test
    void testIsAbsolutePathNullThrows() {
        IllegalArgumentException ex = assertThrows( IllegalArgumentException.class,
                () -> FileUtils.isAbsolutePath( null ) );
        assertEquals( "Provided path is null!", ex.getMessage() );
    }

    @Test
    void testIsAbsolutePathTrue() {
        String path = FileUtils.currentWorkingDir() + File.separator + "dummy";
        assertTrue( FileUtils.isAbsolutePath( path ) );
    }

    @Test
    void testIsAbsolutePathFalse() {
        assertFalse( FileUtils.isAbsolutePath( "dummy" ) );
    }

    @Test
    void testCurrentWorkingDir() {
        String expected = new File( "" ).getAbsolutePath();
        assertEquals( expected, FileUtils.currentWorkingDir() );
    }

    @Test
    void testGetDirectoryNamesInDir( @TempDir Path tempDir ) throws IOException {
        Files.createDirectory( tempDir.resolve( "d1" ) );
        Files.createDirectory( tempDir.resolve( "d2" ) );
        Files.createFile( tempDir.resolve( "file.txt" ) );
        List<String> names = new ArrayList<>( FileUtils.getDirectoryNamesInDir( tempDir.toString() ) );
        List<String> expected = new ArrayList<>( Arrays.asList( "d1", "d2" ) );
        Collections.sort( names );
        Collections.sort( expected );
        assertEquals( expected, names );
    }

    @Test
    void testGetDirectoryPathsInDir( @TempDir Path tempDir ) throws IOException {
        Path d1 = Files.createDirectory( tempDir.resolve( "d1" ) );
        Path d2 = Files.createDirectory( tempDir.resolve( "d2" ) );
        Files.createFile( tempDir.resolve( "file.txt" ) );
        List<String> paths = new ArrayList<>( FileUtils.getDirectoryPathsInDir( tempDir.toString() ) );
        List<String> expected = new ArrayList<>(
                Arrays.asList( d1.toFile().getAbsolutePath(), d2.toFile().getAbsolutePath() ) );
        Collections.sort( paths );
        Collections.sort( expected );
        assertEquals( expected, paths );
    }

    @Test
    void testGetDirectoriesInDir( @TempDir Path tempDir ) throws IOException {
        Files.createDirectory( tempDir.resolve( "dir1" ) );
        Files.createDirectory( tempDir.resolve( "dir2" ) );
        Files.createFile( tempDir.resolve( "file.dat" ) );
        List<File> dirs = FileUtils.getDirectoriesInDir( tempDir.toString() );
        List<String> dirNames = dirs.stream().map( File::getName ).sorted().collect( Collectors.toList() );
        List<String> expected = Arrays.asList( "dir1", "dir2" );
        assertEquals( expected, dirNames );
    }

    @Test
    void testGetDirectoriesInDirWithFile( @TempDir Path tempDir ) throws IOException {
        Path file = Files.createFile( tempDir.resolve( "someFile.txt" ) );
        List<File> dirs = FileUtils.getDirectoriesInDir( file.toString() );
        assertTrue( dirs.isEmpty() );
    }

    @Test
    void testGetDirectoriesInDirNonExistent() {
        List<File> dirs = FileUtils.getDirectoriesInDir( "nonExistentDirXYZ" );
        assertTrue( dirs.isEmpty() );
    }
}
