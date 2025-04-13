package de.haw.processing.module;

import de.haw.misc.pipe.PipeContext;
import de.haw.processing.GraphService;
import de.haw.testcase.GraphTestGenerator;
import org.graphstream.graph.Graph;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class RemoveBlacklistElementsModuleTest {

    @Test
    public void testBlacklistedElements() {

        final GraphService graphService = GraphService.instance();
        Graph graph = GraphTestGenerator.getSimpleGraph();
        graphService.addLabel( graph.getNode( "4" ), RemoveBlacklistElementsModule.NODE_LABEL_BLACKLIST.get( 0 ) );
        graphService.setType( graph.getEdge( "12" ), RemoveBlacklistElementsModule.EDGE_TYPE_BLACKLIST.get( 0 ) );

        RemoveBlacklistElementsModule<Graph> module = RemoveBlacklistElementsModule.instance();
        graph = module.process( graph, PipeContext.empty() );

        assertNull( graph.getNode( "4" ) );
        assertNotNull( graph.getNode( "1" ) );
        assertNotNull( graph.getNode( "2" ) );
        assertNotNull( graph.getNode( "3" ) );

        assertNull( graph.getEdge( "12" ) );
        assertNotNull( graph.getEdge( "23" ) );
    }
}
