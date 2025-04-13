package de.haw.dataset.module;

import de.haw.dataset.model.Dataset;
import de.haw.misc.pipe.PipeContext;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AttachPatternsToContextTest {

    @Test
    public void testProcessThrowsWhenDatasetNotPresent() {
        AttachPatternsToContext<String, Object> attachModule = AttachPatternsToContext.instance();
        PipeContext ctx = mock( PipeContext.class );

        when( ctx.get( PipeContext.CPG_DATASET_KEY, Dataset.class ) ).thenReturn( Optional.empty() );

        assertThrows( IllegalStateException.class, () -> attachModule.processImpl( "pass", ctx ) );
    }
}