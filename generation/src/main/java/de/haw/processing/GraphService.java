package de.haw.processing;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Element;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;

import java.util.*;

@Slf4j
@NoArgsConstructor( staticName = "instance" )
public class GraphService {

    private final static String GRAPH_ID_PREFIX = "graph";

    public Graph getEmptyGraph() {
        return this.getEmptyGraph( false, false );
    }

    public Graph getEmptyGraph( final boolean strict, final boolean autoCreate ) {
        return new MultiGraph( genId( GRAPH_ID_PREFIX ), strict, autoCreate );
    }

    public Edge copyEdgeToGraph( final Graph targetGraph, final Edge sourceEdge ) {

        final Node sourceNode = this.copyNodeToGraph( targetGraph, sourceEdge.getSourceNode() );
        final Node targetNode = this.copyNodeToGraph( targetGraph, sourceEdge.getTargetNode() );

        Edge edge = targetGraph.getEdge( sourceEdge.getId() );
        if ( edge == null ) {
            edge = targetGraph.addEdge( sourceEdge.getId(), sourceNode, targetNode, true );
        }

        if ( edge == null ) {
            edge = targetGraph.getEdge( sourceEdge.getId() );
        }
        if ( edge != null ) {
            edge.setAttributes( this.getAttributes( sourceEdge ) );
        }

        return edge;
    }

    public Node copyNodeToGraph( final Graph targetGraph, final Node sourceNode ) {

        Node node = targetGraph.getNode( sourceNode.getId() );
        if ( node == null ) {
            node = targetGraph.addNode( sourceNode.getId() );
        }

        if ( node == null ) {
            node = targetGraph.getNode( sourceNode.getId() );
        }
        if ( node != null ) {
            node.setAttributes( this.getAttributes( sourceNode ) );
        }
        return node;
    }

    public Map<String, Object> getAttributes( final Element element ) {
        final Map<String, Object> attr = new HashMap<>();
        if ( element == null ) {
            return attr;
        }
        final List<String> attrKeys = element.attributeKeys().toList();
        for ( final String key : attrKeys ) {
            attr.put( key, element.getAttribute( key ) );
        }
        return attr;
    }

    public <T> Optional<T> getAttr( final Element element, final String key, final Class<T> clazz ) {
        if ( !element.hasAttribute( key, clazz ) ) {
            return Optional.empty();
        }
        return Optional.of( element.getAttribute( key, clazz ) );
    }

    @SuppressWarnings( "unchecked" )
    public void addLabel( final Node node, final String label ) {
        final Set<String> labels = this.getAttr( node, "labels", Set.class ).orElse( Collections.emptySet() );
        labels.add( label );
        node.setAttribute( "labels", labels );
    }

    @SuppressWarnings( "unchecked" )
    public boolean hasLabel( final Node node, final String label ) {
        final Set<String> labels = this.getAttr( node, "labels", Set.class ).orElse( Collections.emptySet() );
        return labels.contains( label );
    }

    private String genId( final String prefix ) {
        return ( prefix == null ? "" : prefix ) + "_" + UUID.randomUUID();
    }

}
