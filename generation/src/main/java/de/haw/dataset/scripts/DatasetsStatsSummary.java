package de.haw.dataset.scripts;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvBindByPosition;
import de.haw.dataset.DesignPatternLoader;
import de.haw.dataset.DesignPatternStatAggregator;
import de.haw.dataset.model.*;
import de.haw.misc.utils.CsvUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class DatasetsStatsSummary {

    @Data
    public static class DatasetStatCsv {

        @CsvBindByPosition( position = 0 )
        @CsvBindByName( column = "dataset" )
        private String dataset;

        @CsvBindByPosition( position = 1 )
        @CsvBindByName( column = "dp_names" )
        private String dpNames;

        @CsvBindByPosition( position = 2 )
        @CsvBindByName( column = "dps_unique" )
        private int dpsUnique;

        private Set<DesignPatternType> dpsTypes;

        @CsvBindByPosition( position = 3 )
        @CsvBindByName( column = "dps_total" )
        private long dpsTotal;

        @CsvBindByPosition( position = 4 )
        @CsvBindByName( column = "dps_abstract_factory" )
        private long dpsAbstractFactory;

        @CsvBindByPosition( position = 5 )
        @CsvBindByName( column = "dps_adapter" )
        private long dpsAdapter;

        @CsvBindByPosition( position = 6 )
        @CsvBindByName( column = "dps_builder" )
        private long dpsBuilder;

        @CsvBindByPosition( position = 7 )
        @CsvBindByName( column = "dps_facade" )
        private long dpsFacade;

        @CsvBindByPosition( position = 8 )
        @CsvBindByName( column = "dps_factory_method" )
        private long dpsFactoryMethod;

        @CsvBindByPosition( position = 9 )
        @CsvBindByName( column = "dps_observer" )
        private long dpsObserver;

        @CsvBindByPosition( position = 10 )
        @CsvBindByName( column = "dps_singleton" )
        private long dpsSingleton;

        @CsvBindByPosition( position = 11 )
        @CsvBindByName( column = "dps_decorator" )
        private long dpsDecorator;

        @CsvBindByPosition( position = 12 )
        @CsvBindByName( column = "dps_memento" )
        private long dpsMemento;

        @CsvBindByPosition( position = 13 )
        @CsvBindByName( column = "dps_prototype" )
        private long dpsPrototype;

        @CsvBindByPosition( position = 14 )
        @CsvBindByName( column = "dps_proxy" )
        private long dpsProxy;

        @CsvBindByPosition( position = 15 )
        @CsvBindByName( column = "dps_visitor" )
        private long dpsVisitor;

    }

    private static final String CSV_FILE_NAME = "dataset_stats.csv";

    public static void main( String[] args ) {
        writeSummaries( DatasetType.P_MART, true );
    }

    private static void writeSummaries( final DatasetType datasetType, final boolean allowAllRoles ) {
        final List<Dataset> datasets = DatasetFactory.getAll( datasetType );
        final Map<String, DatasetDesignPatterns> stats = DesignPatternStatAggregator.aggregateStats( datasets );
        final List<DatasetStatCsv> csvStats = new ArrayList<>();

        // dataset entries
        stats.forEach( ( dataset, datasetDps ) -> {

            final Map<DesignPatternType, List<DesignPattern>> dps = datasetDps.getPatterns();

            final DatasetStatCsv stat = new DatasetStatCsv();
            stat.setDataset( dataset );
            stat.setDpsTypes( dps.keySet() );
            stat.setDpsUnique( dps.keySet().size() );
            stat.setDpNames( toDpNames( dps.keySet() ) );

            dps.forEach( ( type, typeDps ) -> {
                final long count = typeDps.stream().filter( dp -> dp.isMajorRole() || allowAllRoles ).count();
                setStat( stat, type, count );
                stat.setDpsTotal( stat.getDpsTotal() + count );
            } );
            csvStats.add( stat );
        } );

        // total
        final DatasetStatCsv totalStat = new DatasetStatCsv();
        totalStat.setDataset( "all" );
        totalStat.setDpsTypes( csvStats.stream()
                .map( DatasetStatCsv::getDpsTypes )
                .flatMap( Set::stream )
                .collect( Collectors.toSet() ) );
        totalStat.setDpsUnique( totalStat.getDpsTypes().size() );
        totalStat.setDpNames( toDpNames( totalStat.getDpsTypes() ) );
        totalStat.setDpsTotal( csvStats.stream().mapToLong( DatasetStatCsv::getDpsTotal ).sum() );
        totalStat.setDpsAbstractFactory( csvStats.stream().mapToLong( DatasetStatCsv::getDpsAbstractFactory ).sum() );
        totalStat.setDpsAdapter( csvStats.stream().mapToLong( DatasetStatCsv::getDpsAdapter ).sum() );
        totalStat.setDpsBuilder( csvStats.stream().mapToLong( DatasetStatCsv::getDpsBuilder ).sum() );
        totalStat.setDpsFacade( csvStats.stream().mapToLong( DatasetStatCsv::getDpsFacade ).sum() );
        totalStat.setDpsFactoryMethod( csvStats.stream().mapToLong( DatasetStatCsv::getDpsFactoryMethod ).sum() );
        totalStat.setDpsObserver( csvStats.stream().mapToLong( DatasetStatCsv::getDpsObserver ).sum() );
        totalStat.setDpsSingleton( csvStats.stream().mapToLong( DatasetStatCsv::getDpsSingleton ).sum() );
        totalStat.setDpsDecorator( csvStats.stream().mapToLong( DatasetStatCsv::getDpsDecorator ).sum() );
        totalStat.setDpsMemento( csvStats.stream().mapToLong( DatasetStatCsv::getDpsMemento ).sum() );
        totalStat.setDpsPrototype( csvStats.stream().mapToLong( DatasetStatCsv::getDpsPrototype ).sum() );
        totalStat.setDpsProxy( csvStats.stream().mapToLong( DatasetStatCsv::getDpsProxy ).sum() );
        totalStat.setDpsVisitor( csvStats.stream().mapToLong( DatasetStatCsv::getDpsVisitor ).sum() );
        csvStats.add( 0, totalStat );

        saveToCsv( csvStats, getCsvPath( datasetType ) );
    }

    private static String toDpNames( final Set<DesignPatternType> types ) {
        return StringUtils.join( types.stream().map( type -> "[" + type.getName() + "]" ).toList(), " " );
    }

    private static void setStat( final DatasetStatCsv stat, final DesignPatternType type, final long count ) {
        switch ( type ) {
            case ABSTRACT_FACTORY -> stat.setDpsAbstractFactory( count );
            case ADAPTER -> stat.setDpsAdapter( count );
            case BUILDER -> stat.setDpsBuilder( count );
            case FACADE -> stat.setDpsFacade( count );
            case FACTORY_METHOD -> stat.setDpsFactoryMethod( count );
            case OBSERVER -> stat.setDpsObserver( count );
            case SINGLETON -> stat.setDpsSingleton( count );
            case DECORATOR -> stat.setDpsDecorator( count );
            case MEMENTO -> stat.setDpsMemento( count );
            case PROTOTYPE -> stat.setDpsPrototype( count );
            case PROXY -> stat.setDpsProxy( count );
            case VISITOR -> stat.setDpsVisitor( count );
        }
    }

    private static void saveToCsv( final List<DatasetStatCsv> csvStats, final String csvPath ) {
        final byte[] csv = CsvUtils.write( csvStats, DatasetStatCsv.class );
        try ( FileOutputStream fileOutputStream = new FileOutputStream( csvPath ) ) {
            fileOutputStream.write( csv );
        } catch ( IOException e ) {
            log.info( "Failed to write csv: {}", e.getMessage() );
        }
    }

    private static String getCsvPath( final DatasetType datasetType ) {
        return switch ( datasetType ) {
            case P_MART -> DesignPatternLoader.BASE_PATH_P_MART + CSV_FILE_NAME;
            case DPDf -> DesignPatternLoader.BASE_PATH_DPDf_EXAMPLE + CSV_FILE_NAME;
            default -> DesignPatternLoader.BASE_PATH_JAVA + CSV_FILE_NAME;
        };
    }

}
