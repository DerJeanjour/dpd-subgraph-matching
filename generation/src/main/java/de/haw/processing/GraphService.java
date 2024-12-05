package de.haw.processing;

import de.haw.repository.model.CpgEdgeType;
import de.haw.translation.CpgConst;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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

        final Edge edge = this.addEdge( targetGraph, sourceEdge.getId(), sourceNode, targetNode );
        edge.setAttributes( this.getAttributes( sourceEdge ) );

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

    public Edge addEdge( final Graph targetGraph, String edgeId, final Node sourceNode, final Node targetNode ) {

        Edge edge = targetGraph.getEdge( edgeId );
        if ( edge == null ) {
            edge = targetGraph.addEdge( edgeId, sourceNode, targetNode, true );
        }
        if ( edge == null ) {
            edge = targetGraph.getEdge( edgeId );
        }

        return edge;

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

    public String getAttr( final Element element, final String key ) {
        return this.getAttr( element, key, String.class ).orElse( null );
    }

    public <T> Optional<T> getAttr( final Element element, final String key, final Class<T> clazz ) {
        if ( element == null || !element.hasAttribute( key, clazz ) ) {
            return Optional.empty();
        }
        return Optional.of( element.getAttribute( key, clazz ) );
    }

    @SuppressWarnings( "unchecked" )
    public void addLabel( final Node node, final String label ) {
        final Set<String> labels = this.getAttr( node, CpgConst.NODE_ATTR_LABELS, Set.class )
                .orElse( Collections.emptySet() );
        labels.add( label );
        node.setAttribute( CpgConst.NODE_ATTR_LABELS, labels );
    }

    public boolean hasLabel( final Node node, final String label ) {
        return this.getLabels( node ).contains( label );
    }

    public boolean hasAnyLabel( final Node node, final List<String> searchLabels ) {
        return this.getLabels( node ).stream().anyMatch( searchLabels::contains );
    }

    public void setType( final Edge edge, CpgEdgeType edgeType ) {
        edge.setAttribute( CpgConst.EDGE_ATTR_TYPE, edgeType.name() );
    }

    public CpgEdgeType getType( final Edge edge ) {
        final Optional<String> edgeType = this.getAttr( edge, CpgConst.EDGE_ATTR_TYPE, String.class );
        return edgeType.map( CpgEdgeType::valueOf ).orElse( null );
    }

    public boolean isType( final Edge edge, final CpgEdgeType type ) {
        final Optional<String> edgeType = this.getAttr( edge, CpgConst.EDGE_ATTR_TYPE, String.class );
        return edgeType.orElse( "" ).equals( type.name() );
    }

    public boolean isAnyType( final Edge edge, final List<CpgEdgeType> types ) {
        return types.stream().anyMatch( type -> this.isType( edge, type ) );
    }

    @SuppressWarnings( "unchecked" )
    public Set<String> getLabels( final Node node ) {
        return this.getAttr( node, CpgConst.NODE_ATTR_LABELS, Set.class ).orElse( Collections.emptySet() );
    }

    public boolean hasAttr( final Element element, final String key ) {
        final var attr = element.getAttribute( key );
        if ( attr instanceof String ) {
            return StringUtils.isNotBlank( ( String ) attr );
        }
        return attr != null;
    }

    public String genId( final String prefix ) {
        return ( prefix == null ? "" : prefix ) + "_" + UUID.randomUUID();
    }

}
