package de.haw.application;

import de.haw.application.model.TranslationRequest;
import de.haw.dataset.model.Dataset;
import de.haw.misc.pipe.PipeContext;
import de.haw.misc.pipe.PipeModule;
import de.haw.repository.GraphRepository;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@NoArgsConstructor( staticName = "instance" )
public class ConvertAndExportCpgDatasets<Target> extends PipeModule<List<TranslationRequest>, Void, Target> {

    private final GraphRepository repository = GraphRepository.instance();

    @Override
    protected Void processImpl( final List<TranslationRequest> translationRequests, final PipeContext ctx ) {

        repository.clearAll();
        ctx.set( PipeContext.CPG_REPOSITORY_PURGE_KEY, false );

        for ( final TranslationRequest translationRequest : translationRequests ) {
            final Dataset dataset = translationRequest.getDataset();
            log.info( "Start processing dataset: {}", dataset.getProjectName() );
            ctx.set( PipeContext.CPG_DEPTH_KEY, translationRequest.getDepth() );
            ConvertAndExportCpgModule.instance().process( dataset, ctx );
        }

        return null;
    }
}
