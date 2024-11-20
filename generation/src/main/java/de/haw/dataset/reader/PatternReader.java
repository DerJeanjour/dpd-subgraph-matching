package de.haw.dataset.reader;

import de.haw.dataset.Dataset;
import de.haw.dataset.model.DatasetDesignPatterns;

import java.io.File;

public interface PatternReader {

    DatasetDesignPatterns read( final Dataset dataset, final File file );

}
