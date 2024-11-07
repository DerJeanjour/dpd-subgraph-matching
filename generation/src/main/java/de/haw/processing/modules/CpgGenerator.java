package de.haw.processing.modules;

import de.fraunhofer.aisec.cpg.*;
import de.fraunhofer.aisec.cpg.frontends.java.JavaLanguage;
import de.haw.datasets.Dataset;
import de.haw.datasets.DatasetLoader;
import de.haw.processing.CpgProcessor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

@Slf4j
@NoArgsConstructor( staticName = "instance" )
public class CpgGenerator implements CpgProcessor {

    @Override
    public Object process( Object input ) {

        final Dataset dataset = ( Dataset ) input;

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

}
