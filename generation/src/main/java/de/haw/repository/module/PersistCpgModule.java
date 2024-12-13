package de.haw.repository.module;

import de.haw.misc.pipe.PipeContext;
import de.haw.misc.pipe.PipeModule;
import de.haw.repository.GraphRepository;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.graphstream.graph.Graph;

@Slf4j
@NoArgsConstructor( staticName = "instance" )
public class PersistCpgModule<Target> extends PipeModule<Graph, Graph, Target> {

    @Override
    protected Graph processImpl( final Graph graph, final PipeContext ctx ) {

        final GraphRepository client = GraphRepository.instance();

        final int depth = ctx.get( PipeContext.CPG_DEPTH_KEY, 10, Integer.class );
        final boolean purge = ctx.get( PipeContext.CPG_REPOSITORY_PURGE_KEY, true, Boolean.class );
        client.writeGraph( graph, ctx.get( PipeContext.CPG_DEPTH_KEY, 10, Integer.class ), purge );

        return graph;
    }
}
