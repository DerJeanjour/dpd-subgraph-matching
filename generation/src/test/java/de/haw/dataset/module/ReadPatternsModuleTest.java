package de.haw.dataset.module;

import de.haw.dataset.model.Dataset;
import de.haw.dataset.model.DatasetDesignPatterns;
import de.haw.dataset.reader.PatternReader;
import de.haw.dataset.reader.PatternReaderCsv;
import de.haw.dataset.reader.PatternReaderXml;
import de.haw.misc.pipe.PipeContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;


class ReadPatternsModuleTest {

    @Mock
    private PipeContext context;

    private ReadPatternsModule<Object> module;

    @TempDir
    Path tempDir;

    private File xmlFile;
    private File csvFile;
    private File unknownFile;
    private File fileWithoutExtension;

    @BeforeEach
    void setUp() throws IOException {
        MockitoAnnotations.openMocks( this );
        module = ReadPatternsModule.instance();

        // Create temporary test files
        xmlFile = createTempFile( "test.xml" );
        csvFile = createTempFile( "test.csv" );
        unknownFile = createTempFile( "test.txt" );
        fileWithoutExtension = createTempFile( "noextension" );
    }

    private File createTempFile( String fileName ) throws IOException {
        Path filePath = tempDir.resolve( fileName );
        Files.createFile( filePath );
        return filePath.toFile();
    }

    @Test
    void testFileDoesNotExistFlag() {
        when( context.get( PipeContext.CPG_DESIGN_PATTERNS_EXISTS, false, Boolean.class ) ).thenReturn( false );

        DatasetDesignPatterns result = module.processImpl( xmlFile, context );

        assertNull( result );
        verify( context ).get( PipeContext.CPG_DESIGN_PATTERNS_EXISTS, false, Boolean.class );
        verify( context, never() ).get( eq( PipeContext.CPG_DATASET_KEY ), any() );
    }

    @Test
    void testFileIsNull() {
        when( context.get( PipeContext.CPG_DESIGN_PATTERNS_EXISTS, false, Boolean.class ) ).thenReturn( true );

        DatasetDesignPatterns result = module.processImpl( null, context );

        assertNull( result );
        verify( context ).get( PipeContext.CPG_DESIGN_PATTERNS_EXISTS, false, Boolean.class );
        verify( context, never() ).get( eq( PipeContext.CPG_DATASET_KEY ), any() );
    }

    @Test
    void testDatasetNotInContextException() {
        when( context.get( PipeContext.CPG_DESIGN_PATTERNS_EXISTS, false, Boolean.class ) ).thenReturn( true );
        when( context.get( PipeContext.CPG_DATASET_KEY, Dataset.class ) ).thenReturn( Optional.empty() );

        assertThrows( IllegalStateException.class, () -> module.processImpl( xmlFile, context ) );

        verify( context ).get( PipeContext.CPG_DESIGN_PATTERNS_EXISTS, false, Boolean.class );
        verify( context ).get( PipeContext.CPG_DATASET_KEY, Dataset.class );
    }

    @Test
    void testCorrectReaderInstanceForXmlFile() {
        PatternReader reader = ReadPatternsModule.getReader( xmlFile );
        assertTrue( reader instanceof PatternReaderXml );
    }

    @Test
    void testCorrectReaderInstanceForCsvFile() {
        PatternReader reader = ReadPatternsModule.getReader( csvFile );
        assertTrue( reader instanceof PatternReaderCsv );
    }

    @Test
    void testUnsupportedExtension() {
        Exception exception = assertThrows(
                IllegalArgumentException.class, () -> ReadPatternsModule.getReader( unknownFile ) );

        assertTrue( exception.getMessage().contains( "extension not supported" ) );
    }

    @Test
    void testValidFileExtension() {
        assertEquals( "xml", ReadPatternsModule.getFileExtension( xmlFile ) );
        assertEquals( "csv", ReadPatternsModule.getFileExtension( csvFile ) );
        assertEquals( "txt", ReadPatternsModule.getFileExtension( unknownFile ) );
    }

    @Test
    void testExtensionOfNullFile() {
        assertThrows( IllegalArgumentException.class, () -> ReadPatternsModule.getFileExtension( null ) );
    }

    @Test
    void testFileWithNoExtension() {
        String result = ReadPatternsModule.getFileExtension( fileWithoutExtension );

        assertEquals( "", result );
    }
}
