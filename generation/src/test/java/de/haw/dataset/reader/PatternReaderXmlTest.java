package de.haw.dataset.reader;

import de.haw.dataset.model.*;
import de.haw.dataset.reader.PatternReaderXml;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class PatternReaderXmlTest {

    private Document createDocument( String xmlContent ) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        ByteArrayInputStream input = new ByteArrayInputStream( xmlContent.getBytes( StandardCharsets.UTF_8 ) );
        return builder.parse( input );
    }

    @Test
    void testReadReturnsEmptyListWhenNoMatchingDataset() throws Exception {
        String xml = "<root><program><name>NonMatching</name></program></root>";
        Document doc = createDocument( xml );
        Dataset dataset = DatasetFactory.J_UNIT;
        List<DatasetDesignPatterns> result = PatternReaderXml.instance()
                .read( Collections.singletonList( dataset ), doc );
        assertTrue( result.isEmpty() );
    }

    @Test
    void testReadReturnsDatasetDesignPatternsWithMatchingDataset() throws Exception {
        String xml = "<root><program><name>TestProject</name><designPattern name=\"Observer\"><microArchitecture number=\"1\"><concreteObserver><entity>com.example.TestClass</entity></concreteObserver></microArchitecture></designPattern></program></root>";
        Document doc = createDocument( xml );
        Dataset dataset = Dataset.of( DatasetLanguage.JAVA, DatasetType.OWN, "TestProject" );
        List<DatasetDesignPatterns> result = PatternReaderXml.instance()
                .read( Collections.singletonList( dataset ), doc );
        assertEquals( 1, result.size() );
        DatasetDesignPatterns dsp = result.get( 0 );
        assertFalse( dsp.getPatterns().isEmpty() );
    }

    @Test
    void testGetChildsReturnsCorrectElements() throws Exception {
        String xml = "<root><parent><child>A</child><child>B</child><other>C</other></parent></root>";
        Document doc = createDocument( xml );
        Element parent = ( Element ) doc.getElementsByTagName( "parent" ).item( 0 );
        List<Element> children = PatternReaderXml.instance().getChilds( parent, "child" );
        assertEquals( 2, children.size() );
    }

    @Test
    void testMapPatternTypeReturnsValueWhenExists() {
        Optional<DesignPatternType> result = PatternReaderXml.instance().mapPatternType( "Observer" );
        assertTrue( result.isPresent() );
        assertEquals( DesignPatternType.OBSERVER, result.get() );
    }

    @Test
    void testMapPatternTypeReturnsEmptyWhenNotFound() {
        Optional<DesignPatternType> result = PatternReaderXml.instance().mapPatternType( "NON_EXISTENT" );
        assertFalse( result.isPresent() );
    }
}
