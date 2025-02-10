package de.haw.processing.model;

import de.haw.processing.GraphService;
import de.haw.processing.visualize.GraphUi;
import de.haw.repository.model.CpgEdgeType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor( staticName = "instance" )
public class RecordInteractionPathCounts {

    private final GraphService GS = GraphService.instance();

    private final Map<RecordInteractionType, Map<List<CpgEdgeType>, Integer>> pathCounts = new HashMap<>();

    public void add( final RecordInteraction interaction ) {
        if ( !this.pathCounts.containsKey( interaction.getType() ) ) {
            this.pathCounts.put( interaction.getType(), new HashMap<>() );
        }

        final Map<List<CpgEdgeType>, Integer> interactionPathCounts = this.pathCounts.get( interaction.getType() );
        for ( final List<CpgEdgeType> edgePath : this.getAllEdgeSubPath( interaction.getPath() ) ) {
            if ( !interactionPathCounts.containsKey( edgePath ) ) {
                interactionPathCounts.put( edgePath, 0 );
            }
            interactionPathCounts.put( edgePath, interactionPathCounts.get( edgePath ) + 1 );
        }
    }

    private List<List<CpgEdgeType>> getAllEdgeSubPath( final CpgPath path ) {
        final List<List<CpgEdgeType>> subPaths = new ArrayList<>();
        final List<CpgEdgeType> edgePath = this.toEdgePath( path.getPath().getEdgePath() );
        for ( int i = 0; i <= edgePath.size(); i++ ) {
            final List<CpgEdgeType> subEdgePath = edgePath.subList( 0, i );
            if ( subEdgePath.isEmpty() ) {
                continue;
            }
            subPaths.add( subEdgePath );
        }
        return subPaths;
    }

    private List<CpgEdgeType> toEdgePath( final List<Edge> path ) {
        return path.stream().map( this.GS::getType ).toList();
    }

    public Graph toGraph() {
        final Graph graph = new SingleGraph( this.GS.genId( "interactions" ), true, false );
        for ( final RecordInteractionType type : this.pathCounts.keySet() ) {

            final Node root = this.GS.addNode( graph, "root" );
            root.setAttribute( GraphUi.ATTR_LABEL, root.getId() );
            final Node interactionRoot = this.GS.addNode( graph, type.name() );
            interactionRoot.setAttribute( GraphUi.ATTR_LABEL, type.name() );
            this.GS.addEdge( graph, root.getId() + "->" + interactionRoot.getId(), root, interactionRoot );

            for ( final List<CpgEdgeType> edgePath : this.pathCounts.get( type ).keySet() ) {
                Node current = interactionRoot;
                for ( int i = 0; i < edgePath.size(); i++ ) {
                    final CpgEdgeType edgeType = edgePath.get( i );
                    final Node next = this.GS.addNode( graph, current.getId() + "_" + edgeType.name() + "_" + i );
                    next.setAttribute( GraphUi.ATTR_LABEL, edgeType.name() );
                    this.GS.addEdge( graph, current.getId() + "->" + next.getId(), current, next );
                    current = next;
                }
                int currentSize = this.GS.getAttr( current, "size", Integer.class ).orElse( 0 );
                current.setAttribute( "size", currentSize + this.pathCounts.get( type ).get( edgePath ) );
            }

        }

        graph.nodes().forEach( node -> {
            final int size = this.GS.getAttr( node, "size", Integer.class ).orElse( 0 );
            final int displaySize = Math.min( Math.max( size, 3 ), 30 );
            GraphUi.addStyleParam( node, GraphUi.getSizeParam( displaySize ) );
        } );

        return graph;
    }

}
