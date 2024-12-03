package de.haw.processing.module;

import de.haw.misc.pipe.PipeContext;
import de.haw.misc.pipe.PipeModule;
import de.haw.processing.GraphService;
import de.haw.translation.CpgConst;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.graphstream.graph.Graph;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor( staticName = "instance" )
public class FilterInternalScopeModule<Target> extends PipeModule<Graph, Graph, Target> {

    private final GraphService GS = GraphService.instance();

    @Override
    protected Graph processImpl( final Graph graph, final PipeContext ctx ) {

        final Set<String> nodesToRemove = new HashSet<>();
        final Set<String> blackListedNames = new HashSet<>();

        graph.forEach( node -> {
            final boolean isInferred = this.GS.getAttr( node, CpgConst.NODE_ATTR_INFERRED, Boolean.class )
                    .orElse( false );
            if ( isInferred ) {
                nodesToRemove.add( node.getId() );
                final String name = this.GS.getAttr( node, CpgConst.NODE_ATTR_NAME, String.class ).orElse( "" );
                if ( StringUtils.isNotBlank( name ) ) {
                    blackListedNames.add( name );
                }
            }
        } );

        graph.forEach( node -> {
            final String name = this.GS.getAttr( node, CpgConst.NODE_ATTR_NAME, String.class ).orElse( "" );
            if ( StringUtils.isNotBlank( name ) && blackListedNames.contains( name ) ) {
                nodesToRemove.add( node.getId() );
            }
        } );

        nodesToRemove.forEach( graph::removeNode );
        return graph;
    }

}
