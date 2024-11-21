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

            final DesignPatterType type = this.mapPatternType( entry.getPatternName() );
            if ( type == null ) {
                return;
            }
            datasetPatterns.add( DesignPattern.of( type, entry.getClassName() ) );
        } );


        return datasetDesignPatterns.values().stream().toList();
    }

    private DesignPatterType mapPatternType( final String patternName ) {
        return switch ( patternName ) {
            case "AbstractFactory" -> DesignPatterType.ABSTRACT_FACTORY;
            case "Builder" -> DesignPatterType.BUILDER;
            case "Observer" -> DesignPatterType.OBSERVER;
            case "Singleton" -> DesignPatterType.SINGLETON;
            case "Adapter" -> DesignPatterType.ADAPTER;
            case "FactoryMethod" -> DesignPatterType.FACTORY_METHOD;
            case "Visitor" -> DesignPatterType.VISITOR;
            case "Decorator" -> DesignPatterType.DECORATOR;
            case "Prototype" -> DesignPatterType.PROTOTYPE;
            case "Facade" -> DesignPatterType.FACADE;
            case "Memento" -> DesignPatterType.MEMENTO;
            case "Proxy" -> DesignPatterType.PROXY;
            default -> null;
        };
    }

}
