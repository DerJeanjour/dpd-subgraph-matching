package de.haw.dataset;

import de.haw.dataset.model.DatasetDesignPatterns;
import de.haw.dataset.module.LoadPatternFileModule;
import de.haw.dataset.module.ReadPatternsModule;
import de.haw.misc.pipe.PipeBuilder;
import de.haw.misc.pipe.PipeContext;
import de.haw.misc.pipe.PipeModule;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

@Slf4j
public class DesignPatternStatAggregator {

    public static Map<String, Integer> aggregateStats() {

        final Map<String, Integer> aggregated = new TreeMap<>();

        final Map<Dataset, DatasetDesignPatterns> datasetDesignPatterns = new HashMap<>();

        final Map<Dataset, Map<String, Integer>> datasetStats = new HashMap<>();

        for ( final Dataset dataset : Dataset.values() ) {

            final PipeContext ctx = PipeContext.empty();
            ctx.set( PipeContext.CPG_DATASET_KEY, dataset );
            final PipeModule<Dataset, ?, DatasetDesignPatterns> pipe = PipeBuilder.<Dataset, DatasetDesignPatterns>builder()
                    .add( LoadPatternFileModule.instance() )
                    .add( ReadPatternsModule.instance() )
                    .build();

            final DatasetDesignPatterns datasetPatterns = pipe.process( dataset, ctx );
            if ( datasetPatterns == null ) {
                continue;
            }

            final Map<String, Integer> stats = datasetPatterns.getStats();
            datasetStats.put( dataset, stats );
            datasetDesignPatterns.put( dataset, datasetPatterns );

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
