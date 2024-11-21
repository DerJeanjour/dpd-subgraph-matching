package de.haw.dataset.module;

import de.haw.dataset.DesignPatternLoader;
import de.haw.dataset.model.Dataset;
import de.haw.dataset.model.DatasetDesignPatterns;
import de.haw.dataset.model.DatasetType;
import de.haw.dataset.reader.PatternReader;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class BulkDesignPatternLoading {

    public static List<DatasetDesignPatterns> load( final List<Dataset> datasets ) {

        final List<DatasetDesignPatterns> datasetDps = new ArrayList<>();
        final Set<DatasetType> types = datasets.stream().map( Dataset::getType ).collect( Collectors.toSet() );

        for ( DatasetType type : types ) {
            final List<Dataset> datasetsByType = datasets.stream().filter( ds -> ds.getType().equals( type ) ).toList();
            final File patternFile = DesignPatternLoader.load( type );
            final PatternReader reader = ReadPatternsModule.getReader( patternFile );
            datasetDps.addAll( reader.read( datasetsByType, patternFile ) );
        }

        return datasetDps;
    }

}
