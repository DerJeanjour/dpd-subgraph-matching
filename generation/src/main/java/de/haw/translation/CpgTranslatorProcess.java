package de.haw.translation;

import de.haw.dataset.model.Dataset;
import de.haw.dataset.module.LoadDatasetFileModule;
import de.haw.misc.pipe.PipeBuilder;
import de.haw.misc.pipe.PipeContext;
import de.haw.misc.pipe.PipeModule;
import de.haw.translation.module.GenerateCpgModule;
import de.haw.translation.module.TranslationToGraphModule;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.graphstream.graph.Graph;

@Slf4j
@NoArgsConstructor( staticName = "instance" )
public class CpgTranslatorProcess<Target> extends PipeModule<Dataset, Graph, Target> {

    @Override
    protected Graph processImpl( final Dataset dataset, final PipeContext ctx ) {
        final PipeModule<Dataset, ?, Graph> pipe = PipeBuilder.<Dataset, Graph>builder()
                .add( LoadDatasetFileModule.instance() )
                .add( GenerateCpgModule.instance() )
                .add( TranslationToGraphModule.instance() )
                .build();

        return pipe.process( dataset, ctx );
    }

}
