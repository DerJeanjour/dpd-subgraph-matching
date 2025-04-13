package de.haw.misc.utils;

import de.haw.repository.model.CpgEdgeType;
import de.haw.testcase.GraphTestGenerator;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PathUtilsTest {

    @Test
    void testGetTypesWithNonEmptyPath() {
        Path path = getExample();
        List<CpgEdgeType> types = PathUtils.getTypes( path );
        assertNotNull( types );
        assertEquals( 3, types.size() );
        types.forEach( Assertions::assertNotNull );
    }

    @Test
    void testGetTypesWithEmptyPath() {
        Path emptyPath = new Path();
        List<CpgEdgeType> types = PathUtils.getTypes( emptyPath );
        assertNotNull( types );
        assertTrue( types.isEmpty() );
    }

    @Test
    void testGetFirstNodeNonEmpty() {
        Path path = getExample();
        // Expected first node is the source node of the first edge in the path.
        Edge firstEdge = path.getEdgePath().get( 0 );
        Node expectedFirst = firstEdge.getSourceNode();
        assertEquals( expectedFirst, PathUtils.getFirstNode( path ) );
    }

    @Test
    void testGetFirstNodeEmpty() {
        Path emptyPath = new Path();
        assertNull( PathUtils.getFirstNode( emptyPath ) );
    }

    @Test
    void testGetLastNodeNonEmpty() {
        Path path = getExample();
        // Expected last node is the target node of the last edge in the path.
        List<Edge> edges = path.getEdgePath();
        Edge lastEdge = edges.get( edges.size() - 1 );
        Node expectedLast = lastEdge.getTargetNode();
        assertEquals( expectedLast, PathUtils.getLastNode( path ) );
    }

    @Test
    void testGetLastNodeEmpty() {
        Path emptyPath = new Path();
        assertNull( PathUtils.getLastNode( emptyPath ) );
    }

    private Path getExample() {
        final Graph graph = GraphTestGenerator.getSimpleGraph();
        final Path path = new Path();
        path.add( graph.getNode( "1" ), graph.getEdge( "12" ) );
        path.add( graph.getNode( "2" ), graph.getEdge( "23" ) );
        path.add( graph.getNode( "3" ), graph.getEdge( "34" ) );
        return path;
    }

}
