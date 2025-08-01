package de.haw.application;

import de.haw.application.model.TranslationRequest;
import de.haw.dataset.DesignPatternLoader;
import de.haw.dataset.model.Dataset;
import de.haw.dataset.model.DatasetType;
import de.haw.misc.Args;
import de.haw.misc.pipe.PipeBenchmark;
import de.haw.misc.pipe.PipeContext;
import de.haw.misc.pipe.PipeModule;
import de.haw.misc.utils.CsvUtils;
import de.haw.repository.GraphRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor( staticName = "of" )
public class ConvertAndExportCpgDatasets<Target> extends PipeModule<List<TranslationRequest>, Void, Target> {

    private final boolean clearRepo;

    private final static String CSV_FILE_NAME = "benchmark.csv";

    @Override
    protected Void processImpl( final List<TranslationRequest> translationRequests, final PipeContext ctx ) {

        List<TranslationRequest> requests = translationRequests;

        final Args args = ctx.get( PipeContext.ARGS_KEY, Args.empty(), Args.class );
        final GraphRepository repository = GraphRepository.instance( args );

        if ( this.clearRepo ) {
            repository.clearAll();
        }

        ctx.set( PipeContext.CPG_REPOSITORY_PURGE_KEY, false );
        log.info( "Converting {} CPG datasets ...", requests.size() );
        for ( final TranslationRequest translationRequest : requests ) {
            final Dataset dataset = translationRequest.getDataset();
            log.info( "Start processing dataset: {}", dataset.getProjectName() );
            ctx.set( PipeContext.CPG_DEPTH_KEY, translationRequest.getDepth() );
            try {
                ConvertAndExportCpgModule.instance().process( dataset, ctx );
            } catch ( Exception e ) {
                log.error( "Couldn't convert dataset {} ...", e.getMessage(), e );
            }
            writeBenchmarks( ctx );
        }

        return null;
    }

    @SuppressWarnings( "unchecked" )
    private void writeBenchmarks( final PipeContext ctx ) {
        final List<PipeBenchmark> benchmarks = ctx.get( PipeContext.PIPE_BENCHMARKS, new ArrayList<>(), List.class );
        final Optional<Dataset> dataset = ctx.get( PipeContext.CPG_DATASET_KEY, Dataset.class );
        if ( benchmarks.isEmpty() || dataset.isEmpty() ) {
            return;
        }
        final String filePath = getCsvPath( dataset.get().getType() );
        saveToCsv( benchmarks, filePath );
    }

    private static void saveToCsv( final List<PipeBenchmark> csvStats, final String csvPath ) {
        final byte[] csv = CsvUtils.write( csvStats, PipeBenchmark.class );
        try ( FileOutputStream fileOutputStream = new FileOutputStream( csvPath ) ) {
            fileOutputStream.write( csv );
        } catch ( IOException e ) {
            log.info( "Failed to write csv: {}", e.getMessage() );
        }
    }

    private static String getCsvPath( final DatasetType datasetType ) {
        return switch ( datasetType ) {
            case P_MART -> DesignPatternLoader.BASE_PATH_P_MART + CSV_FILE_NAME;
            case DPDf -> DesignPatternLoader.BASE_PATH_DPDf_EXAMPLE + CSV_FILE_NAME;
            default -> DesignPatternLoader.BASE_PATH_JAVA + CSV_FILE_NAME;
        };
    }


}
