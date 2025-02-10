package de.haw.application;

import de.haw.application.model.TranslationRequest;
import de.haw.dataset.model.Dataset;
import de.haw.misc.FileLogger;
import de.haw.misc.pipe.PipeContext;
import de.haw.misc.pipe.PipeModule;
import de.haw.repository.GraphRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@RequiredArgsConstructor( staticName = "of" )
public class ConvertAndExportCpgDatasets<Target> extends PipeModule<List<TranslationRequest>, Void, Target> {

    private final boolean continueGen;

    private final GraphRepository repository = GraphRepository.instance();

    @Override
    protected Void processImpl( final List<TranslationRequest> translationRequests, final PipeContext ctx ) {

        final FileLogger fileLogger = FileLogger.of( "generation/gen_data.txt" );
        List<TranslationRequest> requests = translationRequests;
        if ( this.continueGen ) {
            final List<String> generatedData = fileLogger.readContent();
            requests = requests.stream()
                    .filter( r -> !generatedData.contains( r.getDataset().getProjectName() ) )
                    .toList();
        } else {
            repository.clearAll();
            fileLogger.clear();
        }

        ctx.set( PipeContext.CPG_REPOSITORY_PURGE_KEY, false );
        log.info( "Converting {} CPG datasets ...", requests.size() );
        for ( final TranslationRequest translationRequest : requests ) {
            final Dataset dataset = translationRequest.getDataset();
            log.info( "Start processing dataset: {}", dataset.getProjectName() );
            fileLogger.write( "Starting: " + dataset.getProjectName() );
            ctx.set( PipeContext.CPG_DEPTH_KEY, translationRequest.getDepth() );
            try {
                ConvertAndExportCpgModule.instance().process( dataset, ctx );
                fileLogger.clearLastLine();
            } catch ( Exception e ) {
                log.error( "Couldn't convert dataset {} ...", e.getMessage(), e );
                fileLogger.write( "Failed: " + dataset.getProjectName() + " (" + e.getMessage() + ")" );
            }

            fileLogger.write( dataset.getProjectName() );
        }

        return null;
    }
}
