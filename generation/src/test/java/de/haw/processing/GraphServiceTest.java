package de.haw.processing;

import de.haw.repository.model.CpgEdgeType;
import de.haw.translation.CpgConst;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;


public class GraphServiceTest {

    private GraphService graphService;

    @BeforeEach
    public void setup() {
        graphService = GraphService.instance();
    }

    @Test
    public void testGetEmptyGraphWithDatasetString() {
        String dataset = "testDataset";
        Graph graph = graphService.getEmptyGraph( dataset );
        assertNotNull( graph );
        // verify that the dataset attribute is set
        String actualDataset = graph.getAttribute( CpgConst.GRAPH_ATTR_DATASET, String.class );
        assertTrue( graphService.hasAttr( graph, CpgConst.GRAPH_ATTR_DATASET ) );
        assertEquals( dataset, actualDataset );
    }

    @Test
    public void testGetEmptyGraphFromGraph() {
        // create a source graph and set the required dataset attribute
        Graph source = new MultiGraph( "source" );
        String dataset = "sourceDataset";
        source.setAttribute( CpgConst.GRAPH_ATTR_DATASET, dataset );

        Graph emptyGraph = graphService.getEmptyGraph( source );
        assertNotNull( emptyGraph );
        // ensure dataset attribute is preserved in new graph
        assertTrue( graphService.hasAttr( emptyGraph, CpgConst.GRAPH_ATTR_DATASET ) );
        String actualDataset = emptyGraph.getAttribute( CpgConst.GRAPH_ATTR_DATASET, String.class );
        assertEquals( dataset, actualDataset );
    }

    @Test
    public void testAddNode() {
        Graph target = new MultiGraph( "target" );
        String nodeId = "node1";
        Node node = graphService.addNode( target, nodeId );
        assertNotNull( node );
        assertEquals( nodeId, node.getId() );
        // Calling addNode again returns the same node
        Node node2 = graphService.addNode( target, nodeId );
        assertSame( node, node2 );
    }

    @Test
    public void testAddEdge() {
        Graph target = new MultiGraph( "target" );
        Node source = target.addNode( "A" );
        Node targetNode = target.addNode( "B" );
        String edgeId = "edge1";

        Edge edge = graphService.addEdge( target, edgeId, source, targetNode );
        assertNotNull( edge );
        assertEquals( edgeId, edge.getId() );
        // Calling addEdge again return the same edge instance
        Edge edge2 = graphService.addEdge( target, edgeId, source, targetNode );
        assertSame( edge, edge2 );
    }

    @Test
    public void testCopyNodeToGraph() {
        Graph target = new MultiGraph( "target" );
        // Create a dummy source node using target graph for simplicity
        String nodeId = "nodeCopy";
        Node sourceNode = target.addNode( nodeId );
        // Set an attribute on source node
        sourceNode.setAttribute( "dummy", "value" );

        // Use copyNodeToGraph to copy attributes to a new node in a new graph
        Graph newGraph = new MultiGraph( "newGraph" );
        Node copiedNode = graphService.copyNodeToGraph( newGraph, sourceNode );
        assertNotNull( copiedNode );
        assertEquals( nodeId, copiedNode.getId() );
        assertEquals( "value", copiedNode.getAttribute( "dummy" ) );
    }

    @Test
    public void testCopyEdgeToGraph() {
        // Create source graph with two nodes and an edge
        Graph sourceGraph = new MultiGraph( "sourceGraph" );
        Node sourceNode = sourceGraph.addNode( "A" );
        Node targetNode = sourceGraph.addNode( "B" );
        Edge sourceEdge = sourceGraph.addEdge( "AB", sourceNode.getId(), targetNode.getId(), true );
        // Set an attribute on the edge
        sourceEdge.setAttribute( "weight", 10 );

        // Create target graph and copy the edge
        Graph targetGraph = new MultiGraph( "targetGraph" );
        Edge copiedEdge = graphService.copyEdgeToGraph( targetGraph, sourceEdge );
        assertNotNull( copiedEdge );
        // verify that source and target nodes are copied
        Node copiedSource = copiedEdge.getSourceNode();
        Node copiedTarget = copiedEdge.getTargetNode();
        assertNotNull( copiedSource );
        assertNotNull( copiedTarget );
        // verify attributes are copied
        assertEquals( 10, copiedEdge.getAttribute( "weight" ) );
    }

    @Test
    public void testCopyGraph() {
        // Create a source graph with dataset attribute, two nodes and an edge
        Graph sourceGraph = new MultiGraph( "srcGraph" );
        String dataset = "graphDataset";
        sourceGraph.setAttribute( CpgConst.GRAPH_ATTR_DATASET, dataset );
        Node node1 = sourceGraph.addNode( "n1" );
        node1.setAttribute( "prop", "value1" );
        Node node2 = sourceGraph.addNode( "n2" );
        node2.setAttribute( "prop", "value2" );
        Edge edge = sourceGraph.addEdge( "e1", "n1", "n2", true );
        edge.setAttribute( "label", "connects" );

        // Copy graph and verify all nodes and edges exist with attributes
        Graph copiedGraph = graphService.copyGraph( sourceGraph );
        assertNotNull( copiedGraph );
        assertEquals( dataset, copiedGraph.getAttribute( CpgConst.GRAPH_ATTR_DATASET ) );

        Node copyNode1 = copiedGraph.getNode( "n1" );
        Node copyNode2 = copiedGraph.getNode( "n2" );
        assertNotNull( copyNode1 );
        assertNotNull( copyNode2 );
        assertEquals( "value1", copyNode1.getAttribute( "prop" ) );
        assertEquals( "value2", copyNode2.getAttribute( "prop" ) );

        Edge copyEdge = copiedGraph.getEdge( "e1" );
        assertNotNull( copyEdge );
        assertEquals( "connects", copyEdge.getAttribute( "label" ) );
    }

    @Test
    public void testLabelOperations() {
        Graph graph = new MultiGraph( "labelGraph" );
        Node node = graph.addNode( "nodeLabel" );
        // Initially, label list is empty
        Set<String> labels = graphService.getLabels( node );
        assertTrue( labels.isEmpty() );

        // Add a label and check existence
        graphService.addLabel( node, "TestLabel" );
        assertTrue( graphService.hasLabel( node, "TestLabel" ) );

        // Test hasAnyLabel with a list
        assertTrue( graphService.hasAnyLabel( node, Collections.singletonList( "TestLabel" ) ) );
    }

    @Test
    public void testEdgeTypeOperations() {
        Graph graph = new MultiGraph( "typeGraph" );
        // Setup nodes and edge
        Node node1 = graph.addNode( "n1" );
        Node node2 = graph.addNode( "n2" );
        Edge edge = graph.addEdge( "edgeType", node1.getId(), node2.getId(), true );

        // Assume that CpgEdgeType has a value called TEST. This value should be defined in the enum.
        CpgEdgeType testType = CpgEdgeType.TYPE;
        graphService.setType( edge, testType );

        // getType should return the same type
        CpgEdgeType returnedType = graphService.getType( edge );
        assertEquals( testType, returnedType );

        // isType should return true for TEST
        assertTrue( graphService.isType( edge, testType ) );

        // isAnyType should check a list containing TEST returns true
        assertTrue( graphService.isAnyType( edge, Collections.singletonList( testType ) ) );
    }
}