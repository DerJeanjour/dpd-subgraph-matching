package de.haw.processing.graph.module;

import de.haw.processing.graph.GraphUi;
import de.haw.processing.pipe.PipeModule;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.graphstream.graph.Graph;

@Slf4j
@NoArgsConstructor( staticName = "instance" )
public class DisplayGraphModule<Target> extends PipeModule<Graph, Graph, Target> {

    @Override
    protected Graph processImpl( final Graph graph ) {
        GraphUi.display( graph, false );
        return graph;
    }
}