package de.haw.repository;

import de.haw.processing.cpg.model.CpgEdgeType;
import de.haw.processing.graph.GraphService;
import de.haw.repository.model.CpgEdge;
import de.haw.repository.model.CpgNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Element;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class Neo4jMapper {

    public static List<CpgEdge<CpgNode>> map( final Graph graph ) {
        final Map<Long, CpgNode> cpgNodes = map( graph.nodes() );
        return graph.edges()
                .map( edge -> map( edge, cpgNodes ) )
                .filter( Objects::nonNull )
                .collect( Collectors.toList() );
    }

    private static Map<Long, CpgNode> map( final Stream<Node> nodes ) {
        final Map<Long, CpgNode> cpgNodes = new HashMap<>();
        nodes.forEach( node -> {
            final CpgNode cpgNode = map( node );
            cpgNodes.put( cpgNode.getId(), cpgNode );
        } );
        return cpgNodes;
    }

    @SuppressWarnings( "unchecked" )
    private static CpgNode map( final Node node ) {
        final CpgNode cpgNode = new CpgNode();
        cpgNode.setId( mapId( node ) );

        final Map<String, Object> attr = GraphService.instance().getAttributes( node );
        if ( attr.containsKey( "labels" ) ) {
            cpgNode.setLabels( ( Collection<String> ) node.getAttribute( "labels" ) );
        }

        cpgNode.setProperties( mapAttr( attr ) );

        return cpgNode;
    }

    private static CpgEdge<CpgNode> map( final Edge edge, final Map<Long, CpgNode> cpgNodes ) {
        final CpgEdge<CpgNode> cpgEdge = new CpgEdge<>();
        cpgEdge.setId( mapId( edge ) );

        final Map<String, Object> attr = GraphService.instance().getAttributes( edge );
        cpgEdge.setProperties( mapAttr( attr ) );

        final Optional<CpgEdgeType> edgeType = mapEdgeType( edge );
        if ( edgeType.isEmpty() ) {
            return null;
        }

        final CpgNode cpgSourceNode = cpgNodes.get( mapId( edge.getSourceNode() ) );
        cpgEdge.setSource( cpgSourceNode );
        final CpgNode cpgTargetNode = cpgNodes.get( mapId( edge.getTargetNode() ) );
        cpgEdge.setTarget( cpgTargetNode );

        switch ( edgeType.get() ) {
            case ABSTRACT_SYNTAX_TREE -> {
                cpgSourceNode.getNextAstEdges().add( cpgEdge );
            }
            case DATA_FLOW_GRAPH -> {
                cpgSourceNode.getNextDfgEdge().add( cpgEdge );
                cpgTargetNode.getPrevDfgEdges().add( cpgEdge );
            }
            case EVALUATION_ORDER_GRAPH -> {
                cpgSourceNode.getNextEogEdge().add( cpgEdge );
                cpgTargetNode.getPrevEogEdges().add( cpgEdge );
            }
            case PROGRAM_DEPENDENCY_GRAPH -> {
                cpgSourceNode.getNextPdgEdge().add( cpgEdge );
                cpgTargetNode.getPrevPdgEdges().add( cpgEdge );
            }
            case CONTROL_DEPENDENCE_GRAPH -> {
                cpgSourceNode.getNextCdgEdge().add( cpgEdge );
                cpgTargetNode.getPrevCdgEdges().add( cpgEdge );
            }
        }

        return cpgEdge;
    }

    private static Optional<CpgEdgeType> mapEdgeType( final Edge edge ) {
        final String type = edge.getAttribute( "type", String.class );
        if ( StringUtils.isBlank( type ) ) {
            return Optional.empty();
        }
        for ( CpgEdgeType edgeType : CpgEdgeType.values() ) {
            if ( type.startsWith( edgeType.getValue() ) ) {
                return Optional.of( edgeType );
            }
        }
        return Optional.empty();
    }

    private static Long mapId( final Element element ) {
        return Long.valueOf( element.getId() );
    }

    public static Map<String, String> mapAttr( final Map<String, Object> originalMap ) {
        final Map<String, String> map = new HashMap<>();
        for ( Map.Entry<String, Object> entry : originalMap.entrySet() ) {
            final String key = entry.getKey();
            final Object value = entry.getValue();
            if ( value != null && StringUtils.isNotBlank( value.toString() ) ) {
                map.put( key, value.toString() );
            }
        }
        return map;
    }

}
