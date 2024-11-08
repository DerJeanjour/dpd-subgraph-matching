package de.haw.processing.cpg;

import de.fraunhofer.aisec.cpg.TranslationResult;
import de.haw.datasets.Dataset;
import de.haw.processing.cpg.module.CpgGenerateModule;
import de.haw.processing.cpg.module.CpgToGraphModule;
import de.haw.processing.pipe.PipeBuilder;
import de.haw.processing.pipe.PipeModule;
import org.graphstream.graph.Graph;

public class CpgProcessor {

    private final PipeModule<Dataset, ?, Graph> pipe;

    public CpgProcessor() {
        this.pipe = PipeBuilder.<Dataset, Graph>builder()
                .add( CpgGenerateModule.instance() )
                .add( CpgToGraphModule.instance() )
                .build();
    }

    public Graph run( final Dataset dataset ) {
        return this.pipe.process( dataset );
    }
}
