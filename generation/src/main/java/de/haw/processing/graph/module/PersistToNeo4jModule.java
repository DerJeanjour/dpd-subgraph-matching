package de.haw.processing.graph.module;

import de.haw.processing.pipe.PipeModule;
import de.haw.repository.Neo4jClient;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.graphstream.graph.Graph;

@Slf4j
@NoArgsConstructor( staticName = "instance" )
public class PersistToNeo4jModule<Target> extends PipeModule<Graph, Graph, Target> {

    @Override
    protected Graph processImpl( final Graph graph ) {

        final Neo4jClient client = Neo4jClient.instance();
        client.writeGraph( graph, 10, true );

        return graph;
    }
}
