package de.haw.dataset.module;

import de.haw.dataset.DesignPatternLoader;
import de.haw.dataset.model.Dataset;
import de.haw.dataset.model.DatasetType;
import de.haw.misc.pipe.PipeContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;


public class LoadPatternFileModuleTest {

    private LoadPatternFileModule<?> module;

    @Mock
    private Dataset dataset;

    @Mock
    private PipeContext context;

    @Mock
    private File mockFile;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks( this );
        module = LoadPatternFileModule.instance();
    }

    @Test
    public void testProcessImplWhenLoadSucceedsShouldReturnFileAndSetExistsTrue() {
        // Arrange
        DatasetType type = DatasetType.OWN;
        when( dataset.getType() ).thenReturn( type );

        try ( MockedStatic<DesignPatternLoader> mockedLoader = mockStatic( DesignPatternLoader.class ) ) {
            mockedLoader.when( () -> DesignPatternLoader.load( type ) ).thenReturn( mockFile );

            // Act
            File result = module.processImpl( dataset, context );

            // Assert
            verify( context ).set( PipeContext.CPG_DESIGN_PATTERNS_EXISTS, true );
            assertEquals( mockFile, result );
        }
    }

    @Test
    public void testProcessImplWhenLoadFailsShouldReturnNullAndSetExistsFalse() {
        // Arrange
        DatasetType type = DatasetType.OWN;
        when( dataset.getType() ).thenReturn( type );

        try ( MockedStatic<DesignPatternLoader> mockedLoader = mockStatic( DesignPatternLoader.class ) ) {
            mockedLoader.when( () -> DesignPatternLoader.load( type ) )
                    .thenThrow( new IllegalArgumentException( "Pattern not found" ) );

            // Act
            File result = module.processImpl( dataset, context );

            // Assert
            verify( context ).set( PipeContext.CPG_DESIGN_PATTERNS_EXISTS, false );
            assertNull( result );
        }
    }
}