package de.haw.processing.module;

import de.haw.misc.pipe.PipeContext;
import de.haw.testcase.GraphTestGenerator;
import de.haw.translation.CpgConst;
import org.graphstream.graph.Graph;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class CpgFilterEdgesModuleTest {

    @Test
    public void testFilterEdges() {

        Graph graph = GraphTestGenerator.getSimpleGraph();
        graph.getNode( "1" ).setAttribute( CpgConst.NODE_ATTR_INFERRED, true );
        graph.getNode( "1" ).setAttribute( CpgConst.NODE_ATTR_NAME, "blacklisted" );
        graph.getNode( "2" ).setAttribute( CpgConst.NODE_ATTR_NAME, "blacklisted" );

        FilterInternalScopeModule<Graph> module = FilterInternalScopeModule.instance();
        graph = module.process( graph, PipeContext.empty() );

        assertNull( graph.getNode( "1" ) );
        assertNull( graph.getNode( "2" ) );
        assertNotNull( graph.getNode( "3" ) );
        assertNotNull( graph.getNode( "4" ) );

        assertNull( graph.getEdge( "12" ) );
        assertNull( graph.getEdge( "23" ) );
        assertNotNull( graph.getEdge( "34" ) );
        assertNull( graph.getEdge( "14" ) );
    }

}
