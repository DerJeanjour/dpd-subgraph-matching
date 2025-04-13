package de.haw.processing.traversal;

import de.haw.processing.GraphService;
import de.haw.translation.CpgConst;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;


class RecordNeighbourSubgraphCopyTraverserTest {

    @Test
    void testProcessExceedsMaxDepth() {
        // Arrange
        Graph graph = mock( Graph.class );
        int maxDepth = 2;
        try ( MockedStatic<GraphService> gsStatic = Mockito.mockStatic( GraphService.class ) ) {
            GraphService gsMock = mock( GraphService.class );
            gsStatic.when( GraphService::instance ).thenReturn( gsMock );

            RecordNeighbourSubgraphCopyTraverser traverser = RecordNeighbourSubgraphCopyTraverser.of( graph, maxDepth );
            Node node = mock( Node.class );
            GraphProcessTraverser.TraversalContext ctx = mock( GraphProcessTraverser.TraversalContext.class );
            when( ctx.getDepth() ).thenReturn( 3 ); // depth > maxDepth

            // Act
            GraphProcessTraverser.OutputData<Void> result = traverser.process( node, null, ctx );

            // Assert
            assertFalse( result.isProceed() );
            verifyNoInteractions( gsMock );
        }
    }

    @Test
    void testProcessWithoutParent() {
        // Arrange
        Graph graph = mock( Graph.class );
        int maxDepth = 2;
        try ( MockedStatic<GraphService> gsStatic = Mockito.mockStatic( GraphService.class ) ) {
            GraphService gsMock = mock( GraphService.class );
            gsStatic.when( GraphService::instance ).thenReturn( gsMock );

            RecordNeighbourSubgraphCopyTraverser traverser = RecordNeighbourSubgraphCopyTraverser.of( graph, maxDepth );
            Node node = mock( Node.class );
            GraphProcessTraverser.TraversalContext ctx = mock( GraphProcessTraverser.TraversalContext.class );
            when( ctx.getDepth() ).thenReturn( 1 );
            when( ctx.getParent() ).thenReturn( null );

            // Act
            GraphProcessTraverser.OutputData<Void> result = traverser.process( node, null, ctx );

            // Assert
            assertTrue( result.isProceed() );
            verify( gsMock, times( 1 ) ).copyNodeToGraph( graph, node );
            verify( gsMock, never() ).copyEdgeToGraph( any(), any() );
        }
    }

    @Test
    void testProcessWithEdgeHasLabel() {
        // Arrange
        Graph graph = mock( Graph.class );
        int maxDepth = 3;
        try ( MockedStatic<GraphService> gsStatic = Mockito.mockStatic( GraphService.class ) ) {
            GraphService gsMock = mock( GraphService.class );
            gsStatic.when( GraphService::instance ).thenReturn( gsMock );

            RecordNeighbourSubgraphCopyTraverser traverser = RecordNeighbourSubgraphCopyTraverser.of( graph, maxDepth );
            Node node = mock( Node.class );
            GraphProcessTraverser.TraversalContext ctx = mock( GraphProcessTraverser.TraversalContext.class );
            Edge edge = mock( Edge.class );
            // Simulate having a parent so that node is not copied as a root node.
            when( ctx.getDepth() ).thenReturn( 2 );
            when( ctx.getParent() ).thenReturn( mock( Node.class ) );
            when( ctx.getEdge() ).thenReturn( edge );
            // hasLabel returns true, so should not continue traversal.
            when( gsMock.hasLabel( node, CpgConst.NODE_LABEL_DECLARATION_RECORD ) ).thenReturn( true );

            // Act
            GraphProcessTraverser.OutputData<Void> result = traverser.process( node, null, ctx );

            // Assert
            assertFalse( result.isProceed() );
            verify( gsMock, times( 1 ) ).copyEdgeToGraph( graph, edge );
            verify( gsMock, times( 1 ) ).hasLabel( node, CpgConst.NODE_LABEL_DECLARATION_RECORD );
        }
    }

    @Test
    void testProcessWithEdgeWithoutLabel() {
        // Arrange
        Graph graph = mock( Graph.class );
        int maxDepth = 3;
        try ( MockedStatic<GraphService> gsStatic = Mockito.mockStatic( GraphService.class ) ) {
            GraphService gsMock = mock( GraphService.class );
            gsStatic.when( GraphService::instance ).thenReturn( gsMock );

            RecordNeighbourSubgraphCopyTraverser traverser = RecordNeighbourSubgraphCopyTraverser.of( graph, maxDepth );
            Node node = mock( Node.class );
            GraphProcessTraverser.TraversalContext ctx = mock( GraphProcessTraverser.TraversalContext.class );
            Edge edge = mock( Edge.class );
            // Simulate having a parent so that node is not copied as a root node.
            when( ctx.getDepth() ).thenReturn( 2 );
            when( ctx.getParent() ).thenReturn( mock( Node.class ) );
            when( ctx.getEdge() ).thenReturn( edge );
            // hasLabel returns false, so should continue traversal.
            when( gsMock.hasLabel( node, CpgConst.NODE_LABEL_DECLARATION_RECORD ) ).thenReturn( false );

            // Act
            GraphProcessTraverser.OutputData<Void> result = traverser.process( node, null, ctx );

            // Assert
            assertTrue( result.isProceed() );
            verify( gsMock, times( 1 ) ).copyEdgeToGraph( graph, edge );
            verify( gsMock, times( 1 ) ).hasLabel( node, CpgConst.NODE_LABEL_DECLARATION_RECORD );
        }
    }
}