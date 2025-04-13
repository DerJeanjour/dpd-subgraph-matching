package de.haw.translation.module;

import de.fraunhofer.aisec.cpg_vis_neo4j.JsonEdge;
import de.fraunhofer.aisec.cpg_vis_neo4j.JsonGraph;
import de.fraunhofer.aisec.cpg_vis_neo4j.JsonNode;
import de.haw.dataset.model.Dataset;
import de.haw.misc.pipe.PipeContext;
import de.haw.translation.CpgConst;
import org.apache.commons.lang3.StringUtils;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TranslationToGraphModuleTest {

    @Test
    void testGraphIsEmptyWithNull() {
        TranslationToGraphModule<Object> module = TranslationToGraphModule.instance();
        assertTrue( module.graphIsEmpty( null ) );
    }

    @Test
    void testGraphIsEmptyWithEmptyGraph() {
        TranslationToGraphModule<Object> module = TranslationToGraphModule.instance();
        Graph emptyGraph = new SingleGraph( "empty" );
        assertTrue( module.graphIsEmpty( emptyGraph ) );
    }

    @Test
    void testGraphIsNotEmptyGraph() {
        TranslationToGraphModule<Object> module = TranslationToGraphModule.instance();
        Graph nonEmptyGraph = new SingleGraph( "nonEmpty" );
        nonEmptyGraph.addNode( "1" );
        assertFalse( module.graphIsEmpty( nonEmptyGraph ) );
    }

    static Stream<Arguments> toGraphArguments() {
        // Scenario 1: Empty JsonGraph (no nodes, no edges)
        JsonGraph emptyJsonGraph = mock( JsonGraph.class );
        when( emptyJsonGraph.getNodes() ).thenReturn( Collections.emptyList() );
        when( emptyJsonGraph.getEdges() ).thenReturn( Collections.emptyList() );

        // Scenario 2: JsonGraph with two nodes and two edges (one valid, one self-loop skipped)
        // Create two dummy JsonNode objects.
        JsonNode jsonNode1 = mock( JsonNode.class );
        when( jsonNode1.getId() ).thenReturn( 1L );
        Map<String, Object> node1Props = new HashMap<>();
        node1Props.put( "prop", "node1" );
        when( jsonNode1.getProperties() ).thenReturn( node1Props );
        when( jsonNode1.getLabels() ).thenReturn( new HashSet<>( Arrays.asList( "Label1" ) ) );

        JsonNode jsonNode2 = mock( JsonNode.class );
        when( jsonNode2.getId() ).thenReturn( 2L );
        Map<String, Object> node2Props = new HashMap<>();
        node2Props.put( "prop", "node2" );
        when( jsonNode2.getProperties() ).thenReturn( node2Props );
        when( jsonNode2.getLabels() ).thenReturn( new HashSet<>( Arrays.asList( "Label2" ) ) );

        // Prepare list of nodes.
        java.util.List<JsonNode> nodeList = Arrays.asList( jsonNode1, jsonNode2 );

        // Create two dummy JsonEdge objects.
        // Valid edge from node1 (1L) to node2 (2L)
        JsonEdge validEdge = mock( JsonEdge.class );
        when( validEdge.getId() ).thenReturn( 10L );
        when( validEdge.getStartNode() ).thenReturn( 1L );
        when( validEdge.getEndNode() ).thenReturn( 2L );
        when( validEdge.getType() ).thenReturn( "REL" );
        Map<String, Object> edgeProps = new HashMap<>();
        edgeProps.put( "weight", 5 );
        when( validEdge.getProperties() ).thenReturn( edgeProps );

        // Self-loop edge from node1 to node1; should be skipped.
        JsonEdge selfLoopEdge = mock( JsonEdge.class );
        when( selfLoopEdge.getId() ).thenReturn( 11L );
        when( selfLoopEdge.getStartNode() ).thenReturn( 1L );
        when( selfLoopEdge.getEndNode() ).thenReturn( 1L );
        when( selfLoopEdge.getType() ).thenReturn( "SELF" );
        Map<String, Object> selfLoopProps = new HashMap<>();
        selfLoopProps.put( "weight", 3 );
        when( selfLoopEdge.getProperties() ).thenReturn( selfLoopProps );

        java.util.List<JsonEdge> edgeList = Arrays.asList( validEdge, selfLoopEdge );

        JsonGraph nonEmptyJsonGraph = mock( JsonGraph.class );
        when( nonEmptyJsonGraph.getNodes() ).thenReturn( nodeList );
        when( nonEmptyJsonGraph.getEdges() ).thenReturn( edgeList );

        return Stream.of(
                Arguments.of( emptyJsonGraph, 0, 0 ),
                Arguments.of( nonEmptyJsonGraph, 2, 1 ) );
    }

    @ParameterizedTest
    @MethodSource( "toGraphArguments" )
    void testToGraph( JsonGraph inputJsonGraph, int expectedNodeCount, int expectedEdgeCount ) {
        // Create a dummy PipeContext with a Dataset.
        PipeContext ctx = mock( PipeContext.class );
        Dataset dataset = mock( Dataset.class );
        when( dataset.getName() ).thenReturn( "testDataset" );
        when( ctx.get( PipeContext.CPG_DATASET_KEY, Dataset.class ) ).thenReturn( Optional.of( dataset ) );

        TranslationToGraphModule<Object> module = TranslationToGraphModule.instance();
        Graph graph = module.toGraph( inputJsonGraph, ctx );

        // Check graph dataset attribute
        assertEquals( "testDataset", graph.getAttribute( CpgConst.GRAPH_ATTR_DATASET ) );
        // Check node and edge counts.
        assertEquals( expectedNodeCount, graph.getNodeCount() );
        assertEquals( expectedEdgeCount, graph.getEdgeCount() );

        if ( expectedNodeCount > 0 ) {
            // Verify each node has the correct dataset attribute.
            for ( int id = 1; id <= expectedNodeCount; id++ ) {
                Node node = graph.getNode( String.valueOf( id ) );
                assertNotNull( node );
                assertEquals( "testDataset", node.getAttribute( CpgConst.NODE_ATTR_DATASET ) );
                // Verify labels are set if available.
                Object labels = node.getAttribute( CpgConst.NODE_ATTR_LABELS );
                assertNotNull( labels );
            }
        }

        if ( expectedEdgeCount > 0 ) {
            // The valid edge has id "10" (from validEdge mock).
            Edge edge = graph.getEdge( "10" );
            assertNotNull( edge );
            assertEquals( "REL", edge.getAttribute( CpgConst.EDGE_ATTR_TYPE ) );
            // Only set if the type is not blank.
            if ( StringUtils.isNotBlank( "REL" ) ) {
                assertEquals( "REL", edge.getAttribute( CpgConst.EDGE_ATTR_LABEL ) );
            }
            assertEquals( "testDataset", edge.getAttribute( CpgConst.EDGE_ATTR_DATASET ) );
        }
    }
}
