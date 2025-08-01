package de.haw.dataset.reader;

import de.haw.dataset.model.*;
import de.haw.misc.utils.CsvUtils;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Slf4j
@NoArgsConstructor( staticName = "instance" )
public class PatternReaderCsv implements PatternReader {

    @Override
    public List<DatasetDesignPatterns> read( final List<Dataset> datasets, final File file ) {
        final List<CsvDesignPattern> entries = this.getEntries( file );
        return this.read( datasets, entries );
    }

    public List<DatasetDesignPatterns> read( final List<Dataset> datasets, final List<CsvDesignPattern> entries ) {
        final Map<String, DatasetDesignPatterns> datasetDesignPatterns = new HashMap<>();
        for ( int i = 0; i < entries.size(); i++ ) {
            final CsvDesignPattern entry = entries.get( i );
            final Optional<Dataset> dataset = datasets.stream()
                    .filter( ds -> ds.getProjectName().equals( entry.getProjectName() ) )
                    .findFirst();
            if ( dataset.isEmpty() ) {
                continue;
            }

            if ( !datasetDesignPatterns.containsKey( dataset.get().getName() ) ) {
                datasetDesignPatterns.put( dataset.get().getName(), DatasetDesignPatterns.of( dataset.get() ) );
            }
            final DatasetDesignPatterns datasetPatterns = datasetDesignPatterns.get( dataset.get().getName() );

            final DesignPatternType type = this.mapPatternType( entry.getPatternName() );
            if ( type == null ) {
                continue;
            }

            final String instanceId = String.valueOf( i );
            datasetPatterns.add( DesignPattern.of( instanceId, type, entry.getClassName(), "instance", true ) );
        }

        return datasetDesignPatterns.values().stream().toList();
    }

    private DesignPatternType mapPatternType( final String patternName ) {
        return switch ( patternName ) {
            case "AbstractFactory" -> DesignPatternType.ABSTRACT_FACTORY;
            case "Builder" -> DesignPatternType.BUILDER;
            case "Observer" -> DesignPatternType.OBSERVER;
            case "Singleton" -> DesignPatternType.SINGLETON;
            case "Adapter" -> DesignPatternType.ADAPTER;
            case "FactoryMethod" -> DesignPatternType.FACTORY_METHOD;
            case "Visitor" -> DesignPatternType.VISITOR;
            case "Decorator" -> DesignPatternType.DECORATOR;
            case "Prototype" -> DesignPatternType.PROTOTYPE;
            case "Facade" -> DesignPatternType.FACADE;
            case "Memento" -> DesignPatternType.MEMENTO;
            case "Proxy" -> DesignPatternType.PROXY;
            default -> null;
        };
    }

    public List<CsvDesignPattern> getEntries( final File file ) {
        try {
            return new ArrayList<>( CsvUtils.read( FileUtils.readFileToByteArray( file ), CsvDesignPattern.class ) );
        } catch ( IOException e ) {
            log.error( "Could not read csv file ..." );
            throw new RuntimeException( e );
        }
    }

}
