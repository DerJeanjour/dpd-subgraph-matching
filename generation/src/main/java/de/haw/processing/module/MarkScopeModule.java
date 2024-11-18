package de.haw.processing.module;

import de.haw.misc.pipe.PipeContext;
import de.haw.misc.pipe.PipeModule;
import de.haw.processing.GraphService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor( staticName = "instance" )
public class MarkScopeModule<Target> extends PipeModule<Graph, Graph, Target> {

    @Override
    @SuppressWarnings( "unchecked" )
    protected Graph processImpl( final Graph graph, final PipeContext ctx ) {
        final GraphService graphService = GraphService.instance();
        graph.nodes().forEach( node -> {
            final Collection<String> labels = graphService.getAttr( node, "labels", Set.class )
                    .orElse( Collections.emptySet() );
            if ( labels.isEmpty() || !labels.contains( "Scope" ) || StringUtils.isBlank(
                    node.getAttribute( "scopedName", String.class ) ) ) {
                return;
            }
            for ( int i = 0; i < node.getInDegree(); i++ ) {
                final Node child = node.getEnteringEdge( i ).getSourceNode();
                child.setAttribute( "scopedName", node.getAttribute( "scopedName" ) );
            }
        } );
        return graph;
    }

}
