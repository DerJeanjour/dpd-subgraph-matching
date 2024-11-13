package de.haw.processing.module;

import de.haw.dataset.model.DatasetDesignPatterns;
import de.haw.dataset.model.DesignPatterType;
import de.haw.dataset.model.DesignPatternRole;
import de.haw.misc.pipe.PipeContext;
import de.haw.misc.pipe.PipeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.graphstream.graph.Graph;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor( staticName = "instance" )
public class CpgLabelPatternsModule<Target> extends PipeModule<Graph, Graph, Target> {

    @Override
    protected Graph processImpl( final Graph graph, final PipeContext ctx ) {

        if ( !ctx.get( PipeContext.CPG_DESIGN_PATTERNS_EXISTS, false, Boolean.class ) ) {
            log.info( "Can't mark non existing design pattern in cpg." );
            return graph;
        }

        final DatasetDesignPatterns dps = ctx.get( PipeContext.CPG_DESIGN_PATTERNS, null, DatasetDesignPatterns.class );
        graph.nodes().forEach( node -> {

            final String className = node.getAttribute( "scopedName", String.class ); // TODO
            final Map<DesignPatterType, DesignPatternRole> classRoles = this.getRolesClass( className, dps );

            for ( DesignPatterType dpType : classRoles.keySet() ) {
                final DesignPatternRole role = classRoles.get( dpType );
                node.setAttribute( dpType.name(), role.getTag() );
            }

        } );

        return graph;
    }

    private Map<DesignPatterType, DesignPatternRole> getRolesClass(
            final String className, final DatasetDesignPatterns dps ) {
        final Map<DesignPatterType, DesignPatternRole> classRoles = new HashMap<>();
        // TODO
        return classRoles;
    }

}
