package de.haw.processing.traversal;

import de.haw.processing.GraphService;
import de.haw.translation.CpgConst;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;

import java.util.List;

@Slf4j
@RequiredArgsConstructor( staticName = "of" )
public class RecordNeighbourSubgraphCopyTraverser extends GraphProcessTraverser<Void> {

    private final Graph graph;

    private final int maxDepth;

    private final GraphService GS = GraphService.instance();

    @Override
    protected OutputData<Void> process( final Node node, final Void data, final TraversalContext ctx ) {

        if ( ctx.getDepth() > this.maxDepth ) {
            return OutputData.of( null, false );
        }

        if ( ctx.getParent() == null ) {
            this.GS.copyNodeToGraph( this.graph, node );
            return OutputData.of( null, true );
        }

        this.GS.copyEdgeToGraph( this.graph, ctx.getEdge() );

        if ( this.GS.hasLabel( node, CpgConst.NODE_LABEL_DECLARATION_RECORD ) ) {
            return OutputData.of( null, false );
        }

        return OutputData.of( null, true );
    }

    @Override
    protected List<Edge> next( final Node node ) {
        return node.leavingEdges().toList();
    }

}
