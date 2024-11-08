package de.haw;

import de.fraunhofer.aisec.cpg.TranslationResult;
import de.haw.datasets.Dataset;
import de.haw.processing.cpg.CpgProcessor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Main {

    public static void main( String[] args ) {
        final CpgProcessor processor = new CpgProcessor();
        final TranslationResult result = processor.run( Dataset.ANIMAL );
        log.info( "Output: {}", result.getBenchmarks() );
    }

}