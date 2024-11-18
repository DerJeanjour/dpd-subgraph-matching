package de.haw.processing.module;

import de.haw.dataset.model.DatasetDesignPatterns;
import de.haw.dataset.model.DesignPatterType;
import de.haw.dataset.model.DesignPatternRole;
import de.haw.misc.pipe.PipeContext;
import de.haw.misc.pipe.PipeModule;
import de.haw.processing.GraphService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor( staticName = "instance" )
public class MarkPatternsModule<Target> extends PipeModule<Graph, Graph, Target> {

    @Override
    protected Graph processImpl( final Graph graph, final PipeContext ctx ) {

        if ( !ctx.get( PipeContext.CPG_DESIGN_PATTERNS_EXISTS, false, Boolean.class ) ) {
            log.info( "Can't mark non existing design pattern in cpg." );
            return graph;
        }

        final GraphService graphService = GraphService.instance();
        final DatasetDesignPatterns dps = ctx.get( PipeContext.CPG_DESIGN_PATTERNS, null, DatasetDesignPatterns.class );
        graph.nodes().forEach( node -> {

            if( !graphService.hasLabel( node, "Declaration" ) ) {
                return;
            }

            final String className = this.getClassName( node );
            final Map<DesignPatterType, DesignPatternRole> classRoles = this.getRolesClass( className, dps );
            for ( DesignPatterType dpType : classRoles.keySet() ) {
                final DesignPatternRole role = classRoles.get( dpType );
                node.setAttribute( "pattern." + dpType.name(), role.getTag() );
                graphService.addLabel( node, dpType.name() );
            }

        } );

        return graph;
    }

    private String getClassName( final Node node ) {
        if( StringUtils.isNotBlank( node.getAttribute( "scopedName", String.class ) ) ) {
            return node.getAttribute( "scopedName", String.class );
        }
        return null;
    }

    private Map<DesignPatterType, DesignPatternRole> getRolesClass(
            final String className, final DatasetDesignPatterns datasetDps ) {
        final Map<DesignPatterType, DesignPatternRole> classRoles = new HashMap<>();
        if( StringUtils.isBlank( className ) ) {
            return classRoles;
        }
        datasetDps.getPatterns().values().forEach( dps -> {
            dps.forEach( dp -> {
                Optional<DesignPatternRole> classRole = dp.getRoles()
                        .stream()
                        .filter( role -> role.getLocation().equals( className ) )
                        .findAny();
                classRole.ifPresent( designPatternRole -> classRoles.put( dp.getType(), classRole.get() ) );
            } );
        } );

        return classRoles;
    }

}
