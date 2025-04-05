package de.haw.application;

import de.haw.dataset.model.Dataset;
import de.haw.dataset.module.AttachPatternsToContext;
import de.haw.dataset.module.LoadDatasetFileModule;
import de.haw.misc.pipe.PipeBuilder;
import de.haw.misc.pipe.PipeContext;
import de.haw.misc.pipe.PipeModule;
import de.haw.processing.module.*;
import de.haw.repository.model.CpgEdgeType;
import de.haw.repository.module.PersistCpgModule;
import de.haw.translation.module.GenerateCpgModule;
import de.haw.translation.module.TranslationToGraphModule;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.graphstream.graph.Graph;

@Slf4j
@NoArgsConstructor( staticName = "instance" )
public class ConvertAndExportCpgModule<Target> extends PipeModule<Dataset, Graph, Target> {

    @Override
    protected Graph processImpl( final Dataset dataset, final PipeContext ctx ) {

        ctx.set( PipeContext.PROCESS_NAME, dataset.getName() );
        ctx.set( PipeContext.CPG_DATASET_KEY, dataset );
        ctx.set( PipeContext.PROCESS_COUNT, 0 );
        ctx.set( PipeContext.TOTAL_PROCESSING_TIME, 0d );

        final PipeModule<Dataset, ?, Graph> pipe = PipeBuilder.<Dataset, Graph>builder()

                // load data
                .add( AttachPatternsToContext.instance() )
                .add( LoadDatasetFileModule.instance() )

                // generate cpg
                .add( GenerateCpgModule.instance() )
                .add( TranslationToGraphModule.instance() )

                // prepare cpg
                .add( RemoveBlacklistElementsModule.instance() )
                .add( FilterInternalScopeModule.instance() )
                .add( PropagateRecordScopeModule.instance() )

                // simplify cpg
                .add( ComputeRecordPathsModule.instance() )
                .add( ComputeRecordInteractionsModule.instance() )
                .add( CpgFilterEdgesModule.byTypes( CpgEdgeType.OWN, false ) )

                // patterns
                .add( MarkPatternsModule.instance() )

                // persist
                .add( PersistCpgModule.instance() )
                .build();

        return pipe.process( dataset, ctx );
    }

}
