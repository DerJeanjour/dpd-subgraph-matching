package de.haw.dataset.model;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Data
@Slf4j
@RequiredArgsConstructor( staticName = "of" )
public class DatasetDesignPatterns {

    private final Dataset dataset;

    private final Map<DesignPatterType, List<DesignPattern>> patterns = new HashMap<>();

    public void add( final DesignPattern pattern ) {
        if ( !this.patterns.containsKey( pattern.getType() ) ) {
            this.patterns.put( pattern.getType(), new ArrayList<>() );
        }
        if ( this.getPattern( pattern.getType(), pattern.getClassName() ).isEmpty() ) {
            this.patterns.get( pattern.getType() ).add( pattern );
        }
    }

    public Optional<DesignPattern> getPattern( final DesignPatterType type, final String className ) {
        return this.getAllByType( type ).stream().filter( dp -> className.equals( dp.getClassName() ) ).findFirst();
    }

    public List<DesignPattern> getAllByType( final DesignPatterType type ) {
        if ( !this.patterns.containsKey( type ) ) {
            return new ArrayList<>();
        }
        return this.patterns.get( type );
    }

    public Map<String, Integer> getStats() {
        final Map<String, Integer> stats = new TreeMap<>();
        int patternCount = 0;
        for ( final DesignPatterType type : this.patterns.keySet() ) {
            final List<DesignPattern> patterns = this.getAllByType( type );
            stats.put( type + "_count", patterns.size() );
            patternCount += patterns.size();
        }
        stats.put( "pattern_count", patternCount );
        return stats;
    }

}
