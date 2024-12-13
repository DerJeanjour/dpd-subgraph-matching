package de.haw.processing.traversal;

import de.haw.processing.GraphService;
import de.haw.translation.CpgConst;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;

import java.util.List;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor( staticName = "of" )
public class ScopedSubgraphCopyTraverser extends GraphProcessTraverser<Void> {

    private final Graph graph;

    private final GraphService GS  = GraphService.instance();

    @Override
    protected OutputData<Void> process( final Node node, final Void data, final TraversalContext ctx ) {

        final Optional<String> nodeScope = this.GS.getAttr(
                node, CpgConst.NODE_ATTR_NAME_SCOPED_RECORD, String.class );
        final Optional<String> parentScope = this.GS.getAttr(
                ctx.getParent(), CpgConst.NODE_ATTR_NAME_SCOPED_RECORD, String.class );

        if ( parentScope.isEmpty() ) {
            return OutputData.of( null, nodeScope.isPresent() );
        }

        if ( nodeScope.isEmpty() || !nodeScope.equals( parentScope ) ) {
            return OutputData.of( null, false );
        }

        this.GS.copyEdgeToGraph( this.graph, ctx.getEdge() );
        return OutputData.of( null, true );
    }

    @Override
    protected List<Edge> next( final Node node ) {
        return node.leavingEdges().toList();
    }
}
