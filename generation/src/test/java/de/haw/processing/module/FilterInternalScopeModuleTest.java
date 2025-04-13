package de.haw.processing.module;

import de.haw.misc.pipe.PipeContext;
import de.haw.processing.GraphService;
import de.haw.repository.model.CpgEdgeType;
import de.haw.testcase.GraphTestGenerator;
import org.graphstream.graph.Graph;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class FilterInternalScopeModuleTest {

    @Test
    public void testFilter() {

        final GraphService graphService = GraphService.instance();
        Graph graph = GraphTestGenerator.getSimpleGraph();
        graphService.setType( graph.getEdge( "12" ), CpgEdgeType.TYPE );

        final CpgFilterEdgesModule<Graph> module = CpgFilterEdgesModule.byTypes(
                Collections.singletonList( CpgEdgeType.TYPE ), false );
        graph = module.process( graph, PipeContext.empty() );

        assertNotNull( graph.getNode( "1" ) );
        assertNotNull( graph.getNode( "2" ) );
        assertNull( graph.getNode( "3" ) );
        assertNull( graph.getNode( "4" ) );

        assertNotNull( graph.getEdge( "12" ) );
        assertNull( graph.getEdge( "23" ) );
        assertNull( graph.getEdge( "34" ) );
    }

}
