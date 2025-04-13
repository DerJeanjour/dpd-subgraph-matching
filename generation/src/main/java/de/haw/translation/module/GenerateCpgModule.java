package de.haw.translation.module;

import de.fraunhofer.aisec.cpg.*;
import de.fraunhofer.aisec.cpg.frontends.Language;
import de.fraunhofer.aisec.cpg.frontends.cxx.CPPLanguage;
import de.fraunhofer.aisec.cpg.frontends.java.JavaLanguage;
import de.fraunhofer.aisec.cpg.frontends.python.PythonLanguage;
import de.fraunhofer.aisec.cpg.passes.ControlDependenceGraphPass;
import de.fraunhofer.aisec.cpg.passes.ProgramDependenceGraphPass;
import de.haw.dataset.model.Dataset;
import de.haw.misc.pipe.PipeContext;
import de.haw.misc.pipe.PipeModule;
import kotlin.jvm.JvmClassMappingKt;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

@Slf4j
@NoArgsConstructor( staticName = "instance" )
public class GenerateCpgModule<Target> extends PipeModule<File, TranslationResult, Target> {

    @Override
    protected TranslationResult processImpl( final File sourceFile, final PipeContext ctx ) {

        final Dataset dataset = ctx.get( PipeContext.CPG_DATASET_KEY, Dataset.class )
                .orElseThrow( IllegalArgumentException::new );
        log.info( "Translating source file {} of dataset {} ...", sourceFile.getName(), dataset );

        final InferenceConfiguration inferenceConfiguration = InferenceConfiguration.builder().enabled( true ).build();
        TranslationConfiguration translationConfiguration = null;

        try {
            translationConfiguration = new TranslationConfiguration.Builder().inferenceConfiguration(
                            inferenceConfiguration )
                    .defaultPasses()
                    .registerPass( JvmClassMappingKt.getKotlinClass( ControlDependenceGraphPass.class ) )
                    .registerPass( JvmClassMappingKt.getKotlinClass( ProgramDependenceGraphPass.class ) )
                    //.registerPass( JvmClassMappingKt.getKotlinClass( PrepareSerialization.class ) ) // AST but is expensive
                    .registerLanguage( getLanguage( dataset ) )
                    .addIncludesToGraph( false )
                    .loadIncludes( false )
                    .sourceLocations( sourceFile )
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

    private static Language<?> getLanguage( final Dataset dataset ) {
        return switch ( dataset.getLanguage() ) {
            case JAVA -> new JavaLanguage();
            case CPP -> new CPPLanguage();
            case PYTHON -> new PythonLanguage();
            default -> throw new IllegalArgumentException( "Unknown language: " + dataset.getLanguage() );
        };
    }

}
