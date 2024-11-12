package de.haw;

import de.haw.dataset.Dataset;
import de.haw.dataset.DesignPatternStatAggregator;
import de.haw.dataset.model.DatasetDesignPatterns;
import de.haw.dataset.module.AttachPatternsToContext;
import de.haw.misc.pipe.PipeBuilder;
import de.haw.misc.pipe.PipeContext;
import de.haw.misc.pipe.PipeModule;
import lombok.extern.slf4j.Slf4j;
import org.graphstream.graph.Graph;

import java.util.Map;

@Slf4j
public class Main {

    public static void main( String[] args ) {

        final Dataset dataset = Dataset.MAPPER_XML;

        final PipeContext ctx = PipeContext.empty();
        ctx.set( PipeContext.CPG_DATASET_KEY, dataset );
        ctx.set( PipeContext.CPG_DEPTH_KEY, 10 );

        /*
        final PipeModule<Dataset, ?, Graph> pipe = PipeBuilder.<Dataset, Graph>builder()
                .add( AttachPatternsToContext.instance() )
                .add( CpgTranslatorProcess.instance() )
                .add( PersistCpgModule.instance() )
                .build();
        final Graph cpg = pipe.process( dataset, ctx );

        log.info( "CPG: nodes {} / edges {}", cpg.getNodeCount(), cpg.getEdgeCount() );

         */

        final Map<String, Integer> stats = DesignPatternStatAggregator.aggregateStats();
        log.info( "{}", stats );

    }

}