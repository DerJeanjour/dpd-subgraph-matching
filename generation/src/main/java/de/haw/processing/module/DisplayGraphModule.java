package de.haw.processing.module;

import de.haw.misc.pipe.PipeContext;
import de.haw.processing.visualize.GraphUi;
import de.haw.misc.pipe.PipeModule;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.graphstream.graph.Graph;

@Slf4j
@NoArgsConstructor( staticName = "instance" )
public class DisplayGraphModule<Target> extends PipeModule<Graph, Graph, Target> {

    @Override
    protected Graph processImpl( final Graph graph, final PipeContext ctx ) {
        GraphUi.display( graph, false );
        return graph;
    }
}