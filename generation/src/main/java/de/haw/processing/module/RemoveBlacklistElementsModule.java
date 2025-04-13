package de.haw.processing.module;

import de.haw.misc.pipe.PipeContext;
import de.haw.misc.pipe.PipeModule;
import de.haw.processing.GraphService;
import de.haw.repository.model.CpgEdgeType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.graphstream.graph.Graph;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor( staticName = "instance" )
public class RemoveBlacklistElementsModule<Target> extends PipeModule<Graph, Graph, Target> {

    private final GraphService GS = GraphService.instance();

    public final static List<String> NODE_LABEL_BLACKLIST = Arrays.asList( "JavaLanguage" );

    public final static List<CpgEdgeType> EDGE_TYPE_BLACKLIST = Arrays.asList( CpgEdgeType.AST_NODE, CpgEdgeType.EOG );

    @Override
    protected Graph processImpl( final Graph graph, final PipeContext ctx ) {

        final Set<String> nodeIdsToRemove = new HashSet<>();
        graph.nodes().forEach( node -> {
            if ( NODE_LABEL_BLACKLIST.stream().anyMatch( label -> this.GS.hasLabel( node, label ) ) ) {
                nodeIdsToRemove.add( node.getId() );
            }
        } );
        nodeIdsToRemove.forEach( graph::removeNode );

        final Set<String> edgeIdsToRemove = new HashSet<>();
        graph.edges().forEach( edge -> {
            if ( this.GS.isAnyType( edge, EDGE_TYPE_BLACKLIST ) ) {
                edgeIdsToRemove.add( edge.getId() );
            }
        } );
        edgeIdsToRemove.forEach( graph::removeEdge );


        return graph;
    }
}
