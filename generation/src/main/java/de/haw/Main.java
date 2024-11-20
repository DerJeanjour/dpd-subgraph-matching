package de.haw;

import de.haw.dataset.Dataset;
import de.haw.dataset.module.AttachPatternsToContext;
import de.haw.dataset.module.LoadDatasetFileModule;
import de.haw.misc.pipe.PipeBuilder;
import de.haw.misc.pipe.PipeContext;
import de.haw.misc.pipe.PipeModule;
import de.haw.processing.module.*;
import de.haw.repository.module.PersistCpgModule;
import de.haw.translation.module.GenerateCpgModule;
import de.haw.translation.module.PersistTranslationModule;
import de.haw.translation.module.TranslationToGraphModule;
import lombok.extern.slf4j.Slf4j;
import org.graphstream.graph.Graph;

@Slf4j
public class Main {

    public static void main( String[] args ) {

        final Dataset dataset = Dataset.J_UNIT; // J_UNIT is small

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
                .add( IsolateMarkedPatternsModule.instance() )

                //.add( CpgFilterEdgesModule.byTypes( Arrays.asList( CpgEdgeType.CONTROL_DEPENDENCE_GRAPH ), false ) )
                //.add( CpgEdgeTypeVisualizeModule.instance() )
                //.add( DisplayGraphModule.instance() )

                .add( PersistCpgModule.instance() )

                .build();
        final Graph cpg = pipe.process( dataset, ctx );

        log.info( "CPG: nodes {} / edges {}", cpg.getNodeCount(), cpg.getEdgeCount() );
    }

}