package de.haw.dataset.reader;

import de.haw.dataset.model.Dataset;
import de.haw.dataset.model.DatasetDesignPatterns;
import de.haw.dataset.model.DesignPatterType;
import de.haw.dataset.model.DesignPattern;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Slf4j
@NoArgsConstructor( staticName = "instance" )
public class PatternReaderXml implements PatternReader {

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
            final Optional<DesignPatterType> patternType = this.mapPatternType( patternName );
            if ( patternType.isEmpty() ) {
                continue;
            }

            for ( final Element patternInstance : getChilds( pattern, "microArchitecture" ) ) {
                final String instanceId = patternInstance.getAttribute( "number" );
                for ( final Element entity : getChilds( patternInstance, "entity" ) ) {

                    final Element role = ( Element ) entity.getParentNode();
                    final String roleTag = role.getNodeName();
                    final String className = entity.getTextContent();

                    final DesignPattern designPattern = DesignPattern.of( patternType.get(), className );
                    datasetPatterns.add( designPattern );

                }
            }

        }
    }

    private List<Element> getChilds( final Element element, final String tag ) {
        final List<Element> nodes = new ArrayList<>();
        final NodeList childs = element.getElementsByTagName( tag );
        for ( int i = 0; i < childs.getLength(); i++ ) {
            nodes.add( ( Element ) childs.item( i ) );
        }
        return nodes;
    }

    private Optional<DesignPatterType> mapPatternType( final String patternName ) {
        return Arrays.stream( DesignPatterType.values() )
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
