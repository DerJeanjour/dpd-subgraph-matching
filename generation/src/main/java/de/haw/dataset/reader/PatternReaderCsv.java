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

        final List<CsvDesignPattern> entries;
        try {
            entries = new ArrayList<>( CsvUtils.read( FileUtils.readFileToByteArray( file ), CsvDesignPattern.class ) );
        } catch ( IOException e ) {
            log.error( "Could not read csv file ..." );
            throw new RuntimeException( e );
        }

        final Map<String, DatasetDesignPatterns> datasetDesignPatterns = new HashMap<>();
        entries.forEach( entry -> {

            final Optional<Dataset> dataset = datasets.stream()
                    .filter( ds -> ds.getProjectName().equals( entry.getProjectName() ) )
                    .findFirst();
            if ( dataset.isEmpty() ) {
                return;
            }

            if ( !datasetDesignPatterns.containsKey( dataset.get().getName() ) ) {
                datasetDesignPatterns.put( dataset.get().getName(), DatasetDesignPatterns.of( dataset.get() ) );
            }
            final DatasetDesignPatterns datasetPatterns = datasetDesignPatterns.get( dataset.get().getName() );

            final DesignPatternType type = this.mapPatternType( entry.getPatternName() );
            if ( type == null ) {
                return;
            }
            datasetPatterns.add( DesignPattern.of( type, entry.getClassName() ) );
        } );


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

}
