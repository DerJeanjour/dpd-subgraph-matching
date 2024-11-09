package de.haw.repository;

import de.haw.processing.graph.GraphService;
import de.haw.repository.model.Neo4jEdge;
import de.haw.repository.model.Neo4jGraph;
import de.haw.repository.model.Neo4jNode;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;

import java.util.Arrays;
import java.util.stream.Collectors;

public class Neo4jMapper {

    public static Neo4jGraph map( final Graph graph ) {
        final Neo4jGraph neoGraph = new Neo4jGraph();
        neoGraph.setNodes( graph.nodes().map( Neo4jMapper::map ).collect( Collectors.toList() ) );
        neoGraph.setEdges( graph.edges().map( Neo4jMapper::map ).collect( Collectors.toList() ) );
        return neoGraph;
    }

    public static Neo4jNode map( final Node node ) {
        final Neo4jNode neoNode = new Neo4jNode();
        neoNode.setId( node.getId() );
        neoNode.setProperties( GraphService.instance().getAttributes( node ) );
        neoNode.setLabels( Arrays.asList( node.getAttribute( "labels", String[].class ) ) );
        return neoNode;
    }

    public static Neo4jEdge map( final Edge edge ) {
        final Neo4jEdge neoEdge = new Neo4jEdge();
        neoEdge.setId( edge.getId() );
        neoEdge.setProperties( GraphService.instance().getAttributes( edge ) );
        neoEdge.setSource( map( edge.getSourceNode() ) );
        neoEdge.setTarget( map( edge.getTargetNode() ) );
        return neoEdge;
    }

}
