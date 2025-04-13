package de.haw.dataset.reader;

import de.haw.dataset.model.*;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PatternReaderCsvTest {

    @Test
    void testEmpty() {
        PatternReaderCsv reader = PatternReaderCsv.instance();
        List<DatasetDesignPatterns> patterns = reader.read( Collections.emptyList(), Collections.emptyList() );
        assertEquals( patterns.size(), 0 );
    }

    @Test
    void testValidPattern() {
        PatternReaderCsv reader = PatternReaderCsv.instance();
        CsvDesignPattern a = new CsvDesignPattern();
        a.setProjectName( DatasetFactory.J_UNIT.getProjectName() );
        a.setClassName( "x" );
        a.setPatternName( "Observer" );
        List<CsvDesignPattern> entries = Arrays.asList( a );
        Dataset dataset = DatasetFactory.J_UNIT;
        List<DatasetDesignPatterns> patterns = reader.read( Collections.singletonList( dataset ), entries );
        assertEquals( patterns.size(), 1 );
        assertTrue( patterns.get( 0 ).getPatterns().containsKey( DesignPatternType.OBSERVER ) );
    }

    @Test
    void testInvalidPattern() {
        PatternReaderCsv reader = PatternReaderCsv.instance();
        CsvDesignPattern a = new CsvDesignPattern();
        a.setProjectName( DatasetFactory.J_UNIT.getProjectName() );
        a.setClassName( "x" );
        a.setPatternName( "fsdgs" );
        List<CsvDesignPattern> entries = Arrays.asList( a );
        Dataset dataset = DatasetFactory.J_UNIT;
        List<DatasetDesignPatterns> patterns = reader.read( Collections.singletonList( dataset ), entries );
        assertEquals( patterns.size(), 1 );
        assertFalse( patterns.get( 0 ).getPatterns().containsKey( DesignPatternType.OBSERVER ) );
    }


}
