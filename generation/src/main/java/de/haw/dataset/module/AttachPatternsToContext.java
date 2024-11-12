package de.haw.dataset.module;

import de.haw.dataset.Dataset;
import de.haw.dataset.model.DatasetDesignPatterns;
import de.haw.misc.pipe.PipeBuilder;
import de.haw.misc.pipe.PipeContext;
import de.haw.misc.pipe.PipeModule;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor( staticName = "instance" )
public class AttachPatternsToContext<Pass, Target> extends PipeModule<Pass, Pass, Target> {

    @Override
    protected Pass processImpl( final Pass pass, final PipeContext ctx ) {

        final Dataset dataset = ctx.get( PipeContext.CPG_DATASET_KEY, Dataset.class )
                .orElseThrow( IllegalStateException::new );

        final PipeModule<Dataset, ?, DatasetDesignPatterns> pipe = PipeBuilder.<Dataset, DatasetDesignPatterns>builder()
                .add( LoadPatternFileModule.instance() )
                .add( ReadPatternsModule.instance() )
                .build();

        final DatasetDesignPatterns patterns = pipe.process( dataset, ctx );

        if ( patterns != null ) {
            ctx.set( PipeContext.CPG_DESIGN_PATTERNS, patterns );
        }

        return pass;
    }
}
