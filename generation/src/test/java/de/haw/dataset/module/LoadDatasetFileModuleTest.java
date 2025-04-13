package de.haw.dataset.module;

import de.haw.dataset.DatasetLoader;
import de.haw.dataset.model.Dataset;
import de.haw.dataset.model.DatasetFactory;
import de.haw.misc.pipe.PipeContext;
import de.haw.misc.pipe.PipeModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

@ExtendWith( MockitoExtension.class )
class LoadDatasetFileModuleTest {

    @Mock
    private File mockFile;

    @Test
    void testDatasetAttachment() {

        final Dataset dataset = DatasetFactory.J_UNIT;
        final PipeContext ctx = PipeContext.empty();
        final PipeModule<Dataset, File, File> module = LoadDatasetFileModule.instance();

        try ( MockedStatic<DatasetLoader> mockedDatasetLoader = mockStatic( DatasetLoader.class ) ) {
            // Arrange
            mockedDatasetLoader.when( () -> DatasetLoader.load( dataset ) ).thenReturn( mockFile );

            // Act
            File result = module.process( dataset, ctx );

            // Assert
            assertTrue( ctx.get( PipeContext.CPG_DATASET_KEY, Dataset.class ).isPresent() );
            assertEquals( mockFile, result );
        }
    }
}