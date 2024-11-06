package de.haw.processing;

import de.haw.datasets.Dataset;
import de.haw.processing.processor.CpgProcessor;

import java.util.List;


public class CpgPipe implements CpgProcessor<Dataset, Void> {

    private List<CpgProcessor<?, ?>> processors;

    @Override
    public Void process( final Dataset dataset ) {
        // TODO
        return null;
    }
}
