package de.haw;

import de.haw.dataset.DesignPatternStatAggregator;
import de.haw.dataset.model.Dataset;
import de.haw.dataset.model.DatasetFactory;
import de.haw.dataset.model.DatasetType;
import de.haw.dataset.module.AttachPatternsToContext;
import de.haw.dataset.module.LoadDatasetFileModule;
import de.haw.misc.pipe.PipeBuilder;
import de.haw.misc.pipe.PipeContext;
import de.haw.misc.pipe.PipeModule;
import de.haw.processing.module.*;
import de.haw.translation.module.GenerateCpgModule;
import de.haw.translation.module.TranslationToGraphModule;
import lombok.extern.slf4j.Slf4j;
import org.graphstream.graph.Graph;

import java.util.Arrays;

@Slf4j
public class Main {

    public static void main( String[] args ) {

        //DesignPatternStatAggregator.aggregateStats( DatasetFactory.getAll( DatasetType.P_MART ) );
        //DesignPatternStatAggregator.aggregateStats( Arrays.asList( DatasetFactory.SINGLETON_EXAMPLE, DatasetFactory.ABSTRACT_FACTORY_EXAMPLE ) );

        //final Dataset dataset = DatasetFactory.get( DatasetType.DPDf, "magic-config" );
        final Dataset dataset = DatasetFactory.SINGLETON_EXAMPLE;
        final PipeContext ctx = PipeContext.empty();
        ctx.set( PipeContext.CPG_DATASET_KEY, dataset );
        ctx.set( PipeContext.CPG_DEPTH_KEY, 10 );

        final PipeModule<Dataset, ?, Graph> pipe = PipeBuilder.<Dataset, Graph>builder()

                .add( AttachPatternsToContext.instance() )
                .add( LoadDatasetFileModule.instance() )

                .add( GenerateCpgModule.instance() )
                //.add( PersistTranslationModule.instance() )

                .add( TranslationToGraphModule.instance() )
                //.add( TranslationToGraphAlternative.instance() )


                .add( MarkScopeModule.instance() )
                .add( MarkPatternsModule.instance() )
                //.add( IsolateMarkedPatternsModule.instance() )
                .add( RemoveBlacklistNodesModule.instance() )

                //.add( CpgFilterEdgesModule.byTypes( Arrays.asList( CpgEdgeType.PDG ), false ) )
                .add( CpgEdgeTypeVisualizeModule.instance() )
                .add( CpgNodeTypeVisualizeModule.instance() )
                .add( DisplayGraphModule.instance() )

                //.add( PersistCpgModule.instance() )
                .build();

        final Graph cpg = pipe.process( dataset, ctx );

        log.info( "CPG: nodes {} / edges {}", cpg.getNodeCount(), cpg.getEdgeCount() );
    }

}