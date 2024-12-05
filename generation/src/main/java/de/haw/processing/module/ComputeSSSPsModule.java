package de.haw.processing.module;

import de.haw.misc.pipe.PipeContext;
import de.haw.misc.pipe.PipeModule;
import de.haw.processing.GraphService;
import de.haw.translation.CpgConst;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.graphstream.algorithm.Dijkstra;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.Path;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor( staticName = "instance" )
public class ComputeSSSPsModule<Target> extends PipeModule<Graph, Graph, Target> {

    private static final String SSSP_ATTR_NAME = "sssp";

    private final GraphService GS = GraphService.instance();

    @Override
    protected Graph processImpl( final Graph graph, final PipeContext ctx ) {

        final Map<String, List<Data>> recordPaths = new HashMap<>();

        final List<Node> recordNodes = graph.nodes()
                .filter( node -> this.GS.hasLabel( node, CpgConst.NODE_LABEL_DECLARATION_RECORD ) )
                .toList();

        recordNodes.forEach( source -> {

            if ( this.GS.hasLabel( source, CpgConst.NODE_LABEL_DECLARATION_RECORD ) ) {
                final Dijkstra dijkstra = new Dijkstra( null, SSSP_ATTR_NAME, null, null, null, null );
                dijkstra.init( graph );
                dijkstra.setSource( source );
                dijkstra.compute();

                final List<Data> paths = recordNodes.stream()
                        .filter( target -> !target.getId().equals( source.getId() ) )
                        .map( target -> Data.of( source, target, dijkstra.getPath( target ),
                                dijkstra.getPathLength( target ) ) )
                        .toList();
                recordPaths.put( source.getId(), paths );
            }
        } );

        ctx.set( PipeContext.RECORD_PATHS, recordPaths );

        return graph;
    }

    @lombok.Data
    @RequiredArgsConstructor( staticName = "of" )
    public static class Data {

        private final Node source;

        private final Node target;

        private final Path path;

        private final double distance;

    }

}
