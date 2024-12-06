package de.haw.processing.model;

import java.util.*;

public class CpgNodePaths {

    private final Map<String, List<CpgPath>> paths = new HashMap<>();

    public void add( final String nodeId, final CpgPath path ) {
        if ( !this.paths.containsKey( nodeId ) ) {
            this.paths.put( nodeId, new ArrayList<>() );
        }
        this.get( nodeId ).add( path );
    }

    public List<CpgPath> get( final String nodeId ) {
        if ( !this.paths.containsKey( nodeId ) ) {
            return new ArrayList<>();
        }
        return this.paths.get( nodeId );
    }

    public Set<String> getNodes() {
        return this.paths.keySet();
    }

    public List<CpgPath> getAll() {
        return this.getNodes().stream().map( this::get ).flatMap( List::stream ).toList();
    }

}
