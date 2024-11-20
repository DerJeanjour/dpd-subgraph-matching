package de.haw.processing.traversal;

import de.haw.processing.GraphService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor( staticName = "of" )
public class ScopedSubgraphCopyTraverser extends GraphProcessTraverser<String> {

    private final Graph graph;

    private final GraphService graphService = GraphService.instance();

    @Override
    protected OutputData<String> process( final Node node, final Node parent, final String scopeName ) {

        final Optional<String> nodeScope = this.graphService.getAttr( node, "scopedName", String.class );
        if ( nodeScope.isEmpty() ) {
            return OutputData.of( null, false );
        }
        if ( parent == null ) {
            this.graphService.copyNodeToGraph( this.graph, node );
            return OutputData.of( nodeScope.get(), true );
        }

        if ( StringUtils.isNotBlank( scopeName ) && scopeName.equals( nodeScope.get() ) ) {
            this.graphService.copyEdgeToGraph( this.graph, node.getEdgeFrom( parent ) );
            return OutputData.of( scopeName, true );
        }

        return OutputData.of( null, false );
    }

    @Override
    protected List<Edge> next( final Node node ) {
        return node.leavingEdges().toList();
    }
}
