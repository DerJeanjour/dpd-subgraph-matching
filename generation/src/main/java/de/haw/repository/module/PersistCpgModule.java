package de.haw.repository.module;

import de.haw.misc.Args;
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

        final int depth = ctx.get( PipeContext.CPG_DEPTH_KEY, 10, Integer.class );
        final boolean purge = ctx.get( PipeContext.CPG_REPOSITORY_PURGE_KEY, true, Boolean.class );
        final Args args = ctx.get( PipeContext.ARGS_KEY, Args.empty(), Args.class );

        final GraphRepository client = GraphRepository.instance( args );

        if ( purge ) {
            client.clearAll();
        }
        client.writeGraph( graph, depth );

        return graph;
    }
}
