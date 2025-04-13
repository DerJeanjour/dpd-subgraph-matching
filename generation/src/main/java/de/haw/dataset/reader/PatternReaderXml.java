package de.haw.dataset.reader;

import de.haw.dataset.model.Dataset;
import de.haw.dataset.model.DatasetDesignPatterns;
import de.haw.dataset.model.DesignPattern;
import de.haw.dataset.model.DesignPatternType;
import de.haw.misc.utils.NameUtils;
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
        put( DesignPatternType.ABSTRACT_FACTORY, Arrays.asList( "concreteFactory" ) );
        put( DesignPatternType.ADAPTER, Arrays.asList( "adapter" ) );
        put( DesignPatternType.BUILDER, Arrays.asList( "concreteBuilder" ) );
        put( DesignPatternType.FACADE, Arrays.asList( "facade" ) );
        put( DesignPatternType.FACTORY_METHOD, Arrays.asList( "concreteCreator" ) );
        put( DesignPatternType.OBSERVER, Arrays.asList( "concreteObserver" ) );
        put( DesignPatternType.SINGLETON, Arrays.asList( "singleton" ) );
        put( DesignPatternType.DECORATOR, Arrays.asList( "concreteDecorator" ) );
        put( DesignPatternType.MEMENTO, Arrays.asList( "memento" ) );
        put( DesignPatternType.PROTOTYPE, Arrays.asList( "concretePrototype" ) );
        put( DesignPatternType.PROXY, Arrays.asList( "proxy" ) );
        put( DesignPatternType.VISITOR, Arrays.asList( "concreteVisitor" ) );
    }};

    @Override
    public List<DatasetDesignPatterns> read( final List<Dataset> datasets, final File file ) {

        final Document xml = this.readFile( file );
        return this.read( datasets, xml );
    }

    public List<DatasetDesignPatterns> read( final List<Dataset> datasets, final Document xml ) {
        final List<DatasetDesignPatterns> datasetsPatterns = new ArrayList<>();
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

    public void findPatternsFor( final DatasetDesignPatterns datasetPatterns, final Element program ) {
        for ( final Element pattern : getChilds( program, "designPattern" ) ) {

            final String patternName = pattern.getAttribute( "name" );
            final Optional<DesignPatternType> patternType = this.mapPatternType( patternName );
            if ( patternType.isEmpty() ) {
                continue;
            }

            for ( final Element patternInstance : getChilds( pattern, "microArchitecture" ) ) {
                final String instanceId = patternInstance.getAttribute( "number" );
                this.getPatternsFromEntities( patternType.get(), instanceId, getChilds( patternInstance, "entity" ) )
                        .forEach( datasetPatterns::add );
            }

        }
    }

    public List<DesignPattern> getPatternsFromEntities(
            final DesignPatternType type, final String instanceId, final List<Element> entities ) {
        final List<DesignPattern> patterns = new ArrayList<>();
        for ( final Element entity : entities ) {

            final Element role = ( Element ) entity.getParentNode();
            final String roleTag = role.getNodeName();
            final String className = NameUtils.extractClassName( entity.getTextContent() );

            if ( !ROLES_TO_COUNT.containsKey( type ) ) {
                continue;
            }

            final boolean isMajor = ROLES_TO_COUNT.get( type ).isEmpty() || ROLES_TO_COUNT.get( type )
                    .contains( roleTag );
            patterns.add( DesignPattern.of( instanceId, type, className, roleTag, isMajor ) );
        }
        return patterns;
    }

    public List<Element> getChilds( final Element element, final String tag ) {
        final List<Element> nodes = new ArrayList<>();
        final NodeList childs = element.getElementsByTagName( tag );
        for ( int i = 0; i < childs.getLength(); i++ ) {
            nodes.add( ( Element ) childs.item( i ) );
        }
        return nodes;
    }

    public Optional<DesignPatternType> mapPatternType( final String patternName ) {
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
