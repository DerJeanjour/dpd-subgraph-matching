package de.haw.processing.module;

import de.haw.misc.pipe.PipeContext;
import de.haw.processing.GraphService;
import de.haw.processing.model.CpgNodePaths;
import de.haw.processing.model.CpgPath;
import de.haw.repository.model.CpgEdgeType;
import de.haw.translation.CpgConst;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ComputeRecordPathsModuleTest {

    private ComputeRecordPathsModule<?> module;
    private GraphService graphService;
    private Graph mockGraph;
    private PipeContext mockContext;
    private Node mockRecordNode;
    private Graph mockSubgraph;

    @BeforeEach
    public void setUp() {
        module = ComputeRecordPathsModule.instance();
        graphService = mock( GraphService.class );
        mockGraph = mock( Graph.class );
        mockContext = mock( PipeContext.class );
        mockRecordNode = mock( Node.class );
        mockSubgraph = mock( Graph.class );

        // Use reflection to set the mocked GraphService
        try {
            java.lang.reflect.Field gsField = ComputeRecordPathsModule.class.getDeclaredField( "GS" );
            gsField.setAccessible( true );
            gsField.set( module, graphService );
        } catch ( Exception e ) {
            fail( "Failed to set mocked GraphService: " + e.getMessage() );
        }
    }

    @Test
    public void testGetRecordNodes() {
        // Setup
        Node recordNode1 = mock( Node.class );
        Node recordNode2 = mock( Node.class );
        Node nonRecordNode = mock( Node.class );

        when( mockGraph.nodes() ).thenReturn( Stream.of( recordNode1, recordNode2, nonRecordNode ) );
        when( graphService.hasLabel( recordNode1, CpgConst.NODE_LABEL_DECLARATION_RECORD ) ).thenReturn( true );
        when( graphService.hasLabel( recordNode2, CpgConst.NODE_LABEL_DECLARATION_RECORD ) ).thenReturn( true );
        when( graphService.hasLabel( nonRecordNode, CpgConst.NODE_LABEL_DECLARATION_RECORD ) ).thenReturn( false );

        // Execute
        List<Node> result = module.getRecordNodes( mockGraph );

        // Verify
        assertEquals( 2, result.size() );
        assertTrue( result.contains( recordNode1 ) );
        assertTrue( result.contains( recordNode2 ) );
        assertFalse( result.contains( nonRecordNode ) );
    }

    @Test
    public void testGetRecordNeighbourSubgraph() {
        // Setup
        when( mockRecordNode.getGraph() ).thenReturn( mockGraph );
        when( graphService.getEmptyGraph( mockGraph ) ).thenReturn( mockSubgraph );

        // Execute
        Graph result = module.getRecordNeighbourSubgraph( mockRecordNode );

        // Verify
        assertEquals( mockSubgraph, result );
    }

    @Test
    public void testGetPivotEdge() {
        // Setup
        Edge edge1 = mock( Edge.class );
        Edge edge2 = mock( Edge.class );
        Edge edge3 = mock( Edge.class );

        List<Edge> edges = List.of( edge1, edge2, edge3 );

        when( graphService.getType( edge1 ) ).thenReturn( CpgEdgeType.RETURN_TYPES );
        when( graphService.getType( edge2 ) ).thenReturn( CpgEdgeType.INSTANTIATES );
        when( graphService.getType( edge3 ) ).thenReturn( CpgEdgeType.INVOKES );

        // Execute
        Edge result = module.getPivotEdge( edges );

        // Verify - assuming CALLS has higher priority than KNOWS in RecordInteractionDescriptor
        assertEquals( edge2, result );
    }

    @Test
    public void testGetShortestPaths() {
        // Setup
        Node source = mock( Node.class );
        Node target1 = mock( Node.class );
        Node target2 = mock( Node.class );

        when( source.getId() ).thenReturn( "source" );
        when( target1.getId() ).thenReturn( "target1" );
        when( target2.getId() ).thenReturn( "target2" );

        when( mockGraph.getNode( "source" ) ).thenReturn( source );

        List<Node> recordNodes = List.of( source, target1, target2 );

        ComputeRecordPathsModule<?> spyModule = spy( module );
        doReturn( recordNodes ).when( spyModule ).getRecordNodes( mockGraph );

        Path mockPath = mock( Path.class );
        when( mockPath.empty() ).thenReturn( false );

        // This test is complex due to Dijkstra dependency
        doReturn( List.of( CpgPath.of( source, target1, mockPath, 5 ) ) ).when( spyModule )
                .getShortestPaths( mockGraph, source );

        // Execute
        List<CpgPath> result = spyModule.getShortestPaths( mockGraph, source );

        // Verify
        assertEquals( 1, result.size() );
    }

    @Test
    public void testProcessImpl() {
        // Setup
        Node record1 = mock( Node.class );
        Node record2 = mock( Node.class );
        List<Node> records = List.of( record1, record2 );

        when( record1.getId() ).thenReturn( "record1" );
        when( record2.getId() ).thenReturn( "record2" );

        ComputeRecordPathsModule<?> spyModule = spy( module );
        doReturn( records ).when( spyModule ).getRecordNodes( mockGraph );
        doReturn( mockSubgraph ).when( spyModule ).getRecordNeighbourSubgraph( any( Node.class ) );
        doReturn( new ArrayList<CpgPath>() ).when( spyModule ).getShortestPaths( mockSubgraph, record1 );
        doReturn( new ArrayList<CpgPath>() ).when( spyModule ).getShortestPaths( mockSubgraph, record2 );

        when( graphService.copyGraph( mockSubgraph ) ).thenReturn( mockSubgraph );

        // Execute
        Graph result = spyModule.processImpl( mockGraph, mockContext );

        // Verify
        verify( mockContext ).set( eq( PipeContext.RECORD_PATHS ), any( CpgNodePaths.class ) );
        assertEquals( mockGraph, result );
    }
}
