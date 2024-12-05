package de.haw.processing.module;

import de.haw.misc.pipe.PipeContext;
import de.haw.misc.pipe.PipeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.graphstream.algorithm.PageRank;
import org.graphstream.graph.Graph;

@Slf4j
@RequiredArgsConstructor( staticName = "instance" )
public class ComputePagerankModule<Target> extends PipeModule<Graph, Graph, Target> {

    private static final String ATTR_NAME = "pagerank";

    @Override
    protected Graph processImpl( final Graph graph, final PipeContext ctx ) {

        final PageRank algo = new PageRank( PageRank.DEFAULT_DAMPING_FACTOR, PageRank.DEFAULT_PRECISION, ATTR_NAME );
        algo.init( graph );
        algo.compute();

        ctx.set( PipeContext.NODE_SIZE_ATTR, ATTR_NAME );
        ctx.set( PipeContext.NODE_SIZE_SCALE, ( double ) graph.getNodeCount() * 4 );

        return graph;
    }
}
