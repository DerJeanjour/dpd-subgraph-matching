package de.haw.processing.model;

import de.haw.processing.GraphService;
import de.haw.repository.model.CpgEdgeType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.graphstream.graph.Edge;

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

}
