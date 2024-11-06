package de.haw;

import de.fraunhofer.aisec.cpg.*;
import de.fraunhofer.aisec.cpg.frontends.java.JavaLanguage;
import de.fraunhofer.aisec.cpg_vis_neo4j.Application;
import de.haw.datasets.Dataset;
import de.haw.datasets.DatasetLoader;
import de.haw.utils.ReflectionUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.net.ConnectException;

@Slf4j
public class Main {

    public static void main( String[] args ) {
        final TranslationResult result = parseDataset( Dataset.ANIMAL );
        persistResult( result );
    }

    private static TranslationResult parseDataset( final Dataset dataset ) {

        log.info( "Parsing dataset {} ...", dataset );

        final File file = DatasetLoader.load( dataset );
        log.info( "Looking up project file: {}", file.getName() );

        final InferenceConfiguration inferenceConfiguration = InferenceConfiguration.builder().enabled( true ).build();
        TranslationConfiguration translationConfiguration = null;
        try {
            translationConfiguration = new TranslationConfiguration.Builder().inferenceConfiguration(
                            inferenceConfiguration )
                    .defaultPasses()
                    .registerLanguage( new JavaLanguage() )
                    .sourceLocations( file )
                    .build();
        } catch ( ConfigurationException e ) {
            log.error( "Failed config: {}", e.getMessage() );
        }

        TranslationResult result = null;
        if ( translationConfiguration != null ) {

            try {
                result = TranslationManager.builder().config( translationConfiguration ).build().analyze().get();

                result.getBenchmarks()
                        .forEach( benchmark -> log.info( "Benchmark: {} - {} - {}", benchmark.getCaller(),
                                benchmark.getMessage(), benchmark.getMeasurements() ) );
                result.getTranslatedFiles().forEach( translated -> log.info( "Translated: {}", translated ) );

            } catch ( Exception e ) {
                log.error( "Failed result: {}", e.getMessage() );
            }
        }

        return result;
    }

    private static void persistResult( final TranslationResult result ) {

        log.info( "Persisting parsed results ..." );

        if ( result == null ) {
            throw new IllegalArgumentException( "Can't persist null result!" );
        }
        final Application neo4j = new Application();
        neo4j.setNeo4jUsername( "neo4j" );
        neo4j.setNeo4jPassword( "password" );
        ReflectionUtils.setInt( neo4j, "depth", 10 );

        try {
            neo4j.pushToNeo4j( result );
        } catch ( InterruptedException | ConnectException e ) {
            throw new RuntimeException( e );
        }
    }

}