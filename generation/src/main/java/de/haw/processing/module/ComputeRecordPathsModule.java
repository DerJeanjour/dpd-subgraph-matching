package de.haw.processing.module;

import de.haw.misc.pipe.PipeContext;
import de.haw.misc.pipe.PipeModule;
import de.haw.processing.GraphService;
import de.haw.processing.model.CpgNodePaths;
import de.haw.processing.model.CpgPath;
import de.haw.processing.traversal.RecordNeighbourSubgraphCopyTraverser;
import de.haw.translation.CpgConst;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.graphstream.algorithm.Dijkstra;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;

import java.util.List;

@Slf4j
@RequiredArgsConstructor( staticName = "instance" )
public class ComputeRecordPathsModule<Target> extends PipeModule<Graph, Graph, Target> {

    private static final int MAX_PATHS_VARIATIONS = 5;

    private static final int MAX_PATH_DISTANCE = 10;

    private static final String SSSP_ATTR_NAME = "sssp";

    private final GraphService GS = GraphService.instance();

    @Override
    protected Graph processImpl( final Graph graph, final PipeContext ctx ) {

        final CpgNodePaths recordPaths = new CpgNodePaths();

        final List<Node> recordNodes = this.getRecordNodes( graph );

        recordNodes.forEach( source -> {

            Graph recordNeighbourSubgraph = this.getRecordNeighbourSubgraph( source );
            for ( int i = 0; i < MAX_PATHS_VARIATIONS; i++ ) {
                final List<CpgPath> sssps = this.getShortestPaths( recordNeighbourSubgraph, source );
                if ( sssps.isEmpty() ) {
                    break;
                }
                recordNeighbourSubgraph = this.GS.copyGraph( recordNeighbourSubgraph );
                for ( final CpgPath sssp : sssps ) {
                    final List<Edge> edgePath = sssp.getPath().getEdgePath();
                    recordNeighbourSubgraph.removeEdge( edgePath.get( edgePath.size() - 1 ).getId() );
                }
                sssps.forEach( path -> recordPaths.add( source.getId(), path ) );
            }
        } );

        ctx.set( PipeContext.RECORD_PATHS, recordPaths );

        return graph;
    }

    private List<CpgPath> getShortestPaths( final Graph graph, final Node source ) {

        final List<Node> recordNodes = this.getRecordNodes( graph );

        final Dijkstra dijkstra = new Dijkstra( null, SSSP_ATTR_NAME, null, null, null, null );
        dijkstra.init( graph );
        dijkstra.setSource( graph.getNode( source.getId() ) );
        dijkstra.compute();

        return recordNodes.stream()
                .filter( target -> !target.getId().equals( source.getId() ) )
                .map( target -> CpgPath.of( source, target, dijkstra.getPath( target ),
                        dijkstra.getPathLength( target ) ) )
                .filter( path -> !path.getPath().empty() && path.getDistance() <= MAX_PATH_DISTANCE )
                .toList();
    }

    private Graph getRecordNeighbourSubgraph( final Node source ) {
        final Graph subgraph = this.GS.getEmptyGraph( source.getGraph() );
        RecordNeighbourSubgraphCopyTraverser.of( subgraph, MAX_PATH_DISTANCE ).traverse( source );
        return subgraph;
    }

    private List<Node> getRecordNodes( final Graph graph ) {
        return graph.nodes()
                .filter( node -> this.GS.hasLabel( node, CpgConst.NODE_LABEL_DECLARATION_RECORD ) )
                .toList();
    }

}
