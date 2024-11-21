package de.haw.dataset.reader;

import de.haw.dataset.model.Dataset;
import de.haw.dataset.model.DatasetDesignPatterns;
import de.haw.dataset.model.DesignPattern;
import de.haw.dataset.model.DesignPatternType;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.*;

@Slf4j
@NoArgsConstructor( staticName = "instance" )
public class PatternReaderXml implements PatternReader {

    private final static Map<DesignPatternType, List<String>> ROLES_TO_COUNT = new HashMap<>() {{
        // add maybe concreteFactory, abstractProduct, products ???
        //put( DesignPatternType.ABSTRACT_FACTORY, Arrays.asList( "abstractFactory") );
        put( DesignPatternType.ABSTRACT_FACTORY, Collections.emptyList() );
        // add maybe adaptee???
        //put( DesignPatternType.ADAPTER, Arrays.asList( "adapter" ) );
        put( DesignPatternType.ADAPTER, Collections.emptyList() );
        // add maybe concreteBuilder???
        //put( DesignPatternType.BUILDER, Arrays.asList( "builder" ) );
        put( DesignPatternType.BUILDER, Collections.emptyList() );
        //put( DesignPatternType.FACADE, Arrays.asList( "facade" ) );
        put( DesignPatternType.FACADE, Collections.emptyList() );
        // add maybe concreteCreator???
        //put( DesignPatternType.FACTORY_METHOD, Arrays.asList( "creator" ) );
        put( DesignPatternType.FACTORY_METHOD, Collections.emptyList() );
        //put( DesignPatternType.OBSERVER, Arrays.asList( "observer" ) );
        put( DesignPatternType.OBSERVER, Collections.emptyList() );
        //put( DesignPatternType.SINGLETON, Arrays.asList( "singleton" ) );
        put( DesignPatternType.SINGLETON, Collections.emptyList() );
    }};

    @Override
    public List<DatasetDesignPatterns> read( final List<Dataset> datasets, final File file ) {

        final List<DatasetDesignPatterns> datasetsPatterns = new ArrayList<>();

        final Document xml = this.readFile( file );
        for ( final Element program : getChilds( xml.getDocumentElement(), "program" ) ) {
            final String programName = program.getElementsByTagName( "name" ).item( 0 ).getTextContent();

            final Optional<Dataset> dataset = datasets.stream()
                    .filter( ds -> ds.getProjectName().equals( programName ) )
                    .findFirst();

            if ( dataset.isEmpty() ) {
                continue;
            }

            final DatasetDesignPatterns datasetPatterns = DatasetDesignPatterns.of( dataset.get() );
            findPatternsFor( datasetPatterns, program );
            datasetsPatterns.add( datasetPatterns );
        }

        return datasetsPatterns;
    }

    private void findPatternsFor( final DatasetDesignPatterns datasetPatterns, final Element program ) {
        for ( final Element pattern : getChilds( program, "designPattern" ) ) {

            final String patternName = pattern.getAttribute( "name" );
            final Optional<DesignPatternType> patternType = this.mapPatternType( patternName );
            if ( patternType.isEmpty() ) {
                continue;
            }

            for ( final Element patternInstance : getChilds( pattern, "microArchitecture" ) ) {
                //final String instanceId = patternInstance.getAttribute( "number" );
                this.getPatternsFromEntities( patternType.get(), getChilds( patternInstance, "entity" ) )
                        .forEach( datasetPatterns::add );
            }

        }
    }

    private List<DesignPattern> getPatternsFromEntities( final DesignPatternType type, final List<Element> entities ) {
        final List<DesignPattern> patterns = new ArrayList<>();
        for ( final Element entity : entities ) {

            final Element role = ( Element ) entity.getParentNode();
            final String roleTag = role.getNodeName();
            final String className = entity.getTextContent();

            if ( !ROLES_TO_COUNT.containsKey( type ) ) {
                continue;
            }

            final boolean allowAll = ROLES_TO_COUNT.get( type ).isEmpty();
            if ( allowAll || ROLES_TO_COUNT.get( type ).contains( roleTag ) ) {
                patterns.add( DesignPattern.of( type, className ) );
            }
        }
        return patterns;
    }

    private List<Element> getChilds( final Element element, final String tag ) {
        final List<Element> nodes = new ArrayList<>();
        final NodeList childs = element.getElementsByTagName( tag );
        for ( int i = 0; i < childs.getLength(); i++ ) {
            nodes.add( ( Element ) childs.item( i ) );
        }
        return nodes;
    }

    private Optional<DesignPatternType> mapPatternType( final String patternName ) {
        return Arrays.stream( DesignPatternType.values() )
                .filter( dpt -> dpt.getName().equals( patternName ) )
                .findFirst();
    }

    private Document readFile( final File file ) {
        try {
            final DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            return docBuilder.parse( file );
        } catch ( Exception e ) {
            log.error( "Couldn't read design pattern file: {} ...", e.getMessage() );
            throw new IllegalStateException( e );
        }
    }

}
