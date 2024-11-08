package de.haw.processing.cpg;

import de.fraunhofer.aisec.cpg.TranslationResult;
import de.haw.datasets.Dataset;
import de.haw.processing.cpg.module.CpgGenerateModule;
import de.haw.processing.cpg.module.CpgPersistModule;
import de.haw.processing.pipe.PipeBuilder;
import de.haw.processing.pipe.PipeModule;

public class CpgProcessor {

    private final PipeModule<Dataset, ?, TranslationResult> pipe;

    public CpgProcessor() {
        this.pipe = PipeBuilder.<Dataset, TranslationResult>builder()
                .add( CpgGenerateModule.instance() )
                .add( CpgPersistModule.instance() )
                .build();
    }

    public TranslationResult run( final Dataset dataset ) {
        return this.pipe.process( dataset );
    }
}
