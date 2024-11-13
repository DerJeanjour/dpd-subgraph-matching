package de.haw.dataset.model;

import de.haw.dataset.Dataset;
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
        if ( this.getPattern( pattern.getType(), pattern.getId() ).isEmpty() ) {
            this.patterns.get( pattern.getType() ).add( pattern );
        }
    }

    public Optional<DesignPattern> getPattern( final DesignPatterType type, final String id ) {
        return this.getAllByType( type ).stream().filter( dp -> id.equals( dp.getId() ) ).findFirst();
    }

    public List<DesignPattern> getAllByType( final DesignPatterType type ) {
        if ( !this.patterns.containsKey( type ) ) {
            return new ArrayList<>();
        }
        return this.patterns.get( type );
    }

    public Map<String, Integer> getStats() {
        final Map<String, Integer> stats = new TreeMap<>();

        int roleCount = 0;
        int patternCount = 0;
        for ( final DesignPatterType type : this.patterns.keySet() ) {

            final List<DesignPattern> patterns = this.getAllByType( type );
            stats.put( type + "_count", patterns.size() );

            int patternRoleCount = 0;
            for ( final DesignPattern pattern : patterns ) {
                patternCount++;

                for ( final DesignPatternRole role : pattern.getRoles() ) {
                    roleCount++;
                    patternRoleCount++;
                    final String roleTagStat = type + "_" + role.getTag() + "_count";
                    if ( stats.containsKey( roleTagStat ) ) {
                        stats.put( roleTagStat, stats.get( roleTagStat ) + 1 );
                        continue;
                    }
                    stats.put( roleTagStat, 1 );
                }
            }
            stats.put( type + "_role_count", patternRoleCount );
        }

        stats.put( "pattern_count", patternCount );
        stats.put( "role_count", roleCount );
        return stats;
    }

}
