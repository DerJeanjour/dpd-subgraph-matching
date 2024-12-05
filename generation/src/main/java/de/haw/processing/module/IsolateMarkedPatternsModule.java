package de.haw.processing.module;

import de.haw.dataset.model.DesignPatternType;
import de.haw.misc.pipe.PipeContext;
import de.haw.misc.pipe.PipeModule;
import de.haw.processing.GraphService;
import de.haw.processing.traversal.ScopedSubgraphCopyTraverser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;

import java.util.Arrays;
import java.util.List;

@Slf4j
@RequiredArgsConstructor( staticName = "instance" )
public class IsolateMarkedPatternsModule<Target> extends PipeModule<Graph, Graph, Target> {

    private final GraphService graphService = GraphService.instance();

    @Override
    protected Graph processImpl( final Graph graph, final PipeContext ctx ) {

        final Graph isolatedPatternGraph = this.graphService.getEmptyGraph();
        graph.nodes().forEach( node -> {

            final List<DesignPatternType> patternTypes = this.getPatternTypes( node );
            if ( patternTypes.isEmpty() ) {
                return;
            }

            ScopedSubgraphCopyTraverser.of( isolatedPatternGraph ).traverse( node );
        } );

        return isolatedPatternGraph;
    }

    private List<DesignPatternType> getPatternTypes( final Node node ) {
        return Arrays.stream( DesignPatternType.values() )
                .filter( dpt -> this.graphService.hasLabel( node, dpt.name() ) )
                .toList();
    }

}
