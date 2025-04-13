package de.haw.translation.module;

import de.fraunhofer.aisec.cpg.TranslationConfiguration;
import de.fraunhofer.aisec.cpg.TranslationManager;
import de.fraunhofer.aisec.cpg.TranslationResult;
import de.haw.dataset.model.Dataset;
import de.haw.dataset.model.DatasetLanguage;
import de.haw.misc.pipe.PipeContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith( MockitoExtension.class )
public class GenerateCpgModuleTest {

    @Test
    public void testProcessImpl_success() throws Exception {
        // Arrange
        PipeContext ctx = mock( PipeContext.class );
        Dataset dataset = mock( Dataset.class );
        when( ctx.get( PipeContext.CPG_DATASET_KEY, Dataset.class ) ).thenReturn( Optional.of( dataset ) );
        when( dataset.getLanguage() ).thenReturn( DatasetLanguage.JAVA );
        File sourceFile = new File( "dummySource.java" );
        TranslationResult expectedResult = mock( TranslationResult.class );

        // Prepare
        try ( MockedStatic<TranslationManager> mockedStatic = mockStatic( TranslationManager.class ) ) {

            TranslationManager.Builder builderMock = mock( TranslationManager.Builder.class );
            TranslationManager translationManagerMock = mock( TranslationManager.class );
            mockedStatic.when( TranslationManager::builder ).thenReturn( builderMock );
            when( builderMock.config( any( TranslationConfiguration.class ) ) ).thenReturn( builderMock );
            when( builderMock.build() ).thenReturn( translationManagerMock );
            when( translationManagerMock.analyze() ).thenReturn( CompletableFuture.completedFuture( expectedResult ) );

            // Act
            GenerateCpgModule<Object> module = GenerateCpgModule.instance();
            TranslationResult actualResult = module.processImpl( sourceFile, ctx );

            // Assert
            assertSame( expectedResult, actualResult );

            // Verify
            verify( builderMock ).config( any( TranslationConfiguration.class ) );
            verify( builderMock ).build();
            verify( translationManagerMock ).analyze();
        }
    }

    @Test
    public void testProcessImpl_configurationException() throws Exception {
        // Arrange
        PipeContext ctx = mock( PipeContext.class );
        Dataset dataset = mock( Dataset.class );
        when( ctx.get( PipeContext.CPG_DATASET_KEY, Dataset.class ) ).thenReturn( Optional.of( dataset ) );
        when( dataset.getLanguage() ).thenReturn( DatasetLanguage.JAVA );

        File sourceFile = new File( "dummySource.java" );
        try ( MockedStatic<TranslationManager> mockedStatic = mockStatic( TranslationManager.class ) ) {
            TranslationManager.Builder builderMock = mock( TranslationManager.Builder.class );
            mockedStatic.when( TranslationManager::builder ).thenReturn( builderMock );
            when( builderMock.config( any( TranslationConfiguration.class ) ) ).thenThrow(
                    new RuntimeException( "Configuration failed" ) );

            GenerateCpgModule<Object> module = GenerateCpgModule.instance();

            // Act
            TranslationResult actualResult = module.processImpl( sourceFile, ctx );

            // Assert
            assertNull( actualResult );
        }
    }
}
