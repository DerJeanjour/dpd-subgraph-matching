package de.haw.processing.module;

import de.haw.misc.pipe.PipeContext;
import de.haw.repository.model.CpgEdgeType;
import de.haw.processing.GraphService;
import de.haw.misc.pipe.PipeModule;
import de.haw.translation.CpgConst;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.graphstream.graph.Graph;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor( staticName = "byTypes" )
public class CpgFilterEdgesModule<Target> extends PipeModule<Graph, Graph, Target> {

    private final List<CpgEdgeType> edgeTypes;

    private final boolean autoConnect;

    private final GraphService GS  = GraphService.instance();

    @Override
    protected Graph processImpl( final Graph graph, final PipeContext ctx ) {

        log.info( "Filter CPG with types {}: nodes {} / edges {}", this.edgeTypes, graph.getNodeCount(),
                graph.getEdgeCount() );

        final Set<String> typesIncludedExplicitly = new HashSet<>();
        final Graph filtered = this.GS.getEmptyGraph( graph );

        graph.edges().forEach( edge -> {

            final String type = edge.getAttribute( CpgConst.EDGE_ATTR_TYPE, String.class );

            final boolean shouldIncludeEdge = this.edgeTypes.stream()
                    .anyMatch( edgeType -> type.equals( edgeType.name() ) );
            if ( shouldIncludeEdge ) {

                typesIncludedExplicitly.add( type );
                this.GS.copyEdgeToGraph( filtered, edge );
            }

        } );

        log.info( "Filtered CPG includes the following edge types: {}", typesIncludedExplicitly );

        if ( autoConnect ) {

            final Set<String> typesIncludedImplicitly = new HashSet<>();
            graph.edges().forEach( edge -> {

                boolean edgeExists = filtered.getEdge( edge.getId() ) != null;
                if ( edgeExists ) {
                    return;
                }

                boolean sourceNodeExists = filtered.getNode( edge.getSourceNode().getId() ) != null;
                boolean targetNodeExists = filtered.getNode( edge.getTargetNode().getId() ) != null;

                if ( sourceNodeExists && targetNodeExists ) {

                    final String type = edge.getAttribute( CpgConst.EDGE_ATTR_TYPE, String.class );
                    typesIncludedImplicitly.add( type );
                    this.GS.copyEdgeToGraph( filtered, edge );
                }
            } );

            log.info( "Filtered CPG includes the following implicit edge types: {}", typesIncludedImplicitly );

        }


        return filtered;
    }
}
