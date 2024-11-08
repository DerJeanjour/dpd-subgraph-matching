package de.haw;

import de.haw.datasets.Dataset;
import de.haw.processing.cpg.CpgProcessor;
import lombok.extern.slf4j.Slf4j;
import org.graphstream.graph.Graph;

@Slf4j
public class Main {

    public static void main( String[] args ) {
        final CpgProcessor processor = new CpgProcessor();
        final Graph cpg = processor.run( Dataset.ANIMAL );
        log.info( "CPG: nodes {} / edges {}", cpg.getNodeCount(), cpg.getEdgeCount() );
    }

}