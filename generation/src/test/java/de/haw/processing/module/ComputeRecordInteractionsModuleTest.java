package de.haw.processing.module;

import de.haw.misc.pipe.PipeContext;
import de.haw.processing.GraphService;
import de.haw.processing.model.CpgNodePaths;
import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.SingleGraph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ComputeRecordInteractionsModuleTest {

    private GraphService graphService;
    private ComputeRecordInteractionsModule<?> module;
    private PipeContext context;
    private Graph graph;

    @BeforeEach
    public void setUp() {
        this.graphService = GraphService.instance();
        module = ComputeRecordInteractionsModule.instance();
        context = PipeContext.empty();
        graph = new SingleGraph( "testGraph" );
    }

    @Test
    public void testProcessImplEmptyRecordPaths() {
        // No record paths in context
        Graph result = module.processImpl( graph, context );

        // Should return same graph without changes
        assertSame( graph, result );
    }

    @Test
    public void testGetPaths() {
        // Create and add paths to context
        CpgNodePaths recordPaths = new CpgNodePaths();
        context.set( PipeContext.RECORD_PATHS, recordPaths );

        // Get paths from context
        CpgNodePaths result = module.getPaths( context );

        // Verify
        assertSame( recordPaths, result );
    }

    @Test
    public void testGetPathsThrowsException() {
        // Context without paths
        assertThrows( IllegalStateException.class, () -> {
            module.getPaths( context );
        } );
    }
}