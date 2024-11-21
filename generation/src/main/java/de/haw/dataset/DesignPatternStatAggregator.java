package de.haw.dataset;

import de.haw.dataset.model.Dataset;
import de.haw.dataset.model.DatasetDesignPatterns;
import de.haw.dataset.module.BulkDesignPatternLoading;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Slf4j
public class DesignPatternStatAggregator {

    public static Map<String, Integer> aggregateStats( final List<Dataset> datasets ) {

        final Map<String, Integer> aggregated = new TreeMap<>();
        final Map<String, DatasetDesignPatterns> datasetDesignPatterns = new HashMap<>();
        final Map<String, Map<String, Integer>> datasetStats = new HashMap<>();

        final List<DatasetDesignPatterns> datasetsPatterns = BulkDesignPatternLoading.load( datasets );
        for ( DatasetDesignPatterns datasetPatterns : datasetsPatterns ) {

            final Dataset dataset = datasetPatterns.getDataset();
            final Map<String, Integer> stats = datasetPatterns.getStats();

            datasetStats.put( dataset.getName(), stats );
            datasetDesignPatterns.put( dataset.getName(), datasetPatterns );

            stats.forEach( ( key, value ) -> {
                if ( aggregated.containsKey( key ) ) {
                    int incrementedValue = aggregated.get( key ) + value;
                    aggregated.put( key, incrementedValue );
                    return;
                }
                aggregated.put( key, value );
            } );
        }

        return aggregated;
    }

}
