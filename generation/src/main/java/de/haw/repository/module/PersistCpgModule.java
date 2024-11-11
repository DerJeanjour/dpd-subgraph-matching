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
        client.writeGraph( graph, ctx.get( "depth", 10, Integer.class ), true );

        return graph;
    }
}
