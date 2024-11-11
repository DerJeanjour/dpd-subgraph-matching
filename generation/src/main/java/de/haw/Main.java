package de.haw;

import de.haw.dataset.Dataset;
import de.haw.misc.pipe.PipeBuilder;
import de.haw.misc.pipe.PipeContext;
import de.haw.misc.pipe.PipeModule;
import de.haw.repository.module.PersistCpgModule;
import de.haw.translation.CpgTranslatorProcess;
import lombok.extern.slf4j.Slf4j;
import org.graphstream.graph.Graph;

@Slf4j
public class Main {

    public static void main( String[] args ) {

        final PipeContext ctx = PipeContext.empty();
        ctx.set( "depth", 10 );

        final PipeModule<Dataset, ?, Graph> pipe = PipeBuilder.<Dataset, Graph>builder()
                .add( CpgTranslatorProcess.instance() )
                .add( PersistCpgModule.instance() )
                .build();
        final Graph cpg = pipe.process( Dataset.ANIMAL, ctx );

        log.info( "CPG: nodes {} / edges {}", cpg.getNodeCount(), cpg.getEdgeCount() );
    }

}