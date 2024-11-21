package de.haw.dataset.reader;

import de.haw.dataset.Dataset;
import de.haw.dataset.model.CsvDesignPattern;
import de.haw.dataset.model.DatasetDesignPatterns;
import de.haw.dataset.model.DesignPatterType;
import de.haw.dataset.model.DesignPattern;
import de.haw.misc.utils.CsvUtils;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@NoArgsConstructor( staticName = "instance" )
public class PatternReaderCsv implements PatternReader {
    @Override
    public DatasetDesignPatterns read( final Dataset dataset, final File file ) {

        final DatasetDesignPatterns datasetPatterns = DatasetDesignPatterns.of( dataset );

        final List<CsvDesignPattern> entries;
        try {
            entries = new ArrayList<>( CsvUtils.read( FileUtils.readFileToByteArray( file ), CsvDesignPattern.class ) );
        } catch ( IOException e ) {
            log.error( "Could not read csv file ..." );
            throw new RuntimeException( e );
        }

        entries.forEach( entry -> {
            if( !entry.getProjectName().equals( dataset.getName() ) ) {
                return;
            }
            final DesignPatterType type = this.mapPatternType( entry.getPatternName() );
            if ( type == null ) {
                return;
            }
            datasetPatterns.add( DesignPattern.of( type, entry.getClassName() ) );
        } );

        return datasetPatterns;
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
