package de.haw.translation.module;

import de.fraunhofer.aisec.cpg.*;
import de.fraunhofer.aisec.cpg.frontends.java.JavaLanguage;
import de.fraunhofer.aisec.cpg.passes.ControlDependenceGraphPass;
import de.fraunhofer.aisec.cpg.passes.ProgramDependenceGraphPass;
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

        log.info( "Translating source file {} of dataset {} ...", sourceFile.getName(),
                ctx.get( PipeContext.CPG_DATASET_KEY ) );

        final InferenceConfiguration inferenceConfiguration = InferenceConfiguration.builder().enabled( true ).build();
        TranslationConfiguration translationConfiguration = null;
        try {
            //.addIncludesToGraph( false )
            //.loadIncludes( false )
            translationConfiguration = new TranslationConfiguration.Builder().inferenceConfiguration(
                            inferenceConfiguration )
                    .defaultPasses()
                    //.registerPass( JvmClassMappingKt.getKotlinClass( ControlDependenceGraphPass.class ) )
                    //.registerPass( JvmClassMappingKt.getKotlinClass( ProgramDependenceGraphPass.class ) )
                    .registerLanguage( new JavaLanguage() )
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

        /*
        Map<String, Integer> nextCDGsCounts = new HashMap<>();
        Map<String, Integer> nextEOGsCounts = new HashMap<>();
        Map<String, Integer> nextDFGsCounts = new HashMap<>();
        Map<String, Integer> nextPDGsCounts = new HashMap<>();
        Map<String, Integer> nextASTsCounts = new HashMap<>();
        for( final TranslationUnitDeclaration unitDeclaration : result.getComponents().get( 0 ).getTranslationUnits() ) {
            final String name = unitDeclaration.getLocation().toString();
            final List<PropertyEdge<Node>> nextCDGs = unitDeclaration.getNextCDGEdges();
            nextCDGsCounts.put( name, nextCDGs.size() );
            final List<PropertyEdge<Node>> nextEOGs = unitDeclaration.getNextEOGEdges();
            nextEOGsCounts.put( name, nextEOGs.size() );
            final Set<Node> nextDFGs = unitDeclaration.getNextDFG();
            nextDFGsCounts.put( name, nextDFGs.size() );
            final Set<Node> nextPDGs = unitDeclaration.getNextPDG();
            nextPDGsCounts.put( name, nextPDGs.size() );
            final List<Node> nextASTs = unitDeclaration.getAstChildren();
            nextASTsCounts.put( name, nextASTs.size() );
        }

         */


        return result;
    }

}
