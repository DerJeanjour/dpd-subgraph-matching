package de.haw;

import de.haw.application.ConvertAndExportCpgDatasets;
import de.haw.application.model.TranslationRequest;
import de.haw.dataset.DesignPatternStatAggregator;
import de.haw.dataset.model.*;
import de.haw.dataset.module.AttachPatternsToContext;
import de.haw.dataset.module.LoadDatasetFileModule;
import de.haw.dataset.scripts.DirectorySizeSummary;
import de.haw.misc.Args;
import de.haw.misc.pipe.PipeBuilder;
import de.haw.misc.pipe.PipeContext;
import de.haw.misc.pipe.PipeModule;
import de.haw.misc.utils.MemoryUtils;
import de.haw.processing.module.*;
import de.haw.repository.model.CpgEdgeType;
import de.haw.repository.module.PersistCpgModule;
import de.haw.translation.module.GenerateCpgModule;
import de.haw.translation.module.TranslationToGraphModule;
import lombok.extern.slf4j.Slf4j;
import org.graphstream.graph.Graph;

import java.util.*;

@Slf4j
public class Main {

    public static void main( String[] args ) {

        MemoryUtils.logMemoryStats();
        final Args arguments = Args.of( args );

        if ( arguments.has( "source" ) ) {
            final String path = arguments.get( "source" );
            final String projectName = arguments.get( "name" );
            final DatasetLanguage language = DatasetLanguage.valueOf( arguments.get( "language" ).toUpperCase() );
            final int depth = Integer.parseInt( arguments.getOrElse( "depth", "10" ) );
            final TranslationRequest request = TranslationRequest.custom( path, projectName, language, depth );
            convertDatasets( Collections.singletonList( request ), arguments );
            return;
        }

        //test();
        convertDatasets( getPatternRequests(), arguments );
    }

    private static void convertDatasets( final List<TranslationRequest> translationRequests, final Args args ) {
        convertDatasets( translationRequests, false, true, args );
    }

    private static void convertDatasets(
            final List<TranslationRequest> translationRequests, final boolean continueGen, final boolean clearRepo,
            final Args args ) {
        final PipeContext ctx = PipeContext.empty();
        ctx.set( PipeContext.CPG_MIN_DEPTH_KEY, 7 );
        ctx.set( PipeContext.ARGS_KEY, args );
        ConvertAndExportCpgDatasets.of( continueGen, clearRepo ).process( translationRequests, ctx );
    }

    private static List<TranslationRequest> getPatternRequests() {
        final List<DatasetType> datasetTypes = Arrays.asList( DatasetType.CPP_PATTERNS, DatasetType.JAVA_PATTERNS );
        final List<String> patterns = Arrays.asList(
                "abstract-factory", "factory-method", "adapter", "observer", "builder", "decorator" );
        final List<TranslationRequest> requests = new ArrayList<>();
        for ( DatasetType type : datasetTypes ) {
            for ( String pattern : patterns ) {
                final Dataset dataset = Dataset.of( DatasetFactory.getLanguage( type ), type, pattern );
                requests.add( TranslationRequest.of( dataset, 10 ) );
            }
        }
        return requests;
    }

    private static List<TranslationRequest> getPmartRequests() {
        // @formatter:off
        return Arrays.asList(
                TranslationRequest.of( DatasetFactory.J_HOT_DRAW, 10 ),
                TranslationRequest.of( DatasetFactory.J_UNIT, 10 ),
                TranslationRequest.of( DatasetFactory.QUICK_UML, 10 ),
                TranslationRequest.of( DatasetFactory.MAPPER_XML, 10 ),
                TranslationRequest.of( DatasetFactory.PMD, 8 ),
                TranslationRequest.of( DatasetFactory.NUTCH, 8 ) );
        // @formatter:on
    }

    private static List<TranslationRequest> getDpdfRequests() {
        final Set<String> blacklist = Set.of( "" );
        final List<TranslationRequest> translationRequests = new ArrayList<>(
                getDatasetsWithPatterns( DatasetType.DPDf ).stream()
                        .filter( d -> !blacklist.contains( d.getProjectName() ) )
                        .map( d -> TranslationRequest.of( d, 9 ) )
                        .toList() );

        final String datasetSizeFile = "datasets/java/dpdf/size_summary.csv";
        final Map<String, Long> datasetSizes = DirectorySizeSummary.getDirectorySizes( datasetSizeFile );
        translationRequests.sort( Comparator.comparing(
                dto -> datasetSizes.getOrDefault( dto.getDataset().getProjectName(), Long.MAX_VALUE ) ) );
        return translationRequests;
    }

    private static List<Dataset> getDatasetsWithPatterns( final DatasetType type ) {
        final Map<String, DatasetDesignPatterns> datasetDesignPatterns = DesignPatternStatAggregator.aggregateStats(
                DatasetFactory.getAll( type ) );
        final List<Dataset> datasetsWithPatterns = datasetDesignPatterns.values()
                .stream()
                .map( DatasetDesignPatterns::getDataset )
                .toList();
        log.info( "Datasets with patterns size={})", datasetsWithPatterns.size() );
        return datasetsWithPatterns;
    }

    private static void test() {

        final Dataset dataset = Dataset.of( DatasetLanguage.JAVA, DatasetType.JAVA_PATTERNS, "abstract-factory" );
        //final Dataset dataset = Dataset.of( DatasetLanguage.CPP, DatasetType.CPP_PATTERNS, "abstract-factory" );
        //final Dataset dataset = Dataset.of( DatasetLanguage.PYTHON, DatasetType.PYTHON_PATTERNS, "abstract-factory" );

        final PipeContext ctx = PipeContext.empty();
        ctx.set( PipeContext.CPG_DATASET_KEY, dataset );
        ctx.set( PipeContext.CPG_DEPTH_KEY, 10 );
        ctx.set( PipeContext.CPG_REPOSITORY_PURGE_KEY, true );

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

                // persist
                .add( PersistCpgModule.instance() )
                .build();

        final Graph cpg = pipe.process( dataset, ctx );

        log.info( "CPG: nodes {} / edges {}", cpg.getNodeCount(), cpg.getEdgeCount() );
    }

}