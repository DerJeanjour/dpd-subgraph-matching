package de.haw.processing.module;

import de.haw.misc.pipe.PipeContext;
import de.haw.misc.pipe.PipeModule;
import de.haw.processing.GraphService;
import de.haw.translation.CpgConst;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;

@Slf4j
@RequiredArgsConstructor( staticName = "instance" )
public class MarkScopeModule<Target> extends PipeModule<Graph, Graph, Target> {

    private final GraphService graphService = GraphService.instance();

    @Override
    @SuppressWarnings( "unchecked" )
    protected Graph processImpl( final Graph graph, final PipeContext ctx ) {
        graph.nodes().forEach( node -> {
            final String scopeName = node.getAttribute( CpgConst.NODE_ATTR_NAME_SCOPED, String.class );
            if ( !this.graphService.hasLabel( node, CpgConst.NODE_LABEL_SCOPE_NAME ) || StringUtils.isBlank(
                    scopeName ) ) {
                return;
            }
            for ( int i = 0; i < node.getInDegree(); i++ ) {
                final Node child = node.getEnteringEdge( i ).getSourceNode();
                child.setAttribute( CpgConst.NODE_ATTR_NAME_SCOPED, scopeName );
            }
        } );
        return graph;
    }

}
