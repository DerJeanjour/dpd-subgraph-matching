package de.haw.processing.module;

import de.haw.dataset.model.DatasetDesignPatterns;
import de.haw.dataset.model.DesignPatterType;
import de.haw.dataset.model.DesignPattern;
import de.haw.dataset.model.DesignPatternRole;
import de.haw.misc.pipe.PipeContext;
import de.haw.misc.pipe.PipeModule;
import de.haw.processing.GraphService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.util.Pair;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

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
        final Map<String, Integer> stats = new HashMap<>();
        final Map<String, Integer> groundTruthStats = this.getGroundTruthStats( dps );
        graph.nodes().forEach( node -> {

            if ( !graphService.hasLabel( node, "Declaration" ) ) {
                return;
            }

            final String className = this.getClassName( node );
            final Map<DesignPatterType, DesignPatternRole> classRoles = this.getRolesClass( className, dps, stats );
            for ( DesignPatterType dpType : classRoles.keySet() ) {
                final DesignPatternRole role = classRoles.get( dpType );
                node.setAttribute( "pattern." + dpType.name(), role.getTag() );
                graphService.addLabel( node, dpType.name() );
            }

        } );

        final Map<String, Pair<Integer, Integer>> resultStats = new TreeMap<>();
        groundTruthStats.keySet().forEach( key -> {
            final int groundTruthValue = groundTruthStats.get( key );
            final int actualValue = stats.getOrDefault( key, 0 );
            resultStats.put( key, Pair.create( groundTruthValue, actualValue ) );
        } );
        log.info( "Result stats of marking design patterns: {}", resultStats );

        return graph;
    }

    private String getClassName( final Node node ) {
        if ( StringUtils.isNotBlank( node.getAttribute( "scopedName", String.class ) ) ) {
            return node.getAttribute( "scopedName", String.class );
        }
        return null;
    }

    private Map<DesignPatterType, DesignPatternRole> getRolesClass(
            final String className, final DatasetDesignPatterns datasetDps, final Map<String, Integer> stats ) {
        final Map<DesignPatterType, DesignPatternRole> classRoles = new HashMap<>();
        if ( StringUtils.isBlank( className ) ) {
            return classRoles;
        }
        datasetDps.getPatterns().values().forEach( dps -> dps.forEach( dp -> {
            Optional<DesignPatternRole> classRole = dp.getRoles()
                    .stream()
                    .filter( role -> role.getLocation().equals( className ) )
                    .findAny();
            classRole.ifPresent( designPatternRole -> {
                classRoles.put( dp.getType(), classRole.get() );
                this.incrementStat( stats, this.formatStatKey( dp, classRole.get() ) );
                this.setStat( stats, this.formatStatKey( dp ), 1 );
            } );

        } ) );

        return classRoles;
    }

    private Map<String, Integer> getGroundTruthStats( final DatasetDesignPatterns datasetDps ) {
        final Map<String, Integer> groundTruth = new HashMap<>();
        datasetDps.getPatterns().values().forEach( dps -> dps.forEach( dp -> {
            this.setStat( groundTruth, this.formatStatKey( dp ), 1 );
            dp.getRoles().forEach( role -> this.incrementStat( groundTruth, this.formatStatKey( dp, role ), 1 ) );
        } ) );
        return groundTruth;

    }

    private void incrementStat( final Map<String, Integer> stats, final String key ) {
        this.incrementStat( stats, key, 1 );
    }

    private void incrementStat( final Map<String, Integer> stats, final String key, final int inc ) {
        if ( !stats.containsKey( key ) ) {
            this.setStat( stats, key, inc );
            return;
        }
        this.setStat( stats, key, stats.get( key ) + inc );
    }

    private void setStat( final Map<String, Integer> stats, final String key, final int value ) {
        stats.put( key, value );
    }

    private String formatStatKey( final DesignPattern pattern ) {
        return pattern.getType().name() + "_" + pattern.getId();
    }

    private String formatStatKey( final DesignPattern pattern, final DesignPatternRole patterRole ) {
        return this.formatStatKey( pattern ) + "_" + patterRole.getTag();
    }

}
