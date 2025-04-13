package de.haw.repository;

import de.haw.repository.model.CpgEdge;
import de.haw.repository.model.CpgEdgeType;
import de.haw.repository.model.CpgNode;
import de.haw.testcase.GraphTestGenerator;
import de.haw.translation.CpgConst;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class GraphMapperTest {

    @Test
    void testMapWithEmptyGraph() {
        Graph emptyGraph = GraphTestGenerator.getEmpty();
        List<CpgEdge<CpgNode>> edges = GraphMapper.map( emptyGraph );
        assertNotNull( edges );
        assertTrue( edges.isEmpty() );
    }

    @Test
    void testMapWithSimpleGraph() {
        Graph simpleGraph = GraphTestGenerator.getSimpleGraph();
        List<CpgEdge<CpgNode>> edges = GraphMapper.map( simpleGraph );
        // GraphTestGenerator.getSimpleGraph adds 4 edges.
        assertNotNull( edges );
        assertEquals( 4, edges.size() );
        // Check that for every mapped edge, source and target nodes are not null and have proper attributes.
        for ( CpgEdge<CpgNode> edge : edges ) {
            CpgNode src = edge.getSource();
            CpgNode tgt = edge.getTarget();
            assertNotNull( src );
            assertNotNull( tgt );
            // The mapper resets the "id" of each node, but "internalId" should be set.
            assertNull( src.getId() );
            assertNull( tgt.getId() );
            // The labels should have been set.
            assertNotNull( src.getLabels() );
            assertNotNull( tgt.getLabels() );
            // Also, check that the edge type is one of the expected ones.
            Map<String, String> edgeProps = edge.getProperties();
            String typeStr = edgeProps.get( CpgConst.EDGE_ATTR_TYPE );
            assertNotNull( typeStr );
            // Must match one of the CpgEdgeType names.
            Optional<CpgEdgeType> maybeType = Stream.of( CpgEdgeType.values() )
                    .filter( t -> t.name().equals( typeStr ) )
                    .findFirst();
            assertTrue( maybeType.isPresent() );
        }
    }

    @Test
    void testMapSkipsEdgeWithoutType() {
        // Create a simple graph and then remove the edge type attribute from one edge.
        Graph simpleGraph = GraphTestGenerator.getSimpleGraph();
        // Assume the edge with id "ab" exists.
        Edge edgeAb = simpleGraph.getEdge( "12" );
        assertNotNull( edgeAb );
        // Remove the type attribute so that mapEdgeType returns empty and the edge is skipped.
        edgeAb.removeAttribute( CpgConst.EDGE_ATTR_TYPE );

        List<CpgEdge<CpgNode>> edges = GraphMapper.map( simpleGraph );
        // Originally there were 4 edges; now only 3 should be mapped.
        assertEquals( 3, edges.size() );
        // Verify that no mapped edge has a blank type.
        edges.forEach( edge -> {
            String type = edge.getProperties().get( CpgConst.EDGE_ATTR_TYPE );
            assertNotNull( type );
            assertFalse( type.trim().isEmpty() );
        } );
    }

    static Stream<Arguments> mapAttrArguments() {
        // Case 1: empty map should yield empty result.
        Map<String, Object> input1 = new HashMap<>();
        Map<String, String> expected1 = new HashMap<>();

        // Case 2: a simple key-value mapping.
        Map<String, Object> input2 = new HashMap<>();
        input2.put( "key1", "value1" );
        Map<String, String> expected2 = new HashMap<>();
        expected2.put( "key1", "value1" );

        // Case 3: a key mapped to a blank string should be omitted.
        Map<String, Object> input3 = new HashMap<>();
        input3.put( "key1", "   " );
        Map<String, String> expected3 = new HashMap<>();

        // Case 4: a key mapped to null should be omitted.
        Map<String, Object> input4 = new HashMap<>();
        input4.put( "key1", null );
        Map<String, String> expected4 = new HashMap<>();

        // Case 5: a non-string value is converted to its string representation.
        Map<String, Object> input5 = new HashMap<>();
        input5.put( "key1", 123 );
        Map<String, String> expected5 = new HashMap<>();
        expected5.put( "key1", "123" );

        // Case 6: mix of valid and invalid entries.
        Map<String, Object> input6 = new HashMap<>();
        input6.put( "key1", "value1" );
        input6.put( "key2", "" );
        input6.put( "key3", "value3" );
        Map<String, String> expected6 = new HashMap<>();
        expected6.put( "key1", "value1" );
        expected6.put( "key3", "value3" );

        return Stream.of( Arguments.of( input1, expected1 ), Arguments.of( input2, expected2 ),
                Arguments.of( input3, expected3 ), Arguments.of( input4, expected4 ), Arguments.of( input5, expected5 ),
                Arguments.of( input6, expected6 ) );
    }

    @ParameterizedTest
    @MethodSource( "mapAttrArguments" )
    void testMapAttr( Map<String, Object> input, Map<String, String> expected ) {
        Map<String, String> result = GraphMapper.mapAttr( input );
        assertEquals( expected, result );
    }
}
