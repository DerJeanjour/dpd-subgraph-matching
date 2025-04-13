package de.haw.processing.traversal;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Node;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class GraphProcessTraverserTest {

    // A concrete subclass for testing
    private static class TestGraphProcessTraverser extends GraphProcessTraverser<String> {
        private final Map<Node, List<Edge>> neighbors;
        // Record visited node ids for verification.
        public final List<String> visitedNodeIds = new ArrayList<>();
        // Nodes at which traversal should stop proceeding.
        private final Set<String> stopAtNodeIds;

        public TestGraphProcessTraverser( Map<Node, List<Edge>> neighbors ) {
            this( neighbors, Collections.emptySet() );
        }

        public TestGraphProcessTraverser( Map<Node, List<Edge>> neighbors, Set<String> stopAtNodeIds ) {
            this.neighbors = neighbors;
            this.stopAtNodeIds = stopAtNodeIds;
        }

        @Override
        protected OutputData<String> process( Node node, String message, TraversalContext ctx ) {
            visitedNodeIds.add( node.getId() );
            // Stop traversal from this node if its id is in stopAtNodeIds.
            boolean proceed = !stopAtNodeIds.contains( node.getId() );
            return OutputData.of( "Processed:" + node.getId(), proceed );
        }

        @Override
        protected List<Edge> next( Node node ) {
            return neighbors.getOrDefault( node, Collections.emptyList() );
        }
    }

    @Test
    public void testTraverseFullGraph() {
        // Create mock nodes for a simple graph: A -> B -> C.
        Node nodeA = mock( Node.class );
        Node nodeB = mock( Node.class );
        Node nodeC = mock( Node.class );
        when( nodeA.getId() ).thenReturn( "A" );
        when( nodeB.getId() ).thenReturn( "B" );
        when( nodeC.getId() ).thenReturn( "C" );

        // Create mock edges connecting the nodes.
        Edge edgeAB = mock( Edge.class );
        Edge edgeBC = mock( Edge.class );
        when( edgeAB.getId() ).thenReturn( "AB" );
        when( edgeBC.getId() ).thenReturn( "BC" );

        // Define getOpposite behavior for the edges.
        when( edgeAB.getOpposite( nodeA ) ).thenReturn( nodeB );
        when( edgeBC.getOpposite( nodeB ) ).thenReturn( nodeC );

        // Build neighbor relationships.
        Map<Node, List<Edge>> neighbors = new HashMap<>();
        neighbors.put( nodeA, Arrays.asList( edgeAB ) );
        neighbors.put( nodeB, Arrays.asList( edgeBC ) );
        neighbors.put( nodeC, Collections.emptyList() );

        TestGraphProcessTraverser traverser = new TestGraphProcessTraverser( neighbors );
        traverser.traverse( nodeA );

        // Verify that all nodes were visited.
        List<String> visited = traverser.visitedNodeIds;
        assertTrue( visited.contains( "A" ) );
        assertTrue( visited.contains( "B" ) );
        assertTrue( visited.contains( "C" ) );
        assertEquals( 3, visited.size() );
    }

    @Test
    public void testTraverseStopAtCondition() {
        // Test that traversal stops when process returns proceed = false.
        Node nodeA = mock( Node.class );
        Node nodeB = mock( Node.class );
        when( nodeA.getId() ).thenReturn( "A" );
        when( nodeB.getId() ).thenReturn( "B" );

        Edge edgeAB = mock( Edge.class );
        when( edgeAB.getId() ).thenReturn( "AB" );
        when( edgeAB.getOpposite( nodeA ) ).thenReturn( nodeB );

        Map<Node, List<Edge>> neighbors = new HashMap<>();
        neighbors.put( nodeA, Arrays.asList( edgeAB ) );
        neighbors.put( nodeB, Collections.emptyList() );

        // Specify that traversal should not proceed from node A.
        Set<String> stopAt = new HashSet<>();
        stopAt.add( "A" );

        TestGraphProcessTraverser traverser = new TestGraphProcessTraverser( neighbors, stopAt );
        traverser.traverse( nodeA );

        // Verify that only node A was processed.
        List<String> visited = traverser.visitedNodeIds;
        assertEquals( 1, visited.size() );
        assertEquals( "A", visited.get( 0 ) );
    }

    @Test
    public void testMaxDepthLimiting() {
        // Test that traversal stops once maxDepth is reached.
        Node nodeA = mock( Node.class );
        Node nodeB = mock( Node.class );
        Node nodeC = mock( Node.class );
        when( nodeA.getId() ).thenReturn( "A" );
        when( nodeB.getId() ).thenReturn( "B" );
        when( nodeC.getId() ).thenReturn( "C" );

        Edge edgeAB = mock( Edge.class );
        Edge edgeBC = mock( Edge.class );
        when( edgeAB.getId() ).thenReturn( "AB" );
        when( edgeBC.getId() ).thenReturn( "BC" );

        when( edgeAB.getOpposite( nodeA ) ).thenReturn( nodeB );
        when( edgeBC.getOpposite( nodeB ) ).thenReturn( nodeC );

        Map<Node, List<Edge>> neighbors = new HashMap<>();
        neighbors.put( nodeA, Arrays.asList( edgeAB ) );
        neighbors.put( nodeB, Arrays.asList( edgeBC ) );
        neighbors.put( nodeC, Collections.emptyList() );

        TestGraphProcessTraverser traverser = new TestGraphProcessTraverser( neighbors );
        traverser.traverse( nodeA, 2 );

        List<String> visited = traverser.visitedNodeIds;
        assertTrue( visited.contains( "A" ) );
        assertTrue( visited.contains( "B" ) );
        assertFalse( visited.contains( "C" ) );
        assertEquals( 2, visited.size() );
    }
}