package de.haw.dataset.reader;

import de.haw.dataset.model.Dataset;
import de.haw.dataset.model.DatasetDesignPatterns;

import java.io.File;
import java.util.Collections;
import java.util.List;

public interface PatternReader {

    default DatasetDesignPatterns read( final Dataset dataset, final File file ) {
        return this.read( Collections.singletonList( dataset ), file )
                .stream()
                .findFirst()
                .orElse( DatasetDesignPatterns.of( dataset ) );
    }

    List<DatasetDesignPatterns> read( final List<Dataset> datasets, final File file );

}
