package de.haw;

import de.fraunhofer.aisec.cpg.TranslationResult;
import de.haw.datasets.Dataset;
import de.haw.processing.CpgPipe;
import de.haw.processing.modules.CpcPersistence;
import de.haw.processing.modules.CpgGenerator;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Main {

    public static void main( String[] args ) {

        final CpgPipe pipe = CpgPipe.Builder.builder()
                .add( CpgGenerator.instance() )
                .add( CpcPersistence.instance() )
                .build();

        final TranslationResult result = pipe.getResult( Dataset.ANIMAL );
        log.info( "Output: {}", result.getBenchmarks() );
    }

}