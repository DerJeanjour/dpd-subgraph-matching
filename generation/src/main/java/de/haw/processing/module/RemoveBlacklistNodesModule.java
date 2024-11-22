package de.haw.processing.module;

import de.haw.misc.pipe.PipeContext;
import de.haw.misc.pipe.PipeModule;
import de.haw.processing.GraphService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.graphstream.graph.Graph;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor( staticName = "instance" )
public class RemoveBlacklistNodesModule<Target> extends PipeModule<Graph, Graph, Target> {

    private final GraphService graphService = GraphService.instance();

    private final static List<String> LABEL_BLACKLIST = Arrays.asList( "JavaLanguage" );

    @Override
    protected Graph processImpl( final Graph graph, final PipeContext ctx ) {

        final Set<String> nodeIdsToRemove = new HashSet<>();
        graph.nodes().forEach( node -> {
            if ( LABEL_BLACKLIST.stream().anyMatch( label -> this.graphService.hasLabel( node, label ) ) ) {
                nodeIdsToRemove.add( node.getId() );
            }
        } );

        nodeIdsToRemove.forEach( graph::removeNode );
        return graph;
    }
}
